import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketEventTeamQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  sourceTeamId?: number
  teamNameSnapshot?: string
}

export interface BasketEventTeamListVo {
  id?: number
  seasonId?: number
  seasonTitle?: string
  divisionId?: number
  divisionName?: string
  platformCode?: string
  registrationId?: number
  sourceTeamId?: number
  teamNameSnapshot?: string
  captainPlayerName?: string
  eventTeamNo?: string
  eventTeamStatus?: string
  createTime?: string
}

export interface BasketEventPlayerQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  eventTeamId?: number
  sourcePlayerId?: number
}

export interface BasketEventPlayerListVo {
  id?: number
  eventTeamId?: number
  teamNameSnapshot?: string
  sourcePlayerId?: number
  playerNameSnapshot?: string
  gameIdSnapshot?: string
  platformCode?: string
  position?: string
  jerseyNo?: string
  isCaptain?: number
  isStarter?: number
  eventPlayerStatus?: string
}

export function listV4EventTeam(query: BasketEventTeamQuery): Promise<TableDataInfo<BasketEventTeamListVo[]>> {
  return request({ url: '/basket/v4/event-team/list', method: 'get', params: query })
}

export function getV4EventTeam(id: number): Promise<AjaxResult<BasketEventTeamListVo>> {
  return request({ url: `/basket/v4/event-team/${id}`, method: 'get' })
}

export function listV4EventPlayer(query: BasketEventPlayerQuery): Promise<TableDataInfo<BasketEventPlayerListVo[]>> {
  return request({ url: '/basket/v4/event-player/list', method: 'get', params: query })
}
