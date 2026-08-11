/** V4 个人中心 Player API 类型 */

export interface V4AvailableDivision {
  seasonId: string
  divisionId: string
  seriesName?: string
  seasonTitle?: string
  divisionName?: string
  platformCode?: string
  registrationMode?: string
  gameMode?: string
  divisionStatus?: string
  lineupMinCount?: number
  lineupMaxCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
  maxTeamCount?: number
  approvedTeamCount?: number
  remainingTeamCount?: number
  myGrantStatus?: string
  grantTeamId?: string
  grantTeamName?: string
  myRegistrationStatus?: string
  myRegistrationId?: string
  canRegister?: boolean
  canViewRegistration?: boolean
  cannotRegisterReason?: string
  cannotRegisterReasonCode?: string
  registrationWindowStatus?: string
  registrationWindowStatusLabel?: string
}

export interface V4RegistrationRosterPlayer {
  teamMemberId?: string
  playerId?: string
  memberId?: string
  playerNo?: string
  displayName?: string
  roleType?: string
  roleTypeText?: string
  platformCode?: string
  platformGameId?: string
  platformIdentityStatus?: string
  eligible?: boolean
  reasonCode?: string
  reasonMessage?: string
  captain?: boolean
}

/** 按 divisionId 加载的报名上下文（不依赖 available 列表过滤） */
export interface V4RegistrationDivisionContext extends V4AvailableDivision {
  divisionExists?: boolean
  teamEligible?: boolean
  teamMemberCount?: number
  eligibleRosterCount?: number
  ineligibleRosterCount?: number
  canSubmitRegistration?: boolean
  eligiblePlayers?: V4RegistrationRosterPlayer[]
  ineligiblePlayers?: V4RegistrationRosterPlayer[]
}

export interface V4MyRegistration {
  registrationId: string
  seasonId?: string
  divisionId?: string
  teamId?: string
  seasonTitle?: string
  divisionName?: string
  platformCode?: string
  teamName?: string
  registrationStatus?: string
  lineupCount?: number
  submitTime?: string
  approveTime?: string
  rejectReason?: string
}

export interface V4MyEvent {
  seasonId: string
  seasonTitle?: string
  seriesCode?: string
  seriesName?: string
  divisionId: string
  divisionName?: string
  platformCode?: string
  platformName?: string
  gameMode?: string
  gameModeName?: string
  registrationMode?: string
  eventTeamId: string
  eventTeamName?: string
  teamName?: string
  teamLogoFileId?: string
  roleInTeam?: string
  eventTeamStatus?: string
  divisionStatus?: string
  currentStageId?: string
  currentStageName?: string
  stageType?: string
  stageExecutionStatus?: string
  currentGroupId?: string
  currentGroupName?: string
  standingRank?: number
  standingPoints?: number
  qualificationStatus?: string
  qualificationStatusName?: string
  nextMatchId?: string
  nextMatchTime?: string
  latestMatchId?: string
  latestMatchResult?: string
  legacyEvent?: boolean
  matchCount?: number
  finishedMatchCount?: number
  pendingSubmissionCount?: number
}

export interface V4MyScheduleSummary {
  upcomingCount?: number
  pendingSubmitCount?: number
  reviewingCount?: number
  finishedCount?: number
  totalCount?: number
}

export interface V4MyScheduleItem {
  matchId: string
  seasonId?: string
  seasonCode?: string
  seasonTitle?: string
  seriesId?: string
  seriesCode?: string
  seriesName?: string
  divisionId?: string
  divisionCode?: string
  divisionName?: string
  platformCode?: string
  stageCode?: string
  stageId?: string
  stageName?: string
  stageType?: string
  groupId?: string
  groupName?: string
  roundNo?: number
  roundName?: string
  matchNo?: string
  matchTime?: string
  homeEventTeamId?: string
  homeTeamName?: string
  homeTeamShortName?: string
  homeTeamLogoFileId?: string
  awayEventTeamId?: string
  awayTeamName?: string
  awayTeamShortName?: string
  awayTeamLogoFileId?: string
  myEventTeamId?: string
  opponentEventTeamId?: string
  currentSide?: string
  homeScore?: number
  awayScore?: number
  matchStatus?: string
  publishStatus?: string
  mySubmissionStatus?: string
  myLatestSubmissionId?: string
  opponentSubmissionStatus?: string
  canSubmit?: boolean
  canSubmitData?: boolean
  canEditSubmission?: boolean
  canViewSubmission?: boolean
  cannotSubmitReason?: string
  userActionType?: string
  userActionText?: string
  latestVersionNo?: number
  latestAuditRemark?: string
  latestRejectReason?: string
  matchSeriesId?: string
  matchSeriesCode?: string
  seriesGameNo?: number
  bestOf?: number
  seriesHomeWinCount?: number
  seriesAwayWinCount?: number
  matchSeriesStatus?: string
  matchSeriesStatusName?: string
}

export interface V4MySchedulePage {
  summary?: V4MyScheduleSummary
  items?: V4MyScheduleItem[]
}

export interface V4RegistrationSaveReq {
  divisionId: string
  teamId: string
  playerIds: string[]
}

export interface V4RegistrationIdReq {
  registrationId: string
}

export interface V4RegistrationPlayerItem {
  playerId: string
  playerName?: string
  isCaptain?: number
  sortOrder?: number
}

export interface V4RegistrationDetail extends V4MyRegistration {
  players?: V4RegistrationPlayerItem[]
}

export interface V4PersonalAvailableDivision {
  seasonId: string
  divisionId: string
  seriesName?: string
  seasonTitle?: string
  divisionName?: string
  platformCode?: string
  registrationMode?: string
  gameMode?: string
  lineupMinCount?: number
  lineupMaxCount?: number
  maxPersonCount?: number
  approvedPersonCount?: number
  remainingPersonCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
  myRegistrationStatus?: string
  canRegister?: boolean
  cannotRegisterReason?: string
}

export interface V4MyPersonalRegistration {
  personalRegistrationId: string
  seasonId?: string
  divisionId?: string
  seasonTitle?: string
  divisionName?: string
  platformCode?: string
  preferredPosition?: string
  registrationStatus?: string
  submitTime?: string
  approvedTime?: string
  rejectReason?: string
  assignedDrawTeamId?: string
  convertedRegistrationId?: string
}

export interface V4PersonalRegistrationSaveReq {
  divisionId: string
  preferredPosition?: string
}

export interface V4PersonalRegistrationIdReq {
  personalRegistrationId: string
}

export interface V4PersonalFormationMember {
  personalRegistrationId?: string
  playerId?: string
  playerName?: string
  assignmentRole?: string
  joinStatus?: string
}

export interface V4PersonalFormationTask {
  drawTeamId: string
  drawBatchId?: string
  divisionId?: string
  divisionName?: string
  temporaryTeamCode?: string
  temporaryTeamName?: string
  expectedMemberCount?: number
  actualMemberCount?: number
  longTermTeamId?: string
  longTermTeamName?: string
  formationStatus?: string
  convertedRegistrationId?: string
  confirmedTime?: string
  members?: V4PersonalFormationMember[]
}

export interface V4PersonalFormationBindTeamReq {
  drawTeamId: string
  longTermTeamId: string
}

export interface V4PersonalFormationAvailableTeam {
  teamId: string
  teamName?: string
  logoUrl?: string
  teamStatus?: number
  currentMemberCount?: number
  matchedAssignedMemberCount?: number
  missingAssignedMemberCount?: number
  extraMemberCount?: number
  captainMatched?: boolean
  canBind?: boolean
  unavailableReason?: string
}
