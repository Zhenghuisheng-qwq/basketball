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
import { changeAccountPassword } from '@/api/accountSecurity'
import {
  addPlatformAccount,
  changePlatformGameId,
  getPlatformAccountList,
  getPlatformChangeHistory,
  getPlayerEligibilityStatus,
} from '@/api/playerPlatformAccount'
import {
  bindPlayerCertificationMaterial,
  getPlayerCertificationCurrent,
  savePlayerCertificationDraft,
  submitPlayerCertification,
  withdrawPlayerCertification,
} from '@/api/playerCertification'
import { getAccountTournamentPage } from '@/api/accountTournament'
import {
  createOpenRegistrationDraft,
  getOpenAvailableSeasonPage,
  getOpenRegistrationPage,
  submitOpenRegistration,
} from '@/api/openRegistration'
import { isLoggedIn } from '@/utils/auth'
import {
  myTournamentsStorageKey,
  securityStorageKey,
  teamRelationsStorageKey,
} from '@/constants/accountStorageKeys'
import { CLOSED_OPEN_TOURNAMENT } from '@/constants/openRegistrationDefaults'
import { validatePasswordRules } from '@/utils/passwordRules'
import { buildPlayerIdentity, mapVerificationPlatform } from '@/utils/playerIdentity'
import { getDefaultTeamRelations } from '@/utils/teamRelations'
import {
  isRegistrationDeadlinePassed,
  mapOpenDetailToApplication,
  mapOpenSeasonToTournament,
} from '@/utils/participationMappers'
import {
  readStorageItem,
  safeParseStorage,
  writeStorageItem,
} from '@/utils/safeParseStorage'
import {
  applyOverviewToStoreState,
  filterApprovedChangeHistory,
  mapEligibilityStatus,
  mapPlatformAccountItem,
  mapPlatformChangeHistoryItem,
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
  OpenTournament,
  PlatformGameIdRecord,
  PlayerIdentity,
  PlayerVerification,
  PlayerVerificationSnapshot,
  TeamRelationsData,
  AccountSecurityData,
} from '@/types/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'

