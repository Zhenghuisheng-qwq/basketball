import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'

export interface ExceptionAvailableInfo {
  matchId?: string
  canSubmit?: boolean
  cannotReason?: string
  currentApplicationId?: string
  currentApplicationStatus?: string
  homeTeamName?: string
  awayTeamName?: string
  matchStatus?: number
  dataStatus?: number
  scheduledStartTime?: string
}

export interface ExceptionSubmitPayload {
  exceptionType: string
  exceptionReason: string
  materialFileIds: string[]
  remark?: string
}

export interface ExceptionStatusInfo {
  matchId?: string
  applicationId?: string
  applicationNo?: string
  exceptionType?: string
  exceptionTypeText?: string
  exceptionReason?: string
  applicationStatus?: string
  applicationStatusText?: string
  decisionType?: string
  decisionTypeText?: string
  decisionResult?: string
  rejectReason?: string
  submittedTime?: string
  processedTime?: string
  processedByName?: string
  homeTeamName?: string
  awayTeamName?: string
  materialFileIds?: string[]
}

export function getExceptionAvailable(matchId: number | string) {
  return request.get<any, ApiResult<ExceptionAvailableInfo>>(
    `/basket/player/match-exception/${matchId}/available`
  )
}

export function getExceptionStatus(matchId: number | string) {
  return request.get<any, ApiResult<ExceptionStatusInfo>>(
    `/basket/player/match-exception/${matchId}/status`
  )
}

export function getExceptionHistory(matchId: number | string) {
  return request.get<any, ApiResult<ExceptionStatusInfo[]>>(
    `/basket/player/match-exception/${matchId}/history`
  )
}

export function submitMatchException(matchId: number | string, data: ExceptionSubmitPayload) {
  return request.post<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-exception/${matchId}/submit`,
    data
  )
}
