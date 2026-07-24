import request from '@/utils/request'
import type {
  AjaxResult,
  PlayerRankListItem,
  RankingQuery,
  SettlementRebuildResult,
  TableDataInfo,
  TeamRankListItem
} from '@/types'

export function listTeamRanking(query: RankingQuery): Promise<TableDataInfo<TeamRankListItem[]>> {
  return request({
    url: '/basket/ranking/team/list',
    method: 'get',
    params: query
  })
}

export function listPlayerRanking(query: RankingQuery): Promise<TableDataInfo<PlayerRankListItem[]>> {
  return request({
    url: '/basket/ranking/player/list',
    method: 'get',
    params: query
  })
}

export function rebuildRankingSeason(seasonId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/ranking/rebuild/season/${seasonId}`,
    method: 'post'
  })
}
