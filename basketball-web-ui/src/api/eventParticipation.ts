import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PageResult, PlayerMatchTaskItem } from '@/api/matchData'
import type {
  EventRosterMemberDto,
  EventTeamDetailDto,
  EventTeamSummaryDto,
  RosterChangeHistoryItemDto,
} from '@/api/types/eventParticipation'

export function getMyEventTeamList(seasonId?: string) {
  return request.get<any, ApiResult<EventTeamSummaryDto[]>>('/basket/player/event-team/list', {
    params: seasonId ? { seasonId } : undefined,
  })
}

export function getEventTeamDetail(eventTeamId: string) {
  return request.get<any, ApiResult<EventTeamDetailDto>>(
    `/basket/player/event-team/${eventTeamId}`,
  )
}

export function getEventTeamRoster(eventTeamId: string) {
  return request.get<any, ApiResult<EventRosterMemberDto[]>>(
    `/basket/player/event-team/${eventTeamId}/roster`,
  )
}

export function getEventRosterHistory(params: {
  seasonId: string
  playerId?: string
  eventTeamId?: string
}) {
  return request.get<any, ApiResult<RosterChangeHistoryItemDto[]>>(
    '/basket/player/event-team/roster-history',
    { params },
  )
}

export function getEventTeamMatches(eventTeamId: string, params?: Record<string, unknown>) {
  return request.get<any, ApiResult<PageResult<PlayerMatchTaskItem>>>(
    `/basket/player/event-team/${eventTeamId}/matches`,
    { params },
  )
}

export function getEventTeamMatchDetail(eventTeamId: string, matchId: string) {
  return request.get<any, ApiResult<PlayerMatchTaskItem>>(
    `/basket/player/event-team/${eventTeamId}/matches/${matchId}`,
  )
}
