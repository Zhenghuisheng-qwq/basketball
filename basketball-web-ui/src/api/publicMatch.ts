import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type { PublicMatchDetailVo, PublicMatchItemVo } from '@/api/types/public'

export function getPublicSeasonSchedulePage(
  seasonId: string | number,
  params?: {
    currentPage?: number
    pageSize?: number
    stageId?: string | number
    groupId?: string | number
    teamId?: string | number
  },
) {
  return request.get<any, ApiResult<PageResult<PublicMatchItemVo>>>(
    `/basket/public/season/${seasonId}/schedule/page`,
    { params },
  )
}

export function getPublicSeasonResultPage(
  seasonId: string | number,
  params?: {
    currentPage?: number
    pageSize?: number
    stageId?: string | number
    groupId?: string | number
    teamId?: string | number
  },
) {
  return request.get<any, ApiResult<PageResult<PublicMatchItemVo>>>(
    `/basket/public/season/${seasonId}/result/page`,
    { params },
  )
}

export function getPublicMatchDetail(matchId: string | number) {
  return request.get<any, ApiResult<PublicMatchDetailVo>>(`/basket/public/match/${matchId}`)
}
