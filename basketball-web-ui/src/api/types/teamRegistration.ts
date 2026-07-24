export interface AvailableSeasonItemDto {
  seasonId: string
  seasonNo?: string
  seasonName?: string
  tournamentId?: string
  tournamentName?: string
  seasonStatus?: number
  minRosterCount?: number
  maxRosterCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  hasRegistration?: boolean
  registrationId?: string
}

export interface TeamRegistrationItemDto {
  registrationId: string
  registrationNo?: string
  seasonId?: string
  seasonName?: string
  tournamentName?: string
  registrationStatus?: number
  registrationStatusText?: string
  lineupCount?: number
  canEdit?: boolean
  canSubmit?: boolean
  canWithdraw?: boolean
  submittedTime?: string
  createTime?: string
}

export interface TeamRegistrationPlayerItemDto {
  registrationPlayerId?: string
  teamMemberId: string
  memberId?: string
  playerId?: string
  realName?: string
  gameNickname?: string
  jerseyNo?: string
  isStarting?: number
  isCaptain?: number
  validationStatus?: number
  sortNo?: number
}

export interface TeamRegistrationDetailDto {
  registrationId: string
  registrationNo?: string
  seasonId?: string
  seasonName?: string
  tournamentName?: string
  teamId?: string
  teamName?: string
  contactName?: string
  contactMobileMasked?: string
  contactEmailMasked?: string
  registrationStatus?: number
  registrationStatusText?: string
  lineupCount?: number
  remark?: string
  canEdit?: boolean
  canSubmit?: boolean
  canWithdraw?: boolean
  canDeleteDraft?: boolean
  submittedTime?: string
  registrationEndTime?: string
  rosterItems?: TeamRegistrationPlayerItemDto[]
}

export interface TeamRegistrationAuditItemDto {
  auditId: string
  auditAction?: string
  beforeStatus?: number
  afterStatus?: number
  auditorName?: string
  auditRemark?: string
  auditTime?: string
}

export interface TeamRegistrationDraftReq {
  seasonId: string
  contactName?: string
  contactMobile?: string
  contactEmail?: string
  remark?: string
}

export interface TeamRegistrationUpdateReq {
  contactName?: string
  contactMobile?: string
  contactEmail?: string
  remark?: string
}

export interface TeamRegistrationRosterItemReq {
  teamMemberId: string
  rosterRole?: string
  jerseyNo?: string
  isStarting?: number
  isCaptain?: number
  sortNo?: number
}

export interface TeamRegistrationRosterReq {
  rosterItems: TeamRegistrationRosterItemReq[]
}

export interface TeamRegistrationWithdrawReq {
  withdrawalReason?: string
}
