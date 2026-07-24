import request from '@/utils/request'
import type {
  AjaxResult,
  TournamentRule,
  TournamentRuleCreateReq,
  TournamentRuleListResult,
  TournamentRuleUpdateReq
} from '@/types'

export function listTournamentRule(seasonId: number): Promise<TournamentRuleListResult> {
  return request({
    url: `/basket/season/${seasonId}/rule/list`,
    method: 'get'
  })
}

export function addTournamentRule(seasonId: number, data: TournamentRuleCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: `/basket/season/${seasonId}/rule`,
    method: 'post',
    data
  })
}

export function updateTournamentRule(ruleId: number, data: TournamentRuleUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/tournamentRule/${ruleId}`,
    method: 'put',
    data
  })
}

export function delTournamentRule(ruleId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/tournamentRule/${ruleId}`,
    method: 'delete'
  })
}

export type { TournamentRule }
