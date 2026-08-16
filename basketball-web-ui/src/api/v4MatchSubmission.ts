import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  V4MatchSubmissionDetail,
  V4MatchSubmissionEligiblePlayer,
  V4MatchSubmissionIdReq,
  V4MatchSubmissionResubmitReq,
  V4MatchSubmissionSaveReq,
  V4MyMatchItem,
} from '@/api/types/v4MatchSubmission'

export function getV4MyMatches(params?: {
  seasonId?: string
  divisionId?: string
  matchStatus?: string
  publishStatus?: string
  submissionStatus?: string
  upcomingOnly?: boolean
}) {
  return request.get<any, ApiResult<V4MyMatchItem[]>>('/basket/v4/player/match-submission/my-matches', { params })
}

export function getV4MatchSubmissionEligiblePlayers(matchId: string, submitEventTeamId?: string) {
  return request.get<any, ApiResult<V4MatchSubmissionEligiblePlayer[]>>(
    `/basket/v4/player/match-submission/${matchId}/eligible-players`,
    { params: submitEventTeamId ? { submitEventTeamId } : undefined },
  )
}

export function getV4MatchSubmissionDetail(matchId: string, submitEventTeamId?: string) {
  return request.get<any, ApiResult<V4MatchSubmissionDetail>>(
    `/basket/v4/player/match-submission/${matchId}/detail`,
    { params: submitEventTeamId ? { submitEventTeamId } : undefined },
  )
}

export function saveV4MatchSubmissionDraft(data: V4MatchSubmissionSaveReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/match-submission/save', data)
}

export function submitV4MatchSubmission(data: V4MatchSubmissionIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/match-submission/submit', data)
}

export function resubmitV4MatchSubmission(data: V4MatchSubmissionResubmitReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/match-submission/resubmit', data)
}

export function getV4MySubmissions() {
  return request.get<any, ApiResult<unknown[]>>('/basket/v4/player/match-submission/my-list')
}
