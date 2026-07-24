import request from '@/utils/request'
import type {
  AjaxResult,
  MatchCancelReq,
  MatchCreateReq,
  MatchDetail,
  MatchListItem,
  MatchPostponeReq,
  MatchQuery,
  MatchScheduleReq,
  MatchUpdateReq,
  TableDataInfo,
  VenueListResult
} from '@/types'

export function listMatch(query: MatchQuery): Promise<TableDataInfo<MatchListItem[]>> {
  return request({
    url: '/basket/match/list',
    method: 'get',
    params: query
  })
}

export function getMatch(matchId: number): Promise<AjaxResult<MatchDetail>> {
  return request({
    url: `/basket/match/${matchId}`,
    method: 'get'
  })
}

export function listMatchVenues(): Promise<VenueListResult> {
  return request({
    url: '/basket/match/venues',
    method: 'get'
  })
}

export function addMatch(data: MatchCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/match',
    method: 'post',
    data
  })
}

export function updateMatch(matchId: number, data: MatchUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}`,
    method: 'put',
    data
  })
}

export function scheduleMatch(matchId: number, data: MatchScheduleReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/schedule`,
    method: 'post',
    data
  })
}

export function postponeMatch(matchId: number, data: MatchPostponeReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/postpone`,
    method: 'post',
    data
  })
}

export function cancelMatch(matchId: number, data: MatchCancelReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/cancel`,
    method: 'post',
    data
  })
}

export function startMatch(matchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/start`,
    method: 'post'
  })
}

export function delMatch(matchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}`,
    method: 'delete'
  })
}
