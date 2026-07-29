import type { PageResult } from '@/api/types/common'

export interface AvailableSeasonItemDto {
  seasonId: string
  seasonNo?: string
  seasonName?: string
  competitionSeasonId?: string | number
  competitionSeasonName?: string
  tournamentId?: string
  tournamentNo?: string
  tournamentName?: string
  gameTerminal?: string
  tournamentStatus?: number
  seasonStatus?: number
  registrationStatus?: string
  teamId?: string
  teamName?: string
  teamCertificationStatus?: number
  canRegister?: boolean
  cannotRegisterReason?: string
  alreadyRegistered?: boolean
  auditStatus?: number
  minRosterCount?: number
  maxRosterCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  hasRegistration?: boolean
  registrationId?: string
  platformCode?: string
  platformName?: string
}

export interface AvailableSeasonPageDto extends PageResult<AvailableSeasonItemDto> {
  emptyReason?: string
}

export interface TeamRegistrationItemDto {
  registrationId: string
  registrationNo?: string
  seasonId?: string
  seasonName?: string
  tournamentName?: string
  platformCode?: string
  platformName?: string
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
  playerNo?: string
  realName?: string
  gameNickname?: string
  positionCode?: string
  jerseyNo?: string
  rosterRole?: string
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
  platformCode?: string
  platformName?: string
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
  platformCode?: string
  contactName?: string
  contactMobile?: string
  contactEmail?: string
  remark?: string
}

export interface SeasonPlatformItemDto {
  platformCode: string
  platformName?: string
  open?: boolean
  disabledReason?: string
}

export interface RegistrationRosterCandidateDto {
  teamMemberId: string
  playerId?: string
  playerNo?: string
  gameNickname?: string
  nickname?: string
  positionCode?: string
  positionName?: string
  jerseyNo?: string
  roleType?: string
  platformGameId?: string
  platformIdentityStatus?: string
  platformIdentityStatusText?: string
  canSelect?: boolean
  cannotSelectReason?: string
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
