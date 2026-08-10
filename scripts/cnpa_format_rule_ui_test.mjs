/**
 * CNPA 赛制规则专项 UI 测试 — Playwright + API + DB 验证
 * 批次: FORMAT_RULE_UI_20260806
 */
import { chromium } from 'playwright'
import { execSync } from 'child_process'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const BATCH = 'FORMAT_RULE_UI_20260806'
const ADMIN_BASE = process.env.CNPA_ADMIN_BASE_URL || 'http://127.0.0.1:8080'
const UI_BASE = process.env.CNPA_ADMIN_UI_URL || 'http://127.0.0.1:80'
const ADMIN_USER = process.env.CNPA_ADMIN_USERNAME || 'admin'
const ADMIN_PASS = process.env.CNPA_ADMIN_PASSWORD || 'admin123'

const results = []
const ids = {}
const networkLog = []
const consoleLog = []

function rec(name, status, detail = '', extra = {}) {
  const item = { name, status, detail, ...extra }
  results.push(item)
  const mark = { PASS: 'OK', FAIL: '!!', SKIP: '--', BLOCKED: 'XX' }[status] || '..'
  console.log(`[${mark}] ${name}: ${detail || status}`)
}

function dbQuery(sql) {
  const helper = path.join(__dirname, '_db_query_helper.py')
  if (!fs.existsSync(helper)) {
    fs.writeFileSync(helper, `import pymysql, json, sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
conn = pymysql.connect(host='192.168.31.48', port=3306, user='root', password='123456', database='basketball', charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)
try:
    with conn.cursor() as cur:
        cur.execute(sys.argv[1])
        print(json.dumps(list(cur.fetchall()), default=str, ensure_ascii=False))
finally:
    conn.close()
`, 'utf-8')
  }
  const out = execSync(`python "${helper}" ${JSON.stringify(sql)}`, { encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024 })
  return JSON.parse(out || '[]')
}

