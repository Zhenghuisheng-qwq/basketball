import request from '@/utils/request'
import type { DisciplineSelectorOption } from '@/api/basket/discipline'
import type { AjaxResult, TableDataInfo } from '@/types'

export function getPermanentPlayerArchive(playerId: number): Promise<AjaxResult<any>> {
  return request({ url: `/basket/permanentArchive/player/${playerId}`, method: 'get' })
}

export function getPermanentTeamArchive(teamId: number): Promise<AjaxResult<any>> {
  return request({ url: `/basket/permanentArchive/team/${teamId}`, method: 'get' })
}

export function searchPermanentPlayers(params: { keyword?: string; pageNum?: number; pageSize?: number }): Promise<TableDataInfo<DisciplineSelectorOption[]>> {
  return request({ url: '/basket/permanentArchive/player-options', method: 'get', params })
}

export function searchPermanentTeams(params: { keyword?: string; pageNum?: number; pageSize?: number }): Promise<TableDataInfo<DisciplineSelectorOption[]>> {
  return request({ url: '/basket/permanentArchive/team-options', method: 'get', params })
}
