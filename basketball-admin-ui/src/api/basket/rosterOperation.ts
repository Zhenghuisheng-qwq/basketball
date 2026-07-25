import request from '@/utils/request'
import type {
  AjaxResult,
  RosterOperationForceRemoveReq,
  RosterOperationLogItem,
  RosterOperationLogQuery,
  RosterOperationUpdateRoleReq,
  TableDataInfo
} from '@/types'

export function listRosterOperationLogs(query: RosterOperationLogQuery): Promise<TableDataInfo<RosterOperationLogItem[]>> {
  return request({
    url: '/basket/rosterOperation/log/list',
    method: 'get',
    params: query
  })
}

export function forceRemoveRoster(data: RosterOperationForceRemoveReq): Promise<AjaxResult> {
  return request({
    url: '/basket/rosterOperation/force-remove',
    method: 'post',
    data
  })
}

export function updateRosterRole(data: RosterOperationUpdateRoleReq): Promise<AjaxResult> {
  return request({
    url: '/basket/rosterOperation/update-role',
    method: 'post',
    data
  })
}
