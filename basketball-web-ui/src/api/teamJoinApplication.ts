import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  TeamJoinApplicationCreateReq,
  TeamJoinApplicationItemDto,
  TeamJoinApplicationProcessReq,
} from '@/api/types/teamJoinApplication'

export function createTeamJoinApplication(teamId: string, body: TeamJoinApplicationCreateReq) {
  return request.post<any, ApiResult<TeamJoinApplicationItemDto>>(
    `/basket/team/${teamId}/join-application`,
    body,
  )
}

export function getMyTeamJoinApplicationPage(params?: {
  currentPage?: number
  pageSize?: number
  status?: number
}) {
  return request.get<any, ApiResult<PageResult<TeamJoinApplicationItemDto>>>(
    '/basket/team/join-application/mine/page',
    { params },
  )
}

export function cancelTeamJoinApplication(applicationId: string) {
  return request.put<any, ApiResult<null>>(`/basket/team/join-application/${applicationId}/cancel`)
}

export function getTeamJoinApplicationPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number; status?: number },
) {
  return request.get<any, ApiResult<PageResult<TeamJoinApplicationItemDto>>>(
    `/basket/team/${teamId}/join-application/page`,
    { params },
  )
}

export function processTeamJoinApplication(
  applicationId: string,
  body: TeamJoinApplicationProcessReq,
) {
  return request.put<any, ApiResult<TeamJoinApplicationItemDto>>(
    `/basket/team/join-application/${applicationId}/process`,
    body,
  )
}
