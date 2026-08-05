import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  PublicPlayerRankItemVo,
  PublicSeasonItemVo,
  PublicSeasonSummaryVo,
  PublicTeamRankItemVo,
} from '@/api/types/public'

export function getPublicDataCenterSeasons() {
  return request.get<any, ApiResult<PublicSeasonItemVo[]>>('/basket/public/data-center/seasons')
}

export function getPublicDataCenterTeamRanking(params?: {
  seasonId?: string | number
  rankType?: string
  limit?: number
}) {
  return request.get<any, ApiResult<PublicTeamRankItemVo[]>>('/basket/public/data-center/team-ranking', {
    params,
  })
}

export function getPublicDataCenterPlayerRanking(params?: {
  seasonId?: string | number
  rankType?: string
  limit?: number
}) {
  return request.get<any, ApiResult<PublicPlayerRankItemVo[]>>(
    '/basket/public/data-center/player-ranking',
    { params },
  )
}

export function getPublicDataCenterSeasonSummary(params?: { seasonId?: string | number }) {
  return request.get<any, ApiResult<PublicSeasonSummaryVo>>('/basket/public/data-center/season-summary', {
    params,
  })
}
