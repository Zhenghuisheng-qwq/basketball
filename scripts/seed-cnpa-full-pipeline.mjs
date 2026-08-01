/**
 * CNPA 一键编排（全部调用现成 HTTP API，不新增业务方法）
 *
 * 流程：
 *  1) POST /basket/member/init/accounts          —— 批量注册 + 提交球员认证
 *  2) Admin 认证审核通过                          —— /basket/playerCert/{id}/approve
 *  3) 前 6 人授予队长（绑定 2026 常规赛）         —— /basket/captainQualification/grant
 *  4) 6 队长创建公开球队                         —— /basket/team/create
 *  5) Admin 球队审核通过                          —— /basket/teamAudit/{id}/approve
 *  6) 其余队员申请加入 + 队长同意                 —— join-application + process
 *
 * 用法：
 *   node scripts/seed-cnpa-full-pipeline.mjs
 *   node scripts/seed-cnpa-full-pipeline.mjs --skip-init          # 不调 init，处理已有邮箱段
 *   node scripts/seed-cnpa-full-pipeline.mjs --email-start=111141 --count=30
 *   node scripts/seed-cnpa-full-pipeline.mjs --stop-after=create  # 停在建队后（仍会审队）
 *   node scripts/seed-cnpa-full-pipeline.mjs --stop-after=grant   # 授完队长即停（可手工建队）
 *
 * 依赖：admin :8080、web :8881 已启动；member.init.enabled=true
 */

const ADMIN = process.env.CNPA_ADMIN_URL || 'http://localhost:8080'
const WEB = process.env.CNPA_WEB_URL || 'http://localhost:8881'
const ADMIN_USER = process.env.CNPA_ADMIN_USER || 'admin'
const ADMIN_PASS = process.env.CNPA_ADMIN_PASS || 'admin123'
const PASSWORD = process.env.CNPA_MEMBER_PASSWORD || '111111aa'
const SEASON_ID = process.env.CNPA_SEASON_ID || '1'

const args = parseArgs(process.argv.slice(2))
const SKIP_INIT = Boolean(args['skip-init'])
const EMAIL_START = Number(args['email-start'] || 111111)
const COUNT = Number(args['count'] || 30)
const CAPTAIN_COUNT = Number(args['captain-count'] || 6)
const MEMBERS_PER_TEAM = Number(args['members-per-team'] || 4)
const STOP_AFTER = String(args['stop-after'] || 'join') // grant | create | join
const TEAM_PREFIX = String(args['team-prefix'] || `CNPA流水线${Date.now().toString().slice(-6)}`)

const CERT_APPROVED = 3
const CERT_SUBMITTED = 1
const CERT_REVIEWING = 2

function parseArgs(argv) {
  const out = {}
  for (const a of argv) {
    if (!a.startsWith('--')) continue
    const [k, v] = a.slice(2).split('=')
    out[k] = v === undefined ? true : v
  }
  return out
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

function log(...xs) {
  console.log(...xs)
}

async function req(url, { method = 'GET', token, body, allowFail = false } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let data
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    throw new Error(`${method} ${url} non-json: ${text.slice(0, 240)}`)
  }
  if (!allowFail && (!res.ok || (data && data.code !== undefined && data.code !== 200))) {
    throw new Error(`${method} ${url} failed: ${JSON.stringify(data)}`)
  }
  return data
}

async function adminLogin() {
  const data = await req(`${ADMIN}/login`, {
    method: 'POST',
    body: { username: ADMIN_USER, password: ADMIN_PASS },
  })
  return data.token
}

async function webLogin(email) {
  const data = await req(`${WEB}/basket/member/auth/login/password`, {
    method: 'POST',
    body: { email, password: PASSWORD },
  })
  return data.data.token
}

function msgIncludes(err, keywords) {
  const s = String(err && err.message ? err.message : err)
  return keywords.some((k) => s.includes(k))
}

async function stepInit() {
  if (SKIP_INIT) {
    log('[1] skip init')
    return []
  }
  try {
    const data = await req(`${WEB}/basket/member/init/accounts`, { method: 'POST' })
    const accounts = (data.data && data.data.accounts) || []
    log(`[1] init created ${data.data?.createdCount ?? accounts.length} accounts`)
    return accounts.map((a) => ({
      email: a.email,
      memberId: a.memberId != null ? String(a.memberId) : undefined,
      applicationId: a.applicationId != null ? String(a.applicationId) : undefined,
      applicationStatus: a.applicationStatus,
    }))
  } catch (e) {
    log(`[1] init failed/skip: ${e.message}`)
    return []
  }
}

function buildEmailRange() {
  const list = []
  for (let i = 0; i < COUNT; i++) {
    list.push({ email: `${EMAIL_START + i}@qq.com` })
  }
  return list
}

