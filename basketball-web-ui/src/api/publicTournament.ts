import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PublicHonorItemResp,
  PublicSeasonItemVo,
  PublicSeasonOverviewVo,
  PublicSeasonPlacementResp,
  PublicTournamentDetailVo,
  PublicTournamentItemVo,
} from '@/api/types/public'

export function getPublicTournamentPage(params?: {
  currentPage?: number
  pageSize?: number
  tournamentStatus?: number
  keyword?: string
}) {
  return request.get<any, ApiResult<PageResult<PublicTournamentItemVo>>>('/basket/public/tournament/page', {
    params,
  })
}

export function getPublicTournamentDetail(tournamentId: string | number) {
  return request.get<any, ApiResult<PublicTournamentDetailVo>>(
    `/basket/public/tournament/${tournamentId}`,
  )
}

export function getPublicTournamentSeasons(tournamentId: string | number) {
  return request.get<any, ApiResult<PublicSeasonItemVo[]>>(
    `/basket/public/tournament/${tournamentId}/season`,
  )
}

export function getPublicSeasonOverview(seasonId: string | number) {
  return request.get<any, ApiResult<PublicSeasonOverviewVo>>(
    `/basket/public/season/${seasonId}/overview`,
  )
}

export function getPublicSeasonPlacement(seasonId: string | number) {
  return request.get<any, ApiResult<PublicSeasonPlacementResp>>(
    `/basket/public/season/${seasonId}/placement`,
  )
}

export function getPublicSeasonHonors(seasonId: string | number) {
  return request.get<any, ApiResult<PublicHonorItemResp[]>>(
    `/basket/public/season/${seasonId}/honor`,
  )
}
