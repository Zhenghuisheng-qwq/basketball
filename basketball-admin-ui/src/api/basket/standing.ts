import request from '@/utils/request'
import type {
  AjaxResult,
  SettlementRebuildResult,
  StandingListItem,
  StandingQuery,
  TableDataInfo
} from '@/types'

export function listStanding(query: StandingQuery): Promise<TableDataInfo<StandingListItem[]>> {
  return request({
    url: '/basket/standing/list',
    method: 'get',
    params: query
  })
}

export function rebuildStandingSeason(seasonId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/standing/rebuild/season/${seasonId}`,
    method: 'post'
  })
}