async function findPendingApplicationId(adminToken, email) {
  const qs = new URLSearchParams({
    pageNum: '1',
    pageSize: '20',
    email,
  })
  const data = await req(`${ADMIN}/basket/playerCert/list?${qs}`, { token: adminToken })
  const rows = data.rows || []
  const pending = rows.find((r) => r.applicationStatus === CERT_SUBMITTED || r.applicationStatus === CERT_REVIEWING)
  if (pending?.id != null) return String(pending.id)
  const any = rows[0]
  return any?.id != null ? String(any.id) : null
}

async function ensureCertApproved(adminToken, account) {
  const token = await webLogin(account.email)
  const current = await req(`${WEB}/basket/player/certification/current`, { token })
  const cert = current.data || {}
  if (cert.certificationStatus === CERT_APPROVED) {
    log(`[2] cert already approved ${account.email} playerId=${cert.playerId}`)
    return {
      email: account.email,
      playerId: String(cert.playerId),
      playerNo: cert.playerNo,
      token,
    }
  }

  let applicationId = account.applicationId || (cert.currentApplicationId != null ? String(cert.currentApplicationId) : null)
  if (!applicationId) {
    applicationId = await findPendingApplicationId(adminToken, account.email)
  }
  if (!applicationId) {
    throw new Error(`无待审认证申请: ${account.email}`)
  }

  try {
    await req(`${ADMIN}/basket/playerCert/${applicationId}/start-review`, {
      method: 'POST',
      token: adminToken,
      body: {},
      allowFail: true,
    })
  } catch {
    // ignore
  }

  await req(`${ADMIN}/basket/playerCert/${applicationId}/approve`, {
    method: 'POST',
    token: adminToken,
    body: { auditRemark: '流水线自动审核通过' },
  })
  log(`[2] approved cert ${account.email} app=${applicationId}`)

  const after = await req(`${WEB}/basket/player/certification/current`, { token })
  if (after.data?.certificationStatus !== CERT_APPROVED || !after.data?.playerId) {
    throw new Error(`认证通过后仍无 playerId: ${account.email}`)
  }
  return {
    email: account.email,
    playerId: String(after.data.playerId),
    playerNo: after.data.playerNo,
    token,
  }
}

async function leaveCurrentTeamIfAny(user) {
  const mine = await req(`${WEB}/basket/team/mine?currentPage=1&pageSize=20`, { token: user.token })
  const records = mine.data?.records || mine.data?.rows || []
  for (const t of records) {
    const teamId = t.teamId != null ? String(t.teamId) : null
    const role = t.myRoleType || t.roleType
    if (!teamId) continue
    if (role === 'CAPTAIN') {
      log(`[warn] ${user.email} 已是队长，跳过离队 teamId=${teamId}`)
      continue
    }
    try {
      await req(`${WEB}/basket/team/${teamId}/leave`, { method: 'POST', token: user.token })
      log(`[ok] ${user.email} left team ${teamId}`)
    } catch (e) {
      log(`[warn] leave ${user.email}: ${e.message}`)
    }
  }
}

async function grantCaptain(adminToken, user) {
  try {
    await req(`${ADMIN}/basket/captainQualification/grant`, {
      method: 'POST',
      token: adminToken,
      body: {
        seasonId: SEASON_ID,
        playerId: user.playerId,
        paymentConfirmed: 1,
        paymentReference: `PIPE-${user.email.split('@')[0]}`,
        grantRemark: '流水线自动授予队长（2026常规赛）',
      },
    })
    log(`[3] grant captain ${user.email} playerId=${user.playerId}`)
  } catch (e) {
    if (msgIncludes(e, ['已拥有', '队长授权'])) {
      log(`[3] grant skip ${user.email}`)
    } else {
      throw e
    }
  }
}

async function createOrReuseTeam(user, teamName) {
  const token = await webLogin(user.email)
  user.token = token

  const mine = await req(`${WEB}/basket/team/mine?currentPage=1&pageSize=20`, { token })
  const records = mine.data?.records || mine.data?.rows || []
  const existing = records.find((t) => (t.myRoleType || t.roleType) === 'CAPTAIN')
  if (existing?.teamId) {
    log(`[4] reuse team ${existing.teamName || teamName} id=${existing.teamId} captain=${user.email}`)
    return { teamId: String(existing.teamId), teamName: existing.teamName || teamName, captain: user }
  }

  const created = await req(`${WEB}/basket/team/create`, {
    method: 'POST',
    token,
    body: {
      teamName,
      shortName: teamName.slice(-6),
      description: `${teamName} - 流水线种子球队`,
      publicStatus: 1,
      regionName: '全国',
    },
  })
  const teamId = String(created.data.teamId)
  log(`[4] create team ${teamName} id=${teamId} by ${user.email}`)
  return { teamId, teamName, captain: user }
}

