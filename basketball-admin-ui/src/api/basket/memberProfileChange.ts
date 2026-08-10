import request from '@/utils/request'
import type {
  AjaxResult,
  MemberProfileChangeApproveReq,
  MemberProfileChangeDetail,
  MemberProfileChangeListItem,
  MemberProfileChangeQuery,
  MemberProfileChangeRejectReq,
  TableDataInfo
} from '@/types'

export function listMemberProfileChange(query: MemberProfileChangeQuery): Promise<TableDataInfo<MemberProfileChangeListItem[]>> {
  return request({
    url: '/basket/memberProfileChange/list',
    method: 'get',
    params: query
  })
}

export function getMemberProfileChange(id: number): Promise<AjaxResult<MemberProfileChangeDetail>> {
  return request({
    url: `/basket/memberProfileChange/${id}`,
    method: 'get'
  })
}

export function approveMemberProfileChange(id: number, data?: MemberProfileChangeApproveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/memberProfileChange/${id}/approve`,
    method: 'post',
    data: data || {}
  })
}

export function rejectMemberProfileChange(id: number, data: MemberProfileChangeRejectReq): Promise<AjaxResult> {
  return request({
    url: `/basket/memberProfileChange/${id}/reject`,
    method: 'post',
    data
  })
}
