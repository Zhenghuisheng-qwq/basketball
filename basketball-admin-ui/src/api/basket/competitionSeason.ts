import request from '@/utils/request'
import type {
  AjaxResult,
  CompetitionSeasonCreateReq,
  CompetitionSeasonDetail,
  CompetitionSeasonListItem,
  CompetitionSeasonQuery,
  CompetitionSeasonUpdateReq,
  TableDataInfo
} from '@/types'

export function listCompetitionSeason(query?: CompetitionSeasonQuery): Promise<TableDataInfo<CompetitionSeasonListItem[]>> {
  return request({
    url: '/basket/competition-season/list',
    method: 'get',
    params: query
  })
}

export function listCompetitionSeasonOptions(): Promise<AjaxResult<CompetitionSeasonListItem[]>> {
  return request({
    url: '/basket/competition-season/options',
    method: 'get'
  })
}

export function getCompetitionSeason(id: number): Promise<AjaxResult<CompetitionSeasonDetail>> {
  return request({
    url: `/basket/competition-season/${id}`,
    method: 'get'
  })
}

export function addCompetitionSeason(data: CompetitionSeasonCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/competition-season',
    method: 'post',
    data
  })
}

export function updateCompetitionSeason(id: number, data: CompetitionSeasonUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/competition-season/${id}`,
    method: 'put',
    data
  })
}

export function delCompetitionSeason(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/competition-season/${id}`,
    method: 'delete'
  })
}
