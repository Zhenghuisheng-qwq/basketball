import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface DisciplineQuery {
  seasonId?: number
  tournamentId?: number
  teamId?: number
  playerId?: number
  memberId?: number
  targetType?: string
  recordStatus?: string
  penaltyCode?: string
  keyword?: string
}

export interface DisciplineCreateReq {
  targetType?: string
  seasonId?: number
  matchId?: number
  teamId?: number
  eventTeamId?: number
  playerId?: number
  memberId?: number
  disciplineType?: string
  penaltyCode?: string
  title?: string
  reasonText?: string
  penaltyContent?: string
  matchSuspensionCount?: number
  pointDeduction?: number
  effectiveStartTime?: string
  effectiveEndTime?: string
  idempotencyKey?: string
}

export function listDiscipline(query: DisciplineQuery): Promise<AjaxResult<any[]>> {
  return request({ url: '/basket/discipline/list', method: 'get', params: query })
}

export function getDiscipline(id: number): Promise<AjaxResult<any>> {
  return request({ url: `/basket/discipline/${id}`, method: 'get' })
}

export function createDiscipline(data: DisciplineCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/discipline', method: 'post', data })
}

export function activateDiscipline(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/discipline/${id}/activate`, method: 'post' })
}

export function revokeDiscipline(id: number, data: { revokeReason: string }): Promise<AjaxResult> {
  return request({ url: `/basket/discipline/${id}/revoke`, method: 'post', data })
}
