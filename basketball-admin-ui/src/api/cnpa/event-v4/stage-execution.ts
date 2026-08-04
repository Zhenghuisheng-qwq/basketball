import request from '@/utils/request'

export interface BasketDivisionStageExecutionVo {
  stageId: number
  stageCode: string
  stageName: string
  stageOrder: number
  stageType: string
  stageTypeName?: string
  competitionFormat: string
  competitionFormatName?: string
  executionStatus: string
  executionStatusName?: string
  scheduleBatchId?: number
  participantCount?: number
  qualifiedCount?: number
  startedTime?: string
  completedTime?: string
  confirmedTime?: string
  nextStageCode?: string
}

export interface StageSchedulePreviewVo {
  teamCount: number
  matchesPerTeam?: number
  totalMatches: number
  roundCount?: number
  hasBye?: boolean
  warnings?: string[]
  groupPreviews?: Array<{ groupCode: string; groupName: string; teamCount: number; matchCount: number }>
}

export function listV4StageExecution(divisionId: number) {
  return request({ url: '/basket/v4/admin/stages/list', method: 'get', params: { divisionId } })
}

export function initializeV4StageTeams(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/initialize-teams`, method: 'post' })
}

export function previewV4StageSchedule(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule-preview`, method: 'get' })
}

export function generateV4StageSchedule(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/generate-schedule`, method: 'post' })
}

export function publishV4StageSchedule(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/publish-schedule`, method: 'post' })
}

export function cancelV4StageDraftSchedule(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/cancel-draft-schedule`, method: 'post' })
}

export function checkV4StageCompletion(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/check-completion`, method: 'post' })
}

export function previewV4StageQualification(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/qualification-preview`, method: 'get' })
}

export function confirmV4StageQualification(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/confirm-qualification`, method: 'post' })
}

export function cancelV4StageQualificationConfirm(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/cancel-qualification-confirm`, method: 'post' })
}

export function listV4StageStandings(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/standings`, method: 'get' })
}

export function applyV4StageManualRank(stageId: number, data: {
  groupScopeKey?: number
  reason: string
  items: Array<{ eventTeamId: number; manualRankNo: number }>
}) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/manual-rank`, method: 'post', data })
}
