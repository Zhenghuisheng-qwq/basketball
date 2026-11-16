import request from '@/utils/request'

export function listTeamMembershipChange(query: any) {
  return request({
    url: '/basket/teamMembershipChange/list',
    method: 'get',
    params: query
  })
}

export function getTeamMembershipChange(id: number | string) {
  return request({
    url: '/basket/teamMembershipChange/' + id,
    method: 'get'
  })
}

export function approveTeamMembershipChange(id: number | string, data?: { auditRemark?: string }) {
  return request({
    url: '/basket/teamMembershipChange/' + id + '/approve',
    method: 'post',
    data: data || {}
  })
}

export function getCaptainDesk(teamNo: string) {
  return request({
    url: '/basket/teamMembershipChange/desk',
    method: 'get',
    params: { teamNo }
  })
}

export function transferTeamCaptain(data: { teamId: number | string; teamMemberId: number | string }) {
  return request({
    url: '/basket/teamMembershipChange/transfer',
    method: 'post',
    data
  })
}

export function submitTeamDissolve(data: { teamId: number | string; reason?: string }) {
  return request({
    url: '/basket/teamMembershipChange/dissolve',
    method: 'post',
    data
  })
}

export function rejectTeamMembershipChange(id: number | string, data: { rejectReason: string }) {
  return request({
    url: '/basket/teamMembershipChange/' + id + '/reject',
    method: 'post',
    data
  })
}
