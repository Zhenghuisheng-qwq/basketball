import request from '@/utils/request'
import type {
  AjaxResult,
  TableDataInfo,
  TournamentCreateReq,
  TournamentDetail,
  TournamentListItem,
  TournamentQuery,
  TournamentUpdateReq
} from '@/types'

export function listTournament(query: TournamentQuery): Promise<TableDataInfo<TournamentListItem[]>> {
  return request({
    url: '/basket/tournament/list',
    method: 'get',
    params: query
  })
}

export function getTournament(id: number): Promise<AjaxResult<TournamentDetail>> {
  return request({
    url: `/basket/tournament/${id}`,
    method: 'get'
  })
}

export function addTournament(data: TournamentCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/tournament',
    method: 'post',
    data
  })
}

export function updateTournament(id: number, data: TournamentUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/tournament/${id}`,
    method: 'put',
    data
  })
}

export function publishTournament(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/tournament/${id}/publish`,
    method: 'post'
  })
}

export function unpublishTournament(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/tournament/${id}/unpublish`,
    method: 'post'
  })
}

export function closeTournament(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/tournament/${id}/close`,
    method: 'post'
  })
}

export function delTournament(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/tournament/${id}`,
    method: 'delete'
  })
}
