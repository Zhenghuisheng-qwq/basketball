import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { BasketDivisionQuery } from './division'

export type DivisionWorkspaceEntry = 'publish' | 'roster' | 'overview' | 'structure' | 'schedule' | 'publish-preview'

export interface DivisionSelectorItemVo {
  divisionId: number
  divisionName?: string
  seasonId?: number
  seasonTitle?: string
  seriesId?: number
  seriesCode?: string
  seriesName?: string
  platformCode?: string
  gameMode?: string
  registrationMode?: string
  templateType?: string
  templateCode?: string
  ruleStatus?: string
  ruleStatusName?: string
  rosterStatus?: string
  rosterStatusName?: string
  structureSummary?: string
  scheduleSummary?: string
  publishStatus?: string
  publishStatusName?: string
  rosterWorkflowMode?: string
  selectable?: boolean
  blockReason?: string
}

export interface DivisionContextVo {
  divisionId?: number
  divisionName?: string
  seasonId?: number
  seasonTitle?: string
  seriesId?: number
  seriesCode?: string
  seriesName?: string
  platformCode?: string
  platformName?: string
  gameMode?: string
  gameModeName?: string
  registrationMode?: string
  registrationModeName?: string
  templateType?: string
  templateCode?: string
  templateName?: string
  rosterWorkflowMode?: string
  rosterWorkflowModeName?: string
  divisionStatus?: string
  divisionStatusName?: string
}

export interface RuleSummaryVo {
  ruleVersionId?: number
  ruleVersionNo?: number
  ruleStatus?: string
  ruleStatusName?: string
  locked?: boolean
  templateType?: string
  templateCode?: string
}

export interface RosterVersionHistoryItemVo {
  rosterVersionId?: number
  versionNo?: number
  status?: string
  statusName?: string
  isCurrent?: number
  formalCount?: number
  candidateCount?: number
  excludedCount?: number
  createTime?: string
  lockedTime?: string
  lockedBy?: number
}

export interface RosterSummaryVo {
  rosterStatus?: string
  rosterStatusName?: string
  currentRosterVersionId?: number
  currentRosterVersionNo?: number
  formalCount?: number
  candidateCount?: number
  excludedCount?: number
  locked?: boolean
  versions?: RosterVersionHistoryItemVo[]
}

export interface StageOperationCapabilities {
  canGenerateStructure?: boolean
  canEditSchedule?: boolean
  canPublish?: boolean
  canWithdraw?: boolean
  canRecommendSchedule?: boolean
  blockReason?: string
}

export interface DivisionOperationCapabilities {
  canEditRule?: boolean
  canLockRule?: boolean
  canManageRoster?: boolean
  canLockRoster?: boolean
  canGenerateStructure?: boolean
  canEditSchedule?: boolean
  canPublishStage?: boolean
  canWithdrawStage?: boolean
  requireRulePublished?: boolean
  legacyCompatible?: boolean
  blockReason?: string
}

export interface StageWorkflowSummaryVo {
  stageId: number
  stageCode?: string
  stageName?: string
  stageOrder?: number
  stageType?: string
  stageTypeName?: string
  competitionFormat?: string
  competitionFormatName?: string
  participantSource?: string
  participantCount?: number
  structureStatus?: string
  structureStatusName?: string
  executionVersion?: number
  pairingStatus?: string
  pairingStatusName?: string
  qualificationStatus?: string
  qualificationStatusName?: string
  scheduledUnits?: number
  totalUnits?: number
  unresolvedPlaceholders?: number
  resolvedPlaceholders?: number
  publishStatus?: string
  publishStatusName?: string
  publishNo?: number
  scheduleRevision?: number
  finishedCount?: number
  resultTotal?: number
  workflowHint?: string
  capabilities?: StageOperationCapabilities
}

export interface DivisionWorkflowOverviewVo {
  context?: DivisionContextVo
  rule?: RuleSummaryVo
  roster?: RosterSummaryVo
  stages?: StageWorkflowSummaryVo[]
  capabilities?: DivisionOperationCapabilities
  workflowHint?: string
  legacy?: boolean
  readOnly?: boolean
}

export function listV4DivisionSelector(
  query: BasketDivisionQuery & { entry?: DivisionWorkspaceEntry },
): Promise<TableDataInfo<DivisionSelectorItemVo>> {
  return request({ url: '/basket/v4/division/selector-list', method: 'get', params: query })
}

export function getV4DivisionWorkflowOverview(divisionId: number): Promise<AjaxResult<DivisionWorkflowOverviewVo>> {
  return request({ url: `/basket/v4/division/${divisionId}/workflow-overview`, method: 'get' })
}
