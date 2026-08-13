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
