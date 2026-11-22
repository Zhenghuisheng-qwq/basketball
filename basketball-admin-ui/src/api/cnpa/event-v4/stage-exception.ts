import request from '@/utils/request'

export interface MatchForfeitReq {
  matchId: number
  loserEventTeamId: number
  reason: string
  includeRemaining?: boolean
}

export interface SeriesForfeitReq {
  seriesId: number
  forfeitEventTeamId: number
  reason: string
}

export interface ManualAdvanceReq {
  stageId: number
  eventTeamId: number
  reason: string
}

export interface PairingSlotReq {
  bracketSlot: number
  homeEventTeamId: number
  awayEventTeamId: number
}

export interface PairingAdjustReq {
  stageId: number
  slots: PairingSlotReq[]
  reason: string
}

export function forfeitV4Match(data: MatchForfeitReq) {
  return request({ url: '/basket/v4/admin/stage-exception/match-forfeit', method: 'post', data })
}

export function revokeV4MatchForfeit(data: { matchId: number; reason: string }) {
  return request({ url: '/basket/v4/admin/stage-exception/match-forfeit-revoke', method: 'post', data })
}

export function forfeitV4Series(data: SeriesForfeitReq) {
  return request({ url: '/basket/v4/admin/stage-exception/series-forfeit', method: 'post', data })
}

export function manualAdvanceV4(data: ManualAdvanceReq) {
  return request({ url: '/basket/v4/admin/stage-exception/manual-advance', method: 'post', data })
}

export function adjustV4Pairing(data: PairingAdjustReq) {
  return request({ url: '/basket/v4/admin/stage-exception/pairing-adjust', method: 'post', data })
}

export function revokeV4SeriesResolution(data: { seriesId: number; reason: string }) {
  return request({ url: '/basket/v4/admin/stage-exception/series-revoke-resolution', method: 'post', data })
}

export interface AdminOfficialResultReq {
  matchId: number
  homeScore: number
  awayScore: number
  reason: string
  remark?: string
}

export interface SeriesManualResolutionReq {
  seriesId: number
  winnerEventTeamId: number
  decisionType: string
  reason: string
}

export interface BoxScorePlayer {
  eventPlayerId?: number
  playerName?: string
  gameId?: string
  jerseyNo?: string
  isCaptain?: number
  recognizedName?: string
  matched?: boolean
  points?: number
  rebound?: number
  assist?: number
  steal?: number
  block?: number
  turnover?: number
  foul?: number
  fieldGoalMade?: number
  fieldGoalAttempt?: number
  threePointMade?: number
  threePointAttempt?: number
  freeThrowMade?: number
  freeThrowAttempt?: number
}

export interface BoxScoreRoster {
  matchId: number
  homeTeamName?: string
  awayTeamName?: string
  homeEventTeamId?: number
  awayEventTeamId?: number
  gameMode?: string
  periodRequired?: boolean
  homePlayers?: BoxScorePlayer[]
  awayPlayers?: BoxScorePlayer[]
}

export interface BoxScoreResult {
  homeScore?: number
  awayScore?: number
  unmatchedCount?: number
  warnings?: string[]
  periods?: Array<{ periodNo: number; homeScore?: number; awayScore?: number }>
  homePlayers?: BoxScorePlayer[]
  awayPlayers?: BoxScorePlayer[]
}

export function getV4MatchBoxScoreRoster(matchId: number) {
  return request({ url: `/basket/v4/admin/stage-exception/match/${matchId}/box-score-roster`, method: 'get' })
}

export function recognizeV4AdminScreenshot(matchId: number, file: File) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: `/basket/v4/admin/stage-exception/match/${matchId}/recognize-screenshot`,
    method: 'post',
    data,
    timeout: 120000,
    headers: { repeatSubmit: false }
  })
}

export function enterV4MatchBoxScore(data: {
  matchId: number
  reason: string
  homePlayers: BoxScorePlayer[]
  awayPlayers: BoxScorePlayer[]
  periods?: Array<{ periodNo: number; homeScore?: number; awayScore?: number }>
}) {
  return request({ url: '/basket/v4/admin/stage-exception/match-box-score', method: 'post', data, timeout: 60000 })
}

export function enterV4OfficialResult(data: AdminOfficialResultReq) {
  return request({ url: '/basket/v4/admin/stage-exception/match-official-result', method: 'post', data })
}

export function correctV4OfficialResult(data: AdminOfficialResultReq) {
  return request({ url: '/basket/v4/admin/stage-exception/match-result-correction', method: 'post', data })
}

export function reopenV4ApprovedSubmission(data: { matchId?: number; seriesSubmissionId?: number; reason: string }) {
  return request({ url: '/basket/v4/admin/stage-exception/match-reopen-submission', method: 'post', data })
}

export function resolveV4SeriesManually(data: SeriesManualResolutionReq) {
  return request({ url: '/basket/v4/admin/stage-exception/series-manual-resolution', method: 'post', data })
}
