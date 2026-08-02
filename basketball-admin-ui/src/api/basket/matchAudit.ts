import request from '@/utils/request'

export interface MatchDataAuditTeamSide {
  eventTeamId?: number
  teamId?: number
  teamName?: string
  teamCode?: string
  logoUrl?: string
  sideType?: string
  sideText?: string
  homeScore?: number
  awayScore?: number
  submissionStatus?: string
  submissionStatusText?: string
  submissionVersion?: number
  submissionId?: number
  currentSubmissionSide?: boolean
}

export interface MatchDataAuditSubmitter {
  userId?: number
  userName?: string
  nickName?: string
  displayName?: string
  avatarUrl?: string
  displayEmail?: string
  playerId?: number
  playerCode?: string
  playerName?: string
  gameId?: string
  role?: string
  roleText?: string
  teamName?: string
  missing?: boolean
}

export interface MatchDataAuditValidationItem {
  level?: string
  message?: string
  passed?: boolean
}

export interface MatchDataAuditValidationSummary {
  allPassed?: boolean
  errorCount?: number
  warningCount?: number
  items?: MatchDataAuditValidationItem[]
}

export interface MatchDataAuditComparisonDiff {
  field?: string
  fieldLabel?: string
  selfValue?: string
  opponentValue?: string
}

export interface MatchDataAuditComparisonSummary {
  opponentSubmitted?: boolean
  summaryText?: string
  diffCount?: number
  diffs?: MatchDataAuditComparisonDiff[]
}

export interface MatchDataAuditPermissions {
  canApprove?: boolean
  canReject?: boolean
  canViewMaterial?: boolean
}

export interface MatchDataAuditDetail {
  submissionId?: number
  submissionNo?: string
  matchId?: number
  matchNo?: string
  seasonId?: number
  seasonName?: string
  stageName?: string
  roundNo?: number
  roundName?: string
  matchTime?: string
  matchStatus?: number
  matchStatusText?: string
  homeTeamName?: string
  awayTeamName?: string
  homeScore?: number
  awayScore?: number
  submitterTeamName?: string
  submitterRoleText?: string
  versionNo?: number
  submissionStatus?: number
  submissionStatusName?: string
  auditStatus?: string
  auditStatusText?: string
  submittedTime?: string
  submitSide?: string
  submitSideText?: string
  submitSideAbnormal?: boolean
  homeTeam?: MatchDataAuditTeamSide
  awayTeam?: MatchDataAuditTeamSide
  submitter?: MatchDataAuditSubmitter
  submittedScore?: { homeScore?: number; awayScore?: number; confirmed?: boolean }
  officialScore?: { homeScore?: number; awayScore?: number; confirmed?: boolean }
  comparisonResultText?: string
  validationSummary?: MatchDataAuditValidationSummary
  comparisonSummary?: MatchDataAuditComparisonSummary
  permissions?: MatchDataAuditPermissions
  canAudit?: boolean
  cannotAuditReason?: string
  periods?: Array<{
    periodNo?: number
    periodType?: string
    periodTypeName?: string
    homeScore?: number
    awayScore?: number
    scoreDiff?: number
    validationStatus?: string
  }>
  teamStats?: Array<Record<string, unknown>>
  playerStats?: Array<Record<string, unknown>>
  materials?: Array<Record<string, unknown>>
  versions?: Array<Record<string, unknown>>
  auditLogs?: Array<Record<string, unknown>>
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

export function batchApproveSubmission(data: { ids: number[]; auditRemark?: string }) {
  return request({ url: '/basket/matchAudit/submission/batch-approve', method: 'post', data })
}

export function batchRejectSubmission(data: { ids: number[]; rejectReason: string; auditRemark?: string }) {
  return request({ url: '/basket/matchAudit/submission/batch-reject', method: 'post', data })
}
