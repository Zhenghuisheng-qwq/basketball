import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PlatformAccountAddPayload,
  PlatformAccountChangeGameIdPayload,
  PlatformAccountItemDto,
  PlatformChangeHistoryItemDto,
  PlatformChangeSubmitDto,
  PlayerEligibilityStatusDto,
} from '@/api/types/playerPlatformAccount'

export function getPlatformAccountList() {
  return request.get<any, ApiResult<PlatformAccountItemDto[]>>('/basket/player/platform-account/list')
}

export function addPlatformAccount(payload: PlatformAccountAddPayload) {
  return request.post<any, ApiResult<PlatformChangeSubmitDto>>('/basket/player/platform-account/add', payload)
}

export function changePlatformGameId(platformAccountId: string, payload: PlatformAccountChangeGameIdPayload) {
  return request.post<any, ApiResult<PlatformChangeSubmitDto>>(
    `/basket/player/platform-account/${platformAccountId}/change-game-id`,
    payload,
  )
}

export function getPlatformChangeHistory(params?: { currentPage?: number; pageSize?: number }) {
  if (params?.currentPage != null || params?.pageSize != null) {
    return request.get<any, ApiResult<PageResult<PlatformChangeHistoryItemDto>>>(
      '/basket/player/platform-account/change-history',
      { params },
    )
  }
  return request.get<any, ApiResult<PlatformChangeHistoryItemDto[]>>('/basket/player/platform-account/change-history')
}

export function getPlayerEligibilityStatus() {
  return request.get<any, ApiResult<PlayerEligibilityStatusDto>>('/basket/player/platform-account/eligibility')
}
