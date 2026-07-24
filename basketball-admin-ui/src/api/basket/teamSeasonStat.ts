import request from '@/utils/request'
import type {
  AjaxResult,
  SettlementRebuildResult,
  TableDataInfo,
  TeamSeasonStatListItem,
  TeamSeasonStatQuery
} from '@/types'

export function listTeamSeasonStat(query: TeamSeasonStatQuery): Promise<TableDataInfo<TeamSeasonStatListItem[]>> {
  return request({
    url: '/basket/team-season-stat/list',
    method: 'get',
    params: query
  })
}

export function rebuildTeamSeasonStat(seasonId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/team-season-stat/rebuild/season/${seasonId}`,
    method: 'post'
  })
}
