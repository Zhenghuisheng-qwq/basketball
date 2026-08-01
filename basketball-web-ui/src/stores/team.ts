import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getCaptainQualificationCurrent } from '@/api/captainQualification'
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
  const myTeamSummary = ref<TeamSummaryDto | null>(null)
  const myTeams = ref<TeamSummaryDto[]>([])
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

  const hasTeam = computed(() => myTeams.value.length > 0 || Boolean(myTeamSummary.value?.teamId))
  const isCaptainMember = computed(() =>
    Boolean(myTeamSummary.value?.isCaptain || teamDetail.value?.isCaptain),
  )
  const canManageCurrentTeam = computed(() => {
    const grant = captainQualification.value
    const currentTeamId = myTeamSummary.value?.teamId || teamDetail.value?.teamId
    if (!currentTeamId || !grant?.runtimeValid) return false
    if (grant.canManageCurrentTeam != null) {
      return grant.canManageCurrentTeam
    }
    if (grant.teamId) {
      return grant.teamId === currentTeamId
    }
    return Boolean(teamDetail.value?.isCaptain || myTeamSummary.value?.isCaptain)
  })
  const isHistoricalCaptain = computed(() =>
    hasTeam.value && isCaptainMember.value && !canManageCurrentTeam.value,
  )
  const isRegularMember = computed(() =>
    hasTeam.value && !canManageCurrentTeam.value && !isCaptainMember.value,
  )
  const isCaptain = computed(() => canManageCurrentTeam.value)
  const canCreateTeam = computed(() => Boolean(captainQualification.value?.canCreateTeam))
  const canInheritTeam = computed(() => Boolean(captainQualification.value?.canInheritTeam))
  const hasValidCaptainQualification = computed(() => Boolean(captainQualification.value?.runtimeValid))
  const canJoinTeam = computed(() => !hasTeam.value)

  function setError(error: unknown) {
    lastError.value = error instanceof Error ? error.message : '操作失败'
  }

  async function loadQualification(teamId?: string) {
    qualificationLoading.value = true
    lastError.value = ''
    try {
      const res = await getCaptainQualificationCurrent(
        teamId ? { teamId } : undefined,
      )
      captainQualification.value = res.data
      return res.data
    } catch (error) {
      setError(error)
      throw error
    } finally {
      qualificationLoading.value = false
    }
  }

  async function loadMyTeam() {
    myTeamLoading.value = true
    lastError.value = ''
    try {
      const res = await getMyTeamPage({ currentPage: 1, pageSize: 20 })
      const page = unwrapPage(res.data)
      myTeams.value = page.records
      myTeamSummary.value = page.records[0] ?? null
      if (myTeamSummary.value?.teamId) {
        await loadTeamDetail(myTeamSummary.value.teamId)
      } else {
        teamDetail.value = null
      }
      return myTeamSummary.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      myTeamLoading.value = false
    }
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
      myTeamSummary.value = {
        teamId: res.data.teamId,
        teamName: res.data.teamName,
        shortName: res.data.shortName,
        logoUrl: res.data.logoUrl,
        regionName: res.data.regionName,
        gameTerminal: res.data.gameTerminal,
        currentMemberCount: res.data.currentMemberCount,
        maxMemberCount: res.data.maxMemberCount,
        isCaptain: res.data.isCaptain,
        myRole: res.data.myRole,
      }
      await loadQualification(res.data.teamId)
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

  async function loadMembers(teamId: string, params?: { currentPage?: number; pageSize?: number }) {
    membersLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamMemberPage(teamId, { currentPage: 1, pageSize: 50, ...params })
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
      const res = await getMyTeamInvitationPage({ currentPage: 1, pageSize: 20, ...params })
      myInvitations.value = unwrapPage(res.data).records
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
      myInvitations.value = myInvitations.value.filter((item) => item.invitationId !== invitationId)
      await loadMyTeam()
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
    joinableLoading.value = true
    lastError.value = ''
    try {
      const page = params?.currentPage ?? joinablePage.value
      const size = params?.pageSize ?? joinablePageSize.value
      const res = await getJoinableTeamPage({
        currentPage: page,
        pageSize: size,
        keyword: params?.keyword,
      })
      const pageData = unwrapPage(res.data)
      joinableTeams.value = pageData.records
      joinablePage.value = pageData.current || page
      joinablePageSize.value = pageData.size || size
      joinableTotal.value = pageData.total ?? 0
      joinableTotalPages.value = pageData.pages ?? 0
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
      const res = await getMyTeamJoinApplicationPage({ currentPage: 1, pageSize: 20, ...params })
      myJoinApplications.value = unwrapPage(res.data).records
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
      myJoinApplications.value = myJoinApplications.value.filter(
        (item) => item.applicationId !== applicationId,
      )
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

  async function loadInheritableTeams() {
    inheritableLoading.value = true
    lastError.value = ''
    try {
      const res = await getInheritableTeamList()
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

  async function inheritTeamAction(teamId: string) {
    actionLoading.value = true
    lastError.value = ''
    try {
      const res = await bindCaptainGrantToTeam({ teamId })
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
        loadInheritableTeams(),
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
    await loadMyTeam()
    const currentTeamId = myTeamSummary.value?.teamId
    if (currentTeamId) {
      await loadQualification(currentTeamId)
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
    myTeams.value = []
    teamDetail.value = null
    lastError.value = ''
    resetTransientLists()
  }

  return {
    captainQualification,
    myTeamSummary,
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
    myTeams,
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
    loadMyTeam,
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
