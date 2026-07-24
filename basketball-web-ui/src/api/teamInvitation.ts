import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  TeamInvitationCreateReq,
  TeamInvitationItemDto,
  TeamInvitationRespondReq,
} from '@/api/types/teamInvitation'

export function createTeamInvitation(teamId: string, body: TeamInvitationCreateReq) {
  return request.post<any, ApiResult<TeamInvitationItemDto>>(
    `/basket/team/${teamId}/invitation`,
    body,
  )
}

export function getTeamInvitationPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number; status?: number },
) {
  return request.get<any, ApiResult<PageResult<TeamInvitationItemDto>>>(
    `/basket/team/${teamId}/invitation/page`,
    { params },
  )
}

export function getMyTeamInvitationPage(params?: {
  currentPage?: number
  pageSize?: number
  status?: number
}) {
  return request.get<any, ApiResult<PageResult<TeamInvitationItemDto>>>(
    '/basket/team/invitation/mine/page',
    { params },
  )
}

export function respondTeamInvitation(invitationId: string, body: TeamInvitationRespondReq) {
  return request.put<any, ApiResult<TeamInvitationItemDto>>(
    `/basket/team/invitation/${invitationId}/respond`,
    body,
  )
}