async function approveTeam(adminToken, team) {
  await req(`${ADMIN}/basket/teamAudit/${team.teamId}/start-review`, {
    method: 'POST',
    token: adminToken,
    body: {},
    allowFail: true,
  })
  try {
    await req(`${ADMIN}/basket/teamAudit/${team.teamId}/approve`, {
      method: 'POST',
      token: adminToken,
      body: { auditRemark: '流水线自动审队通过' },
    })
    log(`[5] approve team ${team.teamName}`)
  } catch (e) {
    if (msgIncludes(e, ['已通过', '审核通过失败', '状态'])) {
      log(`[5] approve skip ${team.teamName}: ${e.message}`)
    } else {
      throw e
    }
  }
}

async function joinAndApprove(member, team) {
  const memberToken = await webLogin(member.email)
  let applicationId
  try {
    const appRes = await req(`${WEB}/basket/team/${team.teamId}/join-application`, {
      method: 'POST',
      token: memberToken,
      body: { applyReason: `申请加入${team.teamName}` },
    })
    applicationId = String(appRes.data.applicationId || appRes.data.id)
    log(`[6] apply ${member.email} -> ${team.teamName} app=${applicationId}`)
  } catch (e) {
    if (msgIncludes(e, ['已加入', '待处理', '重复'])) {
      log(`[6] apply skip ${member.email}: already in flow`)
      return
    }
    throw e
  }

  const captainToken = await webLogin(team.captain.email)
  await req(`${WEB}/basket/team/join-application/${applicationId}/process`, {
    method: 'PUT',
    token: captainToken,
    body: { approve: true, processRemark: '流水线自动同意入队' },
  })
  log(`[6] approve join ${member.email} into ${team.teamName}`)
}

async function main() {
  log('=== CNPA full pipeline start ===')
  log(`ADMIN=${ADMIN} WEB=${WEB} seasonId=${SEASON_ID} stopAfter=${STOP_AFTER}`)

  const adminToken = await adminLogin()
  log('[ok] admin login')

  let accounts = await stepInit()
  if (!accounts.length) {
    accounts = buildEmailRange()
    log(`[1] fallback email range ${accounts[0].email} .. ${accounts[accounts.length - 1].email}`)
  }

  if (accounts.length < CAPTAIN_COUNT) {
    throw new Error(`账号数 ${accounts.length} 少于队长数 ${CAPTAIN_COUNT}`)
  }

  const users = []
  for (const acc of accounts) {
    const u = await ensureCertApproved(adminToken, acc)
    users.push(u)
    await sleep(50)
  }

  const captains = users.slice(0, CAPTAIN_COUNT)
  const members = users.slice(CAPTAIN_COUNT)

  for (const c of captains) {
    await leaveCurrentTeamIfAny(c)
  }

  for (const c of captains) {
    await grantCaptain(adminToken, c)
    await sleep(80)
  }

  if (STOP_AFTER === 'grant') {
    log('\n=== STOP after grant：队长已就绪，可手工建队 ===')
    captains.forEach((c) => log(`- ${c.email} playerId=${c.playerId}`))
    return
  }

  const teams = []
  for (let i = 0; i < captains.length; i++) {
    const name = `${TEAM_PREFIX}-${String.fromCharCode(65 + i)}`
    const team = await createOrReuseTeam(captains[i], name)
    teams.push(team)
    await sleep(80)
  }

  for (const t of teams) {
    await approveTeam(adminToken, t)
    await sleep(80)
  }

  if (STOP_AFTER === 'create') {
    log('\n=== STOP after create：球队已创建并审核 ===')
    teams.forEach((t) => log(`- ${t.teamName} id=${t.teamId} captain=${t.captain.email}`))
    return
  }

  const expectedMembers = CAPTAIN_COUNT * MEMBERS_PER_TEAM
  if (members.length < expectedMembers) {
    log(`[warn] 队员仅 ${members.length}，期望 ${expectedMembers}，将按现有人数分配`)
  }

  for (let i = 0; i < members.length; i++) {
    const team = teams[Math.floor(i / MEMBERS_PER_TEAM)]
    if (!team) break
    await joinAndApprove(members[i], team)
    await sleep(60)
  }

  log('\n=== DONE ===')
  for (const t of teams) {
    log(`- ${t.teamName}: captain=${t.captain.email}, teamId=${t.teamId}`)
  }
  log('通知由各现成接口自动产生：CAPTAIN_GRANT / TEAM_AUDIT / TEAM_JOIN / TEAM_MEMBER')
  log('提示：每队应为 1 队长 + 4 队员 = 5 人；若 Admin 成员数仍显示 1，请刷新球队审核列表（会自动校准）')
}

main().catch((e) => {
  console.error('\n[FAILED]', e.message)
  process.exit(1)
})
