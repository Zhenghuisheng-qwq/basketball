import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface V4SeriesSubmissionAuditQuery {
  pageNum?: number
  pageSize?: number
  seasonId?: number
  divisionId?: number
  status?: string
  teamName?: string
  seriesCode?: string
}

export interface V4SeriesSubmissionAuditListVo {
  seriesSubmissionId: number
  matchSeriesId: number
  seriesCode?: string
  bestOf?: number
  divisionName?: string
  homeTeamName?: string
  awayTeamName?: string
  submitTeamName?: string
  declaredHomeWinCount?: number
  declaredAwayWinCount?: number
  actualGameCount?: number
  versionNo?: number
  status?: string
  submitTime?: string
  auditTime?: string
}

export interface V4SeriesSubmissionGameAuditVo {
  matchId: number
  submissionId: number
  seriesGameNo?: number
  matchNo?: string
  homeScore?: number
  awayScore?: number
  submissionStatus?: string
  draftComplete?: number
  submitRemark?: string
  homeTeamStat?: Record<string, number>
  awayTeamStat?: Record<string, number>
  homePlayerStats?: Array<Record<string, unknown>>
  awayPlayerStats?: Array<Record<string, unknown>>
  periodScores?: Array<{ periodNo: number; homeScore?: number; awayScore?: number }>
  materials?: Array<{ fileId: number; accessUrl?: string }>
  validationErrors?: string[]
}

export interface V4SeriesSubmissionAuditDetailVo {
  seriesSubmissionId: number
  matchSeriesId: number
  seriesCode?: string
  bestOf?: number
  homeTeamName?: string
  awayTeamName?: string
  submitTeamName?: string
  declaredWinnerTeamName?: string
  declaredHomeWinCount?: number
  declaredAwayWinCount?: number
  actualGameCount?: number
  versionNo?: number
  status?: string
  rejectReason?: string
  games?: V4SeriesSubmissionGameAuditVo[]
  historyVersions?: Array<{ submissionId: number; versionNo: number; submissionStatus?: string; submitTime?: string }>
}

export function listV4SeriesSubmissionAudit(query: V4SeriesSubmissionAuditQuery) {
  return request<TableDataInfo<V4SeriesSubmissionAuditListVo>>({
    url: '/basket/v4/series-submission-audit/list',
    method: 'get',
    params: query,
  })
}

export function getV4SeriesSubmissionAuditDetail(seriesSubmissionId: number) {
  return request<AjaxResult<V4SeriesSubmissionAuditDetailVo>>({
    url: `/basket/v4/series-submission-audit/${seriesSubmissionId}`,
    method: 'get',
  })
}

export function approveV4SeriesSubmissionAudit(seriesSubmissionId: number, auditRemark?: string) {
  return request<AjaxResult<void>>({
    url: `/basket/v4/series-submission-audit/${seriesSubmissionId}/approve`,
    method: 'post',
    data: { auditRemark },
  })
}

export function rejectV4SeriesSubmissionAudit(seriesSubmissionId: number, data: { rejectReason: string; auditRemark?: string }) {
  return request<AjaxResult<void>>({
    url: `/basket/v4/series-submission-audit/${seriesSubmissionId}/reject`,
    method: 'post',
    data,
  })
}
