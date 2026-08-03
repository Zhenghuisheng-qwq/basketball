import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface BasketSeriesVo {
  id?: number
  seriesCode?: string
  seriesName?: string
  seriesDesc?: string
  defaultFormatType?: string
  iconFileId?: number
  coverFileId?: number
  bannerFileId?: number
  sortOrder?: number
  enabled?: number
}

export function listV4Series(params?: { enabled?: number }): Promise<AjaxResult<BasketSeriesVo[]>> {
  return request({ url: '/basket/v4/series/list', method: 'get', params })
}

export function getV4Series(id: number): Promise<AjaxResult<BasketSeriesVo>> {
  return request({ url: `/basket/v4/series/${id}`, method: 'get' })
}

export function updateV4SeriesEnabled(id: number, enabled: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/series/${id}/enabled`, method: 'put', data: { enabled } })
}
