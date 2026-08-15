import request from '@/utils/request'

export interface MatchForfeitReq {
  matchId: number
  loserEventTeamId: number
  reason: string
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

export function enterV4OfficialResult(data: AdminOfficialResultReq) {
  return request({ url: '/basket/v4/admin/stage-exception/match-official-result', method: 'post', data })
}

export function correctV4OfficialResult(data: AdminOfficialResultReq) {
  return request({ url: '/basket/v4/admin/stage-exception/match-result-correction', method: 'post', data })
}

export function resolveV4SeriesManually(data: SeriesManualResolutionReq) {
  return request({ url: '/basket/v4/admin/stage-exception/series-manual-resolution', method: 'post', data })
}
