import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface BasketMatchSeriesGameVo {
  matchId?: number
  matchCode?: string
  seriesGameNo?: number
  matchStatus?: string
  matchStatusName?: string
  homeScore?: number
  awayScore?: number
  winnerEventTeamId?: number
  scheduledTime?: string
  submissionStatus?: string
  submissionStatusName?: string
  auditStatus?: string
  auditStatusName?: string
  skipped?: boolean
  skippedReason?: string
}

export interface BasketMatchSeriesDetailVo {
  id?: number
  seriesCode?: string
  seasonId?: number
  seasonName?: string
  divisionId?: number
  divisionName?: string
  stageId?: number
  stageName?: string
  roundNo?: number
  roundName?: string
  bracketSlot?: number
  scheduledDate?: string
  scheduledStartTime?: string
  publishStatus?: string
  publishStatusName?: string
  gamesPlayed?: number
  seriesScore?: string
  homeEventTeamId?: number
  homeTeamName?: string
  awayEventTeamId?: number
  awayTeamName?: string
  bestOf?: number
  requiredWins?: number
  homeWinCount?: number
  awayWinCount?: number
  seriesStatus?: string
  seriesStatusName?: string
  resolutionType?: string
  resolutionReason?: string
  winnerEventTeamId?: number
  winnerTeamName?: string
  qualificationStatus?: string
  qualificationStatusName?: string
  finalStage?: boolean
  resultLabel?: string
  games?: BasketMatchSeriesGameVo[]
}

export function getV4MatchSeries(id: number): Promise<AjaxResult<BasketMatchSeriesDetailVo>> {
  return request({ url: `/basket/v4/match-series/${id}`, method: 'get' })
}

export function listV4MatchSeriesByStage(stageId: number): Promise<AjaxResult<BasketMatchSeriesDetailVo[]>> {
  return request({ url: `/basket/v4/match-series/stage/${stageId}`, method: 'get' })
}

export function listV4MatchSeriesByDivision(divisionId: number): Promise<AjaxResult<BasketMatchSeriesDetailVo[]>> {
  return request({ url: `/basket/v4/match-series/division/${divisionId}`, method: 'get' })
}

export function getV4MatchSeriesContextByMatch(matchId: number): Promise<AjaxResult<BasketMatchSeriesDetailVo>> {
  return request({ url: `/basket/v4/match-series/context/match/${matchId}`, method: 'get' })
}
