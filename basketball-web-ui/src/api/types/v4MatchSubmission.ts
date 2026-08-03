/** V4 比赛数据提交 Player API 类型 */

export interface V4MatchSubmissionTeamStat {
  score?: number
  fieldGoalMade?: number
  fieldGoalAttempt?: number
  threePointMade?: number
  threePointAttempt?: number
  freeThrowMade?: number
  freeThrowAttempt?: number
  rebound?: number
  assist?: number
  steal?: number
  block?: number
  turnover?: number
  foul?: number
}

export interface V4MatchSubmissionPlayerStat {
  eventPlayerId: string
  playerName?: string
  points?: number
  rebound?: number
  assist?: number
  steal?: number
  block?: number
  turnover?: number
  foul?: number
  minutes?: number
}

export interface V4MatchSubmissionDetail {
  matchId: string
  matchNo?: string
  roundNo?: number
  matchTime?: string
  divisionName?: string
  platformCode?: string
  homeEventTeamId?: string
  homeTeamName?: string
  awayEventTeamId?: string
  awayTeamName?: string
  submitEventTeamId?: string
  submitTeamName?: string
  submissionId?: string
  versionNo?: number
  submissionStatus?: string
  homeScore?: number
  awayScore?: number
  submitRemark?: string
  submitTime?: string
  auditUserName?: string
  auditTime?: string
  auditRemark?: string
  rejectReason?: string
  teamStat?: V4MatchSubmissionTeamStat
  playerStats?: V4MatchSubmissionPlayerStat[]
  materialFileIds?: string[]
}

export interface V4MyMatchItem {
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
  awayEventTeamId?: string
  awayTeamName?: string
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
  latestVersionNo?: number
  latestAuditRemark?: string
  latestRejectReason?: string
}

export interface V4MatchSubmissionSaveReq {
  matchId: string
  submitEventTeamId: string
  homeScore?: number
  awayScore?: number
  teamStat?: V4MatchSubmissionTeamStat
  playerStats?: V4MatchSubmissionPlayerStat[]
  materialFileIds?: string[]
  submitRemark?: string
}

export interface V4MatchSubmissionIdReq {
  submissionId: string
}

export interface V4MatchSubmissionResubmitReq extends V4MatchSubmissionSaveReq {
  oldSubmissionId: string
}
