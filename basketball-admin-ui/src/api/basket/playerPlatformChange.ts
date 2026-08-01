import request from '@/utils/request'
import type {
  AjaxResult,
  PlayerPlatformChangeApproveReq,
  PlayerPlatformChangeDetail,
  PlayerPlatformChangeListItem,
  PlayerPlatformChangeQuery,
  PlayerPlatformChangeRejectReq,
  BatchAuditOperationReq,
  BatchAuditResult,
  TableDataInfo
} from '@/types'

export function listPlayerPlatformChange(query: PlayerPlatformChangeQuery): Promise<TableDataInfo<PlayerPlatformChangeListItem[]>> {
  return request({
    url: '/basket/playerPlatformChange/list',
    method: 'get',
    params: query
  })
}

export function getPlayerPlatformChange(id: number): Promise<AjaxResult<PlayerPlatformChangeDetail>> {
  return request({
    url: `/basket/playerPlatformChange/${id}`,
    method: 'get'
  })
}

export function approvePlayerPlatformChange(id: number, data: PlayerPlatformChangeApproveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/playerPlatformChange/${id}/approve`,
    method: 'post',
    data
  })
}

export function rejectPlayerPlatformChange(id: number, data: PlayerPlatformChangeRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/playerPlatformChange/${id}/reject`,
    method: 'post',
    data
  })
}

export function batchApprovePlayerPlatformChange(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({ url: '/basket/playerPlatformChange/batch-approve', method: 'post', data })
}

export function batchRejectPlayerPlatformChange(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({ url: '/basket/playerPlatformChange/batch-reject', method: 'post', data })
}
