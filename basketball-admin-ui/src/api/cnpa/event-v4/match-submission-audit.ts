import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface V4MatchSubmissionAuditQuery {
  seasonId?: number
  divisionId?: number
  matchId?: number
  submitEventTeamId?: number
  submissionStatus?: string
  teamName?: string
  matchNo?: string
  pageNum?: number
  pageSize?: number
}

export interface V4MatchSubmissionAuditListVo {
  submissionId: number
  matchId: number
  matchNo: string
  roundNo: number
  matchTime: string
  divisionName: string
  platformCode: string
  homeTeamName: string
  awayTeamName: string
  submitTeamName: string
  submitterName: string
  submitterRole: string
  versionNo: number
  submissionStatus: string
  submitTime: string
  auditTime: string
}

export interface V4MatchSubmissionAuditDetailVo {
  submissionId: number
  matchId: number
  matchNo: string
  roundNo: number
  matchTime: string
  divisionName: string
  platformCode: string
  homeEventTeamId: number
  homeTeamName: string
  homeTeamLogoFileId?: number
  awayEventTeamId: number
  awayTeamName: string
  awayTeamLogoFileId?: number
  submitEventTeamId: number
  submitTeamName: string
  submitTeamLogoFileId?: number
  submitterName: string
  submitterRole: string
  versionNo: number
  submissionStatus: string
  homeScore: number
  awayScore: number
  submitRemark?: string
  submitTime: string
  auditUserName?: string
  auditTime?: string
  auditRemark?: string
  rejectReason?: string
  teamStat?: Record<string, number>
  playerStats?: Array<Record<string, unknown>>
  materials?: Array<{ fileId: number; fileName?: string }>
  historyVersions?: Array<{ submissionId: number; versionNo: number; submissionStatus: string; submitTime: string }>
}

export interface V4StatsQuery {
  seasonId?: number
  divisionId?: number
  rankType?: string
}

export function listV4MatchSubmissionAudit(query: V4MatchSubmissionAuditQuery) {
  return request<TableDataInfo<V4MatchSubmissionAuditListVo>>({
    url: '/basket/v4/match-submission-audit/list',
    method: 'get',
    params: query
  })
}

export function getV4MatchSubmissionAuditDetail(submissionId: number) {
  return request<AjaxResult<V4MatchSubmissionAuditDetailVo>>({
    url: `/basket/v4/match-submission-audit/${submissionId}`,
    method: 'get'
  })
}

export function approveV4MatchSubmissionAudit(submissionId: number, auditRemark?: string) {
  return request<AjaxResult>({
    url: `/basket/v4/match-submission-audit/${submissionId}/approve`,
    method: 'post',
    data: { auditRemark }
  })
}

export function rejectV4MatchSubmissionAudit(submissionId: number, data: { rejectReason: string; auditRemark?: string }) {
  return request<AjaxResult>({
    url: `/basket/v4/match-submission-audit/${submissionId}/reject`,
    method: 'post',
    data
  })
}

export function listV4Standing(query: V4StatsQuery) {
  return request<AjaxResult>({ url: '/basket/v4/stats/standing', method: 'get', params: query })
}

export function listV4TeamStat(query: V4StatsQuery) {
  return request<AjaxResult>({ url: '/basket/v4/stats/team-stat', method: 'get', params: query })
}

export function listV4PlayerStat(query: V4StatsQuery) {
  return request<AjaxResult>({ url: '/basket/v4/stats/player-stat', method: 'get', params: query })
}

export function listV4Rank(query: V4StatsQuery) {
  return request<AjaxResult>({ url: '/basket/v4/stats/rank', method: 'get', params: query })
}
