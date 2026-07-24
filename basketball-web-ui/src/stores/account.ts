import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getAccountCenterOverview } from '@/api/accountCenter'
import { bindAccountAvatar, getAccountProfile, updateAccountProfile } from '@/api/accountProfile'
import { uploadAccountFile } from '@/api/accountFile'
import {
  getNotificationSetting,
  getPrivacySetting,
  updateNotificationSetting,
  updatePrivacySetting,
} from '@/api/accountSetting'
import { getAccountTournamentPage } from '@/api/accountTournament'
import {
  bindPlayerCertificationMaterial,
  getPlayerCertificationCurrent,
  savePlayerCertificationDraft,
  submitPlayerCertification,
} from '@/api/playerCertification'
import { isLoggedIn } from '@/utils/auth'
import {
  getDefaultTeamRelations,
  teamRelationsStorageKey,
} from '@/mock/account/teamRelations'
import {
  getDefaultSecurity,
  hashPassword,
  securityStorageKey,
} from '@/mock/account/security'
import { validatePasswordRules } from '@/utils/passwordRules'
import {
  buildPlayerIdentity,
  getDefaultPlatformGameIds,
  getDefaultIdentityChangeHistory,
  platformGameIdsStorageKey,
} from '@/mock/playerIdentityMock'
import {
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
  getDefaultEligibility,
  eligibilityStorageKey,
} from '@/mock/playerIdentityMock'
import {
  readStorageItem,
  safeParseStorage,
  writeStorageItem,
} from '@/utils/safeParseStorage'
import {
  applyOverviewToStoreState,
  mapPlatformToGameTerminal,
  mapPlayerCertToVerification,
  mapProfileDtoToAccountProfile,
  mapTournamentItemToMyTournament,
} from '@/utils/accountMappers'
import type { NotificationSettingDto, PrivacySettingDto } from '@/api/types/accountSetting'
import type { PlayerCertCurrentDto, PlayerCertDraftPayload } from '@/api/types/playerCertification'
import type { PendingTaskDto } from '@/api/types/accountCenter'
import type {
  AccountProfile,
  AccountRole,
  CertifiedPlayerForOpen,
  CurrentAccount,
  EligibilityLimit,
  IdentityChangeRecord,
  IdentityPlatform,
  MyTournament,
  OpenApplication,
  OpenApplicationStatus,
  OpenTournament,
  PlatformGameIdRecord,
  PlayerIdentity,
  PlayerVerification,
  PlayerVerificationSnapshot,
  TeamRelationsData,
  AccountSecurityData,
} from '@/types/account'
import { IDENTITY_PLATFORM_LABELS, OPEN_APPLICATION_STATUS_LABELS } from '@/types/account'
import { mapVerificationPlatform } from '@/mock/playerIdentityMock'

