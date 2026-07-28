import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { MessageItemDto, MessagePageResult } from '@/api/types/accountMessage'

export function getAccountMessagePage(params: {
  currentPage?: number
  pageSize?: number
  readStatus?: number
}) {
  return request.get<any, ApiResult<MessagePageResult>>('/basket/account/message/page', { params })
}

export function getAccountMessageUnreadCount() {
  return request.get<any, ApiResult<number>>('/basket/account/message/unread-count')
}

export function getAccountMessageDetail(receiverId: string | number) {
  return request.get<any, ApiResult<MessageItemDto>>(`/basket/account/message/${receiverId}`)
}

export function markAccountMessageRead(receiverId: string | number) {
  return request.put<any, ApiResult<null>>(`/basket/account/message/${receiverId}/read`)
}

export function markAllAccountMessagesRead() {
  return request.put<any, ApiResult<null>>('/basket/account/message/read-all')
}
