import request from '@/utils/request'
import type {
  AjaxResult,
  OpenRegistrationAuditApproveReq,
  OpenRegistrationAuditDetail,
  OpenRegistrationAuditListItem,
  OpenRegistrationAuditQuery,
  OpenRegistrationAuditRecord,
  OpenRegistrationAuditRejectReq,
  BatchAuditOperationReq,
  BatchAuditResult,
  TableDataInfo
} from '@/types'

export function listOpenRegistrationAudit(query: OpenRegistrationAuditQuery): Promise<TableDataInfo<OpenRegistrationAuditListItem[]>> {
  return request({
    url: '/basket/openRegistrationAudit/list',
    method: 'get',
    params: query
  })
}

export function getOpenRegistrationAudit(id: number): Promise<AjaxResult<OpenRegistrationAuditDetail>> {
  return request({
    url: `/basket/openRegistrationAudit/${id}`,
    method: 'get'
  })
}

export function startOpenRegistrationAuditReview(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/openRegistrationAudit/${id}/start-review`,
    method: 'post'
  })
}

export function approveOpenRegistrationAudit(id: number, data: OpenRegistrationAuditApproveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/openRegistrationAudit/${id}/approve`,
    method: 'post',
    data
  })
}

export function rejectOpenRegistrationAudit(id: number, data: OpenRegistrationAuditRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/openRegistrationAudit/${id}/reject`,
    method: 'post',
    data
  })
}

export function listOpenRegistrationAuditRecords(id: number): Promise<AjaxResult<OpenRegistrationAuditRecord[]>> {
  return request({
    url: `/basket/openRegistrationAudit/${id}/audit-record/list`,
    method: 'get'
  })
}

export function batchApproveOpenRegistrationAudit(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({ url: '/basket/openRegistrationAudit/batch-approve', method: 'post', data })
}

export function batchRejectOpenRegistrationAudit(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({ url: '/basket/openRegistrationAudit/batch-reject', method: 'post', data })
}
