import request from '@/utils/request'
import type {
  AjaxResult,
  CaptainGrantSeasonOption,
  CaptainQualificationBindTeamReq,
  CaptainQualificationDetail,
  CaptainQualificationGrantReq,
  CaptainQualificationListItem,
  CaptainQualificationQuery,
  CaptainQualificationRevokeReq,
  CaptainQualificationUpdateReq,
  EligibleCaptainListResult,
  TableDataInfo
} from '@/types'

export function listGrantSeasonOptions(): Promise<AjaxResult<CaptainGrantSeasonOption[]>> {
  return request({
    url: '/basket/captainQualification/grant-season-options',
    method: 'get'
  })
}

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

export function bindCaptainQualificationTeam(qualificationId: number, data: CaptainQualificationBindTeamReq): Promise<AjaxResult> {
  return request({
    url: `/basket/captainQualification/${qualificationId}/bind-team`,
    method: 'post',
    data
  })
}

export function listCaptainGrantLogs(qualificationId: number): Promise<AjaxResult<import('@/types').CaptainGrantLogItem[]>> {
  return request({
    url: `/basket/captainQualification/${qualificationId}/logs`,
    method: 'get'
  })
}
