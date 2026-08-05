import request from '@/utils/request'
import type {
  AjaxResult,
  MatchAuditLogResult,
  MatchPlayerStatListResult,
  MatchPlayerStatSaveReq,
  MatchReopenReq,
  MatchTeamStatListResult,
  MatchTeamStatSaveReq
} from '@/types'

export function getMatchTeamStat(matchId: number): Promise<MatchTeamStatListResult> {
  return request({
    url: `/basket/match/${matchId}/team-stat`,
    method: 'get'
  })
}

export function saveMatchTeamStat(matchId: number, data: MatchTeamStatSaveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/team-stat`,
    method: 'put',
    data
  })
}

export function submitMatchTeamStat(matchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/team-stat/submit`,
    method: 'post'
  })
}

export function getMatchPlayerStat(matchId: number): Promise<MatchPlayerStatListResult> {
  return request({
    url: `/basket/match/${matchId}/player-stat`,
    method: 'get'
  })
}

export function saveMatchPlayerStat(matchId: number, data: MatchPlayerStatSaveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/player-stat`,
    method: 'put',
    data
  })
}

export function submitMatchPlayerStat(matchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/player-stat/submit`,
    method: 'post'
  })
}

export function confirmMatchResult(matchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/result/confirm`,
    method: 'post'
  })
}

export function rejectMatchResult(matchId: number, data: MatchReopenReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/result/reject`,
    method: 'post',
    data
  })
}

export function reopenMatchResult(matchId: number, data: MatchReopenReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/result/reopen`,
    method: 'post',
    data
  })
}

export function getMatchAuditLog(matchId: number): Promise<MatchAuditLogResult> {
  return request({
    url: `/basket/match/${matchId}/audit-log`,
    method: 'get'
  })
}
