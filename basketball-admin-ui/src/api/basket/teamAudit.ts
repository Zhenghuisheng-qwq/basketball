import request from '@/utils/request'
import type {
  AjaxResult,
  TableDataInfo,
  TeamAuditApproveReq,
  TeamAuditDetail,
  TeamAuditListItem,
  TeamAuditQuery,
  TeamAuditRecord,
  TeamAuditRejectReq
} from '@/types'

export function listTeamAudit(query: TeamAuditQuery): Promise<TableDataInfo<TeamAuditListItem[]>> {
  return request({
    url: '/basket/teamAudit/list',
    method: 'get',
    params: query
  })
}

export function getTeamAudit(teamId: number): Promise<AjaxResult<TeamAuditDetail>> {
  return request({
    url: `/basket/teamAudit/${teamId}`,
    method: 'get'
  })
}

export function listTeamAuditRecords(teamId: number): Promise<AjaxResult<TeamAuditRecord[]>> {
  return request({
    url: `/basket/teamAudit/${teamId}/audit-record/list`,
    method: 'get'
  })
}

export function startTeamAuditReview(teamId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/teamAudit/${teamId}/start-review`,
    method: 'post'
  })
}

export function approveTeamAudit(teamId: number, data: TeamAuditApproveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/teamAudit/${teamId}/approve`,
    method: 'post',
    data
  })
}

export function rejectTeamAudit(teamId: number, data: TeamAuditRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/teamAudit/${teamId}/reject`,
    method: 'post',
    data
  })
}
