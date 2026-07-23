import type {
  EventTeam,
  EventTeamPendingTasks,
  EventTournament,
  RosterMember,
} from '@/types/eventTeam'

export const MOCK_TOURNAMENT_ID = '2026-league-s1'

export const mockTournament: EventTournament = {
  tournamentId: MOCK_TOURNAMENT_ID,
  tournamentName: '2026 CNPA League S1',
  phaseName: '常规赛',
  platform: 'Xbox',
  rosterMin: 5,
  rosterMax: 8,
  transferWindowOpen: true,
  transferWindowEndTime: '2026-08-15 23:59',
  freeAgentQuota: 2,
  freeAgentUsed: 0,
}

export const mockEventTeam: EventTeam = {
  eventTeamId: 'evt-team-rng-s1',
  teamId: 'team-rng',
  teamCode: 'RNG',
  teamName: 'RNG',
  editTeamName: 'RNG Esports',
  teamLogoUrl: '',
  permanentTeamNo: 'CNPA-T-000028',
  tournamentId: MOCK_TOURNAMENT_ID,
  tournamentName: '2026 CNPA League S1',
  platform: 'Xbox',
  captainPlayerId: 'player-001',
  captainUserId: 10001,
  captainName: 'ZLZyoung',
  captainPlayerNo: 'CNPA-P-000128',
  rosterCount: 5,
  rosterMax: 8,
  rosterMin: 5,
  stageName: '常规赛',
  rosterStatus: 'ACTIVE',
  rosterStatusText: '生效中',
  teamProfileStatus: 'APPROVED',
  teamProfileStatusText: '已生效',
  captainPermissionStatus: 'ACTIVE',
  captainPermissionText: '权限生效中',
}

export const mockRoster: RosterMember[] = [
  {
    playerId: 'player-001',
    playerNo: 'CNPA-P-000128',
    nickname: 'ZLZyoung',
    avatarUrl: '',
    platform: 'Xbox',
    gameId: 'ZLZyoung2K',
    position: 'PG',
    role: 'CAPTAIN',
    roleText: '赛事队长',
    joinTime: '2026-07-28',
    status: 'ACTIVE',
    statusText: '生效中',
    canRemove: false,
    removeDisabledReason: '需先移交权限',
    eligibleForCaptain: false,
  },
  {
    playerId: 'player-002',
    nickname: 'RNG_02',
    avatarUrl: '',
    platform: 'Xbox',
    gameId: 'RNG02_2K',
    position: 'SG',
    role: 'MEMBER',
    roleText: '队员',
    joinTime: '2026-07-28',
    status: 'ACTIVE',
    statusText: '生效中',
    canRemove: true,
    eligibleForCaptain: true,
  },
  {
    playerId: 'player-003',
    nickname: 'RNG_03',
    avatarUrl: '',
    platform: 'Xbox',
    gameId: 'RNG03_2K',
    position: 'SF',
    role: 'MEMBER',
    roleText: '队员',
    joinTime: '2026-07-28',
    status: 'ACTIVE',
    statusText: '生效中',
    canRemove: true,
    eligibleForCaptain: true,
  },
  {
    playerId: 'player-004',
    nickname: 'RNG_04',
    avatarUrl: '',
    platform: 'Xbox',
    gameId: 'RNG04_2K',
    position: 'PF',
    role: 'MEMBER',
    roleText: '队员',
    joinTime: '2026-07-28',
    status: 'ACTIVE',
    statusText: '生效中',
    canRemove: true,
    eligibleForCaptain: true,
  },
  {
    playerId: 'player-005',
    nickname: 'RNG_05',
    avatarUrl: '',
    platform: 'Xbox',
    gameId: 'RNG05_2K',
    position: 'C',
    role: 'MEMBER',
    roleText: '队员',
    joinTime: '2026-07-28',
    status: 'ACTIVE',
    statusText: '生效中',
    canRemove: true,
    eligibleForCaptain: false,
  },
]

export const mockPendingTasks: EventTeamPendingTasks = {
  transferConfirmCount: 1,
  matchDataTodoCount: 2,
}

export const EVENT_TEAM_STORAGE_PREFIX = 'cnpa_mock_event_team_'

export function eventTeamStorageKey(userId: number, tournamentId: string): string {
  return `${EVENT_TEAM_STORAGE_PREFIX}${userId}_${tournamentId}`
}

export function getDefaultEventTeamState() {
  return {
    tournament: JSON.parse(JSON.stringify(mockTournament)) as EventTournament,
    eventTeam: JSON.parse(JSON.stringify(mockEventTeam)) as EventTeam,
    roster: JSON.parse(JSON.stringify(mockRoster)) as RosterMember[],
    pendingTasks: JSON.parse(JSON.stringify(mockPendingTasks)) as EventTeamPendingTasks,
    profileDraftName: 'RNG Esports',
    profileDraftLogoUrl: '',
  }
}

export type EventTeamPersistedState = ReturnType<typeof getDefaultEventTeamState>

export function isSupportedEventTeamTournament(tournamentId: string): boolean {
  return tournamentId === MOCK_TOURNAMENT_ID
}
