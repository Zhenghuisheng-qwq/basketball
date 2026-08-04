import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketPersonalRegistrationQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  registrationStatus?: string
  playerName?: string
}

export interface BasketPersonalRegistrationListVo {
  personalRegistrationId?: number
  seasonId?: number
  seasonTitle?: string
  divisionId?: number
  divisionName?: string
  platformCode?: string
  playerId?: number
  playerName?: string
  actualPlatformCode?: string
  preferredPosition?: string
  registrationStatus?: string
  submitTime?: string
  approvedTime?: string
  rejectReason?: string
}

export interface BasketPersonalRegistrationAuditVo {
  auditAction?: string
  auditResult?: string
  beforeStatus?: string
  afterStatus?: string
  auditUserName?: string
  auditTime?: string
  auditRemark?: string
}

export interface BasketPersonalRegistrationDetailVo extends BasketPersonalRegistrationListVo {
  gameAccountId?: string
  assignedDrawTeamId?: number
  convertedRegistrationId?: number
  audits?: BasketPersonalRegistrationAuditVo[]
}

export function listV4PersonalRegistration(query: BasketPersonalRegistrationQuery): Promise<TableDataInfo<BasketPersonalRegistrationListVo[]>> {
  return request({ url: '/basket/v4/admin/personal-registrations/list', method: 'get', params: query })
}

export function getV4PersonalRegistrationDetail(personalRegistrationId: number): Promise<AjaxResult<BasketPersonalRegistrationDetailVo>> {
  return request({ url: `/basket/v4/admin/personal-registrations/${personalRegistrationId}`, method: 'get' })
}

export function approveV4PersonalRegistration(personalRegistrationId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-registrations/${personalRegistrationId}/approve`, method: 'post' })
}

export function rejectV4PersonalRegistration(personalRegistrationId: number, data: { rejectReason: string; auditRemark?: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-registrations/${personalRegistrationId}/reject`, method: 'post', data })
}
