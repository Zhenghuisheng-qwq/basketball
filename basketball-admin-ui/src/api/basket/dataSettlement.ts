import request from '@/utils/request'
import type {
  AjaxResult,
  SeasonSettlementSummary,
  SeasonSettlementSummaryListResult,
  SettlementRebuildResult,
  SettlementValidationResult
} from '@/types'

export function listSeasonSettlementSummaries(tournamentId?: number): Promise<SeasonSettlementSummaryListResult> {
  return request({
    url: '/basket/data-settlement/season/list',
    method: 'get',
    params: { tournamentId }
  })
}

export function getSeasonSettlementSummary(seasonId: number): Promise<AjaxResult<SeasonSettlementSummary>> {
  return request({
    url: `/basket/data-settlement/season/${seasonId}/summary`,
    method: 'get'
  })
}

export function validateSeasonSettlement(seasonId: number): Promise<AjaxResult<SettlementValidationResult>> {
  return request({
    url: `/basket/data-settlement/season/${seasonId}/validation`,
    method: 'get'
  })
}

export function rebuildSeasonSettlement(seasonId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/data-settlement/season/${seasonId}/rebuild`,
    method: 'post'
  })
}

export function rebuildStageSettlement(stageId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/data-settlement/stage/${stageId}/rebuild`,
    method: 'post'
  })
}

export function rebuildGroupSettlement(groupId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/data-settlement/group/${groupId}/rebuild`,
    method: 'post'
  })
}

export function rebuildMatchSettlement(matchId: number): Promise<AjaxResult<SettlementRebuildResult>> {
  return request({
    url: `/basket/data-settlement/match/${matchId}/rebuild`,
    method: 'post'
  })
}
