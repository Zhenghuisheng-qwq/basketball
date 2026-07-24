import request from '@/utils/request'
import type {
  AjaxResult,
  SeasonCreateReq,
  SeasonDetail,
  SeasonListResult,
  SeasonUpdateReq
} from '@/types'

export function listSeasonByTournament(tournamentId: number): Promise<SeasonListResult> {
  return request({
    url: `/basket/tournament/${tournamentId}/season/list`,
    method: 'get'
  })
}

export function getSeason(seasonId: number): Promise<AjaxResult<SeasonDetail>> {
  return request({
    url: `/basket/season/${seasonId}`,
    method: 'get'
  })
}

export function addSeason(tournamentId: number, data: SeasonCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: `/basket/tournament/${tournamentId}/season`,
    method: 'post',
    data
  })
}

export function updateSeason(seasonId: number, data: SeasonUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/season/${seasonId}`,
    method: 'put',
    data
  })
}

export function publishSeason(seasonId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/season/${seasonId}/publish`,
    method: 'post'
  })
}

export function openSeasonRegistration(seasonId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/season/${seasonId}/open-registration`,
    method: 'post'
  })
}

export function closeSeasonRegistration(seasonId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/season/${seasonId}/close-registration`,
    method: 'post'
  })
}

export function startSeason(seasonId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/season/${seasonId}/start`,
    method: 'post'
  })
}

export function finishSeason(seasonId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/season/${seasonId}/finish`,
    method: 'post'
  })
}

export function delSeason(seasonId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/season/${seasonId}`,
    method: 'delete'
  })
}
