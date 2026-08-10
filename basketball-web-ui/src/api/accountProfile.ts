import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  AccountProfileChangeDto,
  AccountProfileChangeSubmitPayload,
  AccountProfileDto,
  AccountProfileUpdatePayload,
} from '@/api/types/accountProfile'

export function getAccountProfile() {
  return request.get<any, ApiResult<AccountProfileDto>>('/basket/account/profile')
}

export function updateAccountProfile(payload: AccountProfileUpdatePayload) {
  return request.put<any, ApiResult<AccountProfileDto>>('/basket/account/profile', payload)
}

/** 预览头像 URL，不写入正式资料 */
export function bindAccountAvatar(fileId: string) {
  return request.post<any, ApiResult<string>>('/basket/account/profile/avatar', { fileId })
}

export function submitAccountProfileChange(payload: AccountProfileChangeSubmitPayload) {
  return request.post<any, ApiResult<AccountProfileChangeDto>>('/basket/account/profile/change', payload)
}
