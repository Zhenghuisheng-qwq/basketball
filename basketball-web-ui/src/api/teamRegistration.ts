import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  AvailableSeasonPageDto,
  TeamRegistrationAuditItemDto,
  TeamRegistrationDetailDto,
  TeamRegistrationDraftReq,
  TeamRegistrationItemDto,
  TeamRegistrationRosterReq,
  TeamRegistrationUpdateReq,
  TeamRegistrationWithdrawReq,
} from '@/api/types/teamRegistration'

export function getAvailableSeasonPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number },
) {
  return request.get<any, ApiResult<AvailableSeasonPageDto>>(
    `/basket/team/${teamId}/registration/available-season/page`,
    { params },
  )
}

export function getTeamRegistrationPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number; status?: number },
) {
  return request.get<any, ApiResult<PageResult<TeamRegistrationItemDto>>>(
    `/basket/team/${teamId}/registration/page`,
    { params },
  )
}

export function createTeamRegistrationDraft(teamId: string, body: TeamRegistrationDraftReq) {
  return request.post<any, ApiResult<TeamRegistrationDetailDto>>(
    `/basket/team/${teamId}/registration/draft`,
    body,
  )
}

export function getTeamRegistrationDetail(registrationId: string) {
  return request.get<any, ApiResult<TeamRegistrationDetailDto>>(
    `/basket/team/registration/${registrationId}`,
  )
}

export function updateTeamRegistration(registrationId: string, body: TeamRegistrationUpdateReq) {
  return request.put<any, ApiResult<TeamRegistrationDetailDto>>(
    `/basket/team/registration/${registrationId}`,
    body,
  )
}

export function saveTeamRegistrationRoster(registrationId: string, body: TeamRegistrationRosterReq) {
  return request.put<any, ApiResult<TeamRegistrationDetailDto>>(
    `/basket/team/registration/${registrationId}/roster`,
    body,
  )
}

export function submitTeamRegistration(registrationId: string) {
  return request.post<any, ApiResult<TeamRegistrationDetailDto>>(
    `/basket/team/registration/${registrationId}/submit`,
  )
}

export function withdrawTeamRegistration(
  registrationId: string,
  body?: TeamRegistrationWithdrawReq,
) {
  return request.post<any, ApiResult<TeamRegistrationDetailDto>>(
    `/basket/team/registration/${registrationId}/withdraw`,
    body,
  )
}

export function deleteTeamRegistrationDraft(registrationId: string) {
  return request.delete<any, ApiResult<null>>(`/basket/team/registration/${registrationId}/draft`)
}

export function getTeamRegistrationAuditPage(
  registrationId: string,
  params?: { currentPage?: number; pageSize?: number },
) {
  return request.get<any, ApiResult<PageResult<TeamRegistrationAuditItemDto>>>(
    `/basket/team/registration/${registrationId}/audit-record/page`,
    { params },
  )
}
