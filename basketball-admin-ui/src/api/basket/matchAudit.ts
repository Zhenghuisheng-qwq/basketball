import request from '@/utils/request'

export function listPendingMatchAudit(query?: Record<string, unknown>) {
  return request({
    url: '/basket/matchAudit/pending',
    method: 'get',
    params: query
  })
}

export function pageSubmissionAudit(query?: Record<string, unknown>) {
  return request({
    url: '/basket/matchAudit/submission/page',
    method: 'get',
    params: query
  })
}

export function getSubmissionDetail(submissionId: number) {
  return request({
    url: `/basket/matchAudit/submission/${submissionId}`,
    method: 'get'
  })
}

export function approveSubmission(submissionId: number, data?: { auditRemark?: string }) {
  return request({
    url: `/basket/matchAudit/submission/${submissionId}/approve`,
    method: 'post',
    data: data || {}
  })
}

export function rejectSubmission(submissionId: number, data: { rejectReason: string; auditRemark?: string }) {
  return request({
    url: `/basket/matchAudit/submission/${submissionId}/reject`,
    method: 'post',
    data
  })
}

export function listSubmissionVersions(matchId: number) {
  return request({
    url: `/basket/matchAudit/submission/${matchId}/versions`,
    method: 'get'
  })
}

export function batchApproveSubmission(data: { ids: number[]; auditRemark?: string }) {
  return request({ url: '/basket/matchAudit/submission/batch-approve', method: 'post', data })
}

export function batchRejectSubmission(data: { ids: number[]; rejectReason: string; auditRemark?: string }) {
  return request({ url: '/basket/matchAudit/submission/batch-reject', method: 'post', data })
}
