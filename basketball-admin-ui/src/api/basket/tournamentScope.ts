import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface TournamentScopeGrantReq {
  adminUserId?: number
  tournamentId?: number
  scopeCode?: string
  expireTime?: string
}

export function listTournamentScope(params?: { pageNum?: number; pageSize?: number; tournamentId?: number; adminUserId?: number }): Promise<TableDataInfo<any[]>> {
  return request({ url: '/basket/tournamentScope/list', method: 'get', params })
}

export function grantTournamentScope(data: TournamentScopeGrantReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/tournamentScope/grant', method: 'post', data })
}

export function revokeTournamentScope(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/tournamentScope/${id}/revoke`, method: 'post' })
}
