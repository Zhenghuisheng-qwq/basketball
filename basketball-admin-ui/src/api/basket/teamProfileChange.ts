import request from '@/utils/request'
import type {
  AjaxResult,
  TeamProfileChangeApproveReq,
  TeamProfileChangeDetail,
  TeamProfileChangeListItem,
  TeamProfileChangeQuery,
  TeamProfileChangeRejectReq,
  TableDataInfo
} from '@/types'

export function listTeamProfileChange(query: TeamProfileChangeQuery): Promise<TableDataInfo<TeamProfileChangeListItem[]>> {
  return request({
    url: '/basket/teamProfileChange/list',
    method: 'get',
    params: query
  })
}

export function getTeamProfileChange(id: number): Promise<AjaxResult<TeamProfileChangeDetail>> {
  return request({
    url: `/basket/teamProfileChange/${id}`,
    method: 'get'
  })
}

export function approveTeamProfileChange(id: number, data: TeamProfileChangeApproveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/teamProfileChange/${id}/approve`,
    method: 'post',
    data
  })
}

export function rejectTeamProfileChange(id: number, data: TeamProfileChangeRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/teamProfileChange/${id}/reject`,
    method: 'post',
    data
  })
}
