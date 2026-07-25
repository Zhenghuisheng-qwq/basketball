import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export function getPermanentPlayerArchive(playerId: number): Promise<AjaxResult<any>> {
  return request({ url: `/basket/permanentArchive/player/${playerId}`, method: 'get' })
}

export function getPermanentTeamArchive(teamId: number): Promise<AjaxResult<any>> {
  return request({ url: `/basket/permanentArchive/team/${teamId}`, method: 'get' })
}
