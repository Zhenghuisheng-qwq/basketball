export interface RosterChangeRequestItemDto {
  requestId: string
  requestNo?: string
  seasonId?: string
  changeType?: string
  changeTypeText?: string
  requestStatus?: number
  requestStatusText?: string
  playerId?: string
  sourceEventTeamId?: string
  targetEventTeamId?: string
  applyReason?: string
  rejectReason?: string
  canWithdraw?: boolean
  submitTime?: string
  reviewedTime?: string
}

export interface RosterChangeSubmitRespDto {
  requestId: string
  requestNo?: string
}

export interface RosterChangeTransferReq {
  seasonId: string
  sourceEventTeamId: string
  targetEventTeamId: string
  applyReason?: string
}

export interface RosterChangeFreeAgentAddReq {
  seasonId: string
  targetEventTeamId: string
  playerId: string
  targetRosterRole?: string
  targetIsStarting?: number
  targetIsSubstitute?: number
  applyReason?: string
}

export interface RosterChangeCaptainTransferReq {
  eventTeamId: string
  newCaptainPlayerId: string
  applyReason?: string
}
