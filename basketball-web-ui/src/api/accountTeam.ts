import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  TeamCreateReq,
  TeamDetailDto,
  TeamLogoBindReq,
  TeamSummaryDto,
  TeamUpdateReq,
} from '@/api/types/teamAccount'

export function getMyTeamPage(params?: { currentPage?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageResult<TeamSummaryDto>>>('/basket/team/mine', { params })
}

export function getJoinableTeamPage(params?: {
  currentPage?: number
  pageSize?: number
  keyword?: string
}) {
  return request.get<any, ApiResult<PageResult<TeamSummaryDto>>>('/basket/team/joinable/page', { params })
}

export function createTeam(body: TeamCreateReq) {
  return request.post<any, ApiResult<TeamDetailDto>>('/basket/team/create', body)
}

export function getTeamDetail(teamId: string) {
  return request.get<any, ApiResult<TeamDetailDto>>(`/basket/team/${teamId}`)
}

export function updateTeam(teamId: string, body: TeamUpdateReq) {
  return request.put<any, ApiResult<TeamDetailDto>>(`/basket/team/${teamId}`, body)
}

export function bindTeamLogo(teamId: string, body: TeamLogoBindReq) {
  return request.post<any, ApiResult<TeamDetailDto>>(`/basket/team/${teamId}/logo`, body)
}
