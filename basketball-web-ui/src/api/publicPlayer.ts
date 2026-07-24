import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PublicMatchItemVo,
  PublicPlayerDetailVo,
  PublicPlayerRankItemVo,
  PublicPlayerSeasonStatVo,
} from '@/api/types/public'

export function getPublicPlayerDetail(playerId: string | number) {
  return request.get<any, ApiResult<PublicPlayerDetailVo>>(`/basket/public/player/${playerId}`)
}

export function getPublicPlayerSeasonStat(
  playerId: string | number,
  params?: { seasonId?: string | number },
) {
  return request.get<any, ApiResult<PublicPlayerSeasonStatVo[]>>(
    `/basket/public/player/${playerId}/season-stat`,
    { params },
  )
}

export function getPublicPlayerMatchPage(
  playerId: string | number,
  params?: {
    currentPage?: number
    pageSize?: number
    seasonId?: string | number
  },
) {
  return request.get<any, ApiResult<PageResult<PublicMatchItemVo>>>(
    `/basket/public/player/${playerId}/match/page`,
    { params },
  )
}

export function getPublicPlayerRanking(
  playerId: string | number,
  params?: { seasonId?: string | number; rankType?: string },
) {
  return request.get<any, ApiResult<PublicPlayerRankItemVo[]>>(
    `/basket/public/player/${playerId}/ranking`,
    { params },
  )
}
