import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PublicPlayerSeasonStatVo,
  PublicSeasonSummaryVo,
  PublicTeamSeasonStatVo,
} from '@/api/types/public'

export function getPublicSeasonTeamStatPage(
  seasonId: string | number,
  params?: {
    currentPage?: number
    pageSize?: number
    stageId?: string | number
    sortField?: string
    sortOrder?: string
  },
) {
  return request.get<any, ApiResult<PageResult<PublicTeamSeasonStatVo>>>(
    `/basket/public/season/${seasonId}/team-stat/page`,
    { params },
  )
}

export function getPublicSeasonPlayerStatPage(
  seasonId: string | number,
  params?: {
    currentPage?: number
    pageSize?: number
    stageId?: string | number
    teamId?: string | number
    sortField?: string
    sortOrder?: string
  },
) {
  return request.get<any, ApiResult<PageResult<PublicPlayerSeasonStatVo>>>(
    `/basket/public/season/${seasonId}/player-stat/page`,
    { params },
  )
}

export function getPublicSeasonSummary(seasonId: string | number) {
  return request.get<any, ApiResult<PublicSeasonSummaryVo>>(
    `/basket/public/season/${seasonId}/summary`,
  )
}
