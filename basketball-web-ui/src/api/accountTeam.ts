import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  TeamCreateReq,
  TeamDetailDto,
  TeamLogoBindReq,
  TeamMineScope,
  TeamSummaryDto,
  TeamUpdateReq,
} from '@/api/types/teamAccount'
import type { JoinableTeamPageDto } from '@/api/types/teamJoinInvite'
import type { CaptainGrantBindTeamReq, InheritableTeamSummaryDto } from '@/api/types/teamInherit'

export function getMyTeamPage(params?: {
  currentPage?: number
  pageSize?: number
  scope?: TeamMineScope
}) {
  return request.get<any, ApiResult<PageResult<TeamSummaryDto>>>('/basket/team/mine', { params })
}

export function getJoinableTeamPage(params?: {
  currentPage?: number
  pageSize?: number
  platformCode?: string
  keyword?: string
}) {
  return request.get<any, ApiResult<JoinableTeamPageDto>>('/basket/team/joinable/page', { params })
}

export function getInheritableTeamList() {
  return request.get<any, ApiResult<InheritableTeamSummaryDto[]>>('/basket/team/inheritable/list')
}

export function bindCaptainGrantToTeam(body: CaptainGrantBindTeamReq) {
  return request.post<any, ApiResult<TeamDetailDto>>('/basket/team/captain/grant/bind-team', body)
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
