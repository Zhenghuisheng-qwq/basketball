import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getCaptainQualificationCurrent,
  getCaptainQualificationList,
} from '@/api/captainQualification'
import {
  bindTeamLogo,
  bindCaptainGrantToTeam,
  createTeam,
  getInheritableTeamList,
  getJoinableTeamPage,
  getMyTeamPage,
  getTeamDetail,
  updateTeam,
} from '@/api/accountTeam'
import {
  getCandidatePlayerPage,
  getTeamMemberPage,
  leaveTeam,
  removeTeamMember,
  updateTeamMember,
} from '@/api/teamMember'
import {
  createTeamInvitation,
  getMyTeamInvitationPage,
  getTeamInvitationPage,
  respondTeamInvitation,
} from '@/api/teamInvitation'
import {
  cancelTeamJoinApplication,
  createTeamJoinApplication,
  getMyTeamJoinApplicationPage,
  getTeamJoinApplicationPage,
  processTeamJoinApplication,
} from '@/api/teamJoinApplication'
import {
  createTeamRegistrationDraft,
  deleteTeamRegistrationDraft,
  getAvailableSeasonPage,
  getRegistrationRosterCandidates,
  remindMemberPlatformGameId,
  getSeasonPlatforms,
  getTeamRegistrationAuditPage,
  getTeamRegistrationDetail,
  getTeamRegistrationPage,
  saveTeamRegistrationRoster,
  submitTeamRegistration,
  updateTeamRegistration,
  withdrawTeamRegistration,
} from '@/api/teamRegistration'
import type { CaptainQualificationDto } from '@/api/types/teamCaptain'
import type { InheritableTeamSummaryDto } from '@/api/types/teamInherit'
import type {
  TeamCreateReq,
  TeamDetailDto,
  TeamSummaryDto,
  TeamUpdateReq,
} from '@/api/types/teamAccount'
import type { CandidatePlayerItemDto, TeamMemberItemDto, TeamMemberUpdateReq } from '@/api/types/teamMember'
import type { TeamInvitationCreateReq, TeamInvitationItemDto } from '@/api/types/teamInvitation'
import type { TeamJoinApplicationItemDto } from '@/api/types/teamJoinApplication'
import type {
  AvailableSeasonItemDto,
  RegistrationRosterCandidateDto,
  SeasonPlatformItemDto,
  TeamRegistrationAuditItemDto,
  TeamRegistrationDetailDto,
  TeamRegistrationDraftReq,
  TeamRegistrationItemDto,
  TeamRegistrationRosterReq,
  TeamRegistrationUpdateReq,
  TeamRegistrationWithdrawReq,
} from '@/api/types/teamRegistration'
import type { JoinableTeamPageDto } from '@/api/types/teamJoinInvite'
import type { PageResult } from '@/api/types/common'

function unwrapPage<T>(data: PageResult<T> | undefined): PageResult<T> {
  return data ?? { records: [], total: 0, size: 10, current: 1, pages: 0 }
}

