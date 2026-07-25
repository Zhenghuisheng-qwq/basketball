import request from '@/utils/request'
import type {
  AjaxResult,
  EventTeamDetail,
  EventTeamListItem,
  EventTeamParticipationStatusReq,
  EventTeamQuery,
  TableDataInfo
} from '@/types'

export function listEventTeam(query: EventTeamQuery): Promise<TableDataInfo<EventTeamListItem[]>> {
  return request({
    url: '/basket/eventTeam/list',
    method: 'get',
    params: query
  })
}

export function getEventTeam(id: number): Promise<AjaxResult<EventTeamDetail>> {
  return request({
    url: `/basket/eventTeam/${id}`,
    method: 'get'
  })
}

export function lockEventTeamRoster(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/eventTeam/${id}/lock-roster`,
    method: 'post'
  })
}

export function unlockEventTeamRoster(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/eventTeam/${id}/unlock-roster`,
    method: 'post'
  })
}

export function updateEventTeamParticipationStatus(id: number, data: EventTeamParticipationStatusReq): Promise<AjaxResult> {
  return request({
    url: `/basket/eventTeam/${id}/participation-status`,
    method: 'post',
    data
  })
}
