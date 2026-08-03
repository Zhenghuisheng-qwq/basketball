/** V4 个人中心 Player API 类型 */

export interface V4AvailableDivision {
  seasonId: string
  divisionId: string
  seriesName?: string
  seasonTitle?: string
  divisionName?: string
  platformCode?: string
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
  canRegister?: boolean
  cannotRegisterReason?: string
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
  eventTeamId: string
  teamName?: string
  teamLogoFileId?: string
  roleInTeam?: string
  eventTeamStatus?: string
  divisionStatus?: string
  matchCount?: number
  finishedMatchCount?: number
  pendingSubmissionCount?: number
}

export interface V4MyScheduleItem {
  matchId: string
  seasonId?: string
  seasonTitle?: string
  divisionId?: string
  divisionName?: string
  platformCode?: string
  roundNo?: number
  matchNo?: string
  matchTime?: string
  homeEventTeamId?: string
  homeTeamName?: string
  homeTeamLogoFileId?: string
  awayEventTeamId?: string
  awayTeamName?: string
  awayTeamLogoFileId?: string
  myEventTeamId?: string
  opponentEventTeamId?: string
  homeScore?: number
  awayScore?: number
  matchStatus?: string
  publishStatus?: string
  mySubmissionStatus?: string
  myLatestSubmissionId?: string
  opponentSubmissionStatus?: string
  canSubmit?: boolean
  canSubmitData?: boolean
  cannotSubmitReason?: string
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
