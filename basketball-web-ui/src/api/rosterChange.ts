import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  RosterChangeCaptainTransferReq,
  RosterChangeFreeAgentAddReq,
  RosterChangeRequestItemDto,
  RosterChangeSubmitRespDto,
  RosterChangeTransferReq,
} from '@/api/types/rosterChange'

export function submitRosterChangeTransfer(body: RosterChangeTransferReq) {
  return request.post<any, ApiResult<RosterChangeSubmitRespDto>>(
    '/basket/player/roster-change/transfer',
    body,
  )
}

export function submitRosterChangeFreeAgentAdd(body: RosterChangeFreeAgentAddReq) {
  return request.post<any, ApiResult<RosterChangeSubmitRespDto>>(
    '/basket/player/roster-change/free-agent-add',
    body,
  )
}

export function submitRosterChangeCaptainTransfer(body: RosterChangeCaptainTransferReq) {
  return request.post<any, ApiResult<RosterChangeSubmitRespDto>>(
    '/basket/player/roster-change/captain-transfer',
    body,
  )
}

export function withdrawRosterChangeRequest(requestId: string) {
  return request.post<any, ApiResult<null>>(`/basket/player/roster-change/${requestId}/withdraw`)
}

export function getRosterChangeRequestPage(params?: {
  currentPage?: number
  pageSize?: number
  status?: number
}) {
  return request.get<any, ApiResult<PageResult<RosterChangeRequestItemDto>>>(
    '/basket/player/roster-change/page',
    { params },
  )
}
