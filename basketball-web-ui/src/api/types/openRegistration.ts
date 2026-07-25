export interface OpenAvailableSeasonItemDto {
  seasonId: string
  seasonName?: string
  tournamentId?: string
  tournamentName?: string
  registrationStartTime?: string
  registrationEndTime?: string
  hasRegistration?: boolean
  registrationId?: string
}

export interface OpenRegistrationItemDto {
  registrationId: string
  registrationNo?: string
  seasonId?: string
  seasonName?: string
  tournamentName?: string
  platform?: string
  displayGameId?: string
  registrationStatus?: number
  registrationStatusText?: string
  canEdit?: boolean
  canSubmit?: boolean
  canWithdraw?: boolean
  submittedTime?: string
  createTime?: string
}

export interface OpenRegistrationDetailDto {
  registrationId: string
  registrationNo?: string
  seasonId?: string
  seasonName?: string
  tournamentName?: string
  playerId?: string
  playerNo?: string
  playerNickname?: string
  platform?: string
  displayGameId?: string
  positionCode?: string
  registrationStatus?: number
  registrationStatusText?: string
  remark?: string
  canEdit?: boolean
  canSubmit?: boolean
  canWithdraw?: boolean
  submittedTime?: string
  registrationEndTime?: string
}

export interface OpenRegistrationDraftReq {
  seasonId: string
  platform?: string
  positionCode?: string
  remark?: string
}

export interface OpenRegistrationWithdrawReq {
  withdrawalReason?: string
}
