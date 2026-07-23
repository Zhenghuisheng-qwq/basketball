export type EventTeamStatus = 'ACTIVE' | 'LOCKED' | 'RESTRICTED' | 'PENDING_REVIEW'

export type EventTeamRole = 'CAPTAIN' | 'MEMBER'

export type TransferWindowStatus = 'OPEN' | 'CLOSED' | 'LOCKED'

export type TransferStatus =
  | 'DRAFT'
  | 'WAIT_PLAYER_CONFIRM'
  | 'WAIT_ORIGINAL_CAPTAIN_CONFIRM'
  | 'WAIT_ADMIN_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'

export type TransferType = 'IN' | 'OUT'

export type FreeAgentInviteStatus =
  | 'WAIT_PLAYER_CONFIRM'
  | 'WAIT_ADMIN_REVIEW'
  | 'JOINED'
  | 'REJECTED'
  | 'EXPIRED'

export type CaptainTransferStatus = 'NOT_SUBMITTED' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'

export type TeamProfileStatus = 'APPROVED' | 'PENDING_REVIEW' | 'REJECTED'

export interface EventTournament {
  tournamentId: string
  tournamentName: string
  phaseName: string
  platform: string
  rosterMin: number
  rosterMax: number
  transferWindowOpen: boolean
  transferWindowEndTime: string
  freeAgentQuota: number
  freeAgentUsed: number
}

export interface EventTeam {
  eventTeamId: string
  teamId: string
  teamCode: string
  teamName: string
  editTeamName: string
  teamLogoUrl: string
  permanentTeamNo: string
  tournamentId: string
  tournamentName: string
  platform: string
  captainPlayerId: string
  captainUserId: number
  captainName: string
  captainPlayerNo: string
  rosterCount: number
  rosterMax: number
  rosterMin: number
  stageName: string
  rosterStatus: EventTeamStatus
  rosterStatusText: string
  teamProfileStatus: TeamProfileStatus
  teamProfileStatusText: string
  captainPermissionStatus: 'ACTIVE' | 'PENDING_REVIEW'
  captainPermissionText: string
}

export interface RosterMember {
  playerId: string
  playerNo?: string
  nickname: string
  avatarUrl: string
  platform: string
  gameId: string
  position: string
  role: EventTeamRole
  roleText: string
  joinTime: string
  status: EventTeamStatus
  statusText: string
  canRemove: boolean
  removeDisabledReason?: string
  eligibleForCaptain?: boolean
}

export interface TransferStep {
  key: string
  text: string
  status: 'done' | 'current' | 'waiting' | 'failed'
  time?: string
}

export interface TransferRecord {
  transferId: string
  type: TransferType
  typeText: string
  playerId: string
  playerName: string
  avatarUrl: string
  position: string
  platform: string
  gameId: string
  fromTeamCode: string
  toTeamCode: string
  targetCaptainName: string
  status: TransferStatus
  statusText: string
  applyTime?: string
  steps: TransferStep[]
}

export interface FreeAgent {
  playerId: string
  playerNo: string
  nickname: string
  avatarUrl: string
  position: string
  platform: string
  gameId: string
  status: 'AVAILABLE'
  statusText: string
}

export interface FreeAgentInvite {
  inviteId: string
  playerId: string
  playerName: string
  position: string
  status: FreeAgentInviteStatus
  statusText: string
  steps: TransferStep[]
}

export interface EventTeamPendingTasks {
  transferConfirmCount: number
  matchDataTodoCount: number
}

export interface TransferEligibilityCheck {
  key: string
  label: string
  passed: boolean
}

export interface CaptainTransferDraft {
  status: CaptainTransferStatus
  selectedPlayerId: string
  confirmed: boolean
}
