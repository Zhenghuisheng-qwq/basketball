import request from '@/utils/request'
import type {
  AjaxResult,
  SeasonCandidateTeam,
  SeasonEndCheck,
  SeasonResultConfirmReq,
  SeasonResultDetail,
  SeasonResultListItem,
  SeasonResultRevokeReq
} from '@/types'

export function listSeasonResult(tournamentId?: number): Promise<AjaxResult<SeasonResultListItem[]>> {
  return request({
    url: '/basket/season-result/list',
    method: 'get',
    params: { tournamentId }
  })
}

export function getSeasonResult(seasonId: number): Promise<AjaxResult<SeasonResultDetail>> {
  return request({
    url: `/basket/season-result/${seasonId}`,
    method: 'get'
  })
}

export function listSeasonCandidateTeam(seasonId: number): Promise<AjaxResult<SeasonCandidateTeam[]>> {
  return request({
    url: `/basket/season-result/${seasonId}/candidate-team`,
    method: 'get'
  })
}

export function checkSeasonEnd(seasonId: number): Promise<AjaxResult<SeasonEndCheck>> {
  return request({
    url: `/basket/season-result/${seasonId}/end-check`,
    method: 'get'
  })
}

export function confirmSeasonResult(seasonId: number, data: SeasonResultConfirmReq): Promise<AjaxResult> {
  return request({
    url: `/basket/season-result/${seasonId}/confirm`,
    method: 'post',
    data
  })
}

export function publishSeasonResult(seasonId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/season-result/${seasonId}/publish`,
    method: 'post'
  })
}

export function revokeSeasonResult(seasonId: number, data: SeasonResultRevokeReq): Promise<AjaxResult> {
  return request({
    url: `/basket/season-result/${seasonId}/revoke`,
    method: 'post',
    data
  })
}
