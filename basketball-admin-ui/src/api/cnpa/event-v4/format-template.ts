import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface BasketFormatTemplateListVo {
  id?: number
  seriesId?: number
  templateCode?: string
  templateName?: string
  versionNo?: number
  templateDescription?: string
  enabled?: number
  isDefault?: number
}

export interface BasketDivisionTieBreakRuleVo {
  id?: number
  stageId?: number
  ruleCode?: string
  ruleCodeName?: string
  priorityNo?: number
  enabled?: number
}

export interface BasketDivisionStageVo {
  id?: number
  stageCode?: string
  stageName?: string
  stageOrder?: number
  stageType?: string
  stageTypeName?: string
  organizationMode?: string
  organizationModeName?: string
  competitionFormat?: string
  competitionFormatName?: string
  groupingMode?: string
  groupingModeName?: string
  groupCount?: number
  roundRobinCycles?: number
  bestOf?: number
  winPoints?: number
  lossPoints?: number
  forfeitWinPoints?: number
  forfeitLossPoints?: number
  advancementType?: string
  advancementTypeName?: string
  advancementCount?: number
  pairingRule?: string
  pairingRuleName?: string
  nextStageCode?: string
  requiresManualConfirmation?: number
  stageStatus?: string
  tieBreakRules?: BasketDivisionTieBreakRuleVo[]
}

export interface BasketFormatTemplateStageVo extends Omit<BasketDivisionStageVo, 'id' | 'tieBreakRules' | 'stageStatus'> {}

export interface BasketFormatTemplateDetailVo extends BasketFormatTemplateListVo {
  stages?: BasketFormatTemplateStageVo[]
}

export interface BasketDivisionFormatSnapshotVo {
  snapshotId?: number
  divisionId?: number
  sourceTemplateId?: number
  sourceTemplateCode?: string
  sourceTemplateName?: string
  sourceTemplateVersion?: number
  snapshotVersion?: number
  snapshotStatus?: string
  snapshotStatusName?: string
  isCurrent?: number
  mutable?: boolean
  lockable?: boolean
  locked?: boolean
  formatSummary?: string
  stages?: BasketDivisionStageVo[]
}

export interface BasketDivisionStageSaveReq {
  stageCode?: string
  stageName?: string
  stageOrder?: number
  stageType?: string
  organizationMode?: string
  competitionFormat?: string
  groupingMode?: string
  groupCount?: number
  roundRobinCycles?: number
  bestOf?: number
  winPoints?: number
  lossPoints?: number
  forfeitWinPoints?: number
  forfeitLossPoints?: number
  advancementType?: string
  advancementCount?: number
  pairingRule?: string
  nextStageCode?: string
  requiresManualConfirmation?: number
}

export interface BasketDivisionTieBreakRuleSaveReq {
  stageId?: number
  rules?: Array<{ ruleCode?: string; priorityNo?: number; enabled?: number }>
}

export function listV4FormatTemplates(seriesId: number): Promise<AjaxResult<BasketFormatTemplateListVo[]>> {
  return request({ url: '/basket/v4/format-template/list', method: 'get', params: { seriesId } })
}

export function getV4FormatTemplate(id: number): Promise<AjaxResult<BasketFormatTemplateDetailVo>> {
  return request({ url: `/basket/v4/format-template/${id}`, method: 'get' })
}

/** @deprecated 请使用 getFormatSnapshot */
export function getV4DivisionFormatSnapshot(divisionId: number): Promise<AjaxResult<BasketDivisionFormatSnapshotVo>> {
  return getFormatSnapshot(divisionId)
}

export function getFormatSnapshot(divisionId: number): Promise<AjaxResult<BasketDivisionFormatSnapshotVo>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-snapshot`, method: 'get' })
}

export function addFormatStage(divisionId: number, data: BasketDivisionStageSaveReq): Promise<AjaxResult<number>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-stages`, method: 'post', data, headers: { repeatSubmit: false } })
}

export function updateFormatStage(divisionId: number, stageId: number, data: BasketDivisionStageSaveReq): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-stages/${stageId}`, method: 'put', data, headers: { repeatSubmit: false } })
}

export function deleteFormatStage(divisionId: number, stageId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-stages/${stageId}`, method: 'delete', headers: { repeatSubmit: false } })
}

export function reorderFormatStages(divisionId: number, stageIds: number[]): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-stages/order`, method: 'put', data: { stageIds } })
}

export function saveTieBreakRules(divisionId: number, data: BasketDivisionTieBreakRuleSaveReq): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/tie-break-rules`, method: 'put', data })
}

export function rebuildFormatSnapshot(divisionId: number, templateId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-snapshot/rebuild`, method: 'post', params: { templateId } })
}

export interface BasketDivisionFormatPlanApplyReq {
  planCode: string
  expectedTeamCount: number
  groupCount?: number
  roundRobinLegs?: number
  qualifiersPerGroup?: number
  overallAdvanceCount?: number
  eliminationPairingRule?: string
  winPoints?: number
  lossPoints?: number
}

export interface FormatPlanValidationResultVo {
  valid: boolean
  errors?: string[]
  warnings?: string[]
  qualifierCount?: number
  expectedGroupSize?: number
  expectedGroupMatchCount?: number
  expectedEliminationMatchCount?: number
  expectedTotalMatchCount?: number
  eliminationStartRound?: string
}

export function applyFormatPlan(divisionId: number, data: BasketDivisionFormatPlanApplyReq): Promise<AjaxResult<BasketDivisionFormatSnapshotVo>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-plan/apply`, method: 'post', data })
}

export function validateFormatPlan(data: BasketDivisionFormatPlanApplyReq): Promise<AjaxResult<FormatPlanValidationResultVo>> {
  return request({ url: '/basket/v4/divisions/format-plan/validate', method: 'post', data })
}

export function lockFormatSnapshot(divisionId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/format-snapshot/lock`, method: 'post' })
}
