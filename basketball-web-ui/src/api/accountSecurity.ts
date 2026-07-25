import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  ChangePasswordPayload,
  ChangePasswordResultDto,
  LoginLogItemDto,
  SecurityLogItemDto,
} from '@/api/types/accountSecurity'

export function getLoginLogPage(params: { currentPage?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageResult<LoginLogItemDto>>>('/basket/account/security/login-log/page', {
    params,
  })
}

export function getOperationLogPage(params: { currentPage?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageResult<SecurityLogItemDto>>>('/basket/account/security/operation-log/page', {
    params,
  })
}

export function changeAccountPassword(payload: ChangePasswordPayload) {
  return request.post<any, ApiResult<ChangePasswordResultDto>>('/basket/account/security/change-password', payload)
}
