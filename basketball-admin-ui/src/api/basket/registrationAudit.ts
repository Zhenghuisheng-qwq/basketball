import request from '@/utils/request'
import type {
  AjaxResult,
  RegistrationAuditApproveReq,
  RegistrationAuditDetail,
  RegistrationAuditListItem,
  RegistrationAuditQuery,
  RegistrationAuditRecord,
  RegistrationAuditRejectReq,
  BatchAuditOperationReq,
  BatchAuditResult,
  TableDataInfo
} from '@/types'

export function listRegistrationAudit(query: RegistrationAuditQuery): Promise<TableDataInfo<RegistrationAuditListItem[]>> {
  return request({
    url: '/basket/registrationAudit/list',
    method: 'get',
    params: query
  })
}

export function getRegistrationAudit(id: number): Promise<AjaxResult<RegistrationAuditDetail>> {
  return request({
    url: `/basket/registrationAudit/${id}`,
    method: 'get'
  })
}

export function approveRegistrationAudit(id: number, data: RegistrationAuditApproveReq = {}): Promise<AjaxResult> {
  return request({
    url: `/basket/registrationAudit/${id}/approve`,
    method: 'post',
    data
  })
}

export function rejectRegistrationAudit(id: number, data: RegistrationAuditRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/registrationAudit/${id}/reject`,
    method: 'post',
    data
  })
}

export function listRegistrationAuditRecords(id: number): Promise<AjaxResult<RegistrationAuditRecord[]>> {
  return request({
    url: `/basket/registrationAudit/${id}/audit-record/list`,
    method: 'get'
  })
}

export function batchApproveRegistrationAudit(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({ url: '/basket/registrationAudit/batch-approve', method: 'post', data })
}

export function batchRejectRegistrationAudit(data: BatchAuditOperationReq): Promise<AjaxResult<BatchAuditResult>> {
  return request({ url: '/basket/registrationAudit/batch-reject', method: 'post', data })
}
