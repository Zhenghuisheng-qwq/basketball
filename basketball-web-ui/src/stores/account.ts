import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getCurrentAuth } from '@/mock/auth'
import {
  accountStorageKey,
  getDefaultAccount,
} from '@/mock/account/account'
import {
  getDefaultProfile,
  profileStorageKey,
} from '@/mock/account/profile'
import {
  applyDevVerificationOverride,
  getDefaultVerification,
  verificationStorageKey,
} from '@/mock/account/verification'
import {
  buildPlayerIdentity,
  buildPlayerNo,
  eligibilityStorageKey,
  getDefaultEligibility,
  getDefaultIdentityChangeHistory,
  getDefaultPlatformGameIds,
  mapVerificationPlatform,
  platformGameIdsStorageKey,
} from '@/mock/playerIdentityMock'
import {
  getDefaultMyTournaments,
  myTournamentsStorageKey,
} from '@/mock/tournamentAccountMock'
import {
  applyDevOpenApplicationOverride,
  buildOpenApplicationNo,
  getDefaultOpenApplication,
  isOpenRegistrationDeadlinePassed,
  mockOpenTournament,
  openApplicationStorageKey,
} from '@/mock/openRegistrationMock'
import {
  countUnread,
  getDefaultNotifications,
  notificationsStorageKey,
} from '@/mock/account/notifications'
import {
  getDefaultTeamRelations,
  teamRelationsStorageKey,
} from '@/mock/account/teamRelations'
import {
  getDefaultSecurity,
  hashPassword,
  securityStorageKey,
  validatePasswordRules,
} from '@/mock/account/security'
import {
  readStorageItem,
  removeStorageItem,
  safeParseStorage,
  writeStorageItem,
} from '@/utils/safeParseStorage'
import { patchAuthSession } from '@/mock/auth'
import type {
  AccountNotification,
  AccountProfile,
  AccountRole,
  CurrentAccount,
  PlayerVerification,
  PlayerVerificationSnapshot,
  PlayerVerificationStatus,
  TeamRelationsData,
  AccountSecurityData,
  PlayerIdentity,
  PlatformGameIdRecord,
  IdentityChangeRecord,
  EligibilityLimit,
  MyTournament,
  IdentityPlatform,
  OpenApplication,
  OpenTournament,
  CertifiedPlayerForOpen,
  OpenApplicationStatus,
} from '@/types/account'
import { IDENTITY_PLATFORM_LABELS, OPEN_APPLICATION_STATUS_LABELS } from '@/types/account'

function syncAccountRoleFromVerification(
  account: CurrentAccount,
  verification: PlayerVerification,
): CurrentAccount {
  const next = { ...account, playerVerificationStatus: verification.status }
  if (verification.status === 'APPROVED' || verification.status === 'CHANGE_PENDING') {
    if (next.accountRole === 'BASIC_USER') {
      next.accountRole = 'PLAYER'
    }
    next.playerNo = buildPlayerNo(account.userId)
  } else if (verification.status === 'NOT_APPLIED' || verification.status === 'PENDING' || verification.status === 'REJECTED') {
    next.playerNo = ''
  }
  return next
}

