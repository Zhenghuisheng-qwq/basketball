import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

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

export interface DisciplineSelectorOption {
  id: number | string
  label: string
  secondaryLabel?: string
  status?: string
  memberId?: number | string
  teamId?: number | string
  eventTeamId?: number | string
  eventPlayerId?: number | string
  teamName?: string
  playerNo?: string
  gameId?: string
  divisionName?: string
  platformCode?: string
  gameMode?: string
}

export function listDiscipline(query: DisciplineQuery & { pageNum?: number; pageSize?: number }): Promise<TableDataInfo<any[]>> {
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

export function retryDisciplineExecution(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/discipline/${id}/retry-execution`, method: 'post' })
}

export function revokeDiscipline(id: number, data: { revokeReason: string }): Promise<AjaxResult> {
  return request({ url: `/basket/discipline/${id}/revoke`, method: 'post', data })
}

export function searchDisciplineSeasons(params: { keyword?: string; pageNum?: number; pageSize?: number }): Promise<TableDataInfo<DisciplineSelectorOption[]>> {
  return request({ url: '/basket/discipline/selector/seasons', method: 'get', params })
}

export function searchDisciplinePlayers(params: { seasonId: number; keyword?: string; pageNum?: number; pageSize?: number }): Promise<TableDataInfo<DisciplineSelectorOption[]>> {
  return request({ url: '/basket/discipline/selector/players', method: 'get', params })
}

export function searchDisciplineTeams(params: {
  seasonId: number
  targetType: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}): Promise<TableDataInfo<DisciplineSelectorOption[]>> {
  return request({ url: '/basket/discipline/selector/teams', method: 'get', params })
}
