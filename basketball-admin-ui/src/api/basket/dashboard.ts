import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface DashboardTrendSummary {
  todayNew: number
  todayHandled: number
  pendingTotal: number
  monthTotal: number
}

export interface DashboardTrendSeries {
  name: string
  code: string
  data: number[]
}

export interface DashboardTrendData {
  dateList: string[]
  series: DashboardTrendSeries[]
  summary: DashboardTrendSummary
}

export function getDashboardSummary(): Promise<AjaxResult<any>> {
  return request({ url: '/basket/dashboard/summary', method: 'get' })
}

export function getDashboardTrend(params: {
  rangeType?: string
  startDate?: string
  endDate?: string
  metricGroup?: string
}): Promise<AjaxResult<DashboardTrendData>> {
  return request({ url: '/basket/dashboard/trend', method: 'get', params })
}
