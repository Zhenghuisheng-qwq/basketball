import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketDivisionQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  seriesCode?: string
  platformCode?: string
  registrationMode?: string
  formatType?: string
  divisionStatus?: string
}

export interface BasketDivisionListVo {
  id?: number
  seasonId?: number
  seasonTitle?: string
  seriesCode?: string
  seriesName?: string
  platformCode?: string
  platformName?: string
  gameMode?: string
  gameModeName?: string
  divisionName?: string
  divisionNameEn?: string
  registrationMode?: string
  registrationModeName?: string
  formatType?: string
  formatTemplateId?: number
  formatTemplateName?: string
  formatSnapshotId?: number
  formatSnapshotStatus?: string
  formatSummary?: string
  maxTeamCount?: number
  maxPersonCount?: number
  lineupMinCount?: number
  lineupMaxCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
  divisionStatus?: string
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface BasketDivisionCreateReq {
  seasonId?: number
  platformCode?: string
  gameMode?: string
  divisionName?: string
  divisionNameEn?: string
  registrationMode?: string
  formatType?: string
  formatTemplateId?: number
  maxTeamCount?: number
  maxPersonCount?: number
  lineupMinCount?: number
  lineupMaxCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
  sortOrder?: number
}

export type BasketDivisionUpdateReq = Partial<Omit<BasketDivisionCreateReq, 'seasonId'>>

export function listV4Division(query: BasketDivisionQuery): Promise<TableDataInfo<BasketDivisionListVo[]>> {
  return request({ url: '/basket/v4/division/list', method: 'get', params: query })
}

export function getV4Division(id: number): Promise<AjaxResult<BasketDivisionListVo>> {
  return request({ url: `/basket/v4/division/${id}`, method: 'get' })
}

export function addV4Division(data: BasketDivisionCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/v4/division', method: 'post', data })
}

export function updateV4Division(id: number, data: BasketDivisionUpdateReq): Promise<AjaxResult> {
  return request({ url: `/basket/v4/division/${id}`, method: 'put', data })
}

export function openV4DivisionRegistration(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/division/${id}/open-registration`, method: 'put' })
}

export function closeV4DivisionRegistration(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/division/${id}/close-registration`, method: 'put' })
}

export function finishV4Division(id: number, data?: { reason?: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/division/${id}/finish`, method: 'post', data: data || {} })
}

export function delV4Division(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/division/${id}`, method: 'delete' })
}
