import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type { PublicHonorItemResp, PublicPlayerDetailVo, PublicPlayerSeasonStatVo } from '@/api/types/public'

export function getOwnPerformancePlayer() {
  return request.get<any, ApiResult<PublicPlayerDetailVo>>('/basket/account/performance/player')
}

export function getOwnPerformanceSeasonStat(params?: { seasonId?: string | number }) {
  return request.get<any, ApiResult<PublicPlayerSeasonStatVo[]>>('/basket/account/performance/season-stat', { params })
}

export function getOwnPerformanceHonorPage(params?: { currentPage?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageResult<PublicHonorItemResp>>>('/basket/account/performance/honor', { params })
}
