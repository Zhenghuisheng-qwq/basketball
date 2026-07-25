import request from '@/utils/request'
import type {
  AjaxResult,
  RosterChangeAuditApproveReq,
  RosterChangeAuditDetail,
  RosterChangeAuditListItem,
  RosterChangeAuditQuery,
  RosterChangeAuditRejectReq,
  TableDataInfo
} from '@/types'

export function listRosterChangeAudit(query: RosterChangeAuditQuery): Promise<TableDataInfo<RosterChangeAuditListItem[]>> {
  return request({
    url: '/basket/rosterChangeAudit/list',
    method: 'get',
    params: query
  })
}

export function getRosterChangeAudit(id: number): Promise<AjaxResult<RosterChangeAuditDetail>> {
  return request({
    url: `/basket/rosterChangeAudit/${id}`,
    method: 'get'
  })
}

export function approveRosterChangeAudit(id: number, data: RosterChangeAuditApproveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/rosterChangeAudit/${id}/approve`,
    method: 'post',
    data
  })
}

export function rejectRosterChangeAudit(id: number, data: RosterChangeAuditRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/rosterChangeAudit/${id}/reject`,
    method: 'post',
    data
  })
}
