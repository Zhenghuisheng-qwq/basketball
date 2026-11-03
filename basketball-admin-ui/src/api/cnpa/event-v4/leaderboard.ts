import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface LeaderboardMetricGate {
  enabled: boolean
  minimumAttemptsPerAppearance: number
}

export interface LeaderboardConfig {
  templateCode?: string
  rankingAudience: string
  overallStageIds: number[]
  team: {
    minimumTeamGames: number
    minimumTeamPlayedRounds: number
    minimumTeamPlayedSeries: number
    minimumDueFixtureRate: number | null
  }
  player: {
    requireEligibleTeam: boolean
    minimumPlayerGames: number
    minimumPlayerPlayedRounds: number
    minimumPlayerPlayedSeries: number
    minimumPlayerTeamRate: number | null
  }
  metricGates: Record<string, LeaderboardMetricGate>
  averageAndTotalSameEligibility: boolean
}

export interface LeaderboardPolicy {
  id: number
  divisionId: number
  scopeKey: string
  stageId?: number | null
  versionNo: number
  lifecycle: string
  templateCode?: string
  rankingAudience?: string
  configJson?: string
  expectedLock: number
}

export interface LeaderboardScopeRow {
  scopeKey: string
  stageId?: number | null
  stageName?: string
  stageType?: string
  competitionFormat?: string
  bestOf?: number | null
  doubleElimination?: boolean
  roundLimitsForbidden?: boolean
  draft?: LeaderboardPolicy | null
  active?: LeaderboardPolicy | null
}

export interface LeaderboardScopes {
  divisionId: number
  stageIds: number[]
  scopes: LeaderboardScopeRow[]
}

export interface LeaderboardEntry {
  id?: number
  runId?: number
  subjectType?: string
  eventTeamId?: number
  eventPlayerId?: number
  eligible?: number
  failedConditions?: string
  validGames?: number
  playedRounds?: number
  playedSeries?: number
  dueFixtureRate?: number
  rankNo?: number
  points?: number
}

const base = (divisionId: number) => `/basket/v4/division/${divisionId}/leaderboard`

export function getV4LeaderboardScopes(divisionId: number) {
  return request.get<any, AjaxResult<LeaderboardScopes>>(`${base(divisionId)}/scopes`)
}

export function createV4LeaderboardDraft(divisionId: number, scopeKey: string) {
  return request.post<any, AjaxResult<LeaderboardPolicy>>(`${base(divisionId)}/policies/drafts`, { scopeKey })
}

export function saveV4LeaderboardDraft(divisionId: number, draftId: number, expectedLock: number, config: LeaderboardConfig) {
  return request.put<any, AjaxResult<LeaderboardPolicy>>(`${base(divisionId)}/policies/drafts/${draftId}`, { expectedLock, config })
}

export function loadV4LeaderboardTemplate(divisionId: number, draftId: number, expectedLock: number) {
  return request.post<any, AjaxResult<LeaderboardPolicy>>(`${base(divisionId)}/policies/drafts/${draftId}/load-template`, { expectedLock })
}

export function validateV4LeaderboardDraft(divisionId: number, draftId: number, expectedLock: number) {
  return request.post<any, AjaxResult<LeaderboardPolicy>>(`${base(divisionId)}/policies/drafts/${draftId}/validate`, { expectedLock })
}

export function reopenV4LeaderboardDraft(divisionId: number, draftId: number, expectedLock: number) {
  return request.post<any, AjaxResult<LeaderboardPolicy>>(`${base(divisionId)}/policies/drafts/${draftId}/reopen`, { expectedLock })
}

export function previewV4Leaderboard(divisionId: number, draftId: number) {
  return request.post<any, AjaxResult<Record<string, any>>>(`${base(divisionId)}/policies/drafts/${draftId}/preview`)
}

export function listV4LeaderboardEligibility(divisionId: number, runId: number, subjectType?: string, status?: string) {
  return request.get<any, AjaxResult<LeaderboardEntry[]>>(`${base(divisionId)}/eligibility`, {
    params: { runId, subjectType, status }
  })
}

export function publishV4Leaderboard(divisionId: number, draftId: number, expectedActiveVersion: number, expectedLock: number, reason: string) {
  return request.post<any, AjaxResult<Record<string, any>>>(`${base(divisionId)}/policies/drafts/${draftId}/publish`, {
    expectedActiveVersion,
    expectedLock,
    reason
  })
}

export function recalculateV4Leaderboard(divisionId: number, stageId?: number) {
  return request.post<any, AjaxResult<void>>(`${base(divisionId)}/recalculate`, null, { params: { stageId } })
}

export function archiveV4Leaderboard(divisionId: number, scopeKey: string, expectedLock: number, reason: string) {
  return request.post<any, AjaxResult<LeaderboardPolicy>>(`${base(divisionId)}/archive`, { scopeKey, expectedLock, reason })
}

export function listV4LeaderboardRuns(divisionId: number, scopeKey?: string) {
  return request.get<any, AjaxResult<Record<string, any>[]>>(`${base(divisionId)}/runs`, { params: { scopeKey } })
}

export function listV4LeaderboardAudits(divisionId: number) {
  return request.get<any, AjaxResult<Record<string, any>[]>>(`${base(divisionId)}/audits`)
}

export function updateV4FeaturedDivision(divisionId: number | null, reason: string) {
  return request.put<any, AjaxResult<void>>('/basket/v4/leaderboard/featured', { divisionId, reason })
}
