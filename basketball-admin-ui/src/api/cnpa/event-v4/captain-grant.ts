import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketCaptainGrantQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  playerId?: number
  grantStatus?: string
  playerName?: string
}

export interface BasketCaptainGrantListVo {
  id?: number
  seasonId?: number
  seasonTitle?: string
  divisionId?: number
  divisionName?: string
  platformCode?: string
  teamId?: number
  teamName?: string
  teamBoundLabel?: string
  playerId?: number
  playerName?: string
  memberId?: number
  memberNickname?: string
  grantStatus?: string
  grantSource?: string
  validFrom?: string
  validTo?: string
  validToMode?: string
  divisionMatchEndTime?: string
  seasonEndTime?: string
  remark?: string
  createTime?: string
}

export interface BasketCaptainGrantCreateReq {
  divisionId?: number
  playerId?: number
  memberId?: number
  validFrom?: string
  remark?: string
}

export function listV4CaptainGrant(query: BasketCaptainGrantQuery): Promise<TableDataInfo<BasketCaptainGrantListVo[]>> {
  return request({ url: '/basket/v4/captain-grant/list', method: 'get', params: query })
}

export function resolveV4CaptainGrantValidTo(divisionId: number): Promise<AjaxResult<string>> {
  return request({ url: '/basket/v4/captain-grant/resolve-valid-to', method: 'get', params: { divisionId } })
}

export function addV4CaptainGrant(data: BasketCaptainGrantCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/v4/captain-grant', method: 'post', data })
}

export function revokeV4CaptainGrant(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/captain-grant/${id}/revoke`, method: 'put' })
}

export function adjustV4CaptainGrantValidTo(id: number, data: { validToMode: string; validTo?: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/captain-grant/${id}/valid-to`, method: 'put', data })
}

export function previewV4CaptainGrantSync(ids: number[]): Promise<AjaxResult<{ items: any[] }>> {
  return request({ url: '/basket/v4/captain-grant/sync-event-time/preview', method: 'post', data: { ids } })
}

export function syncV4CaptainGrantEventTime(ids: number[]): Promise<AjaxResult<{ items: any[] }>> {
  return request({ url: '/basket/v4/captain-grant/sync-event-time', method: 'post', data: { ids } })
}
