import request from '@/utils/request'
import type {
  AjaxResult,
  PlayerSeasonStatListItem,
  PlayerSeasonStatQuery,
  SettlementRebuildResult,
  TableDataInfo
} from '@/types'

export function listPlayerSeasonStat(query: PlayerSeasonStatQuery): Promise<TableDataInfo<PlayerSeasonStatListItem[]>> {
  return request({
    url: '/basket/player-season-stat/list',
    method: 'get',
    params: query
  })
}

export function rebuildPlayerSeasonStat(seasonId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/player-season-stat/rebuild/season/${seasonId}`,
    method: 'post'
  })
}