export const useTeamStore = defineStore('team', () => {
  const captainQualification = ref<CaptainQualificationDto | null>(null)
  const captainQualifications = ref<CaptainQualificationDto[]>([])
  const currentTeams = ref<TeamSummaryDto[]>([])
  const historyTeams = ref<TeamSummaryDto[]>([])
  const currentLoading = ref(false)
  const historyLoading = ref(false)
  /** @deprecated Legacy compatibility only — 取 currentTeams[0]，新页面勿再依赖 */
  const myTeamSummary = ref<TeamSummaryDto | null>(null)
  /** @deprecated 兼容旧引用，等同 currentTeams */
  const myTeams = computed(() => currentTeams.value)
  const teamDetail = ref<TeamDetailDto | null>(null)

  const qualificationLoading = ref(false)
  const myTeamLoading = ref(false)
  const teamDetailLoading = ref(false)
  const actionLoading = ref(false)
  const lastError = ref('')

  const members = ref<TeamMemberItemDto[]>([])
  const membersLoading = ref(false)

  const myInvitations = ref<TeamInvitationItemDto[]>([])
  const teamInvitations = ref<TeamInvitationItemDto[]>([])
  const invitationsLoading = ref(false)

  const myJoinApplications = ref<TeamJoinApplicationItemDto[]>([])
  const teamJoinApplications = ref<TeamJoinApplicationItemDto[]>([])
  const joinApplicationsLoading = ref(false)

  const joinableTeams = ref<TeamSummaryDto[]>([])
  const joinableContext = ref<JoinableTeamPageDto | null>(null)
  const joinableEmptyReason = ref('')
  const joinableLoading = ref(false)
  const joinablePage = ref(1)
  const joinablePageSize = ref(10)
  const joinableTotal = ref(0)
  const joinableTotalPages = ref(0)

  const historyPage = ref(1)
  const historyPageSize = ref(10)
  const historyTotal = ref(0)

  const invitePage = ref(1)
  const invitePageSize = ref(10)
  const inviteTotal = ref(0)

  const myApplicationPage = ref(1)
  const myApplicationPageSize = ref(10)
  const myApplicationTotal = ref(0)

  const candidatePlayers = ref<CandidatePlayerItemDto[]>([])
  const candidateEmptyReason = ref('')
  const candidatesLoading = ref(false)

  const availableSeasons = ref<AvailableSeasonItemDto[]>([])
  const availableSeasonEmptyReason = ref('')
  const registrations = ref<TeamRegistrationItemDto[]>([])
  const registrationDetail = ref<TeamRegistrationDetailDto | null>(null)
  const registrationAudits = ref<TeamRegistrationAuditItemDto[]>([])
  const registrationLoading = ref(false)
  const inheritableTeams = ref<InheritableTeamSummaryDto[]>([])
  const inheritableLoading = ref(false)
  const seasonPlatforms = ref<SeasonPlatformItemDto[]>([])
  const rosterCandidates = ref<RegistrationRosterCandidateDto[]>([])
  const rosterCandidatesLoading = ref(false)

  const hasTeam = computed(() => currentTeams.value.length > 0 || Boolean(myTeamSummary.value?.teamId))
  const isCaptainMember = computed(() =>
    Boolean(teamDetail.value?.isCaptain || myTeamSummary.value?.isCaptain),
  )
  const canManageCurrentTeam = computed(() => {
    if (teamDetail.value?.canManage != null) {
      return Boolean(teamDetail.value.canManage)
    }
    const selected = currentTeams.value.find((item) => item.teamId === teamDetail.value?.teamId)
      || currentTeams.value.find((item) => item.teamId === myTeamSummary.value?.teamId)
    if (selected?.canManage != null) {
      return Boolean(selected.canManage)
    }
    return false
  })
  const isHistoricalCaptain = computed(() =>
    Boolean(teamDetail.value?.isHistorical || teamDetail.value?.isCaptain) && !canManageCurrentTeam.value,
  )
  const isRegularMember = computed(() =>
    hasTeam.value && !canManageCurrentTeam.value && !isCaptainMember.value,
  )
  const isCaptain = computed(() => canManageCurrentTeam.value)
  const creatableQualifications = computed(() =>
    captainQualifications.value.filter((item) => item.canCreateTeam),
  )
  const canCreateTeam = computed(
    () =>
      creatableQualifications.value.length > 0 ||
      Boolean(captainQualification.value?.canCreateTeam),
  )
  const canInheritTeam = computed(() => false)
  const hasValidCaptainQualification = computed(() =>
    captainQualifications.value.some((item) => item.runtimeValid) ||
    Boolean(captainQualification.value?.runtimeValid),
  )
  // 同 Division 一人一队由 joinable / 后端门禁判断，不再用全局 hasTeam 阻断
  const canJoinTeam = computed(() => true)

  function setError(error: unknown) {
    lastError.value = error instanceof Error ? error.message : '操作失败'
  }

  async function loadQualification(teamId?: string, divisionId?: string) {
    qualificationLoading.value = true
    lastError.value = ''
    try {
      const [currentRes, listRes] = await Promise.all([
        getCaptainQualificationCurrent({
          teamId,
          divisionId,
        }),
        getCaptainQualificationList().catch(() => ({ data: [] as CaptainQualificationDto[] })),
      ])
      captainQualification.value = currentRes.data
      captainQualifications.value = listRes.data ?? []
      return currentRes.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      qualificationLoading.value = false
    }
  }

  async function loadCurrentTeams(params?: { currentPage?: number; pageSize?: number }) {
    currentLoading.value = true
    myTeamLoading.value = true
    lastError.value = ''
    try {
      const res = await getMyTeamPage({ currentPage: 1, pageSize: 50, scope: 'current', ...params })
      const page = unwrapPage(res.data)
      currentTeams.value = page.records
      const preferredId = myTeamSummary.value?.teamId || teamDetail.value?.teamId
      myTeamSummary.value =
        (preferredId ? page.records.find((item) => item.teamId === preferredId) : undefined) ??
        page.records[0] ??
        null
      return currentTeams.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      currentLoading.value = false
      myTeamLoading.value = false
    }
  }

  async function loadHistoryTeams(params?: { currentPage?: number; pageSize?: number }) {
    historyLoading.value = true
    lastError.value = ''
    try {
      let pageNum = params?.currentPage ?? historyPage.value
      const size = params?.pageSize ?? historyPageSize.value
      if (pageNum < 1) pageNum = 1
      const res = await getMyTeamPage({ currentPage: pageNum, pageSize: size, scope: 'history' })
      const page = unwrapPage(res.data)
      const total = Number(page.total ?? 0)
      const resolvedSize = Number(page.size || size) || 10
      const totalPages = Math.max(0, Math.ceil(total / resolvedSize))
      let current = Number(page.current || pageNum) || 1
      if (total > 0 && current > totalPages) {
        historyLoading.value = false
        return loadHistoryTeams({ currentPage: totalPages, pageSize: resolvedSize })
      }
      historyTeams.value = page.records ?? []
      historyPage.value = current
      historyPageSize.value = resolvedSize
      historyTotal.value = total
      return historyTeams.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      historyLoading.value = false
    }
  }

  /** @deprecated Legacy compatibility only — 加载当前球队列表 */
  async function loadMyTeam() {
    await loadCurrentTeams()
    if (myTeamSummary.value?.teamId) {
      await loadTeamDetail(myTeamSummary.value.teamId)
    } else {
      teamDetail.value = null
    }
    return myTeamSummary.value
  }

  async function selectTeam(teamId: string) {
    const fromCurrent = currentTeams.value.find((item) => item.teamId === teamId)
    const fromHistory = historyTeams.value.find((item) => item.teamId === teamId)
    myTeamSummary.value = fromCurrent || fromHistory || myTeamSummary.value
    return loadTeamDetail(teamId)
  }

  async function loadTeamDetail(teamId: string) {
    teamDetailLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamDetail(teamId)
      teamDetail.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      teamDetailLoading.value = false
    }
  }

  async function createTeamAction(body: TeamCreateReq) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await createTeam(body)
      teamDetail.value = res.data
      await Promise.all([
        loadQualification(res.data.teamId, res.data.divisionId),
        loadCurrentTeams(),
      ])
      myTeamSummary.value =
        currentTeams.value.find((item) => item.teamId === res.data.teamId) ?? {
          teamId: res.data.teamId,
          divisionId: res.data.divisionId,
          teamName: res.data.teamName,
          shortName: res.data.shortName,
          logoUrl: res.data.logoUrl,
          regionName: res.data.regionName,
          gameTerminal: res.data.gameTerminal,
          currentMemberCount: res.data.currentMemberCount,
          maxMemberCount: res.data.maxMemberCount,
          isCaptain: res.data.isCaptain,
          myRole: res.data.myRole,
          canManage: res.data.canManage,
        }
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function updateTeamAction(teamId: string, body: TeamUpdateReq) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await updateTeam(teamId, body)
      teamDetail.value = res.data
      if (myTeamSummary.value?.teamId === teamId) {
        myTeamSummary.value = {
          ...myTeamSummary.value,
          teamName: res.data.teamName,
          shortName: res.data.shortName,
          logoUrl: res.data.logoUrl,
          regionName: res.data.regionName,
          gameTerminal: res.data.gameTerminal,
        }
      }
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function bindLogoAction(teamId: string, fileId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await bindTeamLogo(teamId, { fileId })
      teamDetail.value = res.data
      if (myTeamSummary.value?.teamId === teamId) {
        myTeamSummary.value = { ...myTeamSummary.value, logoUrl: res.data.logoUrl }
      }
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function loadMembers(
    teamId: string,
    params?: { currentPage?: number; pageSize?: number; includeHistory?: boolean },
  ) {
    membersLoading.value = true
    lastError.value = ''
    try {
      const historical = Boolean(teamDetail.value?.isHistorical)
        || Boolean(historyTeams.value.some((item) => item.teamId === teamId))
      const res = await getTeamMemberPage(teamId, {
        currentPage: 1,
        pageSize: 50,
        includeHistory: historical || params?.includeHistory,
        ...params,
      })
      members.value = unwrapPage(res.data).records
      return members.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      membersLoading.value = false
    }
  }

  async function updateMemberAction(
    teamId: string,
    teamMemberId: string,
    body: TeamMemberUpdateReq,
  ) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await updateTeamMember(teamId, teamMemberId, body)
      const index = members.value.findIndex((item) => item.teamMemberId === teamMemberId)
      if (index >= 0) members.value[index] = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function removeMemberAction(teamId: string, teamMemberId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      await removeTeamMember(teamId, teamMemberId)
      members.value = members.value.filter((item) => item.teamMemberId !== teamMemberId)
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function leaveTeamAction(teamId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      await leaveTeam(teamId)
      myTeamSummary.value = null
      teamDetail.value = null
      members.value = []
      await Promise.all([loadCurrentTeams(), loadHistoryTeams().catch(() => undefined)])
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function loadMyInvitations(params?: { currentPage?: number; pageSize?: number; status?: number }) {
    invitationsLoading.value = true
    lastError.value = ''
    try {
      let pageNum = params?.currentPage ?? invitePage.value
      const size = params?.pageSize ?? invitePageSize.value
      if (pageNum < 1) pageNum = 1
      const res = await getMyTeamInvitationPage({
        currentPage: pageNum,
        pageSize: size,
        status: params?.status,
      })
      const pageData = unwrapPage(res.data)
      const total = Number(pageData.total ?? 0)
      const resolvedSize = Number(pageData.size || size) || 10
      const totalPages = Math.max(0, Math.ceil(total / resolvedSize))
      let current = Number(pageData.current || pageNum) || 1
      if (total > 0 && current > totalPages) {
        invitationsLoading.value = false
        return loadMyInvitations({ currentPage: totalPages, pageSize: resolvedSize, status: params?.status })
      }
      myInvitations.value = pageData.records ?? []
      invitePage.value = current
      invitePageSize.value = resolvedSize
      inviteTotal.value = total
      return myInvitations.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      invitationsLoading.value = false
    }
  }

  async function loadTeamInvitations(
    teamId: string,
    params?: { currentPage?: number; pageSize?: number; status?: number },
  ) {
    invitationsLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamInvitationPage(teamId, { currentPage: 1, pageSize: 20, ...params })
      const pageData = unwrapPage(res.data)
      teamInvitations.value = pageData.records
      return pageData
    } catch (error) {
      setError(error)
      throw error
    } finally {
      invitationsLoading.value = false
    }
  }

  async function sendInvitationAction(teamId: string, body: TeamInvitationCreateReq) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await createTeamInvitation(teamId, body)
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function respondInvitationAction(invitationId: string, accept: boolean, responseRemark?: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      await respondTeamInvitation(invitationId, {
        action: accept ? 'ACCEPT' : 'REJECT',
        accept,
        responseRemark,
      })
      await Promise.all([
        loadMyInvitations({
          currentPage: invitePage.value,
          pageSize: invitePageSize.value,
          status: 0,
        }),
        loadMyTeam(),
      ])
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function loadJoinableTeams(params?: {
    currentPage?: number
    pageSize?: number
    keyword?: string
  }) {
    if (joinableLoading.value) {
      return joinableContext.value
    }
    joinableLoading.value = true
    lastError.value = ''
    try {
      let page = params?.currentPage ?? joinablePage.value
      const size = params?.pageSize ?? joinablePageSize.value
      if (page < 1) page = 1

      const res = await getJoinableTeamPage({
        currentPage: page,
        pageSize: size,
        keyword: params?.keyword,
      })
      const pageData = unwrapPage(res.data)
      const total = Number(pageData.total ?? 0)
      const resolvedSize = Number(pageData.size || size) || 10
      const totalPages = Math.max(0, Math.ceil(total / resolvedSize) || Number(pageData.pages ?? 0))
      let current = Number(pageData.current || page) || 1

      // 操作后当前页被抽空时，回退到合法页再查一次
      if (total > 0 && current > totalPages) {
        joinableLoading.value = false
        return loadJoinableTeams({
          currentPage: totalPages,
          pageSize: resolvedSize,
          keyword: params?.keyword,
        })
      }

      joinableTeams.value = pageData.records ?? []
      joinablePage.value = current
      joinablePageSize.value = resolvedSize
      joinableTotal.value = total
      joinableTotalPages.value = totalPages
      joinableContext.value = res.data ?? null
      joinableEmptyReason.value = res.data?.emptyReason || res.data?.disabledReason || ''
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      joinableLoading.value = false
    }
  }

  async function loadMyJoinApplications(params?: { currentPage?: number; pageSize?: number; status?: number }) {
    joinApplicationsLoading.value = true
    lastError.value = ''
    try {
      let pageNum = params?.currentPage ?? myApplicationPage.value
      const size = params?.pageSize ?? myApplicationPageSize.value
      if (pageNum < 1) pageNum = 1
      const res = await getMyTeamJoinApplicationPage({
        currentPage: pageNum,
        pageSize: size,
        status: params?.status,
      })
      const pageData = unwrapPage(res.data)
      const total = Number(pageData.total ?? 0)
      const resolvedSize = Number(pageData.size || size) || 10
      const totalPages = Math.max(0, Math.ceil(total / resolvedSize))
      let current = Number(pageData.current || pageNum) || 1
      if (total > 0 && current > totalPages) {
        joinApplicationsLoading.value = false
        return loadMyJoinApplications({
          currentPage: totalPages,
          pageSize: resolvedSize,
          status: params?.status,
        })
      }
      myJoinApplications.value = pageData.records ?? []
      myApplicationPage.value = current
      myApplicationPageSize.value = resolvedSize
      myApplicationTotal.value = total
      return myJoinApplications.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      joinApplicationsLoading.value = false
    }
  }

  async function loadTeamJoinApplications(
    teamId: string,
    params?: { currentPage?: number; pageSize?: number; status?: number },
  ) {
    joinApplicationsLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamJoinApplicationPage(teamId, { currentPage: 1, pageSize: 20, ...params })
      const pageData = unwrapPage(res.data)
      teamJoinApplications.value = pageData.records
      return pageData
    } catch (error) {
      setError(error)
      throw error
    } finally {
      joinApplicationsLoading.value = false
    }
  }

  async function applyJoinTeamAction(teamId: string, applyReason?: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await createTeamJoinApplication(teamId, { applyReason })
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function cancelJoinApplicationAction(applicationId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      await cancelTeamJoinApplication(applicationId)
      await loadMyJoinApplications({
        currentPage: myApplicationPage.value,
        pageSize: myApplicationPageSize.value,
      })
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function processJoinApplicationAction(
    applicationId: string,
    approve: boolean,
    processRemark?: string,
  ) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await processTeamJoinApplication(applicationId, {
        action: approve ? 'APPROVE' : 'REJECT',
        approve,
        processRemark,
      })
      const index = teamJoinApplications.value.findIndex(
        (item) => item.applicationId === applicationId,
      )
      if (index >= 0) teamJoinApplications.value[index] = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function loadInheritableTeams(divisionId?: string) {
    inheritableLoading.value = true
    lastError.value = ''
    try {
      const res = await getInheritableTeamList(divisionId)
      inheritableTeams.value = res.data ?? []
      return inheritableTeams.value
    } catch (error) {
      inheritableTeams.value = []
      setError(error)
      throw error
    } finally {
      inheritableLoading.value = false
    }
  }

  async function inheritTeamAction(teamId: string, divisionId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await bindCaptainGrantToTeam({ teamId, divisionId })
      teamDetail.value = res.data
      myTeamSummary.value = {
        teamId: res.data.teamId,
        teamName: res.data.teamName,
        shortName: res.data.shortName,
        logoUrl: res.data.logoUrl,
        regionName: res.data.regionName,
        gameTerminal: res.data.gameTerminal,
        currentMemberCount: res.data.currentMemberCount,
        maxMemberCount: res.data.maxMemberCount,
        isCaptain: true,
        myRole: res.data.myRole,
      }
      await Promise.all([
        loadQualification(res.data.teamId),
        loadMyTeam(),
        loadInheritableTeams(divisionId),
      ])
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function loadSeasonPlatforms(seasonId: string) {
    try {
      const res = await getSeasonPlatforms(seasonId)
      seasonPlatforms.value = res.data ?? []
      return seasonPlatforms.value
    } catch (error) {
      seasonPlatforms.value = []
      setError(error)
      throw error
    }
  }

  async function loadRosterCandidates(registrationId: string) {
    rosterCandidatesLoading.value = true
    lastError.value = ''
    try {
      const res = await getRegistrationRosterCandidates(registrationId)
      rosterCandidates.value = res.data ?? []
      return rosterCandidates.value
    } catch (error) {
      rosterCandidates.value = []
      setError(error)
      throw error
    } finally {
      rosterCandidatesLoading.value = false
    }
  }

  async function loadCandidatePlayers(
    teamId: string,
    params?: { currentPage?: number; pageSize?: number; keyword?: string },
  ) {
    candidatesLoading.value = true
    lastError.value = ''
    try {
      const res = await getCandidatePlayerPage(teamId, { currentPage: 1, pageSize: 20, ...params })
      const pageData = unwrapPage(res.data)
      candidatePlayers.value = pageData.records
      candidateEmptyReason.value = res.data?.emptyReason || ''
      return pageData
    } catch (error) {
      setError(error)
      throw error
    } finally {
      candidatesLoading.value = false
    }
  }

  async function loadAvailableSeasons(
    teamId: string,
    params?: { currentPage?: number; pageSize?: number },
  ) {
    registrationLoading.value = true
    lastError.value = ''
    try {
      const res = await getAvailableSeasonPage(teamId, { currentPage: 1, pageSize: 20, ...params })
      const pageData = unwrapPage(res.data)
      availableSeasons.value = pageData.records
      availableSeasonEmptyReason.value = res.data?.emptyReason || ''
      return pageData
    } catch (error) {
      setError(error)
      throw error
    } finally {
      registrationLoading.value = false
    }
  }

  async function loadRegistrations(
    teamId: string,
    params?: { currentPage?: number; pageSize?: number; status?: number },
  ) {
    registrationLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamRegistrationPage(teamId, { currentPage: 1, pageSize: 20, ...params })
      const pageData = unwrapPage(res.data)
      registrations.value = pageData.records
      return pageData
    } catch (error) {
      setError(error)
      throw error
    } finally {
      registrationLoading.value = false
    }
  }

  async function loadRegistrationDetail(registrationId: string) {
    registrationLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamRegistrationDetail(registrationId)
      registrationDetail.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      registrationLoading.value = false
    }
  }

  async function loadRegistrationAudits(registrationId: string) {
    registrationLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamRegistrationAuditPage(registrationId, { currentPage: 1, pageSize: 20 })
      registrationAudits.value = unwrapPage(res.data).records
      return registrationAudits.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      registrationLoading.value = false
    }
  }

  async function createRegistrationDraftAction(teamId: string, body: TeamRegistrationDraftReq) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await createTeamRegistrationDraft(teamId, body)
      registrationDetail.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function updateRegistrationAction(registrationId: string, body: TeamRegistrationUpdateReq) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await updateTeamRegistration(registrationId, body)
      registrationDetail.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function saveRegistrationRosterAction(registrationId: string, body: TeamRegistrationRosterReq) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await saveTeamRegistrationRoster(registrationId, body)
      registrationDetail.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function submitRegistrationAction(registrationId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await submitTeamRegistration(registrationId)
      registrationDetail.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function withdrawRegistrationAction(
    registrationId: string,
    body?: TeamRegistrationWithdrawReq,
  ) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await withdrawTeamRegistration(registrationId, body)
      registrationDetail.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function remindMemberPlatformGameIdAction(registrationId: string, teamMemberId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await remindMemberPlatformGameId(registrationId, teamMemberId)
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function deleteRegistrationDraftAction(registrationId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      await deleteTeamRegistrationDraft(registrationId)
      registrations.value = registrations.value.filter(
        (item) => item.registrationId !== registrationId,
      )
      if (registrationDetail.value?.registrationId === registrationId) {
        registrationDetail.value = null
      }
    } catch (error) {
      setError(error)
      throw error
    } finally {
      actionLoading.value = false
    }
  }

  async function bootstrap() {
    await loadQualification()
    await loadCurrentTeams()
    const currentTeamId = myTeamSummary.value?.teamId
    if (currentTeamId) {
      await Promise.all([
        loadTeamDetail(currentTeamId),
        loadQualification(currentTeamId, myTeamSummary.value?.divisionId),
      ])
    } else {
      teamDetail.value = null
    }
  }

  function resetTransientLists() {
    members.value = []
    myInvitations.value = []
    teamInvitations.value = []
    myJoinApplications.value = []
    teamJoinApplications.value = []
    joinableTeams.value = []
    joinableContext.value = null
    candidatePlayers.value = []
    availableSeasons.value = []
    availableSeasonEmptyReason.value = ''
    registrations.value = []
    registrationDetail.value = null
    registrationAudits.value = []
  }

  function resetState() {
    captainQualification.value = null
    myTeamSummary.value = null
    currentTeams.value = []
    historyTeams.value = []
    teamDetail.value = null
    lastError.value = ''
    resetTransientLists()
  }

  return {
    captainQualification,
    captainQualifications,
    creatableQualifications,
    currentTeams,
    historyTeams,
    currentLoading,
    historyLoading,
    myTeamSummary,
    myTeams,
    teamDetail,
    qualificationLoading,
    myTeamLoading,
    teamDetailLoading,
    actionLoading,
    lastError,
    members,
    membersLoading,
    myInvitations,
    teamInvitations,
    invitationsLoading,
    myJoinApplications,
    teamJoinApplications,
    joinApplicationsLoading,
    joinableTeams,
    joinableContext,
    joinableEmptyReason,
    joinableLoading,
    joinablePage,
    joinablePageSize,
    joinableTotal,
    joinableTotalPages,
    historyPage,
    historyPageSize,
    historyTotal,
    invitePage,
    invitePageSize,
    inviteTotal,
    myApplicationPage,
    myApplicationPageSize,
    myApplicationTotal,
    inheritableTeams,
    inheritableLoading,
    seasonPlatforms,
    rosterCandidates,
    rosterCandidatesLoading,
    candidatePlayers,
    candidateEmptyReason,
    candidatesLoading,
    availableSeasons,
    availableSeasonEmptyReason,
    registrations,
    registrationDetail,
    registrationAudits,
    registrationLoading,
    hasTeam,
    isCaptain,
    isCaptainMember,
    isHistoricalCaptain,
    isRegularMember,
    canManageCurrentTeam,
    canCreateTeam,
    canInheritTeam,
    hasValidCaptainQualification,
    canJoinTeam,
    loadQualification,
    loadCurrentTeams,
    loadHistoryTeams,
    loadMyTeam,
    selectTeam,
    loadTeamDetail,
    createTeamAction,
    updateTeamAction,
    bindLogoAction,
    loadMembers,
    updateMemberAction,
    removeMemberAction,
    leaveTeamAction,
    loadMyInvitations,
    loadTeamInvitations,
    sendInvitationAction,
    respondInvitationAction,
    loadJoinableTeams,
    loadMyJoinApplications,
    loadTeamJoinApplications,
    applyJoinTeamAction,
    cancelJoinApplicationAction,
    processJoinApplicationAction,
    loadInheritableTeams,
    inheritTeamAction,
    loadSeasonPlatforms,
    loadRosterCandidates,
    remindMemberPlatformGameIdAction,
    loadCandidatePlayers,
    loadAvailableSeasons,
    loadRegistrations,
    loadRegistrationDetail,
    loadRegistrationAudits,
    createRegistrationDraftAction,
    updateRegistrationAction,
    saveRegistrationRosterAction,
    submitRegistrationAction,
    withdrawRegistrationAction,
    deleteRegistrationDraftAction,
    bootstrap,
    resetTransientLists,
    resetState,
  }
})
