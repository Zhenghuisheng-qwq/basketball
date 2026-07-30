import request from '@/utils/request'

export function listMatchException(query?: Record<string, unknown>) {
  return request({
    url: '/basket/matchException/list',
    method: 'get',
    params: query
  })
}

export function getExceptionDetail(applicationId: number | string) {
  return request({
    url: `/basket/matchException/${applicationId}`,
    method: 'get'
  })
}

export function rejectMatchException(applicationId: number | string, data: { rejectReason: string; auditRemark?: string }) {
  return request({
    url: `/basket/matchException/${applicationId}/reject`,
    method: 'post',
    data
  })
}

export function decideMatchException(applicationId: number | string, data: Record<string, unknown>) {
  return request({
    url: `/basket/matchException/${applicationId}/decide`,
    method: 'post',
    data
  })
}
