import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PublicSeasonItemVo,
  PublicSeasonOverviewVo,
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
