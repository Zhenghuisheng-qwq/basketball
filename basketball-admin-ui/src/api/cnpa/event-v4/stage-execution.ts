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
  publishStatus?: string
  structureStatus?: string
  executionVersion?: number
  scheduleRevision?: number
  participantCount?: number
  qualifiedCount?: number
  startedTime?: string
  completedTime?: string
  confirmedTime?: string
  nextStageCode?: string
  roundRobinCycles?: number
  bestOf?: number
  semifinalBestOf?: number
  finalBestOf?: number
  pairingRule?: string
  seedRule?: string
  drawRule?: string
  byeRule?: string
  advancementCount?: number
  advancementType?: string
  hasUnresolvedPlaceholders?: boolean
  participantResolved?: boolean
}

export interface StageSchedulePreviewVo {
  teamCount: number
  matchesPerTeam?: number
  totalMatches: number
  roundCount?: number
  hasBye?: boolean
  seriesCount?: number
  bracketCapacity?: number
  byeCount?: number
  firstRoundActual?: number
  totalActualSeries?: number
  warnings?: string[]
  groupPreviews?: Array<{ groupCode: string; groupName: string; teamCount: number; matchCount: number }>
}

export interface BracketPreviewVo {
  participantCount?: number
  bracketCapacity?: number
  byeCount?: number
  firstRoundActual?: number
  totalActualSeries?: number
  roundCount?: number
  rounds?: Array<{
    roundNo: number
    roundName?: string
    boRole?: string
    bestOf?: number
    pairings?: Array<{
      slot: number
      byeAdvance?: boolean
      nextRoundLabel?: string
      home?: BracketSlotView
      away?: BracketSlotView
    }>
  }>
}

export interface BracketSlotView {
  slotNo?: number
  eventTeamId?: number
  teamName?: string
  seedNo?: number
  rankingNo?: number
  bye?: boolean
  sourceLabel?: string
  sourceType?: string
  sourceGroupCode?: string
  sourceRank?: number
  sourceDisplay?: string
  displayCode?: string
  displayName?: string
  resolutionStatus?: string
  resolvedEventTeamId?: number
  resolvedEventTeamName?: string
}

export interface StagePairingExecutionVo {
  executionId?: number
  stageId?: number
  executionNo?: number
  seedRule?: string
  drawRule?: string
  byeRule?: string
  placementRule?: string
  resolutionStatus?: string
  status?: string
  randomSeed?: number
  participantCount?: number
  bracketCapacity?: number
  byeCount?: number
  firstRoundActual?: number
  totalActualSeries?: number
  hasUnresolvedPlaceholders?: boolean
  participantResolved?: boolean
  sourceStageId?: number
  entries?: Array<{
    slotNo: number
    eventTeamId?: number
    teamName?: string
    seedNo?: number
    rankingNo?: number
    bye?: boolean
    sourceType?: string
    sourceGroupId?: number
    sourceGroupCode?: string
    sourceRank?: number
    displayCode?: string
    displayName?: string
    resolutionStatus?: string
    resolvedEventTeamId?: number
    resolvedEventTeamName?: string
  }>
  bracketPreview?: BracketPreviewVo
}

export function getV4PairingExecution(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/pairing-execution`, method: 'get' })
}

export function previewV4Bracket(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/bracket-preview`, method: 'get' })
}

export function saveV4AdminSeeds(stageId: number, seedEventTeamIds: number[]) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/admin-seeds`, method: 'post', data: { seedEventTeamIds } })
}

export function saveV4ExternalRanking(stageId: number, items: Array<{ eventTeamId: number; rankingNo: number }>) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/external-ranking`, method: 'post', data: { items } })
}

export function generateV4Draw(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/generate-draw`, method: 'post' })
}

export function confirmV4Draw(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/confirm-draw`, method: 'post' })
}

export function saveV4ManualSlots(stageId: number, slots: Array<{
  slotNo: number
  eventTeamId?: number
  bye?: boolean
  sourceType?: string
  sourceGroupId?: number
  sourceRank?: number
}>) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/manual-slots`, method: 'post', data: { slots } })
}

export function redrawV4Draw(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/redraw`, method: 'post' })
}

export function generateV4GroupCross(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/generate-group-cross`, method: 'post' })
}

export function resolveV4Placeholders(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/resolve-placeholders`, method: 'post' })
}

export function listV4StageExecution(divisionId: number) {
  return request({ url: '/basket/v4/admin/stages/list', method: 'get', params: { divisionId } })
}

export function initializeV4StageTeams(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/initialize-teams`, method: 'post' })
}

export interface StageTeamItemVo {
  eventTeamId: number
  teamName?: string
  seedNo?: number
}

export function listV4StageTeams(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/teams`, method: 'get' })
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

