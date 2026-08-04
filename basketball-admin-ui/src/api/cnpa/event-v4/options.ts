import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface V4AdminOptionVo {
  id: number
  label: string
  subLabel?: string
  platformCode?: string
  eventTeamCount?: number
}

export interface V4AdminTeamPlayerOptionVo {
  playerId: number
  playerName: string
  memberId?: number
  memberNickname?: string
  certified: boolean
  hasPlatformIdentity: boolean
  roleType?: string
}

export function listV4AdminSeasonOptions(): Promise<AjaxResult<V4AdminOptionVo[]>> {
  return request({ url: '/basket/v4/admin/options/seasons', method: 'get' })
}

export function listV4AdminDivisionOptions(seasonId: number): Promise<AjaxResult<V4AdminOptionVo[]>> {
  return request({ url: '/basket/v4/admin/options/divisions', method: 'get', params: { seasonId } })
}

export function listV4AdminTeamOptions(divisionId: number): Promise<AjaxResult<V4AdminOptionVo[]>> {
  return request({ url: '/basket/v4/admin/options/teams', method: 'get', params: { divisionId } })
}

export function listV4AdminTeamPlayerOptions(teamId: number, divisionId: number): Promise<AjaxResult<V4AdminTeamPlayerOptionVo[]>> {
  return request({ url: '/basket/v4/admin/options/team-players', method: 'get', params: { teamId, divisionId } })
}
