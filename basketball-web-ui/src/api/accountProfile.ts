import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { AccountProfileDto, AccountProfileUpdatePayload } from '@/api/types/accountProfile'

export function getAccountProfile() {
  return request.get<any, ApiResult<AccountProfileDto>>('/basket/account/profile')
}

export function updateAccountProfile(payload: AccountProfileUpdatePayload) {
  return request.put<any, ApiResult<AccountProfileDto>>('/basket/account/profile', payload)
}

export function bindAccountAvatar(fileId: string) {
  return request.post<any, ApiResult<string>>('/basket/account/profile/avatar', { fileId })
}
