import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'

export interface PlayerMatchTaskItem {
  matchId?: string
  seasonId?: string
  homeTeamName?: string
  awayTeamName?: string
  opponentTeamName?: string
  scheduledStartTime?: string
  matchStatus?: number
  dataStatus?: number
  submissionStatus?: number
  canSubmit?: boolean
  canResubmit?: boolean
  canViewStatus?: boolean
  actionText?: string
  statusText?: string
  dataStatusText?: string
  rejectReason?: string
  submissionId?: string
}

export interface PageResult<T> {
  records?: T[]
  total?: number
}

export function pageMyMatches(params?: Record<string, unknown>) {
  return request.get<any, ApiResult<PageResult<PlayerMatchTaskItem>>>(
    '/basket/player/match-data/page',
    { params }
  )
}

export function getSubmitDetail(matchId: number) {
  return request.get<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-data/${matchId}/submit-detail`
  )
}

export function submitMatchData(matchId: number, data: Record<string, unknown>) {
  return request.post<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-data/${matchId}/submit`,
    data
  )
}

export function getMatchDataStatus(matchId: number) {
  return request.get<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-data/${matchId}/status`
  )
}

export function getResubmitDetail(matchId: number) {
  return request.get<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-data/${matchId}/resubmit-detail`
  )
}

export function resubmitMatchData(matchId: number, data: Record<string, unknown>) {
  return request.post<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-data/${matchId}/resubmit`,
    data
  )
}