function dataUrlToFile(dataUrl: string, filename = 'game-screenshot.jpg'): File | undefined {
  if (!dataUrl.startsWith('data:')) return undefined
  const [header, base64] = dataUrl.split(',')
  if (!base64) return undefined
  const mime = header.match(/:(.*?);/)?.[1] || 'image/jpeg'
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return new File([bytes], filename, { type: mime })
}

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
  const openTournament = ref<OpenTournament>({
    id: '',
    name: '',
    shortName: '',
    seasonName: '',
    type: 'OPEN',
    platform: '',
    registrationType: 'INDIVIDUAL',
    registrationTypeText: '个人报名',
    registrationDeadline: '',
    quotaTotal: 0,
    quotaUsed: 0,
    quotaLeft: 0,
    status: 'REGISTRATION_CLOSED',
    statusText: '报名关闭',
  })
  const openApplication = ref<OpenApplication | null>(null)
  const openSeasonId = ref('')
  const openRegistrationLoading = ref(false)
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
      platform: IDENTITY_PLATFORM_LABELS[platformKey] || '',
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

  function loadLocalTeamRelations(userId: number) {
    teamRelations.value = safeParseStorage<TeamRelationsData>(
      readStorageItem(teamRelationsStorageKey(userId)),
      getDefaultTeamRelations(userId),
    )
  }

  async function loadOpenRegistration(preferredSeasonId?: string) {
    openRegistrationLoading.value = true
    try {
      const [seasonRes, registrationRes] = await Promise.all([
        getOpenAvailableSeasonPage({ currentPage: 1, pageSize: 20 }),
        getOpenRegistrationPage({ currentPage: 1, pageSize: 10 }),
      ])
      const seasons = seasonRes.data?.records || []
      const registrations = registrationRes.data?.records || []
      const selectedSeason = seasons.find(
        (item) =>
          item.seasonId === preferredSeasonId
          || item.tournamentId === preferredSeasonId,
      ) || seasons[0]

      if (selectedSeason) {
        openTournament.value = mapOpenSeasonToTournament(selectedSeason)
        openSeasonId.value = selectedSeason.seasonId
      } else {
        openTournament.value = { ...CLOSED_OPEN_TOURNAMENT, statusText: '暂无开放报名' }
        openSeasonId.value = ''
      }

      const latestRegistration = registrations[0]
      if (latestRegistration?.registrationId) {
        openApplication.value = {
          ...mapOpenDetailToApplication({
            registrationId: latestRegistration.registrationId,
            registrationNo: latestRegistration.registrationNo,
            seasonId: latestRegistration.seasonId,
            seasonName: latestRegistration.seasonName,
            tournamentName: latestRegistration.tournamentName,
            platform: latestRegistration.platform,
            displayGameId: latestRegistration.displayGameId,
            registrationStatus: latestRegistration.registrationStatus,
            registrationStatusText: latestRegistration.registrationStatusText,
          }),
          playerNo: certifiedPlayerForOpen.value?.playerNo || '',
          nickname: certifiedPlayerForOpen.value?.nickname || '',
        }
      } else if (selectedSeason?.hasRegistration && selectedSeason.registrationId) {
        openApplication.value = {
          applicationNo: '',
          tournamentId: selectedSeason.tournamentId || selectedSeason.seasonId,
          tournamentName: selectedSeason.tournamentName || selectedSeason.seasonName || '',
          applicationType: 'INDIVIDUAL',
          applicationTypeText: '个人报名',
          status: 'NOT_SUBMITTED',
          statusText: '未提交',
          playerNo: certifiedPlayerForOpen.value?.playerNo || '',
          nickname: certifiedPlayerForOpen.value?.nickname || '',
          platform: certifiedPlayerForOpen.value?.platform || '',
          gameId: certifiedPlayerForOpen.value?.gameId || '',
          submitTime: '',
          reviewTime: '',
          reviewRemark: '',
        }
      } else {
        openApplication.value = {
          applicationNo: '',
          tournamentId: openTournament.value.id,
          tournamentName: openTournament.value.name,
          applicationType: 'INDIVIDUAL',
          applicationTypeText: '个人报名',
          status: 'NOT_SUBMITTED',
          statusText: '未提交',
          playerNo: certifiedPlayerForOpen.value?.playerNo || '',
          nickname: certifiedPlayerForOpen.value?.nickname || '',
          platform: certifiedPlayerForOpen.value?.platform || '',
          gameId: certifiedPlayerForOpen.value?.gameId || '',
          submitTime: '',
          reviewTime: '',
          reviewRemark: '',
        }
      }
    } catch {
      openTournament.value = { ...CLOSED_OPEN_TOURNAMENT, statusText: '加载失败' }
      openApplication.value = null
    } finally {
      openRegistrationLoading.value = false
    }
  }

  async function loadPlayerPlatformState() {
    const status = verification.value?.status
    const isApproved = status === 'APPROVED' || status === 'CHANGE_PENDING'

    if (isApproved) {
      try {
        const [listRes, historyRes] = await Promise.all([
          getPlatformAccountList(),
          getPlatformChangeHistory(),
        ])
        platformGameIds.value = (listRes.data || []).map(mapPlatformAccountItem)
        identityChangeHistory.value = filterApprovedChangeHistory(
          (historyRes.data || []).map(mapPlatformChangeHistoryItem),
        )
      } catch {
        platformGameIds.value = []
        identityChangeHistory.value = []
      }
    } else {
      platformGameIds.value = []
      identityChangeHistory.value = []
    }

    if (status && status !== 'NOT_APPLIED' && status !== 'WITHDRAWN') {
      try {
        const eligRes = await getPlayerEligibilityStatus()
        eligibility.value = mapEligibilityStatus(eligRes.data)
        eligibilityLimited.value = eligRes.data.eligible === false
      } catch {
        eligibility.value = null
        eligibilityLimited.value = false
      }
    } else {
      eligibility.value = null
      eligibilityLimited.value = false
    }
  }

  function ensureSecurityLoaded(userId: number) {
    security.value = safeParseStorage<AccountSecurityData | null>(
      readStorageItem(securityStorageKey(userId)),
      null,
    ) || {
      userId,
      passwordHash: '',
      passwordUpdatedAt: '',
      lastLoginAt: currentAccount.value?.lastLoginAt || '',
      accountStatus: 'NORMAL',
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
        loadLocalTeamRelations(loadedUserId.value)
        ensureSecurityLoaded(loadedUserId.value)
        await Promise.all([
          loadPlayerPlatformState(),
          loadOpenRegistration(),
        ])
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
    platform: string
    gameAccount: string
    positionCode?: string
    gameNickname?: string
    avatarFile?: File
    gameScreenshotFile?: File
  }) {
    const existingAvatar = profile.value?.avatarUrl?.trim() || currentAccount.value?.avatarUrl?.trim()
    if (data.avatarFile) {
      await updateProfile({
        avatarUrl: profile.value?.avatarUrl || '',
        avatarFile: data.avatarFile,
      })
    } else if (!existingAvatar) {
      throw new Error('请先上传球员头像')
    }

    if (!data.gameScreenshotFile) {
      throw new Error('请上传游戏账号证明截图')
    }

    const draftPayload: PlayerCertDraftPayload = {
      gameTerminal: mapPlatformToGameTerminal(data.platform),
      gameAccount: data.gameAccount,
      gameNickname: data.gameNickname || currentAccount.value?.nickname || profile.value?.nickname,
      positionCode: data.positionCode,
    }
    const draft = await saveCertDraft(draftPayload)
    const applicationId = draft.currentApplicationId
    if (!applicationId) throw new Error('未能创建认证申请')

    const uploaded = await uploadAccountFile(data.gameScreenshotFile, 'PLAYER_CERT_GAME_SCREENSHOT')
    await bindCertMaterial(applicationId, uploaded.data.fileId, 'PLAYER_CERT_GAME_SCREENSHOT')

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
    openTournament.value = {
      id: '',
      name: '',
      shortName: '',
      seasonName: '',
      type: 'OPEN',
      platform: '',
      registrationType: 'INDIVIDUAL',
      registrationTypeText: '个人报名',
      registrationDeadline: '',
      quotaTotal: 0,
      quotaUsed: 0,
      quotaLeft: 0,
      status: 'REGISTRATION_CLOSED',
      statusText: '报名关闭',
    }
    openApplication.value = null
    openSeasonId.value = ''
    eligibilityLimited.value = false
    loadedUserId.value = null
    overviewError.value = ''
    pendingTasks.value = []
    privacySetting.value = null
    notificationSetting.value = null
  }

  function submitPlayerVerification(data: PlayerVerificationSnapshot) {
    return submitPlayerVerificationFromForm({
      platform: mapVerificationPlatform(data.primaryPlatform),
      gameAccount: data.gameId,
      positionCode: data.gamePosition,
      gameNickname: currentAccount.value?.nickname || profile.value?.nickname,
      gameScreenshotFile: dataUrlToFile(data.gameIdScreenshotUrl),
    })
  }

  async function withdrawPlayerVerification() {
    if (!verification.value || !currentAccount.value) return
    if (verification.value.status !== 'PENDING') return
    const res = await withdrawPlayerCertification()
    playerCert.value = res.data
    verification.value = mapPlayerCertToVerification(res.data, currentAccount.value.userId)
    currentAccount.value.playerVerificationStatus = verification.value.status
  }

  function resubmitPlayerVerification(data: PlayerVerificationSnapshot) {
    return submitPlayerVerification(data)
  }

  function submitVerificationChange(data: PlayerVerificationSnapshot) {
    return submitPlayerVerification(data)
  }

  async function submitPlatformGameId(payload: {
    platform: IdentityPlatform
    gameId: string
    proofFileId: string
  }) {
    const record = platformGameIds.value.find((item) => item.platform === payload.platform)
    if (record?.platformAccountId && record.status === 'EFFECTIVE') {
      await changePlatformGameId(record.platformAccountId, {
        newGameId: payload.gameId,
        proofFileId: payload.proofFileId,
      })
    } else {
      await addPlatformAccount({
        platform: payload.platform,
        gameId: payload.gameId,
        proofFileId: payload.proofFileId,
      })
    }
    await loadPlayerPlatformState()
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
    const ruleError = validatePasswordRules(newPassword)
    if (ruleError) return ruleError
    if (newPassword === currentPassword) return '新密码不能与当前密码一致'
    try {
      await changeAccountPassword({
        oldPassword: currentPassword,
        newPassword,
        confirmPassword: newPassword,
      })
      if (security.value) {
        security.value.passwordUpdatedAt = new Date().toISOString()
        persistSecurity()
      }
      return ''
    } catch (error) {
      return error instanceof Error ? error.message : '密码修改失败'
    }
  }

  function getOpenRegistrationBlockReason(): string {
    if (!currentAccount.value?.emailVerified) return '请先完成邮箱验证'
    if (!isVerifiedPlayer.value) return '请先完成球员身份认证'
    if (!certifiedPlayerForOpen.value?.playerNo) return '尚未生成永久球员 ID，无法报名'
    if (!certifiedPlayerForOpen.value.gameId) return '请先绑定并生效对应平台的游戏 ID'
    if (eligibilityLimited.value) return '你在该赛事的参赛资格受限，无法报名'
    if (openTournament.value.quotaLeft <= 0 && openTournament.value.quotaTotal > 0) return '名额已满，无法提交报名'
    if (openTournament.value.status === 'REGISTRATION_CLOSED') return '报名已关闭'
    if (isRegistrationDeadlinePassed(openTournament.value.registrationDeadline)) return '报名已截止'
    if (openApplication.value?.status === 'PENDING') return '当前报名审核中，不能重复提交'
    if (openApplication.value?.status === 'APPROVED') return '你已报名并通过审核，不能重复提交'
    if (openApplication.value?.status === 'CANCELLED') return '该赛事报名已取消'
    return ''
  }

  function canSubmitOpenApplication(): boolean {
    return !getOpenRegistrationBlockReason()
      && (openApplication.value?.status === 'NOT_SUBMITTED' || openApplication.value?.status === 'REJECTED')
  }

  async function submitOpenApplication() {
    if (!canSubmitOpenApplication() || !certifiedPlayerForOpen.value || !currentAccount.value) return
    if (!openSeasonId.value) throw new Error('当前无可报名 Open 赛季')
    const platformKey = certifiedPlayerForOpen.value.platform
    const draft = await createOpenRegistrationDraft({
      seasonId: openSeasonId.value,
      platform: platformKey,
      positionCode: playerCert.value?.positionCode,
    })
    const submitted = await submitOpenRegistration(draft.data.registrationId)
    openApplication.value = {
      ...mapOpenDetailToApplication(submitted.data),
      playerNo: certifiedPlayerForOpen.value.playerNo,
      nickname: certifiedPlayerForOpen.value.nickname,
      platform: certifiedPlayerForOpen.value.platform,
      gameId: certifiedPlayerForOpen.value.gameId,
    }
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
    openSeasonId,
    openRegistrationLoading,
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
    loadPlayerPlatformState,
    acceptTournamentInvitation,
    rejectTournamentInvitation,
    loadOpenRegistration,
    submitOpenApplication,
    getOpenRegistrationBlockReason,
    canSubmitOpenApplication,
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