export function applyV4StandingPointAdjustment(stageId: number, data: {
  divisionId: number
  eventTeamId: number
  delta: number
  reason: string
  remark?: string
  clientRequestId: string
}) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/standing/point-adjustment`, method: 'post', data })
}

export function listV4StandingPointAdjustments(stageId: number, divisionId: number, eventTeamId: number) {
  return request({
    url: `/basket/v4/admin/stages/${stageId}/standing/point-adjustments`,
    method: 'get',
    params: { divisionId, eventTeamId },
  })
}

export interface ScheduleUnitView {
  unitType: string
  unitId: number
  divisionId?: number
  stageId?: number
  structureBatchId?: number
  roundNo?: number
  roundRole?: string
  matchOrder?: number
  homeParticipantId?: number
  awayParticipantId?: number
  homeParticipantName?: string
  awayParticipantName?: string
  homeSourceType?: string
  awaySourceType?: string
  homeSourceDisplay?: string
  awaySourceDisplay?: string
  bestOf?: number
  scheduledDate?: string
  scheduledStartTime?: string
  scheduleStatus?: string
  scheduleSource?: string
  participantResolutionStatus?: string
  publishStatus?: string
  canEditSchedule?: boolean
  canAdjustSchedule?: boolean
  required?: boolean
  matchStatus?: string
  seriesStatus?: string
}

export interface StageScheduleSummaryVo {
  stageId?: number
  structureBatchId?: number
  totalUnits?: number
  scheduledUnits?: number
  unscheduledUnits?: number
  resolvedUnits?: number
  unresolvedUnits?: number
  publishStatus?: string
  structureStatus?: string
  executionVersion?: number
  scheduleRevision?: number
  publishNo?: number
  ruleStatus?: string
  rosterStatus?: string
  pairingStatus?: string
  canRecommend?: boolean
  recommendationType?: string
}

export interface StageScheduleBoardVo {
  summary?: StageScheduleSummaryVo
  units?: ScheduleUnitView[]
}

export interface ValidationIssueVo {
  severity?: string
  code?: string
  message?: string
  unitType?: string
  unitId?: number
}

export interface StagePublishPreviewVo {
  canPublish?: boolean
  summary?: StageScheduleSummaryVo
  units?: ScheduleUnitView[]
  errors?: ValidationIssueVo[]
  warnings?: ValidationIssueVo[]
}

export interface ScheduleRecommendationPreviewVo {
  recommendationType?: string
  overwriteScheduled?: boolean
  proposedCount?: number
  skippedManualCount?: number
  units?: ScheduleUnitView[]
}

export interface StagePublishRecordVo {
  id?: number
  stageId?: number
  structureBatchId?: number
  structureExecutionVersion?: number
  scheduleRevision?: number
  publishNo?: number
  actionType?: string
  publishStatus?: string
  scheduleFingerprint?: string
  operatorId?: number
  operationTime?: string
  reason?: string
  isCurrent?: number
}

export function listV4StageScheduleUnits(stageId: number, scheduleStatus?: string) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule-units`, method: 'get', params: { scheduleStatus } })
}

export function getV4StageScheduleSummary(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule-summary`, method: 'get' })
}

export function assignV4StageSchedule(stageId: number, data: { unitType: string; unitId: number; scheduledDate: string; scheduledStartTime: string }) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule/assign`, method: 'post', data })
}

export function rescheduleV4StageSchedule(stageId: number, data: { unitType: string; unitId: number; scheduledDate: string; scheduledStartTime: string; reason: string }) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule/reschedule`, method: 'post', data })
}

export function batchAssignV4StageSchedule(stageId: number, data: { scheduledDate: string; scheduledStartTime: string; roundNo?: number; items?: Array<{ unitType: string; unitId: number }> }) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule/batch-assign`, method: 'post', data })
}

export function clearV4StageSchedule(stageId: number, data: { unitType: string; unitId: number }) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule/clear`, method: 'post', data })
}

export function previewV4ScheduleRecommend(stageId: number, overwriteScheduled = false) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule/recommend-preview`, method: 'get', params: { overwriteScheduled } })
}

export function applyV4ScheduleRecommend(stageId: number, overwriteScheduled = false) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/schedule/recommend-apply`, method: 'post', data: { overwriteScheduled } })
}

export function previewV4StagePublish(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/publish-preview`, method: 'get' })
}

export function withdrawV4StageSchedule(stageId: number, reason: string) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/withdraw-schedule`, method: 'post', data: { reason } })
}

export function republishV4StageSchedule(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/republish-schedule`, method: 'post' })
}

export function listV4StagePublishRecords(stageId: number) {
  return request({ url: `/basket/v4/admin/stages/${stageId}/publish-records`, method: 'get' })
}

