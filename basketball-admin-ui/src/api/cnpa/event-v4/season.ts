import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { BasketDivisionListVo } from './division'

export interface BasketSeasonQuery {
  pageNum?: number
  pageSize?: number
  seriesId?: number
  seriesCode?: string
  seasonYear?: number
  seasonName?: string
  displayTitle?: string
  publishStatus?: string
  seasonStatus?: string
}

export interface BasketSeasonListVo {
  id?: number
  seriesId?: number
  seriesCode?: string
  seriesName?: string
  seasonYear?: number
  seasonName?: string
  displayTitle?: string
  description?: string
  coverFileId?: number
  bannerFileId?: number
  coverUrl?: string
  bannerUrl?: string
  seasonStartTime?: string
  seasonEndTime?: string
  publishStatus?: string
  seasonStatus?: string
  divisionCount?: number
  createTime?: string
  updateTime?: string
}

export interface BasketSeasonDetailVo extends BasketSeasonListVo {
  divisions?: BasketDivisionListVo[]
}

export interface BasketSeasonCreateReq {
  seriesId?: number
  seasonYear?: number
  seasonName?: string
  displayTitle?: string
  description?: string
  coverFileId?: number
  bannerFileId?: number
  seasonStartTime?: string
  seasonEndTime?: string
}

export type BasketSeasonUpdateReq = Partial<BasketSeasonCreateReq>

export function listV4Season(query: BasketSeasonQuery): Promise<TableDataInfo<BasketSeasonListVo[]>> {
  return request({ url: '/basket/v4/season/list', method: 'get', params: query })
}

export function getV4Season(id: number): Promise<AjaxResult<BasketSeasonDetailVo>> {
  return request({ url: `/basket/v4/season/${id}`, method: 'get' })
}

export function addV4Season(data: BasketSeasonCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/v4/season', method: 'post', data })
}

export function updateV4Season(id: number, data: BasketSeasonUpdateReq): Promise<AjaxResult> {
  return request({ url: `/basket/v4/season/${id}`, method: 'put', data })
}

export function publishV4Season(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/season/${id}/publish`, method: 'put' })
}

export function offlineV4Season(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/season/${id}/offline`, method: 'put' })
}

export function delV4Season(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/season/${id}`, method: 'delete' })
}
