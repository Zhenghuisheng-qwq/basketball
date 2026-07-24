import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  PublicHomeVo,
  PublicMatchItemVo,
  PublicPlayerRankItemVo,
  PublicSeasonSummaryVo,
  PublicTeamRankItemVo,
} from '@/api/types/public'

export function getPublicHome() {
  return request.get<any, ApiResult<PublicHomeVo>>('/basket/public/home')
}

export function getPublicHomeRecentMatches(limit = 10) {
  return request.get<any, ApiResult<PublicMatchItemVo[]>>('/basket/public/home/recent-match', {
    params: { limit },
  })
}

export function getPublicHomeTeamRanking(limit = 10) {
  return request.get<any, ApiResult<PublicTeamRankItemVo[]>>('/basket/public/home/team-ranking', {
    params: { limit },
  })
}

export function getPublicHomePlayerRanking(limit = 10) {
  return request.get<any, ApiResult<PublicPlayerRankItemVo[]>>('/basket/public/home/player-ranking', {
    params: { limit },
  })
}

export function getPublicHomeStatSummary() {
  return request.get<any, ApiResult<PublicSeasonSummaryVo>>('/basket/public/home/stat-summary')
}