export const useAccountStore = defineStore('account', () => {
  const currentAccount = ref<CurrentAccount | null>(null)
  const profile = ref<AccountProfile | null>(null)
  const verification = ref<PlayerVerification | null>(null)
  const notifications = ref<AccountNotification[]>([])
  const teamRelations = ref<TeamRelationsData | null>(null)
  const security = ref<AccountSecurityData | null>(null)
  const platformGameIds = ref<PlatformGameIdRecord[]>([])
  const identityChangeHistory = ref<IdentityChangeRecord[]>([])
  const eligibility = ref<EligibilityLimit | null>(null)
  const myTournaments = ref<MyTournament[]>([])
  const openTournament = ref<OpenTournament>({ ...mockOpenTournament })
  const openApplication = ref<OpenApplication | null>(null)
  const eligibilityLimited = ref(false)
  const loadedUserId = ref<number | null>(null)

  const isAuthenticated = computed(() => Boolean(getCurrentAuth()?.loggedIn))
  const unreadCount = computed(() => countUnread(notifications.value))

  const pendingInvitationCount = computed(() =>
    myTournaments.value.filter((item) => item.status === 'INVITED').length
      || teamRelations.value?.invitations.filter((item) => item.status === 'PENDING').length
      || 0,
  )

  const playerIdentity = computed<PlayerIdentity | null>(() => {
    if (!verification.value || !currentAccount.value) return null
    return buildPlayerIdentity(
      verification.value,
      currentAccount.value.nickname,
      eligibilityLimited.value,
    )
  })

  const isVerifiedPlayer = computed(() =>
    verification.value?.status === 'APPROVED' || verification.value?.status === 'CHANGE_PENDING',
  )

  const certifiedPlayerForOpen = computed<CertifiedPlayerForOpen | null>(() => {
    if (!currentAccount.value || !isVerifiedPlayer.value) return null
    const effective = verification.value?.currentEffectiveData || verification.value
    const platformKey = effective?.primaryPlatform
      ? mapVerificationPlatform(effective.primaryPlatform)
      : 'XBOX'
    const platformRecord = platformGameIds.value.find(
      (item) => item.platform === platformKey && item.status === 'EFFECTIVE',
    )
    const gameId = platformRecord?.gameId || effective?.gameId || ''
    return {
      userNo: currentAccount.value.userNo,
      playerNo: currentAccount.value.playerNo,
      nickname: currentAccount.value.nickname,
      avatarUrl: currentAccount.value.avatarUrl,
      identityStatus: 'APPROVED',
      identityStatusText: '认证球员',
      platform: IDENTITY_PLATFORM_LABELS[platformKey] || mockOpenTournament.platform,
      gameId,
    }
  })

  const openRegistrationStats = computed(() => ({
    joined: myTournaments.value.filter((item) => item.status === 'JOINED').length
      + (openApplication.value?.status === 'APPROVED' ? 1 : 0),
    pending: openApplication.value?.status === 'PENDING' ? 1 : 0,
    rejected: openApplication.value?.status === 'REJECTED' ? 1 : 0,
    available: openApplication.value?.status === 'NOT_SUBMITTED'
      && openTournament.value.status === 'REGISTRATION_OPEN'
      && isVerifiedPlayer.value
      ? 1
      : 0,
  }))

  function persistAccount() {
    if (!currentAccount.value) return
    writeStorageItem(accountStorageKey(currentAccount.value.userId), JSON.stringify(currentAccount.value))
  }

  function persistProfile() {
    if (!profile.value) return
    writeStorageItem(profileStorageKey(profile.value.userId), JSON.stringify(profile.value))
  }

  function persistVerification() {
    if (!verification.value) return
    writeStorageItem(verificationStorageKey(verification.value.userId), JSON.stringify(verification.value))
  }

  function persistNotifications() {
    if (loadedUserId.value == null) return
    writeStorageItem(notificationsStorageKey(loadedUserId.value), JSON.stringify(notifications.value))
  }

  function persistTeamRelations() {
    if (!teamRelations.value) return
    writeStorageItem(teamRelationsStorageKey(teamRelations.value.userId), JSON.stringify(teamRelations.value))
  }

  function persistSecurity() {
    if (!security.value) return
    writeStorageItem(securityStorageKey(security.value.userId), JSON.stringify(security.value))
  }

  function persistPlatformGameIds() {
    if (loadedUserId.value == null) return
    writeStorageItem(platformGameIdsStorageKey(loadedUserId.value), JSON.stringify(platformGameIds.value))
  }

  function persistMyTournaments() {
    if (loadedUserId.value == null) return
    writeStorageItem(myTournamentsStorageKey(loadedUserId.value), JSON.stringify(myTournaments.value))
  }

  function persistOpenApplication() {
    if (loadedUserId.value == null || !openApplication.value) return
    writeStorageItem(openApplicationStorageKey(loadedUserId.value), JSON.stringify(openApplication.value))
  }

  function formatOpenDateTime(iso: string): string {
    if (!iso) return ''
    return iso.replace('T', ' ').slice(0, 16)
  }

  function syncOpenTournamentEntry() {
    if (!openApplication.value) return
    const openId = openTournament.value.id
    let entry = myTournaments.value.find((item) => item.id === openId)
    if (!entry) {
      entry = {
        id: openId,
        name: openTournament.value.name,
        type: 'Open',
        period: openTournament.value.seasonName,
        status: 'NOT_JOINED',
        statusText: '未参加',
        teamName: '',
        role: '个人参赛',
        eligibility: 'OK',
        openRegistrationStatus: openApplication.value.status,
      }
      myTournaments.value.unshift(entry)
    }
    entry.openRegistrationStatus = openApplication.value.status
    if (openApplication.value.status === 'APPROVED') {
      entry.status = 'JOINED'
      entry.statusText = '正式参赛'
      entry.role = '个人参赛'
    } else if (openApplication.value.status === 'PENDING') {
      entry.status = 'WAITING_REGISTRATION'
      entry.statusText = '报名审核中'
    } else if (openApplication.value.status === 'REJECTED') {
      entry.status = 'NOT_JOINED'
      entry.statusText = '报名未通过'
    } else if (openApplication.value.status === 'CANCELLED') {
      entry.status = 'NOT_JOINED'
      entry.statusText = '已取消'
    } else {
      entry.status = 'NOT_JOINED'
      entry.statusText = '未参加'
    }
    persistMyTournaments()
  }

  function getOpenRegistrationBlockReason(): string {
    if (!currentAccount.value?.emailVerified) return '请先完成邮箱验证'
    if (!isVerifiedPlayer.value) return '请先完成球员身份认证'
    if (!certifiedPlayerForOpen.value?.playerNo) return '尚未生成永久球员 ID，无法报名'
    if (!certifiedPlayerForOpen.value.gameId) return '请先绑定并生效对应平台的游戏 ID'
    if (eligibilityLimited.value) return '你在该赛事的参赛资格受限，无法报名'
    if (openTournament.value.quotaLeft <= 0) return '名额已满，无法提交报名'
    if (openTournament.value.status === 'REGISTRATION_CLOSED') return '报名已关闭'
    if (isOpenRegistrationDeadlinePassed(openTournament.value.registrationDeadline)) return '报名已截止'
    if (openApplication.value?.status === 'PENDING') return '当前报名审核中，不能重复提交'
    if (openApplication.value?.status === 'APPROVED') return '你已报名并通过审核，不能重复提交'
    if (openApplication.value?.status === 'CANCELLED') return '该赛事报名已取消'
    return ''
  }

  function canSubmitOpenApplication(): boolean {
    return !getOpenRegistrationBlockReason()
      && (openApplication.value?.status === 'NOT_SUBMITTED' || openApplication.value?.status === 'REJECTED')
  }

  function submitOpenApplication() {
    if (!canSubmitOpenApplication() || !certifiedPlayerForOpen.value || !currentAccount.value) return
    const now = formatOpenDateTime(new Date().toISOString())
    const player = certifiedPlayerForOpen.value
    openApplication.value = {
      applicationNo: buildOpenApplicationNo(currentAccount.value.userId),
      tournamentId: openTournament.value.id,
      tournamentName: openTournament.value.name,
      applicationType: 'INDIVIDUAL',
      applicationTypeText: '个人报名',
      status: 'PENDING',
      statusText: OPEN_APPLICATION_STATUS_LABELS.PENDING,
      playerNo: player.playerNo,
      nickname: player.nickname,
      platform: player.platform,
      gameId: player.gameId,
      submitTime: now,
      reviewTime: '',
      reviewRemark: '',
    }
    persistOpenApplication()
    syncOpenTournamentEntry()
    addSystemNotification(
      '2026 CNPA Open 个人报名已提交',
      '你的 Open 个人报名已进入管理员审核，审核完成后状态将自动更新。',
      'EVENT',
      'VIEW_OPEN_APPLICATION',
      openTournament.value.id,
    )
  }

  function applyDevOpenStatusOverride(override: OpenApplicationStatus | null) {
    if (!import.meta.env.DEV || !loadedUserId.value) return
    if (!override || override === 'NOT_SUBMITTED') {
      openApplication.value = getDefaultOpenApplication(loadedUserId.value)
      openTournament.value = { ...mockOpenTournament }
      persistOpenApplication()
      syncOpenTournamentEntry()
      return
    }
    if (override === 'CLOSED') {
      openTournament.value = { ...mockOpenTournament, status: 'REGISTRATION_CLOSED', statusText: '报名关闭' }
      return
    }
    openApplication.value = applyDevOpenApplicationOverride(
      openApplication.value || getDefaultOpenApplication(loadedUserId.value),
      override,
      loadedUserId.value,
    )
    persistOpenApplication()
    syncOpenTournamentEntry()
  }

  function syncUnreadToAccount() {
    if (!currentAccount.value) return
    currentAccount.value.unreadNotificationCount = unreadCount.value
    persistAccount()
  }

  async function loadAccountMock(
    devVerificationOverride?: PlayerVerificationStatus | null,
    devEligibilityLimited = false,
    devOpenStatusOverride?: OpenApplicationStatus | null,
  ) {
    const session = getCurrentAuth()
    if (!session?.loggedIn) {
      clearAccountState()
      return
    }

    const userId = session.userId
    loadedUserId.value = userId

    const storedAccount = safeParseStorage<CurrentAccount | null>(
      readStorageItem(accountStorageKey(userId)),
      null,
    )
    const baseAccount = storedAccount || getDefaultAccount(userId, session.email, session.nickname)

    const storedProfile = safeParseStorage<AccountProfile | null>(
      readStorageItem(profileStorageKey(userId)),
      null,
    )
    profile.value = storedProfile || getDefaultProfile(userId, session.email, session.nickname)

    let storedVerification = safeParseStorage<PlayerVerification | null>(
      readStorageItem(verificationStorageKey(userId)),
      null,
    )
    if (!storedVerification) {
      storedVerification = getDefaultVerification(userId)
    }
    if (import.meta.env.DEV && devVerificationOverride) {
      storedVerification = applyDevVerificationOverride(storedVerification, devVerificationOverride)
    }
    verification.value = storedVerification

    notifications.value = safeParseStorage<AccountNotification[]>(
      readStorageItem(notificationsStorageKey(userId)),
      getDefaultNotifications(userId),
    )

    teamRelations.value = safeParseStorage<TeamRelationsData>(
      readStorageItem(teamRelationsStorageKey(userId)),
      getDefaultTeamRelations(userId),
    )

    security.value = safeParseStorage<AccountSecurityData | null>(
      readStorageItem(securityStorageKey(userId)),
      null,
    )
    if (!security.value) {
      security.value = await getDefaultSecurity(userId)
      persistSecurity()
    }

    platformGameIds.value = safeParseStorage<PlatformGameIdRecord[]>(
      readStorageItem(platformGameIdsStorageKey(userId)),
      getDefaultPlatformGameIds(userId),
    )

    identityChangeHistory.value = safeParseStorage<IdentityChangeRecord[]>(
      readStorageItem(`${platformGameIdsStorageKey(userId)}_history`),
      getDefaultIdentityChangeHistory(userId),
    )

    eligibility.value = safeParseStorage<EligibilityLimit>(
      readStorageItem(eligibilityStorageKey(userId)),
      getDefaultEligibility(userId),
    )

    myTournaments.value = safeParseStorage<MyTournament[]>(
      readStorageItem(myTournamentsStorageKey(userId)),
      getDefaultMyTournaments(userId),
    )

    openTournament.value = { ...mockOpenTournament }
    openApplication.value = safeParseStorage<OpenApplication | null>(
      readStorageItem(openApplicationStorageKey(userId)),
      getDefaultOpenApplication(userId),
    )

    if (import.meta.env.DEV && devOpenStatusOverride) {
      applyDevOpenStatusOverride(devOpenStatusOverride)
    } else {
      syncOpenTournamentEntry()
    }

    eligibilityLimited.value = devEligibilityLimited
      || (devVerificationOverride === null && eligibility.value.status === 'LIMITED')
      || false

    if (import.meta.env.DEV && devVerificationOverride === 'APPROVED') {
      eligibilityLimited.value = devEligibilityLimited
    }

    currentAccount.value = syncAccountRoleFromVerification(
      {
        ...baseAccount,
        nickname: profile.value.nickname,
        avatarUrl: profile.value.avatarUrl,
        email: profile.value.email,
        maskedEmail: profile.value.maskedEmail,
        unreadNotificationCount: countUnread(notifications.value),
        lastLoginAt: security.value.lastLoginAt,
      },
      verification.value,
    )
    persistAccount()
  }

  function clearAccountState() {
    currentAccount.value = null
    profile.value = null
    verification.value = null
    notifications.value = []
    teamRelations.value = null
    security.value = null
    platformGameIds.value = []
    identityChangeHistory.value = []
    eligibility.value = null
    myTournaments.value = []
    openTournament.value = { ...mockOpenTournament }
    openApplication.value = null
    eligibilityLimited.value = false
    loadedUserId.value = null
  }

  function updateProfile(payload: Partial<Pick<AccountProfile, 'nickname' | 'avatarUrl'>>) {
    if (!profile.value || !currentAccount.value) return
    if (payload.nickname !== undefined) {
      profile.value.nickname = payload.nickname
      currentAccount.value.nickname = payload.nickname
    }
    if (payload.avatarUrl !== undefined) {
      profile.value.avatarUrl = payload.avatarUrl
      currentAccount.value.avatarUrl = payload.avatarUrl
    }
    persistProfile()
    persistAccount()
    patchAuthSession({
      nickname: currentAccount.value.nickname,
      avatarUrl: currentAccount.value.avatarUrl,
    })
  }

  function buildApplicationNo(userId: number): string {
    const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    return `CNPA-PV-${date}-${String(userId).padStart(5, '0')}`
  }

  function submitPlayerVerification(data: PlayerVerificationSnapshot) {
    if (!verification.value || !currentAccount.value) return
    const now = new Date().toISOString()
    verification.value = {
      ...verification.value,
      primaryPlatform: data.primaryPlatform,
      gameId: data.gameId,
      gameIdScreenshotUrl: data.gameIdScreenshotUrl,
      gamePosition: data.gamePosition,
      commonPosition: data.commonPosition,
      backupContact: data.backupContact,
      status: 'PENDING',
      submittedAt: now,
      reviewedAt: '',
      effectiveAt: '',
      reviewerComment: '',
      rejectedFields: [],
      applicationNo: buildApplicationNo(currentAccount.value.userId),
      pendingChangeData: undefined,
    }
    currentAccount.value.playerVerificationStatus = 'PENDING'
    persistVerification()
    persistAccount()
    addSystemNotification('球员认证申请已提交', '你的球员认证申请已进入审核队列。', 'VERIFICATION', 'VIEW_PLAYER_IDENTITY')
  }

  function withdrawPlayerVerification() {
    if (!verification.value || !currentAccount.value) return
    if (verification.value.status === 'CHANGE_PENDING') {
      verification.value.status = 'APPROVED'
      verification.value.pendingChangeData = undefined
      currentAccount.value.playerVerificationStatus = 'APPROVED'
    } else if (verification.value.status === 'PENDING') {
      verification.value.status = 'NOT_APPLIED'
      verification.value.submittedAt = ''
      currentAccount.value.playerVerificationStatus = 'NOT_APPLIED'
    }
    persistVerification()
    persistAccount()
  }

  function resubmitPlayerVerification(data: PlayerVerificationSnapshot) {
    if (!verification.value || !currentAccount.value) return
    submitPlayerVerification(data)
  }

  function submitVerificationChange(data: PlayerVerificationSnapshot) {
    if (!verification.value || !currentAccount.value) return
    verification.value.pendingChangeData = data
    verification.value.status = 'CHANGE_PENDING'
    verification.value.submittedAt = new Date().toISOString()
    verification.value.applicationNo = buildApplicationNo(currentAccount.value.userId)
    currentAccount.value.playerVerificationStatus = 'CHANGE_PENDING'
    persistVerification()
    persistAccount()
    addSystemNotification('认证资料变更已提交', '变更申请审核期间，当前资料继续生效。', 'VERIFICATION', 'VIEW_VERIFICATION')
  }

  function addSystemNotification(
    title: string,
    content: string,
    type: AccountNotification['type'] = 'SYSTEM',
    actionType: AccountNotification['actionType'] = 'NONE',
    actionTargetId = '',
  ) {
    if (loadedUserId.value == null) return
    notifications.value.unshift({
      id: `n-${loadedUserId.value}-${Date.now()}`,
      userId: loadedUserId.value,
      type,
      title,
      summary: content.slice(0, 60),
      content,
      createdAt: new Date().toISOString(),
      read: false,
      pendingAction: actionType !== 'NONE',
      actionType,
      actionTargetId,
      metadata: { source: 'CNPA 系统' },
    })
    persistNotifications()
    syncUnreadToAccount()
  }

  function markNotificationRead(id: string) {
    const item = notifications.value.find((n) => n.id === id)
    if (!item || item.read) return
    item.read = true
    persistNotifications()
    syncUnreadToAccount()
  }

  function markAllNotificationsRead() {
    notifications.value.forEach((item) => {
      item.read = true
    })
    persistNotifications()
    syncUnreadToAccount()
  }

  function submitPlatformGameId(payload: { platform: IdentityPlatform; gameId: string }) {
    if (!loadedUserId.value) return
    const record = platformGameIds.value.find((item) => item.platform === payload.platform)
    if (!record) return
    record.gameId = payload.gameId
    record.status = 'PENDING'
    record.statusText = record.gameId ? '新增审核中' : '修改审核中'
    record.locked = false
    persistPlatformGameIds()
    addSystemNotification(
      '平台与游戏 ID 修改审核中',
      `${payload.platform} 平台游戏 ID 变更申请已提交，审核通过前继续使用当前已生效 ID。`,
      'VERIFICATION',
      'VIEW_PLATFORMS',
    )
  }

  function acceptTournamentInvitation(invitationId: string) {
    if (!isVerifiedPlayer.value) return
    const tournament = myTournaments.value.find((item) => item.invitationId === invitationId)
    if (!tournament || tournament.status !== 'INVITED') return
    tournament.status = 'JOINED'
    tournament.statusText = '已加入阵容'
    persistMyTournaments()
    addSystemNotification('已接受球队邀请', `你已加入 ${tournament.teamName}（${tournament.name}）。`, 'TEAM', 'VIEW_MY_TOURNAMENTS')
    acceptTeamInvitation(invitationId)
  }

  function rejectTournamentInvitation(invitationId: string) {
    const tournament = myTournaments.value.find((item) => item.invitationId === invitationId)
    if (!tournament || tournament.status !== 'INVITED') return
    tournament.status = 'NOT_JOINED'
    tournament.statusText = '未参加'
    tournament.teamName = ''
    tournament.role = ''
    persistMyTournaments()
    addSystemNotification('已拒绝球队邀请', `你已拒绝 ${tournament.name} 的球队邀请。`, 'TEAM')
    rejectTeamInvitation(invitationId)
  }

  function acceptTeamInvitation(invitationId: string) {
    if (!teamRelations.value || !currentAccount.value) return
    const invitation = teamRelations.value.invitations.find((item) => item.id === invitationId)
    if (!invitation || invitation.status !== 'PENDING') return

    invitation.status = 'ACCEPTED'
    teamRelations.value.lineupRelations.push({
      id: `lr-${currentAccount.value.userId}-${Date.now()}`,
      userId: currentAccount.value.userId,
      tournamentId: invitation.tournamentId,
      tournamentName: invitation.tournamentName,
      teamId: invitation.teamId,
      teamName: invitation.teamName,
      roleName: invitation.inviteRole,
      status: 'WAITING_TEAM_REGISTRATION',
    })
    persistTeamRelations()
    addSystemNotification(
      '已接受球队邀请',
      `你已加入 ${invitation.teamName}（${invitation.tournamentName}）。`,
      'TEAM',
      'NONE',
    )
  }

  function rejectTeamInvitation(invitationId: string) {
    if (!teamRelations.value) return
    const invitation = teamRelations.value.invitations.find((item) => item.id === invitationId)
    if (!invitation || invitation.status !== 'PENDING') return
    invitation.status = 'REJECTED'
    persistTeamRelations()
    addSystemNotification(
      '已拒绝球队邀请',
      `你已拒绝 ${invitation.teamName} 的邀请。`,
      'TEAM',
      'NONE',
    )
  }

  function submitTeamCreateRequest(payload: { teamName: string; platform: string; description: string }) {
    if (!teamRelations.value) return
    teamRelations.value.createRequestStatus = 'PENDING'
    teamRelations.value.createRequestSubmittedAt = new Date().toISOString()
    persistTeamRelations()
    addSystemNotification(
      '创建球队申请已提交',
      `球队「${payload.teamName}」创建申请已提交，等待管理员审核。`,
      'TEAM',
    )
  }

  async function changePassword(currentPassword: string, newPassword: string): Promise<string> {
    if (!security.value) return '账号数据未加载'
    const ruleError = validatePasswordRules(newPassword)
    if (ruleError) return ruleError
    if (newPassword === currentPassword) return '新密码不能与当前密码一致'

    const currentHash = await hashPassword(currentPassword)
    if (currentHash !== security.value.passwordHash) {
      return '当前密码不正确'
    }

    security.value.passwordHash = await hashPassword(newPassword)
    security.value.passwordUpdatedAt = new Date().toISOString()
    persistSecurity()
    addSystemNotification('密码已更新', '你的账号密码已成功修改。', 'SECURITY')
    return ''
  }

  function resetMockAccountData(userId: number) {
    if (!import.meta.env.DEV) return
    removeStorageItem(accountStorageKey(userId))
    removeStorageItem(profileStorageKey(userId))
    removeStorageItem(verificationStorageKey(userId))
    removeStorageItem(notificationsStorageKey(userId))
    removeStorageItem(teamRelationsStorageKey(userId))
    removeStorageItem(securityStorageKey(userId))
    removeStorageItem(openApplicationStorageKey(userId))
  }

  function getProfileCompleteness() {
    if (!profile.value) return { percent: 0, items: [] as { key: string; label: string; done: boolean; weight: number }[] }
    const items = [
      { key: 'email', label: '邮箱已验证', done: profile.value.emailVerified, weight: 40 },
      { key: 'avatar', label: '上传个人头像', done: Boolean(profile.value.avatarUrl?.trim()), weight: 30 },
      { key: 'nickname', label: '设置个人昵称', done: Boolean(profile.value.nickname?.trim()), weight: 30 },
    ]
    const percent = items.reduce((sum, item) => sum + (item.done ? item.weight : 0), 0)
    return { percent, items }
  }

  return {
    currentAccount,
    profile,
    verification,
    playerIdentity,
    platformGameIds,
    identityChangeHistory,
    eligibility,
    myTournaments,
    openTournament,
    openApplication,
    certifiedPlayerForOpen,
    openRegistrationStats,
    eligibilityLimited,
    notifications,
    teamRelations,
    security,
    isAuthenticated,
    unreadCount,
    pendingInvitationCount,
    isVerifiedPlayer,
    loadAccountMock,
    clearAccountState,
    updateProfile,
    submitPlayerVerification,
    withdrawPlayerVerification,
    resubmitPlayerVerification,
    submitVerificationChange,
    markNotificationRead,
    markAllNotificationsRead,
    acceptTeamInvitation,
    rejectTeamInvitation,
    submitTeamCreateRequest,
    submitPlatformGameId,
    acceptTournamentInvitation,
    rejectTournamentInvitation,
    submitOpenApplication,
    getOpenRegistrationBlockReason,
    canSubmitOpenApplication,
    applyDevOpenStatusOverride,
    changePassword,
    resetMockAccountData,
    getProfileCompleteness,
  }
})

export function getAccountRoleLabel(role: AccountRole): string {
  const labels: Record<AccountRole, string> = {
    BASIC_USER: '基础用户',
    PLAYER: '认证球员',
    CAPTAIN: '认证队长',
  }
  return labels[role]
}
