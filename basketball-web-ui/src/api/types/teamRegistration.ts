import type { PageResult } from '@/api/types/common'

export interface AvailableSeasonItemDto {
  seasonId: string
  operationalSeasonId?: string
  seasonNo?: string
  seasonName?: string
  competitionSeasonId?: string | number
  competitionSeasonName?: string
  tournamentId?: string
  tournamentNo?: string
  tournamentName?: string
  gameTerminal?: string
  eventType?: string
  eventTypeName?: string
  operationalSeasonName?: string
  tournamentStatus?: number
  seasonStatus?: number
  registrationStatus?: string
  teamId?: string
  userTeamId?: string
  teamName?: string
  userTeamName?: string
  teamCertificationStatus?: number
  canRegister?: boolean
  cannotRegisterReason?: string
  alreadyRegistered?: boolean
  auditStatus?: number
  minRosterCount?: number
  maxRosterCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  signupStartTime?: string
  signupEndTime?: string
  hasRegistration?: boolean
  registrationId?: string
  platformCode?: string
  platformName?: string
  openedPlatforms?: string[]
  registeredPlatforms?: string[]
  availableRegisterPlatforms?: string[]
  userRosterStatus?: string
  myRosterStatus?: string
  userRosterStatusText?: string
  myRosterStatusName?: string
  hasActiveCaptainGrantForEvent?: boolean
  hasCaptainGrant?: boolean
  captainGrantStatus?: string
  captainGrantStatusText?: string
  captainGrantExpireTime?: string
  teamRegistrationStatus?: string
  teamRegistrationStatusText?: string
  teamRegistrationStatusName?: string
  userMembershipStatusText?: string
  disabledReason?: string
  userTeamRole?: string
  userRoleInTeam?: string
  isCaptain?: boolean
  actionCode?: string
  actionText?: string
  operationType?: string
  matchStartTime?: string
  matchEndTime?: string
  canRemindCaptain?: boolean
  cannotRemindReason?: string
  eventStatus?: string
  eventStatusName?: string
  remindedRecently?: boolean
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
  rosterLocked?: boolean
  rosterLockStatusText?: string
  submittedTime?: string
  createTime?: string
  auditTime?: string
  approvedTime?: string
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
  rosterLocked?: boolean
  rosterLockStatusText?: string
  rosterLockedHint?: string
  canRequestRosterChange?: boolean
  submittedTime?: string
  registrationEndTime?: string
  minRosterCount?: number
  maxRosterCount?: number
  formatCode?: string
  teamPlayCount?: number
  requiredStartingCount?: number
  certifiedMemberCount?: number
  platformEligibleMemberCount?: number
  selectedRosterCount?: number
  missingCount?: number
  missingPlatformGameIdCount?: number
  canStartRegistration?: boolean
  canSubmitRegistration?: boolean
  cannotReason?: string
  rosterItems?: TeamRegistrationPlayerItemDto[]
}

export interface TeamRegistrationRemindPlatformResultDto {
  success?: boolean
  message?: string
  lastRemindTime?: string
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
