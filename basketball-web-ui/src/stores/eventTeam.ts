import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getEventTeamDetail,
  getEventTeamRoster,
  getEventRosterHistory,
  getMyEventTeamList,
} from '@/api/eventParticipation'
import { pageMyMatches, type PlayerMatchTaskItem } from '@/api/matchData'
import {
  getRosterChangeRequestPage,
  submitRosterChangeCaptainTransfer,
  submitRosterChangeFreeAgentAdd,
  submitRosterChangeTransfer,
  withdrawRosterChangeRequest,
} from '@/api/rosterChange'
import type { RosterChangeRequestItemDto } from '@/api/types/rosterChange'
import {
  mapEventRosterMember,
  mapEventTeamDetailToEventTeam,
  mapEventTeamDetailToTournament,
  mapRosterChangeToTransfer,
  mapRosterHistoryItem,
} from '@/utils/participationMappers'
import type {
  EventTeam,
  EventTeamPendingTasks,
  EventTournament,
  FreeAgentInvite,
  RosterMember,
  TransferEligibilityCheck,
  TransferRecord,
} from '@/types/eventTeam'

export const useEventTeamStore = defineStore('eventTeam', () => {
  const loadedSeasonKey = ref('')
  const eventTeamId = ref('')
  const seasonId = ref('')
  const currentUserIsCaptain = ref(false)
  const loading = ref(false)
  const lastError = ref('')

  const tournament = ref<EventTournament>({
    tournamentId: '',
    tournamentName: '',
    phaseName: '',
    platform: '',
    rosterMin: 5,
    rosterMax: 8,
    transferWindowOpen: false,
    transferWindowEndTime: '',
    freeAgentQuota: 0,
    freeAgentUsed: 0,
  })

  const eventTeam = ref<EventTeam>({
    eventTeamId: '',
    teamId: '',
    teamCode: '',
    teamName: '',
    editTeamName: '',
    teamLogoUrl: '',
    permanentTeamNo: '',
    tournamentId: '',
    tournamentName: '',
    platform: '',
    captainPlayerId: '',
    captainUserId: 0,
    captainName: '',
    captainPlayerNo: '',
    rosterCount: 0,
    rosterMax: 8,
    rosterMin: 5,
    stageName: '',
    rosterStatus: 'ACTIVE',
    rosterStatusText: '',
    teamProfileStatus: 'APPROVED',
    teamProfileStatusText: '',
    captainPermissionStatus: 'ACTIVE',
    captainPermissionText: '',
  })

  const roster = ref<RosterMember[]>([])
  const rosterChangeRequests = ref<RosterChangeRequestItemDto[]>([])
  const rosterHistory = ref<ReturnType<typeof mapRosterHistoryItem>[]>([])
  const pendingTasks = ref<EventTeamPendingTasks>({
    transferConfirmCount: 0,
    matchDataTodoCount: 0,
  })

  const freeAgentInvite = ref<FreeAgentInvite>({
    inviteId: '',
    playerId: '',
    playerName: '',
    position: '',
    status: 'WAIT_PLAYER_CONFIRM',
    statusText: '暂无进行中的邀请',
    steps: [],
  })

  const isCaptain = computed(() => currentUserIsCaptain.value)

  const rosterCount = computed(() => roster.value.length)
  const rosterRemaining = computed(() => Math.max(0, eventTeam.value.rosterMax - rosterCount.value))
  const freeAgentRemaining = computed(() =>
    Math.max(0, tournament.value.freeAgentQuota - tournament.value.freeAgentUsed))

  const transfers = computed<TransferRecord[]>(() =>
    rosterChangeRequests.value
      .filter((item) => item.changeType === 'TRANSFER')
      .map((item) => mapRosterChangeToTransfer(item, eventTeamId.value)))

  const incomingTransfers = computed(() =>
    transfers.value.filter((item) => item.type === 'IN'))

  const outgoingTransfers = computed(() =>
    transfers.value.filter((item) => item.type === 'OUT'))

  const captainCandidates = computed(() =>
    roster.value.filter((item) => item.role !== 'CAPTAIN' && item.eligibleForCaptain !== false))

  const transferEligibilityChecks = computed((): TransferEligibilityCheck[] => [
    {
      key: 'window',
      label: '转会窗口开放。',
      passed: tournament.value.transferWindowOpen,
    },
    {
      key: 'target_max',
      label: `目标球队阵容不超过 ${eventTeam.value.rosterMax} 人。`,
      passed: rosterCount.value < eventTeam.value.rosterMax,
    },
    {
      key: 'captain',
      label: '当前用户为本届赛事队长。',
      passed: isCaptain.value,
    },
  ])

  const canSubmitTransfer = computed(() =>
    isCaptain.value
    && tournament.value.transferWindowOpen
    && transferEligibilityChecks.value.every((item) => item.passed))

  function setError(error: unknown) {
    lastError.value = error instanceof Error ? error.message : '加载失败'
  }

  async function resolveEventTeam(seasonOrTournamentKey: string) {
    const listRes = await getMyEventTeamList()
    const teams = listRes.data || []
    const matched = teams.find(
      (item) =>
        item.seasonId === seasonOrTournamentKey
        || item.eventTeamId === seasonOrTournamentKey,
    ) || teams[0]
    if (!matched) {
      throw new Error('未找到本届赛事球队')
    }
    return matched
  }

  async function loadRosterChangeRequests() {
    const res = await getRosterChangeRequestPage({ currentPage: 1, pageSize: 50 })
    rosterChangeRequests.value = res.data?.records || []
    pendingTasks.value.transferConfirmCount = rosterChangeRequests.value.filter(
      (item) => item.requestStatus === 1 || item.requestStatus === 0,
    ).length
  }

  async function loadRosterHistory(seasonKey?: string) {
    const sid = seasonKey || seasonId.value
    if (!sid) return []
    const res = await getEventRosterHistory({
      seasonId: sid,
      eventTeamId: eventTeamId.value || undefined,
    })
    rosterHistory.value = (res.data || []).map(mapRosterHistoryItem)
    return rosterHistory.value
  }

  async function loadMatchDataTodoCount() {
    if (!seasonId.value) {
      pendingTasks.value.matchDataTodoCount = 0
      return
    }
    try {
      const res = await pageMyMatches({ seasonId: seasonId.value, pageNum: 1, pageSize: 100 })
      const records = res.data?.records || []
      pendingTasks.value.matchDataTodoCount = records.filter((item: PlayerMatchTaskItem) => item.canSubmit).length
    } catch {
      pendingTasks.value.matchDataTodoCount = 0
    }
  }

  async function ensureLoaded(seasonOrTournamentKey: string) {
    if (loadedSeasonKey.value === seasonOrTournamentKey && eventTeamId.value) {
      return true
    }
    loading.value = true
    lastError.value = ''
    try {
      const summary = await resolveEventTeam(seasonOrTournamentKey)
      eventTeamId.value = summary.eventTeamId
      seasonId.value = summary.seasonId || seasonOrTournamentKey

      const [detailRes, rosterRes] = await Promise.all([
        getEventTeamDetail(summary.eventTeamId),
        getEventTeamRoster(summary.eventTeamId),
      ])

      tournament.value = mapEventTeamDetailToTournament(detailRes.data)
      eventTeam.value = mapEventTeamDetailToEventTeam(detailRes.data)
      currentUserIsCaptain.value = Boolean(detailRes.data.currentUserIsCaptain)
      roster.value = (rosterRes.data || []).map(mapEventRosterMember)
      eventTeam.value.rosterCount = roster.value.length

      await Promise.all([
        loadRosterChangeRequests(),
        loadRosterHistory(seasonId.value),
        loadMatchDataTodoCount(),
      ])

      const latestFreeAgent = rosterChangeRequests.value.find(
        (item) => item.changeType === 'FREE_AGENT_ADD' && (item.requestStatus ?? 0) < 3,
      )
      if (latestFreeAgent) {
        freeAgentInvite.value = {
          inviteId: latestFreeAgent.requestId,
          playerId: latestFreeAgent.playerId || '',
          playerName: latestFreeAgent.playerId || '待补球员',
          position: '-',
          status: 'WAIT_ADMIN_REVIEW',
          statusText: latestFreeAgent.requestStatusText || '审核中',
          steps: [
            { key: 'submit', text: '已提交', status: 'done' },
            { key: 'review', text: '管理员审核', status: 'current' },
          ],
        }
      }

      loadedSeasonKey.value = seasonOrTournamentKey
      return true
    } catch (error) {
      setError(error)
      return false
    } finally {
      loading.value = false
    }
  }

  function getTransfer(transferId: string) {
    return transfers.value.find((item) => item.transferId === transferId)
  }

  async function submitTransferApplication(payload: {
    sourceEventTeamId: string
    applyReason?: string
  }) {
    if (!canSubmitTransfer.value || !seasonId.value) return false
    await submitRosterChangeTransfer({
      seasonId: seasonId.value,
      sourceEventTeamId: payload.sourceEventTeamId,
      targetEventTeamId: eventTeamId.value,
      applyReason: payload.applyReason,
    })
    await loadRosterChangeRequests()
    return true
  }

  async function submitFreeAgentApplication(payload: {
    playerId: string
    targetRosterRole?: string
    applyReason?: string
  }) {
    if (!isCaptain.value || !seasonId.value) return false
    if (rosterRemaining.value <= 0 || freeAgentRemaining.value <= 0) return false
    if (!tournament.value.transferWindowOpen) return false
    const res = await submitRosterChangeFreeAgentAdd({
      seasonId: seasonId.value,
      targetEventTeamId: eventTeamId.value,
      playerId: payload.playerId,
      targetRosterRole: payload.targetRosterRole || 'MEMBER',
      applyReason: payload.applyReason,
    })
    freeAgentInvite.value = {
      inviteId: res.data.requestId,
      playerId: payload.playerId,
      playerName: payload.playerId,
      position: '-',
      status: 'WAIT_ADMIN_REVIEW',
      statusText: '补员申请已提交',
      steps: [
        { key: 'submit', text: '已提交', status: 'done' },
        { key: 'review', text: '管理员审核', status: 'current' },
      ],
    }
    await loadRosterChangeRequests()
    return true
  }

  async function submitCaptainTransfer(selectedPlayerId: string, applyReason?: string) {
    if (!isCaptain.value || !selectedPlayerId) return false
    await submitRosterChangeCaptainTransfer({
      eventTeamId: eventTeamId.value,
      newCaptainPlayerId: selectedPlayerId,
      applyReason,
    })
    eventTeam.value.captainPermissionStatus = 'PENDING_REVIEW'
    eventTeam.value.captainPermissionText = '移交审核中'
    await loadRosterChangeRequests()
    return true
  }

  async function withdrawChangeRequest(requestId: string) {
    await withdrawRosterChangeRequest(requestId)
    await loadRosterChangeRequests()
  }

  async function refreshRoster() {
    if (!eventTeamId.value) return
    const rosterRes = await getEventTeamRoster(eventTeamId.value)
    roster.value = (rosterRes.data || []).map(mapEventRosterMember)
    eventTeam.value.rosterCount = roster.value.length
  }

  return {
    loadedSeasonKey,
    eventTeamId,
    seasonId,
    currentUserIsCaptain,
    loading,
    lastError,
    tournament,
    eventTeam,
    roster,
    rosterChangeRequests,
    rosterHistory,
    pendingTasks,
    freeAgentInvite,
    isCaptain,
    rosterCount,
    rosterRemaining,
    freeAgentRemaining,
    transfers,
    incomingTransfers,
    outgoingTransfers,
    captainCandidates,
    transferEligibilityChecks,
    canSubmitTransfer,
    ensureLoaded,
    getTransfer,
    submitTransferApplication,
    submitFreeAgentApplication,
    submitCaptainTransfer,
    withdrawChangeRequest,
    loadRosterHistory,
    refreshRoster,
    loadRosterChangeRequests,
  }
})
