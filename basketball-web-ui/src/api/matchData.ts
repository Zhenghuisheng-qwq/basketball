import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { EventRosterMemberDto } from '@/api/types/eventParticipation'

export interface MatchDetailPlayerStat {
  playerId?: string
  rosterId?: string
  playerNo?: string
  playerName?: string
  avatarUrl?: string
  teamNameSnapshot?: string
  eventTeamId?: string
  teamId?: string
  position?: string
  gameId?: string
  points?: number | null
  rebounds?: number | null
  assists?: number | null
  steals?: number | null
  blocks?: number | null
  turnovers?: number | null
  fouls?: number | null
  statusText?: string
}

export interface PlayerMatchTaskItem {
  matchId?: string
  seasonId?: string
  eventTeamId?: string
  myEventTeamId?: string
  opponentEventTeamId?: string
  homeTeamName?: string
  awayTeamName?: string
  homeTeamNameSnapshot?: string
  awayTeamNameSnapshot?: string
  opponentTeamNameSnapshot?: string
  myTeamNameSnapshot?: string
  myTeamName?: string
  roundNo?: number
  matchDate?: string
  weekday?: string
  matchTime?: string
  scheduledStartTime?: string
  matchStatus?: number
  matchStatusText?: string
  publishStatus?: number
  publishStatusText?: string
  dataStatus?: number
  dataStatusText?: string
  homeScore?: number
  awayScore?: number
  submissionStatus?: number
  submissionStatusText?: string
  mySubmissionStatus?: number
  mySubmissionStatusText?: string
  opponentSubmissionStatus?: number
  opponentSubmissionStatusText?: string
  canSubmit?: boolean
  canResubmit?: boolean
  canSubmitMatchData?: boolean
  submitDisabledReason?: string
  isMatchTimePassed?: boolean
  isCurrentUserCaptain?: boolean
  canViewStatus?: boolean
  canViewData?: boolean
  canDispute?: boolean
  canSubmitException?: boolean
  isCaptain?: boolean
  isRosterMember?: boolean
  actionText?: string
  statusText?: string
  rejectReason?: string
  submissionId?: string
  eventName?: string
  stageName?: string
  platformCode?: string
  homeTeamId?: string
  awayTeamId?: string
  homeEventTeamId?: string
  awayEventTeamId?: string
  myTeamId?: string
  homeLogoUrl?: string
  awayLogoUrl?: string
  homeWinCount?: number
  homeLossCount?: number
  awayWinCount?: number
  awayLossCount?: number
  homeRecordText?: string
  awayRecordText?: string
  homeRosterCount?: number
  awayRosterCount?: number
  myRosterCount?: number
  rosterStatusText?: string
  homeRosterList?: EventRosterMemberDto[]
  awayRosterList?: EventRosterMemberDto[]
  homePlayerStatList?: MatchDetailPlayerStat[]
  awayPlayerStatList?: MatchDetailPlayerStat[]
}

export interface PageResult<T> {
  records?: T[]
  total?: number
  size?: number
  current?: number
  pages?: number
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

export interface TeamPlayerStatSubmitItem {
  playerId: string
  rosterId?: string
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
}

export interface TeamPlayerStatsSubmitReq {
  submitterTeamId: string
  playerStats: TeamPlayerStatSubmitItem[]
  remark?: string
}

export function submitTeamPlayerStats(matchId: number, data: TeamPlayerStatsSubmitReq) {
  return request.post<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-data/${matchId}/team-player-stats/submit`,
    data
  )
}

export function resubmitTeamPlayerStats(matchId: number, data: TeamPlayerStatsSubmitReq) {
  return request.post<any, ApiResult<Record<string, unknown>>>(
    `/basket/player/match-data/${matchId}/team-player-stats/resubmit`,
    data
  )
}
