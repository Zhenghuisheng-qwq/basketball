import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketRegistrationAuditQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  registrationStatus?: string
  teamName?: string
  captainPlayerName?: string
}

export interface BasketRegistrationAuditListVo {
  registrationId?: number
  seasonTitle?: string
  divisionName?: string
  platformCode?: string
  teamId?: number
  teamName?: string
  captainPlayerId?: number
  captainPlayerName?: string
  lineupCount?: number
  registrationStatus?: string
  submitTime?: string
}

export interface BasketRegistrationPlayerVo {
  playerId?: number
  playerName?: string
  gameIdSnapshot?: string
  platformCode?: string
  position?: string
  jerseyNo?: string
  isCaptain?: number
  isStarter?: number
}

export interface BasketRegistrationAuditLogVo {
  auditAction?: string
  auditResult?: string
  beforeStatus?: string
  afterStatus?: string
  auditUserName?: string
  auditTime?: string
  auditRemark?: string
}

export interface BasketRegistrationAuditDetailVo {
  registrationId?: number
  seasonTitle?: string
  divisionName?: string
  platformCode?: string
  teamName?: string
  captainPlayerName?: string
  lineupCount?: number
  registrationStatus?: string
  submitTime?: string
  approveTime?: string
  rejectReason?: string
  players?: BasketRegistrationPlayerVo[]
  audits?: BasketRegistrationAuditLogVo[]
}

export function listV4RegistrationAudit(query: BasketRegistrationAuditQuery): Promise<TableDataInfo<BasketRegistrationAuditListVo[]>> {
  return request({ url: '/basket/v4/registration-audit/list', method: 'get', params: query })
}

export function getV4RegistrationAuditDetail(registrationId: number): Promise<AjaxResult<BasketRegistrationAuditDetailVo>> {
  return request({ url: `/basket/v4/registration-audit/${registrationId}`, method: 'get' })
}

export function approveV4RegistrationAudit(registrationId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/registration-audit/${registrationId}/approve`, method: 'post' })
}

export function rejectV4RegistrationAudit(registrationId: number, data: { rejectReason: string; auditRemark?: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/registration-audit/${registrationId}/reject`, method: 'post', data })
}
