import request from '@/utils/request'
import type {
  AjaxResult,
  AvailableMatchPlayerListResult,
  MatchLineupResult,
  MatchLineupSaveReq
} from '@/types'

export function getMatchLineup(matchId: number, teamId: number): Promise<MatchLineupResult> {
  return request({
    url: `/basket/match/${matchId}/lineup`,
    method: 'get',
    params: { teamId }
  })
}

export function listAvailableMatchPlayers(matchId: number, teamId: number): Promise<AvailableMatchPlayerListResult> {
  return request({
    url: `/basket/match/${matchId}/available-player/${teamId}`,
    method: 'get'
  })
}

export function saveMatchLineup(matchId: number, teamId: number, data: MatchLineupSaveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/lineup/${teamId}`,
    method: 'put',
    data
  })
}

export function confirmMatchLineup(matchId: number, teamId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/match/${matchId}/lineup/${teamId}/confirm`,
    method: 'post'
  })
}
