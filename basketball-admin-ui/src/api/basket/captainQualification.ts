import request from '@/utils/request'
import type {
  AjaxResult,
  CaptainQualificationDetail,
  CaptainQualificationGrantReq,
  CaptainQualificationListItem,
  CaptainQualificationQuery,
  CaptainQualificationRevokeReq,
  CaptainQualificationUpdateReq,
  EligibleCaptainListResult,
  TableDataInfo
} from '@/types'

export function listEligibleCaptains(): Promise<EligibleCaptainListResult> {
  return request({
    url: '/basket/captainQualification/eligible-list',
    method: 'get'
  })
}

export function listCaptainQualification(query: CaptainQualificationQuery): Promise<TableDataInfo<CaptainQualificationListItem[]>> {
  return request({
    url: '/basket/captainQualification/list',
    method: 'get',
    params: query
  })
}

export function getCaptainQualification(qualificationId: number): Promise<AjaxResult<CaptainQualificationDetail>> {
  return request({
    url: `/basket/captainQualification/${qualificationId}`,
    method: 'get'
  })
}

export function grantCaptainQualification(data: CaptainQualificationGrantReq): Promise<AjaxResult> {
  return request({
    url: '/basket/captainQualification/grant',
    method: 'post',
    data
  })
}

export function updateCaptainQualification(qualificationId: number, data: CaptainQualificationUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/captainQualification/${qualificationId}`,
    method: 'put',
    data
  })
}

export function revokeCaptainQualification(qualificationId: number, data: CaptainQualificationRevokeReq): Promise<AjaxResult> {
  return request({
    url: `/basket/captainQualification/${qualificationId}/revoke`,
    method: 'post',
    data
  })
}