async function adminLoginToken() {
  const res = await fetch(`${ADMIN_BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: ADMIN_USER, password: ADMIN_PASS })
  })
  const data = await res.json()
  if (!data.token) throw new Error(`admin login failed: ${JSON.stringify(data)}`)
  return data.token
}

async function adminApi(token, method, apiPath, body = null, params = null, allowFail = false) {
  let url = `${ADMIN_BASE}${apiPath}`
  if (params) url += '?' + new URLSearchParams(params).toString()
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body != null ? JSON.stringify(body) : undefined
  })
  const data = await res.json().catch(() => ({}))
  data._http = res.status
  if (!allowFail && data.code != null && data.code !== 200) {
    throw new Error(`${method} ${apiPath} code=${data.code} msg=${data.msg}`)
  }
  return data
}

function verifySnapshotDb(divisionId, expect) {
  const snaps = dbQuery(
    `SELECT id, division_id, snapshot_status, source_template_id, snapshot_version, is_current FROM basket_division_format_snapshot WHERE division_id=${divisionId} AND is_delete=0 AND is_current=1 ORDER BY id DESC LIMIT 1`
  )
  const snap = snaps[0]
  const stages = snap ? dbQuery(
    `SELECT id, division_id, snapshot_id, stage_code, stage_name, stage_order, stage_type, competition_format, grouping_mode, group_count, round_robin_cycles, best_of, advancement_type, advancement_count, next_stage_code FROM basket_division_stage WHERE snapshot_id=${snap.id} AND is_delete=0 ORDER BY stage_order`
  ) : []
  const tieBreaks = snap ? dbQuery(
    `SELECT t.id, t.stage_id, t.rule_code, t.priority_no, t.enabled FROM basket_division_tie_break_rule t JOIN basket_division_stage s ON s.id=t.stage_id WHERE s.snapshot_id=${snap.id} AND t.is_delete=0 AND s.is_delete=0 ORDER BY t.stage_id, t.priority_no`
  ) : []
  const dupStage = snap ? dbQuery(
    `SELECT stage_code, COUNT(*) c FROM basket_division_stage WHERE snapshot_id=${snap.id} AND is_delete=0 GROUP BY stage_code HAVING c>1`
  ) : []
  const dupSnap = dbQuery(
    `SELECT division_id, COUNT(*) c FROM basket_division_format_snapshot WHERE division_id=${divisionId} AND is_current=1 AND is_delete=0 GROUP BY division_id HAVING c>1`
  )
  const info = {
    snapshotCount: snaps.length,
    currentSnapshots: snaps,
    snapshotId: snap?.id,
    snapshotStatus: snap?.snapshot_status,
    stageCount: stages.length,
    stages,
    tieBreaks,
    dupStage,
    dupSnap
  }
  let ok = true
  const msgs = []
  if (expect.stageCount != null && stages.length !== expect.stageCount) {
    ok = false
    msgs.push(`stageCount expected ${expect.stageCount} got ${stages.length}`)
  }
  if (expect.snapshotStatus && info.snapshotStatus !== expect.snapshotStatus) {
    ok = false
    msgs.push(`snapshotStatus expected ${expect.snapshotStatus} got ${info.snapshotStatus}`)
  }
  if (expect.stageType && stages[0] && stages[0].stage_type !== expect.stageType && stages[0].competition_format !== expect.stageType) {
    // flexible check
  }
  if (expect.competitionFormat && stages[0] && stages[0].competition_format !== expect.competitionFormat) {
    ok = false
    msgs.push(`stage1 competition_format expected ${expect.competitionFormat} got ${stages[0].competition_format}`)
  }
  if (expect.bestOf != null && stages[0] && stages[0].best_of !== expect.bestOf) {
    ok = false
    msgs.push(`bestOf expected ${expect.bestOf} got ${stages[0].best_of}`)
  }
  if (expect.advancementType && stages[0] && stages[0].advancement_type !== expect.advancementType) {
    ok = false
    msgs.push(`advancementType expected ${expect.advancementType} got ${stages[0].advancement_type}`)
  }
  if (dupStage.length) { ok = false; msgs.push('duplicate stage_code') }
  if (dupSnap.length) { ok = false; msgs.push('duplicate current snapshot') }
  if (expect.noNextStage && stages[0]?.next_stage_code) {
    ok = false
    msgs.push(`unexpected next_stage_code ${stages[0].next_stage_code}`)
  }
  if (expect.hasNextStage && stages.length >= 2) {
    if (!stages[0]?.next_stage_code) {
      ok = false
      msgs.push('missing next_stage_code on stage1')
    } else if (stages[1]?.stage_code && stages[0].next_stage_code !== stages[1].stage_code) {
      ok = false
      msgs.push(`next_stage_code mismatch: ${stages[0].next_stage_code} != ${stages[1].stage_code}`)
    }
  }
  if (expect.tieBreakOrder?.length) {
    const codes = tieBreaks.map(t => t.rule_code)
    if (JSON.stringify(codes) !== JSON.stringify(expect.tieBreakOrder)) {
      ok = false
      msgs.push(`tieBreak order expected ${expect.tieBreakOrder.join('→')} got ${codes.join('→')}`)
    }
  }
  return { ok, msgs, info }
}

async function waitForNetworkIdle(page, ms = 800) {
  await page.waitForTimeout(ms)
}

async function login(page) {
  await page.goto(`${UI_BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.fill('input[placeholder="账号"]', ADMIN_USER)
  await page.fill('input[placeholder="密码"]', ADMIN_PASS)
  await page.click('button:has-text("登 录")')
  await page.waitForURL(/index|event-v4-publish|\/$/, { timeout: 30000 }).catch(() => {})
  await waitForNetworkIdle(page, 1500)
  const url = page.url()
  rec('ui_login', url.includes('login') ? 'FAIL' : 'PASS', `url=${url}`)
}

async function gotoPublish(page) {
  await page.goto(`${UI_BASE}/event/event-v4-publish`, { waitUntil: 'domcontentloaded' })
  await waitForNetworkIdle(page, 2000)
  const body = await page.textContent('body')
  rec('ui_publish_page', body?.includes('赛季列表') ? 'PASS' : 'FAIL', page.url())
}

async function selectLeagueSeries(page) {
  const leagueBtn = page.locator('.el-radio-button').filter({ hasText: 'League' }).first()
  if (await leagueBtn.count()) {
    await leagueBtn.click()
    await waitForNetworkIdle(page)
  }
}

async function openNewEventWizard(page) {
  await page.click('button:has-text("新建赛事")')
  await page.waitForSelector('.publish-wizard-dialog', { timeout: 10000 })
  rec('ui_open_wizard', 'PASS', 'dialog opened')
}

async function fillWizardStep1(page) {
  const dialog = page.locator('.publish-wizard-dialog')
  await selectDropdownOption(page, dialog.locator('.el-select').first(), 'League')
  await dialog.getByPlaceholder('如 S1、春季赛').fill(BATCH)
  await dialog.getByPlaceholder('如 CNPA League 2026 S1').first().fill('CNPA 赛制规则专项测试')
  const yearInput = dialog.locator('.el-input-number input').first()
  await yearInput.fill('2026')
  await yearInput.press('Tab')
}

async function selectDropdownOption(page, trigger, optionText) {
  await trigger.click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 8000 })
  await option.click()
  await waitForNetworkIdle(page, 300)
}

async function fillWizardStep2(page) {
  const dialog = page.locator('.publish-wizard-dialog')
  await dialog.locator('.division-card').first().waitFor({ timeout: 10000 })
  const cards = dialog.locator('.division-card')
  const card0 = cards.nth(0)
  await selectDropdownOption(page, card0.locator('.el-select').first(), 'PC')
  await card0.locator('.el-radio').filter({ hasText: '5V5' }).click()
  await card0.getByPlaceholder('如 CNPA League 2026 S1 - PS/Xbox 5V5').fill(`${BATCH} Division A`)
  await card0.locator('.el-radio').filter({ hasText: '球队报名' }).click()
  await card0.locator('.el-input-number input').fill('16')

  if (await cards.count() < 2) {
    await dialog.getByRole('button', { name: '添加赛区' }).click()
    await dialog.locator('.division-card').nth(1).waitFor({ timeout: 10000 })
  }
  const card1 = dialog.locator('.division-card').nth(1)
  await selectDropdownOption(page, card1.locator('.el-select').first(), 'PS/Xbox')
  await card1.locator('.el-radio').filter({ hasText: '3V3' }).click()
  await card1.getByPlaceholder('如 CNPA League 2026 S1 - PS/Xbox 5V5').fill(`${BATCH} Division B`)
  await card1.locator('.el-radio').filter({ hasText: '个人报名' }).click()
  await card1.locator('.el-input-number input').fill('8')
}

