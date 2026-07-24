import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PublicHallOverviewResp,
  PublicHonorDetailResp,
  PublicHonorItemResp,
} from '@/api/types/public'

export type PublicHonorPageParams = {
  currentPage?: number
  pageSize?: number
  tournamentId?: string | number
  seasonId?: string | number
  keyword?: string
  sortField?: string
  sortOrder?: string
}

export function getPublicHallOverview() {
  return request.get<any, ApiResult<PublicHallOverviewResp>>('/basket/public/hall-of-fame/overview')
}

export function getPublicHallTournamentHonorPage(params?: PublicHonorPageParams) {
  return request.get<any, ApiResult<PageResult<PublicHonorItemResp>>>(
    '/basket/public/hall-of-fame/tournament/page',
    { params },
  )
}

export function getPublicHallPlayerHonorPage(params?: PublicHonorPageParams) {
  return request.get<any, ApiResult<PageResult<PublicHonorItemResp>>>(
    '/basket/public/hall-of-fame/player/page',
    { params },
  )
}

export function getPublicHallTeamHonorPage(params?: PublicHonorPageParams) {
  return request.get<any, ApiResult<PageResult<PublicHonorItemResp>>>(
    '/basket/public/hall-of-fame/team/page',
    { params },
  )
}

export function getPublicHallHonorDetail(honorId: string | number) {
  return request.get<any, ApiResult<PublicHonorDetailResp>>(
    `/basket/public/hall-of-fame/${honorId}`,
  )
}