export const useAccountStore = defineStore('account', () => {
  const currentAccount = ref<CurrentAccount | null>(null)
  const profile = ref<AccountProfile | null>(null)
  const verification = ref<PlayerVerification | null>(null)
  const playerCert = ref<PlayerCertCurrentDto | null>(null)
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

  const overviewLoading = ref(false)
  const overviewError = ref('')
  const pendingTasks = ref<PendingTaskDto[]>([])
  const privacySetting = ref<PrivacySettingDto | null>(null)
  const notificationSetting = ref<NotificationSettingDto | null>(null)
  const profileLoading = ref(false)
  const tournamentsLoading = ref(false)
  const certLoading = ref(false)

  const isAuthenticated = computed(() => isLoggedIn())

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
    const gameId = platformRecord?.gameId || effective?.gameId || playerCert.value?.gameAccount || ''
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

  const pendingInvitationCount = computed(() =>
    myTournaments.value.filter((item) => item.status === 'INVITED').length
      || teamRelations.value?.invitations.filter((item) => item.status === 'PENDING').length
      || 0,
  )

  function persistTeamRelations() {
    if (!teamRelations.value) return
    writeStorageItem(teamRelationsStorageKey(teamRelations.value.userId), JSON.stringify(teamRelations.value))
  }

  function persistSecurity() {
    if (!security.value) return
    writeStorageItem(securityStorageKey(security.value.userId), JSON.stringify(security.value))
  }

  function persistMyTournaments() {
    if (loadedUserId.value == null) return
    writeStorageItem(myTournamentsStorageKey(loadedUserId.value), JSON.stringify(myTournaments.value))
  }

  function persistOpenApplication() {
    if (loadedUserId.value == null || !openApplication.value) return
    writeStorageItem(openApplicationStorageKey(loadedUserId.value), JSON.stringify(openApplication.value))
  }

  function loadOutOfScopeMocks(userId: number) {
    teamRelations.value = safeParseStorage<TeamRelationsData>(
      readStorageItem(teamRelationsStorageKey(userId)),
      getDefaultTeamRelations(userId),
    )
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
    openTournament.value = { ...mockOpenTournament }
    openApplication.value = safeParseStorage<OpenApplication | null>(
      readStorageItem(openApplicationStorageKey(userId)),
      getDefaultOpenApplication(userId),
    )
  }

  async function ensureSecurityLoaded(userId: number) {
    security.value = safeParseStorage<AccountSecurityData | null>(
      readStorageItem(securityStorageKey(userId)),
      null,
    )
    if (!security.value) {
      security.value = await getDefaultSecurity(userId)
      persistSecurity()
    }
  }

  function applyOverviewData(overview: ReturnType<typeof applyOverviewToStoreState>) {
    currentAccount.value = overview.currentAccount
    profile.value = overview.profile
    verification.value = overview.verification
    myTournaments.value = overview.myTournaments
    pendingTasks.value = overview.pendingTasks
    loadedUserId.value = overview.currentAccount.userId
  }

  async function loadOverview() {
    if (!isLoggedIn()) {
      clearAccountState()
      return
    }
    overviewLoading.value = true
    overviewError.value = ''
    try {
      const res = await getAccountCenterOverview()
      const mapped = applyOverviewToStoreState(res.data, eligibilityLimited.value)
      applyOverviewData(mapped)
      if (loadedUserId.value != null) {
        loadOutOfScopeMocks(loadedUserId.value)
        await ensureSecurityLoaded(loadedUserId.value)
        if (currentAccount.value && security.value) {
          currentAccount.value.lastLoginAt = security.value.lastLoginAt || currentAccount.value.lastLoginAt
        }
      }
    } catch (error) {
      overviewError.value = error instanceof Error ? error.message : '加载失败'
      throw error
    } finally {
      overviewLoading.value = false
    }
  }

  async function loadProfile() {
    profileLoading.value = true
    try {
      const res = await getAccountProfile()
      profile.value = mapProfileDtoToAccountProfile(res.data)
      if (currentAccount.value) {
        currentAccount.value.nickname = res.data.nickname
        currentAccount.value.avatarUrl = res.data.avatarUrl || ''
        currentAccount.value.maskedEmail = res.data.emailMasked
        currentAccount.value.email = res.data.emailMasked
        currentAccount.value.emailVerified = Boolean(res.data.emailVerified)
        currentAccount.value.lastLoginAt = res.data.lastLoginTime || currentAccount.value.lastLoginAt
      }
    } finally {
      profileLoading.value = false
    }
  }

  async function updateProfile(payload: Partial<Pick<AccountProfile, 'nickname' | 'avatarUrl'>> & {
    avatarFile?: File
  }) {
    if (!profile.value || !currentAccount.value) return

    if (payload.avatarFile) {
      const uploadRes = await uploadAccountFile(payload.avatarFile, 'ACCOUNT_AVATAR')
      await bindAccountAvatar(uploadRes.data.fileId)
      payload.avatarUrl = uploadRes.data.accessUrl
    }

    if (payload.nickname !== undefined) {
      const res = await updateAccountProfile({ nickname: payload.nickname.trim() })
      profile.value = mapProfileDtoToAccountProfile(res.data)
      currentAccount.value.nickname = res.data.nickname
    }

    if (payload.avatarUrl !== undefined) {
      profile.value.avatarUrl = payload.avatarUrl
      currentAccount.value.avatarUrl = payload.avatarUrl
    }
  }

  async function loadPrivacy() {
    const res = await getPrivacySetting()
    privacySetting.value = res.data
    return res.data
  }

  async function updatePrivacy(payload: PrivacySettingDto) {
    const res = await updatePrivacySetting(payload)
    privacySetting.value = res.data
    return res.data
  }

  async function loadNotificationSetting() {
    const res = await getNotificationSetting()
    notificationSetting.value = res.data
    return res.data
  }

  async function updateNotificationSettingStore(payload: NotificationSettingDto) {
    const res = await updateNotificationSetting(payload)
    notificationSetting.value = res.data
    return res.data
  }

  async function loadPlayerCertification() {
    certLoading.value = true
    try {
      const res = await getPlayerCertificationCurrent()
      playerCert.value = res.data
      if (currentAccount.value) {
        verification.value = mapPlayerCertToVerification(res.data, currentAccount.value.userId)
        currentAccount.value.playerVerificationStatus = verification.value.status
        currentAccount.value.playerNo = res.data.playerNo || currentAccount.value.playerNo
        if (res.data.certificationStatus === 3) {
          currentAccount.value.accountRole = 'PLAYER'
        }
      }
      return res.data
    } finally {
      certLoading.value = false
    }
  }

  async function loadMyTournaments(page = 1, pageSize = 50) {
    tournamentsLoading.value = true
    try {
      const res = await getAccountTournamentPage({ currentPage: page, pageSize })
      myTournaments.value = (res.data.records || []).map(mapTournamentItemToMyTournament)
      persistMyTournaments()
      return res.data
    } finally {
      tournamentsLoading.value = false
    }
  }

  async function saveCertDraft(payload: PlayerCertDraftPayload) {
    const res = await savePlayerCertificationDraft(payload)
    playerCert.value = res.data
    if (currentAccount.value) {
      verification.value = mapPlayerCertToVerification(res.data, currentAccount.value.userId)
    }
    return res.data
  }

  async function bindCertMaterial(applicationId: string, fileId: string, materialType: string) {
    const res = await bindPlayerCertificationMaterial({ applicationId, fileId, materialType })
    await loadPlayerCertification()
    return res.data
  }

  async function submitCertApplication(applicationId: string) {
    const res = await submitPlayerCertification({ applicationId })
    playerCert.value = res.data
    if (currentAccount.value) {
      verification.value = mapPlayerCertToVerification(res.data, currentAccount.value.userId)
      currentAccount.value.playerVerificationStatus = verification.value.status
    }
    return res.data
  }

  async function submitPlayerVerificationFromForm(data: {
    realName: string
    idType: string
    idNo: string
    platform: string
    gameAccount: string
    gameNickname: string
    positionCode?: string
    contactMobile: string
    idFrontFile?: File
    idBackFile?: File
    gameScreenshotFile?: File
  }) {
    const draftPayload: PlayerCertDraftPayload = {
      realName: data.realName,
      idType: data.idType,
      idNo: data.idNo,
      gameTerminal: mapPlatformToGameTerminal(data.platform),
      gameAccount: data.gameAccount,
      gameNickname: data.gameNickname,
      positionCode: data.positionCode,
      contactMobile: data.contactMobile,
    }
    const draft = await saveCertDraft(draftPayload)
    const applicationId = draft.currentApplicationId
    if (!applicationId) throw new Error('未能创建认证申请')

    if (data.idFrontFile) {
      const uploaded = await uploadAccountFile(data.idFrontFile, 'PLAYER_CERT_ID_FRONT')
      await bindCertMaterial(applicationId, uploaded.data.fileId, 'PLAYER_CERT_ID_FRONT')
    }
    if (data.idBackFile) {
      const uploaded = await uploadAccountFile(data.idBackFile, 'PLAYER_CERT_ID_BACK')
      await bindCertMaterial(applicationId, uploaded.data.fileId, 'PLAYER_CERT_ID_BACK')
    }
    if (data.gameScreenshotFile) {
      const uploaded = await uploadAccountFile(data.gameScreenshotFile, 'PLAYER_CERT_GAME_SCREENSHOT')
      await bindCertMaterial(applicationId, uploaded.data.fileId, 'PLAYER_CERT_GAME_SCREENSHOT')
    }

    return submitCertApplication(applicationId)
  }

  function clearAccountState() {
    currentAccount.value = null
    profile.value = null
    verification.value = null
    playerCert.value = null
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
    overviewError.value = ''
    pendingTasks.value = []
    privacySetting.value = null
    notificationSetting.value = null
  }

  function submitPlayerVerification(data: PlayerVerificationSnapshot) {
    return submitPlayerVerificationFromForm({
      realName: data.backupContact || currentAccount.value?.nickname || '',
      idType: 'ID_CARD',
      idNo: '000000000000000000',
      platform: data.primaryPlatform,
      gameAccount: data.gameId,
      gameNickname: currentAccount.value?.nickname || '',
      positionCode: data.gamePosition,
      contactMobile: data.backupContact || '13800000000',
      gameScreenshotFile: undefined,
    })
  }

  function withdrawPlayerVerification() {
    if (!verification.value || !currentAccount.value) return
    if (verification.value.status === 'PENDING') {
      verification.value.status = 'NOT_APPLIED'
      verification.value.submittedAt = ''
      currentAccount.value.playerVerificationStatus = 'NOT_APPLIED'
    }
  }

  function resubmitPlayerVerification(data: PlayerVerificationSnapshot) {
    return submitPlayerVerification(data)
  }

  function submitVerificationChange(data: PlayerVerificationSnapshot) {
    return submitPlayerVerification(data)
  }

  function submitPlatformGameId(payload: { platform: IdentityPlatform; gameId: string }) {
    if (!loadedUserId.value) return
    const record = platformGameIds.value.find((item) => item.platform === payload.platform)
    if (!record) return
    record.gameId = payload.gameId
    record.status = 'PENDING'
    record.statusText = record.gameId ? '新增审核中' : '修改审核中'
    record.locked = false
  }

  function acceptTournamentInvitation(invitationId: string) {
    if (!isVerifiedPlayer.value) return
    const tournament = myTournaments.value.find((item) => item.invitationId === invitationId)
    if (!tournament || tournament.status !== 'INVITED') return
    tournament.status = 'JOINED'
    tournament.statusText = '已加入阵容'
    persistMyTournaments()
  }

  function rejectTournamentInvitation(invitationId: string) {
    const tournament = myTournaments.value.find((item) => item.invitationId === invitationId)
    if (!tournament || tournament.status !== 'INVITED') return
    tournament.status = 'NOT_JOINED'
    tournament.statusText = '未参加'
    tournament.teamName = ''
    tournament.role = ''
    persistMyTournaments()
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
  }

  function rejectTeamInvitation(invitationId: string) {
    if (!teamRelations.value) return
    const invitation = teamRelations.value.invitations.find((item) => item.id === invitationId)
    if (!invitation || invitation.status !== 'PENDING') return
    invitation.status = 'REJECTED'
    persistTeamRelations()
  }

  function submitTeamCreateRequest(_payload: { teamName: string; platform: string; description: string }) {
    if (!teamRelations.value) return
    teamRelations.value.createRequestStatus = 'PENDING'
    teamRelations.value.createRequestSubmittedAt = new Date().toISOString()
    persistTeamRelations()
  }

  async function changePassword(currentPassword: string, newPassword: string): Promise<string> {
    if (!security.value) return '账号数据未加载'
    const ruleError = validatePasswordRules(newPassword)
    if (ruleError) return ruleError
    if (newPassword === currentPassword) return '新密码不能与当前密码一致'
    const currentHash = await hashPassword(currentPassword)
    if (currentHash !== security.value.passwordHash) return '当前密码不正确'
    security.value.passwordHash = await hashPassword(newPassword)
    security.value.passwordUpdatedAt = new Date().toISOString()
    persistSecurity()
    return ''
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
    const now = new Date().toISOString().replace('T', ' ').slice(0, 16)
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
  }

  function applyDevOpenStatusOverride(override: OpenApplicationStatus | null) {
    if (!import.meta.env.DEV || !loadedUserId.value) return
    if (!override || override === 'NOT_SUBMITTED') {
      openApplication.value = getDefaultOpenApplication(loadedUserId.value)
      openTournament.value = { ...mockOpenTournament }
      persistOpenApplication()
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
  }

  function getProfileCompleteness() {
    if (!profile.value) {
      return { percent: 0, items: [] as { key: string; label: string; done: boolean; weight: number }[] }
    }
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
    playerCert,
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
    teamRelations,
    security,
    privacySetting,
    notificationSetting,
    pendingTasks,
    overviewLoading,
    overviewError,
    profileLoading,
    tournamentsLoading,
    certLoading,
    isAuthenticated,
    pendingInvitationCount,
    isVerifiedPlayer,
    loadOverview,
    loadProfile,
    updateProfile,
    loadPrivacy,
    updatePrivacy,
    loadNotificationSetting,
    updateNotificationSetting: updateNotificationSettingStore,
    loadPlayerCertification,
    loadMyTournaments,
    saveCertDraft,
    bindCertMaterial,
    submitCertApplication,
    submitPlayerVerificationFromForm,
    clearAccountState,
    submitPlayerVerification,
    withdrawPlayerVerification,
    resubmitPlayerVerification,
    submitVerificationChange,
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
