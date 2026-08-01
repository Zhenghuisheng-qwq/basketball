import request from '@/utils/request'
import type {
  AjaxResult,
  PlayerCertApproveReq,
  PlayerCertAuditRecord,
  PlayerCertDetail,
  PlayerCertListItem,
  PlayerCertQuery,
  PlayerCertRejectReq,
  BatchAuditOperationReq,
  BatchAuditResult,
  TableDataInfo
} from '@/types'

export function listPlayerCert(query: PlayerCertQuery): Promise<TableDataInfo<PlayerCertListItem[]>> {
  return request({
    url: '/basket/playerCert/list',
    method: 'get',
    params: query
  })
}

export function getPlayerCert(applicationId: number): Promise<AjaxResult<PlayerCertDetail>> {
  return request({
    url: `/basket/playerCert/${applicationId}`,
    method: 'get'
  })
}

export function listPlayerCertAuditRecords(applicationId: number): Promise<AjaxResult<PlayerCertAuditRecord[]>> {
  return request({
    url: `/basket/playerCert/${applicationId}/audit-record/list`,
    method: 'get'
  })
}

export function startPlayerCertReview(applicationId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/playerCert/${applicationId}/start-review`,
    method: 'post'
  })
}

export function approvePlayerCert(applicationId: number, data: PlayerCertApproveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/playerCert/${applicationId}/approve`,
    method: 'post',
    data
  })
}

export function rejectPlayerCert(applicationId: number, data: PlayerCertRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/playerCert/${applicationId}/reject`,
    method: 'post',
    data
  })
}

export function batchApprovePlayerCert(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({
    url: '/basket/playerCert/batch-approve',
    method: 'post',
    data
  })
}

export function batchRejectPlayerCert(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({
    url: '/basket/playerCert/batch-reject',
    method: 'post',
    data
  })
}
