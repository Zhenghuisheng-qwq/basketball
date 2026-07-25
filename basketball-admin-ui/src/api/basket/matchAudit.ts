import request from '@/utils/request'

export function listPendingMatchAudit(query?: Record<string, unknown>) {
  return request({
    url: '/basket/matchAudit/pending',
    method: 'get',
    params: query
  })
}
