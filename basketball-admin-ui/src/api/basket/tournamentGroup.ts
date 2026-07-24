import request from '@/utils/request'
import type {
  AjaxResult,
  AvailableTeamListResult,
  GroupCreateReq,
  GroupDetail,
  GroupListResult,
  GroupTeamAssignReq,
  GroupTeamListResult,
  GroupUpdateReq
} from '@/types'

export function listGroupByStage(stageId: number): Promise<GroupListResult> {
  return request({
    url: `/basket/stage/${stageId}/group/list`,
    method: 'get'
  })
}

export function getGroup(groupId: number): Promise<AjaxResult<GroupDetail>> {
  return request({
    url: `/basket/group/${groupId}`,
    method: 'get'
  })
}

export function addGroup(stageId: number, data: GroupCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: `/basket/stage/${stageId}/group`,
    method: 'post',
    data
  })
}

export function updateGroup(groupId: number, data: GroupUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/group/${groupId}`,
    method: 'put',
    data
  })
}

export function delGroup(groupId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/group/${groupId}`,
    method: 'delete'
  })
}

export function listGroupTeams(groupId: number): Promise<GroupTeamListResult> {
  return request({
    url: `/basket/group/${groupId}/team/list`,
    method: 'get'
  })
}

export function listAvailableTeams(groupId: number): Promise<AvailableTeamListResult> {
  return request({
    url: `/basket/group/${groupId}/available-team`,
    method: 'get'
  })
}

export function assignGroupTeams(groupId: number, data: GroupTeamAssignReq): Promise<AjaxResult> {
  return request({
    url: `/basket/group/${groupId}/team`,
    method: 'put',
    data
  })
}
