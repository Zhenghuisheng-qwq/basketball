export interface EventTeamSummaryDto {
  eventTeamId: string
  eventTeamNo?: string
  teamId?: string
  teamName?: string
  shortName?: string
  seasonId?: string
  seasonName?: string
  tournamentName?: string
  gameTerminal?: string
  participationStatus?: number
  participationStatusText?: string
  rosterLockStatus?: number
  rosterLockStatusText?: string
  rosterCount?: number
  isCaptain?: boolean
  rosterRole?: string
}

export interface EventTeamDetailDto {
  eventTeamId: string
  eventTeamNo?: string
  teamId?: string
  teamName?: string
  shortName?: string
  description?: string
  seasonId?: string
  seasonName?: string
  tournamentId?: string
  tournamentName?: string
  gameTerminal?: string
  registrationSource?: string
  participationStatus?: number
  participationStatusText?: string
  rosterLockStatus?: number
  rosterLockStatusText?: string
  rosterCount?: number
  maxRosterCount?: number
  minRosterCount?: number
  transferEnabled?: boolean
  freeAgentEnabled?: boolean
  transferEndTime?: string
  freeAgentEndTime?: string
  freeAgentQuota?: number
  freeAgentUsed?: number
  captainPlayerId?: string
  captainNickname?: string
  currentUserIsCaptain?: boolean
  approvedTime?: string
}

export interface EventRosterMemberDto {
  rosterId?: string
  playerId: string
  playerNo?: string
  nickname?: string
  avatarUrl?: string
  platform?: string
  gameId?: string
  position?: string
  rosterRole?: string
  rosterRoleText?: string
  isCaptain?: number
  isStarting?: number
  rosterStatus?: number
  rosterStatusText?: string
  sourceType?: string
  effectiveTime?: string
}

export interface RosterChangeHistoryItemDto {
  recordId: string
  changeType?: string
  changeTypeText?: string
  playerId?: string
  sourceEventTeamId?: string
  targetEventTeamId?: string
  beforeSnapshot?: string
  afterSnapshot?: string
  operateReason?: string
  operatorName?: string
  effectiveTime?: string
  createTime?: string
}
