import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'

export interface DisputeAvailableInfo {
  matchId?: string
  canDispute?: boolean
  cannotReason?: string
  deadlineTime?: string
  remainingSeconds?: number
  currentDisputeId?: string
  currentDisputeStatus?: string
  homeTeamName?: string
  awayTeamName?: string
  homeScore?: number
  awayScore?: number
  confirmedTime?: string
  publicTime?: string
}

export interface DisputeSubmitPayload {
  disputeType: string
  disputeReason: string
  materialFileIds: string[]
  expectedHomeScore?: number
  expectedAwayScore?: number
  remark?: string
}

export interface DisputeStatusInfo {
  matchId?: string
  disputeId?: string
  disputeNo?: string
  disputeType?: string
  disputeTypeText?: string
  disputeReason?: string
  disputeStatus?: string
  disputeStatusText?: string
  rejectReason?: string
  processResult?: string
  deadlineTime?: string
  submittedTime?: string
  processedTime?: string
  processedByName?: string
  homeTeamName?: string
  awayTeamName?: string
  homeScore?: number
  awayScore?: number
  materialFileIds?: string[]
}

export function getDisputeAvailable(matchId: number | string) {
  return request.get<any, ApiResult<DisputeAvailableInfo>>(
    `/basket/player/match-dispute/${matchId}/available`
  )
}

export function getDisputeStatus(matchId: number | string) {
  return request.get<any, ApiResult<DisputeStatusInfo>>(
    `/basket/player/match-dispute/${matchId}/status`
  )
}

export function getDisputeHistory(matchId: number | string) {
  return request.get<any, ApiResult<DisputeStatusInfo[]>>(
    `/basket/player/match-dispute/${matchId}/history`
  )
}

export function submitMatchDispute(matchId: number | string, data: DisputeSubmitPayload) {
  return request.post<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-dispute/${matchId}/submit`,
    data
  )
}
