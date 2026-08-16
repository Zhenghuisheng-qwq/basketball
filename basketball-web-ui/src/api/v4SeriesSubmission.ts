import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  V4SeriesSubmissionDeclareReq,
  V4SeriesSubmissionIdReq,
  V4SeriesSubmissionResubmitReq,
  V4SeriesSubmissionSaveGameReq,
  V4SeriesSubmissionWorkspace,
} from '@/api/types/v4SeriesSubmission'

export function getV4SeriesSubmissionWorkspace(matchSeriesId: string) {
  return request.get<any, ApiResult<V4SeriesSubmissionWorkspace>>(
    `/basket/v4/player/series-submission/${matchSeriesId}/workspace`,
  )
}

export function declareV4SeriesScore(data: V4SeriesSubmissionDeclareReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/series-submission/declare-score', data)
}

export function saveV4SeriesGame(data: V4SeriesSubmissionSaveGameReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/series-submission/save-game', data)
}

export function submitV4SeriesSubmission(data: V4SeriesSubmissionIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/series-submission/submit', data)
}

export function resubmitV4SeriesSubmission(data: V4SeriesSubmissionResubmitReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/series-submission/resubmit', data)
}
