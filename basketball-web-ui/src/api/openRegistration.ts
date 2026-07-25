import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  OpenAvailableSeasonItemDto,
  OpenRegistrationDetailDto,
  OpenRegistrationDraftReq,
  OpenRegistrationItemDto,
  OpenRegistrationWithdrawReq,
} from '@/api/types/openRegistration'

export function getOpenAvailableSeasonPage(params?: { currentPage?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageResult<OpenAvailableSeasonItemDto>>>(
    '/basket/player/open-registration/available-season/page',
    { params },
  )
}

export function createOpenRegistrationDraft(body: OpenRegistrationDraftReq) {
  return request.post<any, ApiResult<OpenRegistrationDetailDto>>(
    '/basket/player/open-registration/draft',
    body,
  )
}

export function getOpenRegistrationDetail(registrationId: string) {
  return request.get<any, ApiResult<OpenRegistrationDetailDto>>(
    `/basket/player/open-registration/${registrationId}`,
  )
}

export function submitOpenRegistration(registrationId: string) {
  return request.post<any, ApiResult<OpenRegistrationDetailDto>>(
    `/basket/player/open-registration/${registrationId}/submit`,
  )
}

export function withdrawOpenRegistration(
  registrationId: string,
  body?: OpenRegistrationWithdrawReq,
) {
  return request.post<any, ApiResult<OpenRegistrationDetailDto>>(
    `/basket/player/open-registration/${registrationId}/withdraw`,
    body,
  )
}

export function getOpenRegistrationPage(params?: {
  currentPage?: number
  pageSize?: number
  status?: number
}) {
  return request.get<any, ApiResult<PageResult<OpenRegistrationItemDto>>>(
    '/basket/player/open-registration/page',
    { params },
  )
}
