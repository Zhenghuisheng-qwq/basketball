import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  TeamProfileChangeDetailDto,
  TeamProfileChangeHistoryPage,
  TeamProfileChangeSubmitReq,
  TeamProfileChangeSubmitResp,
  TeamProfileEditDto,
} from '@/api/types/teamProfileChange'

export function getTeamProfileEdit(teamId: string) {
  return request.get<any, ApiResult<TeamProfileEditDto>>(`/basket/team/${teamId}/profile/edit`)
}

export function submitTeamProfileChange(teamId: string, body: TeamProfileChangeSubmitReq) {
  return request.post<any, ApiResult<TeamProfileChangeSubmitResp>>(
    `/basket/team/${teamId}/profile/change/submit`,
    body,
  )
}

export function cancelTeamProfileChange(teamId: string, changeId: string, cancelReason?: string) {
  return request.post<any, ApiResult<void>>(
    `/basket/team/${teamId}/profile/change/${changeId}/cancel`,
    cancelReason ? { cancelReason } : {},
  )
}

export function getTeamProfileChangeHistory(
  teamId: string,
  params?: { pageNum?: number; pageSize?: number; auditStatus?: number; keyword?: string },
) {
  return request.get<any, ApiResult<TeamProfileChangeHistoryPage>>(
    `/basket/team/${teamId}/profile/change/history`,
    { params },
  )
}

export function getTeamProfileChangeDetail(teamId: string, changeId: string) {
  return request.get<any, ApiResult<TeamProfileChangeDetailDto>>(
    `/basket/team/${teamId}/profile/change/${changeId}`,
  )
}