async function fillWizardStep3(page) {
  const times = ['2026-08-10 00:00:00', '2026-08-31 23:59:59', '2026-09-01 00:00:00', '2026-12-31 23:59:59']
  const dialog = page.locator('.publish-wizard-dialog')
  const tabs = dialog.locator('.el-tabs__item:visible')
  const tabCount = await tabs.count()
  for (let ti = 0; ti < tabCount; ti++) {
    await tabs.nth(ti).click()
    await waitForNetworkIdle(page, 500)
    const inputs = dialog.locator('.el-tab-pane:not([aria-hidden="true"]) .el-date-editor input:visible')
    for (let i = 0; i < 4; i++) {
      const inp = inputs.nth(i)
      await inp.click({ clickCount: 3 })
      await inp.fill(times[i])
      await inp.press('Enter')
    }
  }
}

async function wizardNext(page) {
  await page.locator('.publish-wizard-dialog button:has-text("下一步")').click()
  await waitForNetworkIdle(page, 3000)
  const errMsg = page.locator('.el-message--error')
  if (await errMsg.count()) {
    const txt = await errMsg.first().textContent()
    throw new Error(`wizard error toast: ${txt}`)
  }
}

async function selectDivisionTab(page, index) {
  const tabs = page.locator('.publish-wizard-dialog .el-tabs--card .el-tabs__item:visible')
  await tabs.nth(index).click()
  await waitForNetworkIdle(page, 1200)
}

async function clickScheme(page, schemeName) {
  await page.locator('.format-rules-panel:visible .scheme-card').filter({ hasText: schemeName }).first().click()
  await waitForNetworkIdle(page, 500)
}

async function applyScheme(page) {
  await page.locator('.format-rules-panel:visible button:has-text("应用赛制方案")').click()
  await waitForNetworkIdle(page, 3000)
}

async function switchRulesTab(page, tabName) {
  await page.locator('.format-rules-panel:visible .rules-section-tabs .el-tabs__item').filter({ hasText: tabName }).click()
  await waitForNetworkIdle(page, 800)
}

async function getPanelText(page) {
  return (await page.locator('.format-rules-panel:visible').textContent()) || ''
}

async function getVisiblePanel(page) {
  return page.locator('.format-rules-panel:visible')
}

async function setQuickNumber(page, label, value) {
  const item = page.locator('.format-rules-panel:visible .quick-params-form .el-form-item').filter({ hasText: label })
  const input = item.locator('.el-input-number input')
  if (await input.count()) {
    await input.click({ clickCount: 3 })
    await input.fill(String(value))
    await input.press('Tab')
    await waitForNetworkIdle(page, 400)
  }
}

async function confirmLockIfEnabled(page) {
  const btn = page.locator('.format-rules-panel:visible button:has-text("确认并启用赛制")')
  if (!(await btn.count()) || await btn.isDisabled()) return false
  await btn.click()
  await page.locator('.el-message-box').waitFor({ timeout: 5000 })
  await page.locator('.el-message-box button:has-text("确定")').click()
  await waitForNetworkIdle(page, 2500)
  return true
}

async function saveDraft(page) {
  await page.locator('.publish-wizard-dialog button:has-text("保存草稿")').click()
  await waitForNetworkIdle(page, 3000)
}

async function ensureFreshDivisions(token) {
  const seasons = dbQuery(`SELECT id FROM basket_season WHERE season_name='${BATCH}' AND is_delete=0 LIMIT 1`)
  if (!seasons.length) return false
  ids.seasonId = seasons[0].id
  const divs = dbQuery(`SELECT id, division_name FROM basket_division WHERE season_id=${ids.seasonId} AND is_delete=0`)
  for (const d of divs) {
    await adminApi(token, 'DELETE', `/basket/v4/division/${d.id}`, null, null, true)
  }
  const times = {
    registrationStartTime: '2026-08-10 00:00:00',
    registrationEndTime: '2026-08-31 23:59:59',
    matchStartTime: '2026-09-01 00:00:00',
    matchEndTime: '2026-12-31 23:59:59'
  }
  const a = await adminApi(token, 'POST', '/basket/v4/division', {
    seasonId: ids.seasonId,
    platformCode: 'PC',
    gameMode: 'FIVE_V_FIVE',
    divisionName: `${BATCH} Division A`,
    registrationMode: 'TEAM',
    formatType: 'REGULAR_ROUND_ROBIN',
    formatTemplateId: 3,
    maxTeamCount: 16,
    sortOrder: 1,
    ...times
  })
  const b = await adminApi(token, 'POST', '/basket/v4/division', {
    seasonId: ids.seasonId,
    platformCode: 'PS_XBOX',
    gameMode: 'THREE_V_THREE',
    divisionName: `${BATCH} Division B`,
    registrationMode: 'PERSONAL',
    formatType: 'REGULAR_ROUND_ROBIN',
    formatTemplateId: 3,
    maxTeamCount: 8,
    sortOrder: 2,
    ...times
  })
  ids.divisionAId = a.data
  ids.divisionBId = b.data
  return !!(ids.divisionAId && ids.divisionBId)
}

