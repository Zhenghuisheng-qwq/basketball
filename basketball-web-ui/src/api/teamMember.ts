import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  TeamMemberItemDto,
  TeamMemberUpdateReq,
} from '@/api/types/teamMember'
import type { CandidatePlayerPageDto } from '@/api/types/teamJoinInvite'

export function getTeamMemberPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number },
) {
  return request.get<any, ApiResult<PageResult<TeamMemberItemDto>>>(
    `/basket/team/${teamId}/member/page`,
    { params },
  )
}

export function updateTeamMember(
  teamId: string,
  teamMemberId: string,
  body: TeamMemberUpdateReq,
) {
  return request.put<any, ApiResult<TeamMemberItemDto>>(
    `/basket/team/${teamId}/member/${teamMemberId}`,
    body,
  )
}

export function removeTeamMember(teamId: string, teamMemberId: string) {
  return request.delete<any, ApiResult<null>>(`/basket/team/${teamId}/member/${teamMemberId}`)
}

export function leaveTeam(teamId: string) {
  return request.post<any, ApiResult<null>>(`/basket/team/${teamId}/leave`)
}

export function getCandidatePlayerPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number; keyword?: string },
) {
  return request.get<any, ApiResult<CandidatePlayerPageDto>>(
    `/basket/team/${teamId}/candidate-player/page`,
    { params },
  )
}
