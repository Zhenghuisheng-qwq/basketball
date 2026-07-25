import request from '@/utils/request'
import type {
  AjaxResult,
  SeasonRosterChangeRecord,
  SeasonRosterListItem,
  SeasonRosterQuery,
  TableDataInfo
} from '@/types'

export function listSeasonRoster(query: SeasonRosterQuery): Promise<TableDataInfo<SeasonRosterListItem[]>> {
  return request({
    url: '/basket/seasonRoster/list',
    method: 'get',
    params: query
  })
}

export function listSeasonRosterChangeHistory(playerId: number, seasonId: number): Promise<AjaxResult<SeasonRosterChangeRecord[]>> {
  return request({
    url: '/basket/seasonRoster/change-history',
    method: 'get',
    params: { playerId, seasonId }
  })
}