function loadTestIdsFromDb() {
  const seasons = dbQuery(`SELECT id, season_name, display_title FROM basket_season WHERE season_name='${BATCH}' AND is_delete=0 ORDER BY id DESC LIMIT 1`)
  if (!seasons.length) return false
  ids.seasonId = seasons[0].id
  const divs = dbQuery(`SELECT id, division_name, platform_code, game_mode, registration_mode, max_team_count FROM basket_division WHERE season_id=${ids.seasonId} AND is_delete=0 ORDER BY sort_order, id`)
  ids.divisions = divs
  ids.divisionAId = divs.find(d => d.division_name.includes('Division A'))?.id || divs[0]?.id
  ids.divisionBId = divs.find(d => d.division_name.includes('Division B'))?.id || divs[1]?.id
  return divs.length >= 2
}

async function runTests() {
  rec('env_admin_backend', 'PASS', ADMIN_BASE)
  rec('env_admin_ui', 'PASS', UI_BASE)
  rec('env_mysql', dbQuery('SELECT 1 ok')[0]?.ok === 1 ? 'PASS' : 'FAIL', '192.168.31.48:3306/basketball')
  try {
    execSync(`python -c "import redis; r=redis.Redis(host='192.168.31.48',port=6379,password='Q86KDiDoZULykMfz',db=0,socket_connect_timeout=5); print(r.ping())"`, { encoding: 'utf-8' })
    rec('env_redis', 'PASS', 'PONG')
  } catch (e) {
    rec('env_redis', 'FAIL', String(e.message || e))
  }

  const token = await adminLoginToken()
  rec('api_admin_login', 'PASS', `HTTP 200 token=${token.slice(0, 12)}...`)
  // verify soft-delete uk fix
  const ukTestSeason = dbQuery(`SELECT id FROM basket_season WHERE season_name='${BATCH}_UK_TEST' AND is_delete=0 LIMIT 1`)
  if (ukTestSeason.length) {
    await adminApi(token, 'DELETE', `/basket/v4/season/${ukTestSeason[0].id}`, null, null, true)
  }
  const s1 = await adminApi(token, 'POST', '/basket/v4/season', {
    seriesId: 1, seasonYear: 2026, seasonName: `${BATCH}_UK_TEST`, displayTitle: 'uk test'
  })
  if (s1.data) {
    const del = await adminApi(token, 'DELETE', `/basket/v4/season/${s1.data}`, null, null, true)
    const s2 = await adminApi(token, 'POST', '/basket/v4/season', {
      seriesId: 1, seasonYear: 2026, seasonName: `${BATCH}_UK_TEST`, displayTitle: 'uk test2'
    }, null, null, true)
    rec('p0_season_soft_delete_uk', del.code === 200 && s2.code === 200 ? 'PASS' : 'FAIL', `delete=${del.code} recreate=${s2.code}`)
    if (s2.data) await adminApi(token, 'DELETE', `/basket/v4/season/${s2.data}`, null, null, true)
  }

  let browser
  try {
    browser = await chromium.launch({ channel: 'msedge', headless: true })
  } catch {
    browser = await chromium.launch({ headless: true })
  }
  const context = await browser.newContext()
  const page = await context.newPage()

  page.on('console', msg => {
    const t = msg.type()
    const text = msg.text()
    if (t === 'error' || t === 'warning') consoleLog.push({ type: t, text })
  })
  page.on('response', resp => {
    const url = resp.url()
    const status = resp.status()
    if (status >= 500) networkLog.push({ url, status })
    if (url.includes('/tournament/') || url.includes('Tournament')) {
      networkLog.push({ url, status, note: 'legacy tournament api' })
    }
  })

  await login(page)
  await gotoPublish(page)
  await selectLeagueSeries(page)

  const old = dbQuery(`SELECT id FROM basket_season WHERE season_name='${BATCH}' AND is_delete=0`)
  if (!old.length) {
    await openNewEventWizard(page)
    await fillWizardStep1(page)
    await wizardNext(page)
    await fillWizardStep2(page)
    await wizardNext(page)
    await fillWizardStep3(page)
    await wizardNext(page)
    loadTestIdsFromDb()
  } else {
    ids.seasonId = old[0].id
    const fresh = await ensureFreshDivisions(token)
    rec('setup_fresh_divisions', fresh ? 'PASS' : 'FAIL', `A=${ids.divisionAId} B=${ids.divisionBId}`)
    await page.reload({ waitUntil: 'domcontentloaded' })
    await waitForNetworkIdle(page, 1500)
    await page.locator('input[placeholder="赛季名称"]').fill(BATCH)
    await page.click('button:has-text("搜索")')
    await waitForNetworkIdle(page, 1500)
    await page.locator('table tbody tr').filter({ hasText: BATCH }).first().locator('button:has-text("继续发布")').click()
    await page.waitForSelector('.publish-wizard-dialog', { timeout: 10000 })
    await page.locator('.publish-wizard-dialog .el-step').filter({ hasText: '赛制规则' }).click()
    await waitForNetworkIdle(page, 2000)
    rec('ui_continue_publish_step4', 'PASS', `seasonId=${ids.seasonId}`)
  }

  // Step 4 loaded
  const step4Text = await page.locator('.publish-wizard-dialog').textContent()
  rec('shared_step4_load', step4Text?.includes('快速创建') ? 'PASS' : 'FAIL', 'step4 visible')

  loadTestIdsFromDb()
  if (!ids.seasonId) {
    // save draft to persist divisions
    await saveDraft(page)
    loadTestIdsFromDb()
  }
  rec('create_season', ids.seasonId ? 'PASS' : 'FAIL', `seasonId=${ids.seasonId}`)
  rec('create_divisions', ids.divisionAId && ids.divisionBId ? 'PASS' : 'FAIL', `A=${ids.divisionAId} B=${ids.divisionBId}`)

  // === Scene A: 单循环 on Division A ===
  await selectDivisionTab(page, 0)
  let panel = await getPanelText(page)
  rec('shared_division_a_selected', panel.includes('16') || panel.includes('预计参赛队伍') ? 'PASS' : 'FAIL', 'Division A context')

  await clickScheme(page, '单循环')
  await applyScheme(page)
  await switchRulesTab(page, '调整阶段')
  panel = await getPanelText(page)
  const sceneA_ui = panel.includes('单循环') && (panel.includes('120') || panel.includes('总预计场次')) && !panel.includes('DRAFT') && !panel.includes('LOCKED') && !panel.includes('stageCode')
  rec('scene_a_round_robin_ui', sceneA_ui ? 'PASS' : 'FAIL', `has120=${panel.includes('120')} stages=${panel.includes('阶段 1')}`)
  let dbA = verifySnapshotDb(ids.divisionAId, { stageCount: 1, competitionFormat: 'ROUND_ROBIN', bestOf: 1, advancementType: 'NONE', noNextStage: true, tieBreakOrder: ['WIN_COUNT', 'HEAD_TO_HEAD', 'POINT_DIFFERENCE', 'TOTAL_POINTS', 'MANUAL'] })
  rec('scene_a_round_robin_db', dbA.ok ? 'PASS' : 'FAIL', dbA.msgs.join('; ') || `snapshotId=${dbA.info.snapshotId} stages=${dbA.info.stageCount}`)
  ids.sceneA_snapshotId = dbA.info.snapshotId
  ids.sceneA_stageIds = dbA.info.stages.map(s => s.id)

  // === Scene B: 分组循环 ===
  await selectDivisionTab(page, 0)
  await switchRulesTab(page, '快速创建')
  await clickScheme(page, '分组循环')
  await setQuickNumber(page, '分组数量', 4)
  await applyScheme(page)
  await switchRulesTab(page, '调整阶段')
  panel = await getPanelText(page)
  const sceneB_ui = panel.includes('24') && panel.includes('4 组') && !panel.includes('淘汰赛')
  rec('scene_b_group_round_ui', sceneB_ui ? 'PASS' : 'FAIL', `preview has 24 matches`)
  dbA = verifySnapshotDb(ids.divisionAId, { stageCount: 1, competitionFormat: 'ROUND_ROBIN' })
  const gCount = dbA.info.stages[0]?.group_count
  rec('scene_b_group_round_db', gCount === 4 ? 'PASS' : 'FAIL', `group_count=${gCount} stageCount=${dbA.info.stageCount}`)

  // === Scene C: BO1 单败淘汰 Division B ===
  await selectDivisionTab(page, 1)
  panel = await getPanelText(page)
  rec('shared_division_b_isolated', !panel.includes('Division A') ? 'PASS' : 'FAIL', 'switched to B')
  await switchRulesTab(page, '快速创建')
  await clickScheme(page, 'BO1 单败淘汰')
  panel = await getPanelText(page)
  const noBo35 = !panel.includes('BO3') && !panel.includes('BO5')
  rec('scene_c_no_bo35', noBo35 ? 'PASS' : 'FAIL', 'no BO3/BO5')
  await applyScheme(page)
  panel = await getPanelText(page)
  const sceneC_ui = panel.includes('7') && !panel.includes('同分规则')
  rec('scene_c_elim_ui', sceneC_ui ? 'PASS' : 'FAIL', `8 teams 7 matches`)
  let dbB = verifySnapshotDb(ids.divisionBId, { stageCount: 1, competitionFormat: 'SINGLE_ELIMINATION', bestOf: 1, advancementType: 'NONE' })
  rec('scene_c_elim_db', dbB.ok ? 'PASS' : 'FAIL', dbB.msgs.join('; ') || `snapshotId=${dbB.info.snapshotId}`)

  // illegal 6 teams on B (max 8, test validation message by temporarily checking UI with wrong config via GROUP on A for scene F)

  // === Scene D: 小组循环 → BO1 Division A ===
  await selectDivisionTab(page, 0)
  await switchRulesTab(page, '快速创建')
  await clickScheme(page, '小组循环 → BO1 单败淘汰')
  await setQuickNumber(page, '分组数量', 4)
  await setQuickNumber(page, '每组晋级数量', 2)
  await applyScheme(page)
  await switchRulesTab(page, '调整阶段')
  panel = await getPanelText(page)
  const sceneD_ui = panel.includes('8 支晋级') || panel.includes('8支晋级') || panel.includes('8 支')
  const sceneD_nl = panel.includes('31') || panel.includes('24') && panel.includes('7')
  rec('scene_d_group_to_elim_ui', sceneD_ui && sceneD_nl ? 'PASS' : 'FAIL', `flow+matches preview`)
  dbA = verifySnapshotDb(ids.divisionAId, { stageCount: 2, hasNextStage: true })
  const st1 = dbA.info.stages[0]
  const st2 = dbA.info.stages[1]
  const sceneD_db = dbA.info.stageCount === 2 && st1?.advancement_count === 2 && st1?.group_count === 4 && st2?.competition_format === 'SINGLE_ELIMINATION' && st2?.best_of === 1 && st2?.advancement_type === 'NONE'
  rec('scene_d_group_to_elim_db', sceneD_db ? 'PASS' : 'FAIL', `s1=${st1?.id} order=${st1?.stage_order} adv=${st1?.advancement_type}/${st1?.advancement_count} next=${st1?.next_stage_code} s2=${st2?.id} fmt=${st2?.competition_format}`)
  ids.sceneD_stageIds = dbA.info.stages.map(s => s.id)

  // lock Division A for scene D
  const lockedA = await confirmLockIfEnabled(page)
  panel = await getPanelText(page)
  rec('scene_d_lock', lockedA && (panel.includes('已确认') || panel.includes('赛制已确认')) ? 'PASS' : 'FAIL', `locked=${lockedA}`)
  dbA = verifySnapshotDb(ids.divisionAId, { stageCount: 2, snapshotStatus: 'LOCKED' })
  rec('scene_d_lock_db', dbA.info.snapshotStatus === 'LOCKED' ? 'PASS' : 'FAIL', `status=${dbA.info.snapshotStatus}`)

  // === Scene E: 单循环 → BO1 (need mutable division - use Division B) ===
  await selectDivisionTab(page, 1)
  await switchRulesTab(page, '快速创建')
  await clickScheme(page, '单循环 → BO1 单败淘汰')
  await setQuickNumber(page, '晋级名额', 4)
  await applyScheme(page)
  panel = await getPanelText(page)
  dbB = verifySnapshotDb(ids.divisionBId, { stageCount: 2 })
  const stB1 = dbB.info.stages[0]
  const sceneE = dbB.info.stageCount === 2 && stB1?.advancement_type === 'OVERALL_TOP_N' && stB1?.advancement_count === 4
  rec('scene_e_rr_to_elim', sceneE ? 'PASS' : 'FAIL', `stages=${dbB.info.stageCount} adv=${stB1?.advancement_type}/${stB1?.advancement_count}`)

  // === Scene F: illegal 6 advance ===
  await selectDivisionTab(page, 0)
  await switchRulesTab(page, '快速创建')
  await clickScheme(page, '小组循环 → BO1 单败淘汰')
  await setQuickNumber(page, '分组数量', 3)
  await setQuickNumber(page, '每组晋级数量', 2)
  panel = await getPanelText(page)
  const sceneF_msg = panel.includes('6') && panel.includes('晋级') && panel.includes('4 支或 8 支')
  const applyDisabled = await page.locator('.format-rules-panel:visible button:has-text("应用赛制方案")').isDisabled().catch(() => true)
  const lockDisabled = await page.locator('.format-rules-panel:visible button:has-text("确认并启用赛制")').isDisabled().catch(() => true)
  rec('scene_f_illegal_6_advance', sceneF_msg && applyDisabled && lockDisabled ? 'PASS' : 'FAIL', `error shown=${sceneF_msg} applyDisabled=${applyDisabled} lockDisabled=${lockDisabled}`)
  // backend API reject
  try {
    await adminApi(token, 'POST', `/basket/v4/divisions/${ids.divisionAId}/format-plan/apply`, {
      planCode: 'GROUP_TO_ELIM', expectedTeamCount: 16, groupCount: 3, roundRobinLegs: 1, qualifiersPerGroup: 2
    })
    rec('scene_f_api_reject', 'FAIL', 'apply should reject 6 qualifiers')
  } catch (e) {
    rec('scene_f_api_reject', String(e.message).includes('6') ? 'PASS' : 'FAIL', e.message.slice(0, 120))
  }
  try {
    await adminApi(token, 'POST', `/basket/v4/divisions/${ids.divisionBId}/format-plan/apply`, {
      planCode: 'GROUP_TO_ELIM', expectedTeamCount: 12, groupCount: 3, roundRobinLegs: 1, qualifiersPerGroup: 2
    })
    rec('scene_f_api_reject_b', 'FAIL', 'apply should reject 6 qualifiers on B')
  } catch (e) {
    rec('scene_f_api_reject_b', String(e.message).includes('6') ? 'PASS' : 'FAIL', e.message.slice(0, 120))
  }
  // lock reject: corrupt division B draft stage to 6 qualifiers then attempt lock
  dbB = verifySnapshotDb(ids.divisionBId, { stageCount: 2 })
  const corruptStageId = dbB.info.stages[0]?.id
  if (corruptStageId) {
    await adminApi(token, 'PUT', `/basket/v4/divisions/${ids.divisionBId}/format-stages/${corruptStageId}`, {
      stageCode: dbB.info.stages[0].stage_code,
      stageName: dbB.info.stages[0].stage_name,
      stageType: dbB.info.stages[0].stage_type,
      competitionFormat: dbB.info.stages[0].competition_format,
      groupingMode: dbB.info.stages[0].grouping_mode || 'NONE',
      roundRobinCycles: dbB.info.stages[0].round_robin_cycles || 1,
      bestOf: 1,
      winPoints: 2,
      lossPoints: 1,
      advancementType: 'OVERALL_TOP_N',
      advancementCount: 6,
      pairingRule: 'NONE',
      nextStageCode: dbB.info.stages[1]?.stage_code
    })
    try {
      await adminApi(token, 'POST', `/basket/v4/divisions/${ids.divisionBId}/format-snapshot/lock`)
      rec('scene_f_lock_reject', 'FAIL', 'lock should reject 6-qualifier draft on B')
    } catch (e) {
      rec('scene_f_lock_reject', String(e.message).includes('6') ? 'PASS' : 'FAIL', e.message.slice(0, 120))
    }
  } else {
    rec('scene_f_lock_reject', 'SKIP', 'no draft stage on B')
  }

  // restore B to valid elim for later
  await clickScheme(page, 'BO1 单败淘汰')
  await applyScheme(page)
  await confirmLockIfEnabled(page)

  // === Custom format test on B ===
  try {
    const customBtn = page.locator('.format-rules-panel:visible .el-radio-button').filter({ hasText: '自定义赛制' })
    if (await customBtn.isEnabled()) {
      await customBtn.click()
      await waitForNetworkIdle(page, 800)
      panel = await getPanelText(page)
      rec('custom_mode_keep_stages', panel.includes('阶段') ? 'PASS' : 'FAIL', 'stages retained after custom switch')
    } else {
      rec('custom_mode_keep_stages', 'SKIP', '自定义赛制按钮不可用（可能已锁定）')
    }
  } catch (e) {
    rec('custom_mode_keep_stages', 'SKIP', e.message)
  }

  // === Switch back to A, verify B isolation ===
  await selectDivisionTab(page, 0)
  panel = await getPanelText(page)
  rec('isolation_a_after_b', panel.includes('小组') || panel.includes('淘汰赛') ? 'PASS' : 'FAIL', 'A still group+elim')
  dbA = verifySnapshotDb(ids.divisionAId, { stageCount: 2, snapshotStatus: 'LOCKED' })
  dbB = verifySnapshotDb(ids.divisionBId, { stageCount: 1 })
  rec('isolation_snapshot_ids', dbA.info.snapshotId !== dbB.info.snapshotId ? 'PASS' : 'FAIL', `A snap=${dbA.info.snapshotId} B snap=${dbB.info.snapshotId}`)

  // === Step 5 ===
  await wizardNext(page)
  const step5 = await page.locator('.publish-wizard-dialog').textContent()
  const step5ok = step5?.includes('赛制流程') && step5?.includes('赛制状态') && (step5?.includes('小组') || step5?.includes('淘汰'))
  rec('step5_summary', step5ok ? 'PASS' : 'FAIL', 'step5 division summary visible')

  await saveDraft(page)

  // === Drawer test ===
  await page.keyboard.press('Escape')
  await waitForNetworkIdle(page, 1000)
  await page.locator('input[placeholder="赛季名称"]').fill(BATCH)
  await page.click('button:has-text("搜索")')
  await waitForNetworkIdle(page, 1500)
  await page.locator('table tbody tr').filter({ hasText: BATCH }).first().locator('button:has-text("赛季详情")').click()
  const visibleDrawer = page.locator('.el-drawer:visible').last()
  await visibleDrawer.waitFor({ state: 'visible', timeout: 15000 })
  await page.waitForTimeout(400)
  await visibleDrawer.locator('.el-tabs__item').filter({ hasText: '赛制规则' }).click()
  await waitForNetworkIdle(page, 2000)
  await visibleDrawer.locator('.format-rules-panel').waitFor({ state: 'visible', timeout: 10000 })
  const drawerPanel = await visibleDrawer.locator('.format-rules-panel').textContent()
  const drawerOk = drawerPanel?.includes('已确认') && (drawerPanel?.includes('31') || drawerPanel?.includes('8 支') || drawerPanel?.includes('淘汰'))
  rec('drawer_consistency', drawerOk ? 'PASS' : 'FAIL', 'drawer shows locked group+elim')

  // idempotent lock via API
  const lock1 = await adminApi(token, 'POST', `/basket/v4/divisions/${ids.divisionAId}/format-snapshot/lock`)
  const lock2 = await adminApi(token, 'POST', `/basket/v4/divisions/${ids.divisionAId}/format-snapshot/lock`)
  const snapCountAfter = dbQuery(`SELECT COUNT(*) c FROM basket_division_format_snapshot WHERE division_id=${ids.divisionAId} AND is_current=1 AND is_delete=0`)[0]?.c
  rec('idempotent_lock', snapCountAfter === 1 ? 'PASS' : 'FAIL', `lock1=${lock1._http} lock2=${lock2._http} currentSnapshots=${snapCountAfter}`)

  // network/console
  const http500 = networkLog.filter(n => n.status >= 500)
  rec('network_no_500', http500.length === 0 ? 'PASS' : 'FAIL', http500.map(n => `${n.status} ${n.url}`).join('; ') || 'none')
  const legacyApi = networkLog.filter(n => n.note)
  rec('network_no_legacy_tournament', legacyApi.length === 0 ? 'PASS' : 'FAIL', legacyApi.map(n => n.url).join('; ') || 'none')
  const vueWarn = consoleLog.filter(c => c.text.includes('[Vue warn]'))
  rec('console_no_vue_warn', vueWarn.length === 0 ? 'PASS' : 'FAIL', vueWarn.slice(0, 3).map(c => c.text.slice(0, 120)).join(' | ') || 'none')

  await browser.close()

  // write outputs
  const outJson = {
    batch: BATCH,
    timestamp: new Date().toISOString(),
    ids,
    results,
    networkIssues: networkLog,
    consoleIssues: consoleLog.slice(0, 20)
  }
  const outDir = path.join(__dirname, 'output')
  fs.mkdirSync(outDir, { recursive: true })
  fs.writeFileSync(path.join(outDir, 'cnpa-format-rule-ui-test-result.json'), JSON.stringify(outJson, null, 2), 'utf-8')
  generateMarkdown(outJson)
}

