import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketCaptainGrantQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  teamId?: number
  playerId?: number
  grantStatus?: string
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
  playerId?: number
  playerName?: string
  memberId?: number
  grantStatus?: string
  validFrom?: string
  validTo?: string
  remark?: string
  createTime?: string
}

export interface BasketCaptainGrantCreateReq {
  divisionId?: number
  teamId?: number
  playerId?: number
  memberId?: number
  validFrom?: string
  validTo?: string
  remark?: string
}

export function listV4CaptainGrant(query: BasketCaptainGrantQuery): Promise<TableDataInfo<BasketCaptainGrantListVo[]>> {
  return request({ url: '/basket/v4/captain-grant/list', method: 'get', params: query })
}

export function addV4CaptainGrant(data: BasketCaptainGrantCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/v4/captain-grant', method: 'post', data })
}

export function revokeV4CaptainGrant(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/captain-grant/${id}/revoke`, method: 'put' })
}
