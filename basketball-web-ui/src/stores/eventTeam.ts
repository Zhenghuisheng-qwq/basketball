import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getCurrentAuth } from '@/mock/auth'
import {
  eventTeamStorageKey,
  getDefaultEventTeamState,
  isSupportedEventTeamTournament,
  type EventTeamPersistedState,
} from '@/mock/eventTeamMock'
import { mockFreeAgentInvite, mockFreeAgents, searchFreeAgents } from '@/mock/freeAgentMock'
import {
  getTransferById,
  mockInitiateTransferPlayer,
  mockTransfers,
} from '@/mock/transferMock'
import { readStorageItem, writeStorageItem } from '@/utils/safeParseStorage'
import type {
  CaptainTransferDraft,
  FreeAgentInvite,
  RosterMember,
  TransferEligibilityCheck,
  TransferRecord,
} from '@/types/eventTeam'

export const useEventTeamStore = defineStore('eventTeam', () => {
  const loadedTournamentId = ref('')
  const tournament = ref(getDefaultEventTeamState().tournament)
  const eventTeam = ref(getDefaultEventTeamState().eventTeam)
  const roster = ref<RosterMember[]>(getDefaultEventTeamState().roster)
  const pendingTasks = ref(getDefaultEventTeamState().pendingTasks)
  const transfers = ref<TransferRecord[]>(JSON.parse(JSON.stringify(mockTransfers)))
  const freeAgents = ref(JSON.parse(JSON.stringify(mockFreeAgents)))
  const freeAgentInvite = ref<FreeAgentInvite>(JSON.parse(JSON.stringify(mockFreeAgentInvite)))
  const profileDraftName = ref('RNG Esports')
  const profileDraftLogoUrl = ref('')
  const captainTransferDraft = ref<CaptainTransferDraft>({
    status: 'NOT_SUBMITTED',
    selectedPlayerId: '',
    confirmed: false,
  })

  const auth = computed(() => getCurrentAuth())

  const isCaptain = computed(() => {
    if (!auth.value) return false
    return eventTeam.value.captainUserId === auth.value.userId
  })

  const rosterCount = computed(() => roster.value.length)
  const rosterRemaining = computed(() => eventTeam.value.rosterMax - rosterCount.value)
  const freeAgentRemaining = computed(() => tournament.value.freeAgentQuota - tournament.value.freeAgentUsed)

  const incomingTransfers = computed(() =>
    transfers.value.filter((item) => item.type === 'IN' && item.toTeamCode === eventTeam.value.teamCode),
  )

  const outgoingTransfers = computed(() =>
    transfers.value.filter((item) => item.type === 'OUT' && item.fromTeamCode === eventTeam.value.teamCode),
  )

  const captainCandidates = computed(() =>
    roster.value.filter(
      (item) =>
        item.role !== 'CAPTAIN'
        && item.eligibleForCaptain !== false
        && item.status === 'ACTIVE',
    ),
  )

  const transferEligibilityChecks = computed((): TransferEligibilityCheck[] => {
    const player = mockInitiateTransferPlayer
    const targetAfter = rosterCount.value + 1
    const sourceAfter = player.fromTeamRosterCount - 1
    const hasPending = transfers.value.some(
      (item) =>
        item.playerId === player.playerId
        && !['APPROVED', 'REJECTED', 'CANCELLED'].includes(item.status),
    )
    return [
      {
        key: 'target_max',
        label: '目标球队阵容不超过 8 人。',
        passed: targetAfter <= eventTeam.value.rosterMax,
      },
      {
        key: 'source_min',
        label: '原球队转出后不少于 5 人。',
        passed: sourceAfter >= eventTeam.value.rosterMin,
      },
      {
        key: 'not_captain',
        label: '该球员不是队长。',
        passed: true,
      },
      {
        key: 'no_pending',
        label: '无其他待处理转会。',
        passed: !hasPending,
      },
    ]
  })

  const canSubmitTransfer = computed(() =>
    isCaptain.value
    && tournament.value.transferWindowOpen
    && transferEligibilityChecks.value.every((item) => item.passed),
  )

  function persist(userId: number, tournamentId: string) {
    const payload: EventTeamPersistedState & {
      transfers: TransferRecord[]
      freeAgentInvite: FreeAgentInvite
      captainTransferDraft: CaptainTransferDraft
    } = {
      tournament: tournament.value,
      eventTeam: eventTeam.value,
      roster: roster.value,
      pendingTasks: pendingTasks.value,
      profileDraftName: profileDraftName.value,
      profileDraftLogoUrl: profileDraftLogoUrl.value,
      transfers: transfers.value,
      freeAgentInvite: freeAgentInvite.value,
      captainTransferDraft: captainTransferDraft.value,
    }
    writeStorageItem(eventTeamStorageKey(userId, tournamentId), JSON.stringify(payload))
  }

  function loadFromStorage(userId: number, tournamentId: string) {
    const raw = readStorageItem(eventTeamStorageKey(userId, tournamentId))
    if (!raw) return false
    try {
      const parsed = JSON.parse(raw) as EventTeamPersistedState & {
        transfers?: TransferRecord[]
        freeAgentInvite?: FreeAgentInvite
        captainTransferDraft?: CaptainTransferDraft
      }
      tournament.value = parsed.tournament
      eventTeam.value = parsed.eventTeam
      roster.value = parsed.roster
      pendingTasks.value = parsed.pendingTasks
      profileDraftName.value = parsed.profileDraftName
      profileDraftLogoUrl.value = parsed.profileDraftLogoUrl
      if (parsed.transfers) transfers.value = parsed.transfers
      if (parsed.freeAgentInvite) freeAgentInvite.value = parsed.freeAgentInvite
      if (parsed.captainTransferDraft) captainTransferDraft.value = parsed.captainTransferDraft
      eventTeam.value.rosterCount = roster.value.length
      return true
    } catch {
      return false
    }
  }

  function ensureLoaded(tournamentId: string) {
    if (!isSupportedEventTeamTournament(tournamentId)) return false
    const session = getCurrentAuth()
    if (!session) return false
    if (loadedTournamentId.value === tournamentId) return true
    const defaults = getDefaultEventTeamState()
    tournament.value = defaults.tournament
    eventTeam.value = defaults.eventTeam
    roster.value = defaults.roster
    pendingTasks.value = defaults.pendingTasks
    profileDraftName.value = defaults.profileDraftName
    profileDraftLogoUrl.value = defaults.profileDraftLogoUrl
    transfers.value = JSON.parse(JSON.stringify(mockTransfers))
    freeAgentInvite.value = JSON.parse(JSON.stringify(mockFreeAgentInvite))
    captainTransferDraft.value = { status: 'NOT_SUBMITTED', selectedPlayerId: '', confirmed: false }
    loadFromStorage(session.userId, tournamentId)
    loadedTournamentId.value = tournamentId
    return true
  }

  function getTransfer(transferId: string) {
    return transfers.value.find((item) => item.transferId === transferId) || getTransferById(transferId)
  }

  function searchFreeAgentPool(query: string) {
    return searchFreeAgents(query)
  }

  function requestRemoveMember(playerId: string) {
    const member = roster.value.find((item) => item.playerId === playerId)
    if (!member?.canRemove) return false
    if (!tournament.value.transferWindowOpen) return false
    return true
  }

  function sendFreeAgentInvitation(playerId: string) {
    if (!isCaptain.value) return false
    if (rosterRemaining.value <= 0) return false
    if (freeAgentRemaining.value <= 0) return false
    const agent = mockFreeAgents.find((item) => item.playerId === playerId)
    if (!agent) return false
    freeAgentInvite.value = {
      inviteId: `fa-invite-${Date.now()}`,
      playerId: agent.playerId,
      playerName: agent.nickname,
      position: agent.position,
      status: 'WAIT_PLAYER_CONFIRM',
      statusText: '等待球员确认',
      steps: [
        { key: 'player_confirm', text: '球员确认', status: 'current' },
        { key: 'admin_review', text: '管理员审核', status: 'waiting' },
        { key: 'joined', text: '正式加入', status: 'waiting' },
      ],
    }
    persistState()
    return true
  }

  function submitTransferApplication() {
    if (!canSubmitTransfer.value) return false
    persistState()
    return true
  }

  function submitProfileReview() {
    if (!isCaptain.value) return false
    eventTeam.value.teamProfileStatus = 'PENDING_REVIEW'
    eventTeam.value.teamProfileStatusText = '审核中'
    persistState()
    return true
  }

  function submitCaptainTransfer(selectedPlayerId: string, confirmed: boolean) {
    if (!isCaptain.value || !confirmed || !selectedPlayerId) return false
    captainTransferDraft.value = {
      status: 'PENDING_REVIEW',
      selectedPlayerId,
      confirmed: true,
    }
    eventTeam.value.captainPermissionStatus = 'PENDING_REVIEW'
    eventTeam.value.captainPermissionText = '移交审核中'
    persistState()
    return true
  }

  function persistState() {
    const session = getCurrentAuth()
    if (!session || !loadedTournamentId.value) return
    persist(session.userId, loadedTournamentId.value)
  }

  return {
    loadedTournamentId,
    tournament,
    eventTeam,
    roster,
    pendingTasks,
    transfers,
    freeAgents,
    freeAgentInvite,
    profileDraftName,
    profileDraftLogoUrl,
    captainTransferDraft,
    isCaptain,
    rosterCount,
    rosterRemaining,
    freeAgentRemaining,
    incomingTransfers,
    outgoingTransfers,
    captainCandidates,
    transferEligibilityChecks,
    canSubmitTransfer,
    mockInitiateTransferPlayer,
    ensureLoaded,
    getTransfer,
    searchFreeAgentPool,
    requestRemoveMember,
    sendFreeAgentInvitation,
    submitTransferApplication,
    submitProfileReview,
    submitCaptainTransfer,
  }
})
