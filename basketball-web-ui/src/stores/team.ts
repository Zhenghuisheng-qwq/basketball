import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getCaptainQualificationCurrent } from '@/api/captainQualification'
import {
  bindTeamLogo,
  createTeam,
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
  getTeamRegistrationAuditPage,
  getTeamRegistrationDetail,
  getTeamRegistrationPage,
  saveTeamRegistrationRoster,
  submitTeamRegistration,
  updateTeamRegistration,
  withdrawTeamRegistration,
} from '@/api/teamRegistration'
import type { CaptainQualificationDto } from '@/api/types/teamCaptain'
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
  TeamRegistrationAuditItemDto,
  TeamRegistrationDetailDto,
  TeamRegistrationDraftReq,
  TeamRegistrationItemDto,
  TeamRegistrationRosterReq,
  TeamRegistrationUpdateReq,
  TeamRegistrationWithdrawReq,
} from '@/api/types/teamRegistration'
import type { PageResult } from '@/api/types/common'

function unwrapPage<T>(data: PageResult<T> | undefined): PageResult<T> {
  return data ?? { records: [], total: 0, size: 10, current: 1, pages: 0 }
}

export const useTeamStore = defineStore('team', () => {
  const captainQualification = ref<CaptainQualificationDto | null>(null)
  const myTeamSummary = ref<TeamSummaryDto | null>(null)
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
  const joinableLoading = ref(false)

  const candidatePlayers = ref<CandidatePlayerItemDto[]>([])
  const candidatesLoading = ref(false)

  const availableSeasons = ref<AvailableSeasonItemDto[]>([])
  const availableSeasonEmptyReason = ref('')
  const registrations = ref<TeamRegistrationItemDto[]>([])
  const registrationDetail = ref<TeamRegistrationDetailDto | null>(null)
  const registrationAudits = ref<TeamRegistrationAuditItemDto[]>([])
  const registrationLoading = ref(false)

  const hasTeam = computed(() => Boolean(myTeamSummary.value?.teamId))
  const isCaptain = computed(() =>
    Boolean(myTeamSummary.value?.isCaptain || teamDetail.value?.isCaptain),
  )
  const canCreateTeam = computed(() => Boolean(captainQualification.value?.canCreateTeam))
  const hasValidCaptainQualification = computed(() => canCreateTeam.value)
  const canJoinTeam = computed(() => !hasTeam.value && !canCreateTeam.value)

  function setError(error: unknown) {
    lastError.value = error instanceof Error ? error.message : '操作失败'
  }

  async function loadQualification() {
    qualificationLoading.value = true
    lastError.value = ''
    try {
      const res = await getCaptainQualificationCurrent()
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
      const res = await getMyTeamPage({ currentPage: 1, pageSize: 1 })
      const page = unwrapPage(res.data)
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
      teamInvitations.value = unwrapPage(res.data).records
      return teamInvitations.value
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

  async function loadJoinableTeams(params?: { currentPage?: number; pageSize?: number; keyword?: string }) {
    joinableLoading.value = true
    lastError.value = ''
    try {
      const res = await getJoinableTeamPage({ currentPage: 1, pageSize: 20, ...params })
      joinableTeams.value = unwrapPage(res.data).records
      return joinableTeams.value
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
      teamJoinApplications.value = unwrapPage(res.data).records
      return teamJoinApplications.value
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

  async function loadCandidatePlayers(
    teamId: string,
    params?: { currentPage?: number; pageSize?: number; keyword?: string },
  ) {
    candidatesLoading.value = true
    lastError.value = ''
    try {
      const res = await getCandidatePlayerPage(teamId, { currentPage: 1, pageSize: 20, ...params })
      candidatePlayers.value = unwrapPage(res.data).records
      return candidatePlayers.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      candidatesLoading.value = false
    }
  }

  async function loadAvailableSeasons(teamId: string) {
    registrationLoading.value = true
    lastError.value = ''
    try {
      const res = await getAvailableSeasonPage(teamId, { currentPage: 1, pageSize: 20 })
      availableSeasons.value = unwrapPage(res.data).records
      availableSeasonEmptyReason.value = res.data?.emptyReason || ''
      return availableSeasons.value
    } catch (error) {
      setError(error)
      throw error
    } finally {
      registrationLoading.value = false
    }
  }

  async function loadRegistrations(teamId: string, status?: number) {
    registrationLoading.value = true
    lastError.value = ''
    try {
      const res = await getTeamRegistrationPage(teamId, { currentPage: 1, pageSize: 20, status })
      registrations.value = unwrapPage(res.data).records
      return registrations.value
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
    await Promise.all([loadQualification(), loadMyTeam()])
  }

  function resetTransientLists() {
    members.value = []
    myInvitations.value = []
    teamInvitations.value = []
    myJoinApplications.value = []
    teamJoinApplications.value = []
    joinableTeams.value = []
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
    joinableLoading,
    candidatePlayers,
    candidatesLoading,
    availableSeasons,
    availableSeasonEmptyReason,
    registrations,
    registrationDetail,
    registrationAudits,
    registrationLoading,
    hasTeam,
    isCaptain,
    canCreateTeam,
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
