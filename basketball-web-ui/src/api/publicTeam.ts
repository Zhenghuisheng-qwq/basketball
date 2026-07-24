import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PublicHonorItemResp,
  PublicMatchItemVo,
  PublicTeamCompareVo,
  PublicTeamDetailVo,
  PublicTeamRankItemVo,
  PublicTeamRosterItemVo,
  PublicTeamSeasonHistoryVo,
  PublicTeamSeasonStatVo,
} from '@/api/types/public'

export function getPublicTeamDetail(teamId: string | number) {
  return request.get<any, ApiResult<PublicTeamDetailVo>>(`/basket/public/team/${teamId}`)
}

export function getPublicTeamSeasonHistory(teamId: string | number) {
  return request.get<any, ApiResult<PublicTeamSeasonHistoryVo[]>>(
    `/basket/public/team/${teamId}/season-history`,
  )
}

export function getPublicTeamMatchPage(
  teamId: string | number,
  params?: {
    currentPage?: number
    pageSize?: number
    seasonId?: string | number
  },
) {
  return request.get<any, ApiResult<PageResult<PublicMatchItemVo>>>(
    `/basket/public/team/${teamId}/match/page`,
    { params },
  )
}

export function getPublicTeamRoster(teamId: string | number) {
  return request.get<any, ApiResult<PublicTeamRosterItemVo[]>>(`/basket/public/team/${teamId}/roster`)
}

export function getPublicTeamStat(
  teamId: string | number,
  params?: { seasonId?: string | number },
) {
  return request.get<any, ApiResult<PublicTeamSeasonStatVo[]>>(`/basket/public/team/${teamId}/stat`, {
    params,
  })
}

export function getPublicTeamRanking(
  teamId: string | number,
  params?: { seasonId?: string | number; rankType?: string },
) {
  return request.get<any, ApiResult<PublicTeamRankItemVo[]>>(`/basket/public/team/${teamId}/ranking`, {
    params,
  })
}

export function getPublicTeamCompare(params: {
  leftTeamId: string | number
  rightTeamId: string | number
  seasonId?: string | number
}) {
  return request.get<any, ApiResult<PublicTeamCompareVo>>('/basket/public/team/compare', { params })
}

export function getPublicTeamHonorPage(
  teamId: string | number,
  params?: { currentPage?: number; pageSize?: number },
) {
  return request.get<any, ApiResult<PageResult<PublicHonorItemResp>>>(
    `/basket/public/team/${teamId}/honor`,
    { params },
  )
}
