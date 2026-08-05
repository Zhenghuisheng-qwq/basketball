import request from '@/utils/request'

export function listMatchDispute(query?: Record<string, unknown>) {
  return request({
    url: '/basket/matchDispute/list',
    method: 'get',
    params: query
  })
}

export function getMatchDispute(id: number | string) {
  return request({
    url: `/basket/matchDispute/${id}`,
    method: 'get'
  })
}

/** @alias getMatchDispute */
export function getDisputeDetail(id: number | string) {
  return getMatchDispute(id)
}

export function createMatchDispute(data: Record<string, unknown>) {
  return request({
    url: '/basket/matchDispute',
    method: 'post',
    data
  })
}

export function rejectMatchDispute(id: number | string, data: Record<string, unknown>) {
  return request({
    url: `/basket/matchDispute/${id}/reject`,
    method: 'post',
    data
  })
}

export function upholdMatchDispute(id: number | string, data: Record<string, unknown>) {
  return request({
    url: `/basket/matchDispute/${id}/uphold`,
    method: 'post',
    data
  })
}

export function applyDisputeCorrection(id: number | string, data: Record<string, unknown>) {
  return request({
    url: `/basket/matchDispute/${id}/apply-correction`,
    method: 'post',
    data
  })
}

export function batchRejectMatchDispute(data: { ids: number[]; rejectReason?: string; auditRemark?: string }) {
  return request({ url: '/basket/matchDispute/batch-reject', method: 'post', data })
}