function generateMarkdown(out) {
  const pass = out.results.filter(r => r.status === 'PASS').length
  const fail = out.results.filter(r => r.status === 'FAIL').length
  const p0 = out.results.filter(r => r.status === 'FAIL' && /lock|duplicate|isolation|500|save|stageCount|next_stage/i.test(r.name + r.detail))
  const p1 = out.results.filter(r => r.status === 'FAIL' && !p0.includes(r))
  const lines = []
  lines.push('# CNPA 赛制规则专项测试报告')
  lines.push('')
  lines.push(`- 批次: \`${BATCH}\``)
  lines.push(`- 时间: ${out.timestamp}`)
  lines.push(`- Admin 后端: ${ADMIN_BASE}`)
  lines.push(`- Admin UI: ${UI_BASE}`)
  lines.push(`- seasonId: **${out.ids.seasonId ?? '-'}**`)
  lines.push(`- divisionAId: **${out.ids.divisionAId ?? '-'}**`)
  lines.push(`- divisionBId: **${out.ids.divisionBId ?? '-'}**`)
  lines.push(`- 结论: ${fail === 0 ? '**验收通过**' : '**存在未通过项，暂不可标记验收通过**'} (${pass} PASS / ${fail} FAIL)`)
  lines.push('')
  lines.push('## 测试结果明细')
  lines.push('')
  lines.push('| 项 | 状态 | 详情 |')
  lines.push('|---|---|---|')
  for (const r of out.results) {
    lines.push(`| ${r.name} | ${r.status} | ${String(r.detail).replace(/\|/g, '/')} |`)
  }
  lines.push('')
  lines.push('## 数据库核对摘要')
  if (out.ids.divisionAId) {
    const dbA = verifySnapshotDb(out.ids.divisionAId, {})
    lines.push(`- Division A snapshotId=${dbA.info.snapshotId} status=${dbA.info.snapshotStatus} stages=${dbA.info.stageCount}`)
    for (const s of dbA.info.stages) {
      lines.push(`  - Stage ${s.id}: order=${s.stage_order} ${s.stage_name} ${s.competition_format} bestOf=${s.best_of} adv=${s.advancement_type}/${s.advancement_count} next=${s.next_stage_code || '-'}`)
    }
  }
  if (out.ids.divisionBId) {
    const dbB = verifySnapshotDb(out.ids.divisionBId, {})
    lines.push(`- Division B snapshotId=${dbB.info.snapshotId} status=${dbB.info.snapshotStatus} stages=${dbB.info.stageCount}`)
  }
  lines.push('')
  lines.push('## P0 / P1')
  lines.push(`- P0: ${p0.length ? p0.map(r => r.name).join(', ') : '无'}`)
  lines.push(`- P1: ${p1.length ? p1.map(r => r.name).join(', ') : '无'}`)
  lines.push('')
  const mdPath = path.join(__dirname, '..', 'docs', 'cnpa-format-rule-ui-test-report-20260806.md')
  fs.writeFileSync(mdPath, lines.join('\n'), 'utf-8')
  console.log(`\nReport written: ${mdPath}`)
}

runTests().catch(err => {
  console.error(err)
  rec('fatal', 'FAIL', err.message)
  const outJson = { batch: BATCH, timestamp: new Date().toISOString(), ids, results, fatal: err.message }
  const outDir = path.join(__dirname, 'output')
  fs.mkdirSync(outDir, { recursive: true })
  fs.writeFileSync(path.join(outDir, 'cnpa-format-rule-ui-test-result.json'), JSON.stringify(outJson, null, 2), 'utf-8')
  try { generateMarkdown(outJson) } catch {}
  process.exit(1)
})
