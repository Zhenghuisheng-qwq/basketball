import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  V4PublicDivisionDetail,
  V4PublicEventTeamDetail,
  V4PublicEventTeamItem,
  V4PublicMatchDetail,
  V4PublicMatchItem,
  V4PublicPlayerStatItem,
  V4PublicRankItem,
  V4PublicSeasonDetail,
  V4PublicSeasonItem,
  V4PublicSeriesItem,
  V4PublicStageItem,
  V4PublicStageStandingScope,
  V4PublicStandingItem,
  V4PublicTeamStatItem,
} from '@/api/types/v4Public'

export function getV4PublicSeriesList() {
  return request.get<any, ApiResult<V4PublicSeriesItem[]>>('/basket/v4/public/series/list')
}

export function getV4PublicSeasonList(params?: {
  seriesCode?: string
  seasonYear?: number
  seasonStatus?: string
}) {
  return request.get<any, ApiResult<V4PublicSeasonItem[]>>('/basket/v4/public/season/list', { params })
}

export function getV4PublicSeasonDetail(seasonId: string) {
  return request.get<any, ApiResult<V4PublicSeasonDetail>>(`/basket/v4/public/season/${seasonId}`)
}

export function getV4PublicDivisionDetail(divisionId: string) {
  return request.get<any, ApiResult<V4PublicDivisionDetail>>(`/basket/v4/public/division/${divisionId}`)
}

export function getV4PublicTeamList(divisionId: string) {
  return request.get<any, ApiResult<V4PublicEventTeamItem[]>>('/basket/v4/public/team/list', {
    params: { divisionId },
  })
}

export function getV4PublicTeamDetail(eventTeamId: string) {
  return request.get<any, ApiResult<V4PublicEventTeamDetail>>(`/basket/v4/public/team/${eventTeamId}`)
}

export function getV4PublicMatchList(params: {
  divisionId: string
  roundNo?: number
  matchStatus?: string
  upcomingOnly?: boolean
  finishedOnly?: boolean
  eventTeamId?: string
}) {
  return request.get<any, ApiResult<V4PublicMatchItem[]>>('/basket/v4/public/match/list', { params })
}

export function getV4PublicMatchDetail(matchId: string) {
  return request.get<any, ApiResult<V4PublicMatchDetail>>(`/basket/v4/public/match/${matchId}`)
}

export function getV4PublicStanding(divisionId: string) {
  return request.get<any, ApiResult<V4PublicStandingItem[]>>('/basket/v4/public/stats/standing', {
    params: { divisionId },
  })
}

export function getV4PublicStageList(divisionId: string) {
  return request.get<any, ApiResult<V4PublicStageItem[]>>('/basket/v4/public/stage/list', {
    params: { divisionId },
  })
}

export function getV4PublicStageStandings(stageId: string) {
  return request.get<any, ApiResult<V4PublicStageStandingScope[]>>(
    `/basket/v4/public/stage/${stageId}/standings`,
  )
}

export function getV4PublicStageBracket(stageId: string) {
  return request.get<any, ApiResult<V4PublicMatchItem[]>>(`/basket/v4/public/stage/${stageId}/bracket`)
}

export function getV4PublicTeamStat(divisionId: string) {
  return request.get<any, ApiResult<V4PublicTeamStatItem[]>>('/basket/v4/public/stats/team-stat', {
    params: { divisionId },
  })
}

export function getV4PublicPlayerStat(divisionId: string) {
  return request.get<any, ApiResult<V4PublicPlayerStatItem[]>>('/basket/v4/public/stats/player-stat', {
    params: { divisionId },
  })
}

export function getV4PublicRank(divisionId: string, rankType?: string) {
  return request.get<any, ApiResult<V4PublicRankItem[]>>('/basket/v4/public/stats/rank', {
    params: { divisionId, rankType },
  })
}
