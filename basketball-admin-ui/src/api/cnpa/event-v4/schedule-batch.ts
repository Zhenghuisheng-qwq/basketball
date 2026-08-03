import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketScheduleBatchQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  formatType?: string
  generateStatus?: string
  publishStatus?: string
}

export interface BasketScheduleBatchListVo {
  id?: number
  seasonId?: number
  seasonTitle?: string
  divisionId?: number
  divisionName?: string
  platformCode?: string
  formatType?: string
  ruleCode?: string
  teamCount?: number
  matchCount?: number
  generateStatus?: string
  publishStatus?: string
  generateTime?: string
  publishTime?: string
  remark?: string
}

export interface BasketScheduleBatchDetailVo extends BasketScheduleBatchListVo {
  batchNo?: string
  generateType?: string
  matches?: BasketV4MatchListVo[]
}

export interface BasketV4MatchListVo {
  id?: number
  seasonId?: number
  seasonTitle?: string
  divisionId?: number
  divisionName?: string
  platformCode?: string
  scheduleBatchId?: number
  roundNo?: number
  matchNo?: string
  matchTime?: string
  homeEventTeamId?: number
  homeTeamName?: string
  awayEventTeamId?: number
  awayTeamName?: string
  homeScore?: number
  awayScore?: number
  matchStatus?: string
  publishStatus?: string
  venueText?: string
  remark?: string
}

export interface BasketScheduleBatchGenerateReq {
  divisionId: number
  remark?: string
}

export function listV4ScheduleBatch(query: BasketScheduleBatchQuery): Promise<TableDataInfo<BasketScheduleBatchListVo[]>> {
  return request({ url: '/basket/v4/schedule-batch/list', method: 'get', params: query })
}

export function getV4ScheduleBatch(id: number): Promise<AjaxResult<BasketScheduleBatchDetailVo>> {
  return request({ url: `/basket/v4/schedule-batch/${id}`, method: 'get' })
}

export function generateV4ScheduleBatch(data: BasketScheduleBatchGenerateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/v4/schedule-batch/generate', method: 'post', data })
}

export function publishV4ScheduleBatch(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/schedule-batch/${id}/publish`, method: 'put' })
}

export function cancelV4ScheduleBatch(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/schedule-batch/${id}/cancel`, method: 'put' })
}

export function listV4Match(query: Record<string, unknown>): Promise<TableDataInfo<BasketV4MatchListVo[]>> {
  return request({ url: '/basket/v4/match/list', method: 'get', params: query })
}

export function getV4Match(id: number): Promise<AjaxResult<BasketV4MatchListVo>> {
  return request({ url: `/basket/v4/match/${id}`, method: 'get' })
}

export function updateV4Match(id: number, data: { matchTime?: string; venueText?: string; remark?: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/match/${id}`, method: 'put', data })
}

export function deleteV4Match(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/match/${id}`, method: 'delete' })
}
