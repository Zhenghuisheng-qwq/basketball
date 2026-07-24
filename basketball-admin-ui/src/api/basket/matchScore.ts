import request from '@/utils/request'
import type {
  AjaxResult,
  MatchForfeitReq,
  MatchPeriodScoreSaveReq,
  MatchReopenReq,
  MatchScoreResult
} from '@/types'

export function getMatchScore(matchId: number): Promise<MatchScoreResult> {
  return request({
    url: `/basket/match/${matchId}/score`,
    method: 'get'
  })
}

export function saveMatchPeriodScore(matchId: number, data: MatchPeriodScoreSaveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/period-score`,
    method: 'put',
    data
  })
}

export function submitMatchScore(matchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/score/submit`,
    method: 'post'
  })
}

export function confirmMatchScore(matchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/score/confirm`,
    method: 'post'
  })
}

export function reopenMatchScore(matchId: number, data: MatchReopenReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/score/reopen`,
    method: 'post',
    data
  })
}

export function forfeitMatch(matchId: number, data: MatchForfeitReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/forfeit`,
    method: 'post',
    data
  })
}
