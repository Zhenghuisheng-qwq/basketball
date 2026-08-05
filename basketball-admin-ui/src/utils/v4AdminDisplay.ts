import { parseTime } from '@/utils/ruoyi'

/** V4 后台运营展示辅助（纯展示，不改业务值） */

const PUBLISH_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下线'
}

const SEASON_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '未开始',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名截止',
  PLAYING: '比赛中',
  FINISHED: '已结束',
  ARCHIVED: '已归档'
}

const SEASON_DIVISION_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '未开始',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名截止',
  SCHEDULED: '已排赛',
  PLAYING: '比赛中',
  FINISHED: '已结束',
  CANCELLED: '已取消',
  DRAFT: '草稿',
  ARCHIVED: '已归档'
}

const GRANT_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '生效中',
  REVOKED: '已撤销',
  EXPIRED: '已过期'
}

const REGISTRATION_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  CANCELLED: '已取消'
}

const GENERATE_STATUS_LABELS: Record<string, string> = {
  SUCCESS: '生成成功',
  FAILED: '生成失败',
  PROCESSING: '生成中'
}

const REGISTRATION_MODE_LABELS: Record<string, string> = {
  TEAM: '球队报名',
  PERSONAL: '个人报名'
}

const PLATFORM_CODE_LABELS: Record<string, string> = {
  PC: 'PC',
  PS_XBOX: 'PS/Xbox',
  PS: 'PS（历史）',
  XBOX: 'Xbox（历史）'
}

const GAME_MODE_LABELS: Record<string, string> = {
  FIVE_V_FIVE: '5V5',
  THREE_V_THREE: '3V3'
}

const SNAPSHOT_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  LOCKED: '已锁定',
  IN_USE: '使用中',
  ARCHIVED: '已归档'
}

const PERSONAL_REGISTRATION_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  ASSIGNED: '已分配',
  CONVERTED: '已转换'
}

const FORMATION_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  CAPTAIN_ASSIGNED: '已指定队长',
  TEAM_FORMING: '组队中',
  READY_TO_CONFIRM: '待确认',
  CONFIRMED: '已确认',
  CONVERTED: '已转换',
  CANCELLED: '已取消'
}

const DRAW_BATCH_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  CANCELLED: '已取消',
  CONVERTED: '已转换'
}

const GROUPING_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  LOCKED: '已锁定',
  CANCELLED: '已取消'
}

const STAGE_TYPE_LABELS: Record<string, string> = {
  REGULAR: '常规赛',
  GROUP: '小组赛',
  KNOCKOUT: '淘汰赛',
  SEMI_FINAL: '半决赛',
  FINAL: '总决赛',
  CUSTOM: '自定义'
}

const STAGE_EXECUTION_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  READY: '就绪',
  SCHEDULE_GENERATED: '已生成赛程',
  RUNNING: '进行中',
  RESULT_PENDING_CONFIRM: '待确认晋级',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const STAGE_EXECUTION_STATUS_TAG: Record<string, string> = {
  DRAFT: 'info',
  READY: '',
  SCHEDULE_GENERATED: 'warning',
  RUNNING: 'success',
  RESULT_PENDING_CONFIRM: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'info'
}

const COMPETITION_FORMAT_LABELS: Record<string, string> = {
  ROUND_ROBIN: '循环赛',
  SINGLE_ELIMINATION: '单败淘汰',
  MANUAL: '人工安排'
}

const GROUPING_MODE_LABELS: Record<string, string> = {
  NONE: '无分组',
  OFFLINE_RANDOM_RESULT: '导入线下分组结果',
  MANUAL: '人工分组',
  SEEDED: '种子分组'
}

/** 向导/运营页赛制状态（隐藏 DRAFT/LOCKED 等英文枚举） */
const WIZARD_FORMAT_STATUS_LABELS: Record<string, string> = {
  DRAFT: '待确认',
  LOCKED: '已确认',
  IN_USE: '使用中',
  ARCHIVED: '已归档'
}

const ADVANCEMENT_TYPE_LABELS: Record<string, string> = {
  NONE: '无晋级',
  OVERALL_TOP_N: '总排名前N',
  GROUP_TOP_N: '小组前N',
  SERIES_WINNER: '系列赛胜者',
  MANUAL: '人工晋级'
}

const TIE_BREAK_RULE_LABELS: Record<string, string> = {
  HEAD_TO_HEAD: '相互战绩',
  WIN_COUNT: '胜场数',
  POINT_DIFFERENCE: '净胜分',
  TOTAL_POINTS: '总得分',
  MANUAL: '人工裁定'
}

const FORMAT_TYPE_LABELS: Record<string, string> = {
  REGULAR_ROUND_ROBIN: '常规赛单循环',
  GROUP_KNOCKOUT: '小组赛+淘汰赛（暂未开放）',
  KNOCKOUT: '单败淘汰（暂未开放）'
}

const MATCH_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  SCHEDULED: '已排赛',
  PLAYING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消'
}

const SUBMISSION_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  SUPERSEDED: '已被新版本替代'
}

const AUDIT_ACTION_LABELS: Record<string, string> = {
  SUBMIT: '提交',
  APPROVE: '通过',
  REJECT: '驳回',
  WITHDRAW: '撤回',
  RESUBMIT: '重新提交'
}

const AUDIT_RESULT_LABELS: Record<string, string> = {
  APPROVED: '通过',
  REJECTED: '驳回',
  PASSED: '通过',
  FAILED: '失败'
}

const RANK_TYPE_LABELS: Record<string, string> = {
  TEAM_STANDING: '球队积分榜',
  PLAYER_SCORE: '球员得分',
  PLAYER_ASSIST: '球员助攻',
  PLAYER_REBOUND: '球员篮板'
}

const SERIES_CODE_LABELS: Record<string, string> = {
  LEAGUE: '联赛',
  ARENA: '竞技场',
  CUP: '杯赛',
  OPEN: '公开赛'
}

type TagType = '' | 'success' | 'warning' | 'info' | 'danger'

function lookup(map: Record<string, string>, code?: string | null, fallback = '-'): string {
  if (!code) return fallback
  return map[code.toUpperCase()] || code
}

export function v4PublishStatusLabel(status?: string | null): string {
  return lookup(PUBLISH_STATUS_LABELS, status)
}

export function v4SeasonStatusLabel(status?: string | null): string {
  return lookup(SEASON_STATUS_LABELS, status, lookup(SEASON_DIVISION_STATUS_LABELS, status))
}

export function v4DivisionStatusLabel(status?: string | null): string {
  return lookup(SEASON_DIVISION_STATUS_LABELS, status)
}

export function v4GrantStatusLabel(status?: string | null): string {
  return lookup(GRANT_STATUS_LABELS, status)
}

export function v4RegistrationStatusLabel(status?: string | null): string {
  return lookup(REGISTRATION_STATUS_LABELS, status)
}

export function v4GenerateStatusLabel(status?: string | null): string {
  return lookup(GENERATE_STATUS_LABELS, status)
}

export function v4RegistrationModeLabel(mode?: string | null): string {
  return lookup(REGISTRATION_MODE_LABELS, mode)
}

export function v4PlatformLabel(code?: string | null): string {
  return lookup(PLATFORM_CODE_LABELS, code, code || '-')
}

export function v4GameModeLabel(code?: string | null): string {
  return lookup(GAME_MODE_LABELS, code, code || '-')
}

export function v4SnapshotStatusLabel(status?: string | null): string {
  return lookup(SNAPSHOT_STATUS_LABELS, status, status || '-')
}

/** 向导 Step4/5 赛制状态展示 */
export function v4WizardFormatStatusLabel(status?: string | null): string {
  return lookup(WIZARD_FORMAT_STATUS_LABELS, status, status ? '待确认' : '-')
}

export function v4WizardFormatStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'LOCKED':
    case 'IN_USE': return 'success'
    case 'ARCHIVED': return 'info'
    case 'DRAFT':
    default: return 'warning'
  }
}

export function v4PersonalRegistrationStatusLabel(status?: string | null): string {
  return lookup(PERSONAL_REGISTRATION_STATUS_LABELS, status)
}

export function v4FormationStatusLabel(status?: string | null): string {
  return lookup(FORMATION_STATUS_LABELS, status)
}

export function v4DrawBatchStatusLabel(status?: string | null): string {
  return lookup(DRAW_BATCH_STATUS_LABELS, status)
}

export function v4GroupingStatusLabel(status?: string | null): string {
  return lookup(GROUPING_STATUS_LABELS, status)
}

export function v4StageTypeLabel(type?: string | null): string {
  return lookup(STAGE_TYPE_LABELS, type, type || '-')
}

export function v4StageExecutionStatusLabel(status?: string | null): string {
  return lookup(STAGE_EXECUTION_STATUS_LABELS, status, status || '-')
}

export function v4StageExecutionStatusTagType(status?: string | null): string {
  return STAGE_EXECUTION_STATUS_TAG[status || ''] || 'info'
}

export function v4CompetitionFormatLabel(format?: string | null): string {
  return lookup(COMPETITION_FORMAT_LABELS, format, format || '-')
}

export function v4GroupingModeLabel(mode?: string | null): string {
  return lookup(GROUPING_MODE_LABELS, mode, mode || '-')
}

export function v4AdvancementTypeLabel(type?: string | null): string {
  return lookup(ADVANCEMENT_TYPE_LABELS, type, type || '-')
}

export function v4TieBreakRuleLabel(code?: string | null): string {
  return lookup(TIE_BREAK_RULE_LABELS, code, code || '-')
}

export function v4RosterRuleText(gameMode?: string | null): string {
  const mode = (gameMode || 'FIVE_V_FIVE').toUpperCase()
  if (mode === 'THREE_V_THREE') return '3～8 人'
  return '5～8 人'
}

/** 向导允许的赛制模板（排除 Legacy / BO3/BO5 等） */
export const V4_WIZARD_DISALLOWED_TEMPLATE_CODES = new Set([
  'LEAGUE_STANDARD_V1',
  'V4_LEGACY_ROUND_ROBIN_V1'
])

export function isV4WizardTemplateAllowed(templateCode?: string | null): boolean {
  if (!templateCode) return false
  return !V4_WIZARD_DISALLOWED_TEMPLATE_CODES.has(templateCode.toUpperCase())
}

const SERIES_DEFAULT_PREFIX: Record<string, string> = {
  LEAGUE: 'CNPA League',
  ARENA: 'CNPA Arena',
  CUP: 'CNPA Cup',
  OPEN: 'CNPA Open'
}

export function buildV4SeasonDefaultSeasonName(_seriesCode?: string | null): string {
  return 'S1'
}

export function buildV4SeasonDefaultDisplayTitle(seriesCode?: string | null, seasonYear?: number, seasonName?: string): string {
  const code = (seriesCode || 'LEAGUE').toUpperCase()
  const prefix = SERIES_DEFAULT_PREFIX[code] || `CNPA ${code.charAt(0)}${code.slice(1).toLowerCase()}`
  const year = seasonYear ?? new Date().getFullYear()
  const name = seasonName?.trim() || 'S1'
  return `${prefix} ${year} ${name}`
}

export function buildV4DivisionDefaultName(
  displayTitle?: string | null,
  platformCode?: string | null,
  gameMode?: string | null
): string {
  const base = displayTitle?.trim() || '赛区'
  return `${base} - ${v4PlatformLabel(platformCode)} ${v4GameModeLabel(gameMode)}`
}

export function formatV4DivisionTabLabel(
  div: { divisionName?: string; platformCode?: string; gameMode?: string; registrationMode?: string },
  index: number
): string {
  const platform = v4PlatformLabel(div.platformCode)
  const mode = v4GameModeLabel(div.gameMode)
  const reg = v4RegistrationModeLabel(div.registrationMode)
  if (div.divisionName?.trim()) {
    return `${div.divisionName.trim()} · ${platform} · ${mode}`
  }
  return `赛区 ${index + 1} · ${platform} · ${mode}`
}

export function formatV4DivisionComboLabel(
  platformCode?: string | null,
  gameMode?: string | null,
  registrationMode?: string | null
): string {
  return `${v4PlatformLabel(platformCode)} · ${v4GameModeLabel(gameMode)} · ${v4RegistrationModeLabel(registrationMode)}`
}

export type V4DivisionTimeStatus = 'valid' | 'incomplete' | 'invalid'

export interface V4DivisionTimeValidation {
  status: V4DivisionTimeStatus
  fieldErrors: Partial<Record<'registrationStartTime' | 'registrationEndTime' | 'matchStartTime' | 'matchEndTime' | 'seasonPeriod', string>>
  messages: string[]
}

export function validateV4DivisionTimeRelations(
  div: V4DivisionReadinessInput,
  seasonStartTime?: string,
  seasonEndTime?: string
): V4DivisionTimeValidation {
  const fieldErrors: V4DivisionTimeValidation['fieldErrors'] = {}
  const messages: string[] = []

  if (!div.registrationStartTime || !div.registrationEndTime
    || !div.matchStartTime || !div.matchEndTime) {
    if (!div.registrationStartTime) fieldErrors.registrationStartTime = '请选择报名开始时间'
    if (!div.registrationEndTime) fieldErrors.registrationEndTime = '请选择报名结束时间'
    if (!div.matchStartTime) fieldErrors.matchStartTime = '请选择比赛开始时间'
    if (!div.matchEndTime) fieldErrors.matchEndTime = '请选择比赛结束时间'
    messages.push('时间配置不完整')
    return { status: 'incomplete', fieldErrors, messages }
  }

  if (div.registrationStartTime >= div.registrationEndTime) {
    fieldErrors.registrationEndTime = '报名结束时间必须晚于报名开始时间'
    messages.push('报名开始时间必须早于报名结束时间')
  }
  if (div.registrationEndTime > div.matchStartTime) {
    fieldErrors.matchStartTime = '比赛开始时间不得早于报名结束时间'
    messages.push('报名结束时间不得晚于比赛开始时间')
  }
  if (div.matchStartTime >= div.matchEndTime) {
    fieldErrors.matchEndTime = '比赛结束时间必须晚于比赛开始时间'
    messages.push('比赛开始时间必须早于比赛结束时间')
  }
  if (seasonStartTime && div.registrationStartTime < seasonStartTime) {
    fieldErrors.registrationStartTime = '报名开始时间不能早于赛季展示周期开始时间'
    messages.push('报名开始时间应处于赛季展示周期内')
  }
  if (seasonEndTime && div.matchEndTime > seasonEndTime) {
    fieldErrors.matchEndTime = '比赛结束时间不能晚于赛季展示周期结束时间'
    messages.push('比赛结束时间应处于赛季展示周期内')
  }

  if (Object.keys(fieldErrors).length) {
    return { status: 'invalid', fieldErrors, messages: [...new Set(messages)] }
  }
  return { status: 'valid', fieldErrors, messages: [] }
}

export function v4DivisionStageStatusLabel(status?: string | null): string {
  return lookup(STAGE_EXECUTION_STATUS_LABELS, status, status || '-')
}

export interface V4DivisionReadinessInput {
  id?: number
  platformCode?: string
  gameMode?: string
  divisionName?: string
  registrationMode?: string
  maxTeamCount?: number
  formatTemplateId?: number
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
}

export interface V4DivisionSnapshotReadinessInput {
  snapshotStatus?: string
  locked?: boolean
  stages?: Array<{
    bestOf?: number
    advancementType?: string
    competitionFormat?: string
    stageStatus?: string
  }>
}

export interface V4DivisionReadinessResult {
  basicComplete: boolean
  timeValid: boolean
  teamLimitValid: boolean
  rosterValid: boolean
  snapshotLocked: boolean
  snapshotDraft: boolean
  hasValidStage: boolean
  openRegistrationReady: boolean
  reasons: string[]
}

function isDivisionTimeRangeValid(div: V4DivisionReadinessInput): boolean {
  return validateV4DivisionTimeRelations(div).status === 'valid'
}

function isStageConfigSupported(stage: NonNullable<V4DivisionSnapshotReadinessInput['stages']>[number]): boolean {
  const format = (stage.competitionFormat || '').toUpperCase()
  if (format !== 'ROUND_ROBIN' && format !== 'SINGLE_ELIMINATION') {
    return false
  }
  if (stage.bestOf != null && stage.bestOf !== 1) {
    return false
  }
  if ((stage.advancementType || '').toUpperCase() === 'SERIES_WINNER') {
    return false
  }
  return true
}

export function assessV4DivisionReadiness(
  div: V4DivisionReadinessInput,
  snapshot?: V4DivisionSnapshotReadinessInput | null
): V4DivisionReadinessResult {
  const reasons: string[] = []
  const basicComplete = !!div.id
    && !!div.platformCode
    && !!div.gameMode
    && !!div.divisionName
    && !!div.registrationMode
    && !!div.formatTemplateId
    && div.maxTeamCount != null

  if (!div.id) reasons.push('赛区尚未保存')
  else if (!div.platformCode) reasons.push('缺少平台')
  else if (!div.gameMode) reasons.push('缺少比赛模式')
  else if (!div.divisionName) reasons.push('缺少赛区名称')
  else if (!div.registrationMode) reasons.push('缺少报名方式')
  else if (!div.formatTemplateId) reasons.push('缺少赛制模板')
  else if (div.maxTeamCount == null) reasons.push('缺少队伍上限')

  const timeValid = isDivisionTimeRangeValid(div)
  if (!timeValid) {
    const timeCheck = validateV4DivisionTimeRelations(div)
    if (timeCheck.status === 'incomplete') {
      reasons.push('时间配置不完整')
    } else {
      reasons.push(...timeCheck.messages)
    }
  }

  const teamLimitValid = div.maxTeamCount != null && div.maxTeamCount > 0
  if (!teamLimitValid) reasons.push('队伍上限无效')

  const mode = (div.gameMode || '').toUpperCase()
  const rosterValid = mode === 'FIVE_V_FIVE' || mode === 'THREE_V_THREE'
  if (!rosterValid) reasons.push('阵容人数规则与比赛模式不匹配')

  const snapshotStatus = (snapshot?.snapshotStatus || '').toUpperCase()
  const snapshotLocked = snapshotStatus === 'LOCKED' || snapshot?.locked === true
  const snapshotDraft = snapshotStatus === 'DRAFT'
  if (snapshotDraft) reasons.push('请先确认并启用赛制')
  else if (!snapshot?.snapshotStatus) reasons.push('尚未配置赛制')

  const stages = snapshot?.stages || []
  const hasValidStage = stages.length > 0 && stages.every(isStageConfigSupported)
  if (!stages.length) reasons.push('至少需要添加一个比赛阶段')
  else if (!stages.every(isStageConfigSupported)) {
    reasons.push('存在暂不支持的赛制配置（当前仅支持 BO1 循环赛/分组赛/单败淘汰）')
  }

  const openRegistrationReady = basicComplete
    && timeValid
    && teamLimitValid
    && rosterValid
    && snapshotLocked
    && hasValidStage

  return {
    basicComplete,
    timeValid,
    teamLimitValid,
    rosterValid,
    snapshotLocked,
    snapshotDraft,
    hasValidStage,
    openRegistrationReady,
    reasons: [...new Set(reasons)]
  }
}

export function v4FormatTypeLabel(type?: string | null): string {
  return lookup(FORMAT_TYPE_LABELS, type)
}

export function v4MatchStatusLabel(status?: string | null): string {
  return lookup(MATCH_STATUS_LABELS, status)
}

export function v4SubmissionStatusLabel(status?: string | null): string {
  return lookup(SUBMISSION_STATUS_LABELS, status)
}

export function v4AuditActionLabel(action?: string | null): string {
  return lookup(AUDIT_ACTION_LABELS, action, action || '-')
}

export function v4AuditResultLabel(result?: string | null): string {
  return lookup(AUDIT_RESULT_LABELS, result, result || '-')
}

export function v4RankTypeLabel(type?: string | null): string {
  return lookup(RANK_TYPE_LABELS, type)
}

export function v4SeriesDisplayLabel(seriesName?: string | null, seriesCode?: string | null): string {
  const code = (seriesCode || '').toUpperCase()
  const cn = SERIES_CODE_LABELS[code] || seriesName || code
  if (!code) return cn
  const en = code.charAt(0) + code.slice(1).toLowerCase()
  return `${cn} ${en}`
}

export function v4PublishStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'PUBLISHED': return 'success'
    case 'OFFLINE': return 'info'
    case 'DRAFT':
    default: return 'warning'
  }
}

export function v4SeasonStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'REGISTRATION_OPEN': return 'success'
    case 'NOT_STARTED': return 'info'
    case 'PLAYING': return 'danger'
    case 'SCHEDULED': return ''
    case 'REGISTRATION_CLOSED': return 'warning'
    case 'FINISHED':
    case 'ARCHIVED':
    case 'CANCELLED':
      return 'info'
    default: return 'info'
  }
}

export function v4DivisionStatusTagType(status?: string | null): TagType {
  return v4SeasonStatusTagType(status)
}

export function v4GrantStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'ACTIVE': return 'success'
    case 'REVOKED': return 'danger'
    case 'EXPIRED': return 'info'
    default: return 'info'
  }
}

export function v4RegistrationStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'APPROVED': return 'success'
    case 'PENDING_REVIEW': return 'warning'
    case 'REJECTED': return 'danger'
    case 'WITHDRAWN':
    case 'CANCELLED':
      return 'info'
    case 'DRAFT':
    default: return 'info'
  }
}

export function v4PersonalRegistrationStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'APPROVED':
    case 'CONVERTED': return 'success'
    case 'PENDING_REVIEW': return 'warning'
    case 'REJECTED': return 'danger'
    case 'ASSIGNED': return ''
    case 'WITHDRAWN': return 'info'
    default: return 'info'
  }
}

export function v4FormationStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'CONFIRMED':
    case 'CONVERTED': return 'success'
    case 'READY_TO_CONFIRM': return 'warning'
    case 'CANCELLED': return 'info'
    default: return ''
  }
}

export function v4SnapshotStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'LOCKED':
    case 'IN_USE': return 'success'
    case 'ARCHIVED': return 'info'
    case 'DRAFT':
    default: return 'warning'
  }
}

export function v4GenerateStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'PROCESSING': return 'warning'
    default: return 'info'
  }
}

export function v4RegistrationModeTagType(mode?: string | null): TagType {
  return (mode || '').toUpperCase() === 'TEAM' ? '' : 'info'
}

export function v4FormatTypeTagType(_type?: string | null): TagType {
  return 'info'
}

export function v4MatchStatusTagType(status?: string | null): TagType {
  switch ((status || '').toUpperCase()) {
    case 'FINISHED': return 'info'
    case 'PLAYING': return 'danger'
    case 'SCHEDULED': return 'success'
    case 'CANCELLED': return 'info'
    case 'DRAFT':
    default: return 'warning'
  }
}

export function v4SubmissionStatusTagType(status?: string | null): TagType {
  return v4RegistrationStatusTagType(status)
}

/** 简化时间区间：去掉秒，同年结束时间省略年份 */
export function formatV4CompactRange(start?: string, end?: string): string {
  const compactPattern = '{y}-{m}-{d} {h}:{i}'
  const s = start ? parseTime(start, compactPattern) : null
  const e = end ? parseTime(end, compactPattern) : null
  if (!s && !e) return '-'
  if (!s) return `~ ${e}`
  if (!e) return `${s} ~`
  const startYear = start!.substring(0, 4)
  const endYear = end!.substring(0, 4)
  const endShort = startYear === endYear ? parseTime(end!, '{m}-{d} {h}:{i}') : e
  return `${s} ~ ${endShort || e}`
}

export function formatV4MatchScore(homeScore?: number | null, awayScore?: number | null): string {
  if (homeScore != null && awayScore != null) {
    return `${homeScore} : ${awayScore}`
  }
  return 'VS'
}

/** 单循环预计场数 */
export function calcRoundRobinMatchCount(teamCount?: number | null): number {
  const n = teamCount || 0
  if (n < 2) return 0
  return (n * (n - 1)) / 2
}

export const V4_PUBLISH_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' }
] as const

export const V4_GRANT_STATUS_FILTER_OPTIONS = [
  { label: '生效中', value: 'ACTIVE' },
  { label: '已撤销', value: 'REVOKED' },
  { label: '已过期', value: 'EXPIRED' }
] as const

export const V4_REGISTRATION_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已撤回', value: 'WITHDRAWN' },
  { label: '已取消', value: 'CANCELLED' }
] as const

export const V4_PERSONAL_REGISTRATION_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已撤回', value: 'WITHDRAWN' },
  { label: '已分配', value: 'ASSIGNED' },
  { label: '已转换', value: 'CONVERTED' }
] as const

export const V4_DRAW_BATCH_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已转换', value: 'CONVERTED' }
] as const

export const V4_GROUPING_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已锁定', value: 'LOCKED' },
  { label: '已取消', value: 'CANCELLED' }
] as const

export const V4_STAGE_TYPE_OPTIONS = [
  { label: '常规赛', value: 'REGULAR' },
  { label: '小组赛', value: 'GROUP' },
  { label: '淘汰赛', value: 'KNOCKOUT' },
  { label: '半决赛', value: 'SEMI_FINAL' },
  { label: '总决赛', value: 'FINAL' },
  { label: '自定义', value: 'CUSTOM' }
] as const

export const V4_COMPETITION_FORMAT_OPTIONS = [
  { label: '循环赛', value: 'ROUND_ROBIN' },
  { label: '单败淘汰', value: 'SINGLE_ELIMINATION' },
  { label: '人工安排', value: 'MANUAL' }
] as const

export const V4_GROUPING_MODE_OPTIONS = [
  { label: '无分组', value: 'NONE' },
  { label: '导入线下分组结果', value: 'OFFLINE_RANDOM_RESULT' },
  { label: '人工分组', value: 'MANUAL' },
  { label: '种子分组', value: 'SEEDED' }
] as const

export const V4_ADVANCEMENT_TYPE_OPTIONS = [
  { label: '无晋级', value: 'NONE' },
  { label: '总排名前N', value: 'OVERALL_TOP_N' },
  { label: '小组前N', value: 'GROUP_TOP_N' },
  { label: '系列赛胜者', value: 'SERIES_WINNER' },
  { label: '人工晋级', value: 'MANUAL' }
] as const

export const V4_WIZARD_COMPETITION_FORMAT_OPTIONS = V4_COMPETITION_FORMAT_OPTIONS.filter(
  (item) => item.value !== 'MANUAL'
)

export const V4_WIZARD_ADVANCEMENT_TYPE_OPTIONS = V4_ADVANCEMENT_TYPE_OPTIONS.filter(
  (item) => item.value !== 'SERIES_WINNER'
)

export const V4_TIE_BREAK_RULE_OPTIONS = [
  { label: '相互战绩', value: 'HEAD_TO_HEAD' },
  { label: '胜场数', value: 'WIN_COUNT' },
  { label: '净胜分', value: 'POINT_DIFFERENCE' },
  { label: '总得分', value: 'TOTAL_POINTS' },
  { label: '人工裁定', value: 'MANUAL' }
] as const

/** 向导默认同分规则顺序：胜场数固定第一，人工裁定固定最后 */
export const V4_WIZARD_TIE_BREAK_DEFAULTS = [
  { ruleCode: 'WIN_COUNT', priorityNo: 1, enabled: 1 },
  { ruleCode: 'HEAD_TO_HEAD', priorityNo: 2, enabled: 1 },
  { ruleCode: 'POINT_DIFFERENCE', priorityNo: 3, enabled: 1 },
  { ruleCode: 'TOTAL_POINTS', priorityNo: 4, enabled: 1 },
  { ruleCode: 'MANUAL', priorityNo: 5, enabled: 1 }
] as const

const WIZARD_ALLOWED_TIE_BREAK_CODES = new Set(['WIN_COUNT', 'HEAD_TO_HEAD', 'POINT_DIFFERENCE', 'TOTAL_POINTS', 'MANUAL'])

export const V4_WIZARD_TIE_BREAK_MIDDLE_OPTIONS = [
  { label: '相互战绩', value: 'HEAD_TO_HEAD' },
  { label: '净胜分', value: 'POINT_DIFFERENCE' },
  { label: '总得分', value: 'TOTAL_POINTS' }
] as const

export const V4_WIZARD_STAGE_PRESET_OPTIONS = [
  { label: '循环赛', value: 'ROUND_ROBIN', description: '全部参赛队伍单循环对阵' },
  { label: '分组循环赛', value: 'GROUP_ROUND_ROBIN', description: '先分组，组内循环对阵' },
  { label: 'BO1 单败淘汰赛', value: 'SINGLE_ELIMINATION', description: '单场淘汰，败者出局' }
] as const

export const V4_WIZARD_GROUPING_MODE_OPTIONS = V4_GROUPING_MODE_OPTIONS

export type V4WizardStagePreset = typeof V4_WIZARD_STAGE_PRESET_OPTIONS[number]['value']

export function generateV4StageCode(
  preset: 'REGULAR' | 'GROUP' | 'KNOCKOUT' | 'STAGE',
  existingCodes: string[]
): string {
  const base = preset === 'STAGE' ? 'STAGE' : preset
  const upperExisting = existingCodes.map((c) => c.toUpperCase())
  if (!upperExisting.includes(base)) return base
  let n = 2
  while (upperExisting.includes(`${base}_${n}`)) n += 1
  return `${base}_${n}`
}

export function inferV4StageCodePreset(stage: {
  competitionFormat?: string | null
  groupingMode?: string | null
  stageType?: string | null
}): 'REGULAR' | 'GROUP' | 'KNOCKOUT' | 'STAGE' {
  const format = (stage.competitionFormat || '').toUpperCase()
  const grouping = (stage.groupingMode || '').toUpperCase()
  if (format === 'SINGLE_ELIMINATION') return 'KNOCKOUT'
  if (grouping !== 'NONE' && grouping) return 'GROUP'
  if ((stage.stageType || '').toUpperCase() === 'GROUP') return 'GROUP'
  if (format === 'ROUND_ROBIN') return 'REGULAR'
  return 'STAGE'
}

export function buildV4WizardStagePresetForm(preset: V4WizardStagePreset) {
  if (preset === 'GROUP_ROUND_ROBIN') {
    return {
      stageName: '分组赛',
      stageType: 'GROUP',
      competitionFormat: 'ROUND_ROBIN',
      groupingMode: 'MANUAL',
      groupCount: 4,
      roundRobinCycles: 1,
      bestOf: 1,
      winPoints: 2,
      lossPoints: 1,
      advancementType: 'GROUP_TOP_N',
      advancementCount: 2,
      pairingRule: 'NONE',
      requiresManualConfirmation: 0
    }
  }
  if (preset === 'SINGLE_ELIMINATION') {
    return {
      stageName: '淘汰赛',
      stageType: 'KNOCKOUT',
      competitionFormat: 'SINGLE_ELIMINATION',
      groupingMode: 'NONE',
      groupCount: undefined,
      roundRobinCycles: 1,
      bestOf: 1,
      winPoints: undefined,
      lossPoints: undefined,
      advancementType: 'NONE',
      advancementCount: undefined,
      pairingRule: 'HIGH_VS_LOW',
      requiresManualConfirmation: 0
    }
  }
  return {
    stageName: '循环赛',
    stageType: 'REGULAR',
    competitionFormat: 'ROUND_ROBIN',
    groupingMode: 'NONE',
    groupCount: undefined,
    roundRobinCycles: 1,
    bestOf: 1,
    winPoints: 2,
    lossPoints: 1,
    advancementType: 'NONE',
    advancementCount: undefined,
    pairingRule: 'NONE',
    requiresManualConfirmation: 0
  }
}

export function isV4GroupCompetitionStage(stage: {
  competitionFormat?: string | null
  groupingMode?: string | null
}): boolean {
  const grouping = (stage.groupingMode || '').toUpperCase()
  return (stage.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN'
    && grouping !== 'NONE'
    && !!grouping
}

export function normalizeV4WizardTieBreakRules(
  rules?: Array<{ ruleCode?: string; priorityNo?: number; enabled?: number }> | null
): Array<{ ruleCode: string; priorityNo: number; enabled: number }> {
  const filtered = (rules || [])
    .filter((r) => r.ruleCode && WIZARD_ALLOWED_TIE_BREAK_CODES.has(r.ruleCode.toUpperCase()))
    .map((r) => ({
      ruleCode: r.ruleCode!.toUpperCase(),
      priorityNo: r.priorityNo ?? 99,
      enabled: r.enabled ?? 1
    }))
  const byCode = new Map(filtered.map((r) => [r.ruleCode, r]))
  const middle = V4_WIZARD_TIE_BREAK_MIDDLE_OPTIONS
    .map((opt) => byCode.get(opt.value))
    .filter((r): r is { ruleCode: string; priorityNo: number; enabled: number } => !!r && r.enabled === 1)
    .sort((a, b) => a.priorityNo - b.priorityNo)
  const manual = byCode.get('MANUAL')
  return [
    { ruleCode: 'WIN_COUNT', priorityNo: 1, enabled: 1 },
    ...middle.map((r, idx) => ({ ...r, priorityNo: idx + 2 })),
    { ruleCode: 'MANUAL', priorityNo: middle.length + 2, enabled: manual?.enabled ?? 1 }
  ]
}

export function formatV4TieBreakPreview(
  rules?: Array<{ ruleCode?: string; priorityNo?: number; enabled?: number }> | null
): string {
  const normalized = normalizeV4WizardTieBreakRules(rules)
  const enabled = normalized.filter((r) => r.enabled === 1)
  if (!enabled.length) return '暂未配置同分规则'
  const nonManual = enabled.filter((r) => r.ruleCode !== 'MANUAL').map((r) => v4TieBreakRuleLabel(r.ruleCode))
  const hasManual = enabled.some((r) => r.ruleCode === 'MANUAL')
  if (!nonManual.length && hasManual) return '同分时由运营人员人工裁定。'
  if (nonManual.length === 1 && !hasManual) return `同分时先比较${nonManual[0]}。`
  if (hasManual) {
    if (nonManual.length === 1) {
      return `同分时先比较${nonManual[0]}，仍相同则最后由运营人员人工裁定。`
    }
    const [first, ...rest] = nonManual
    const restText = rest.length === 1 ? rest[0] : `${rest.slice(0, -1).join('、')}和${rest[rest.length - 1]}`
    return `同分时先比较${first}，仍相同则比较${restText}，最后由运营人员人工裁定。`
  }
  return `同分时按 ${nonManual.join(' → ')} 决定排名。`
}

export function formatV4TieBreakSummaryArrow(
  rules?: Array<{ ruleCode?: string; priorityNo?: number; enabled?: number }> | null
): string {
  const normalized = normalizeV4WizardTieBreakRules(rules)
  return normalized
    .filter((r) => r.enabled === 1)
    .map((r) => v4TieBreakRuleLabel(r.ruleCode))
    .join(' → ') || '-'
}

export interface V4StageSummaryInput {
  stageCode?: string
  stageName?: string
  stageOrder?: number
  stageType?: string
  competitionFormat?: string
  groupingMode?: string
  groupCount?: number
  roundRobinCycles?: number
  winPoints?: number
  lossPoints?: number
  advancementType?: string
  advancementCount?: number
  nextStageCode?: string
  tieBreakRules?: Array<{ ruleCode?: string; priorityNo?: number; enabled?: number }>
}

export function formatV4StageCompetitionFormLabel(stage: V4StageSummaryInput): string {
  const format = (stage.competitionFormat || '').toUpperCase()
  if (format === 'SINGLE_ELIMINATION') return '单败淘汰'
  if (isV4GroupCompetitionStage(stage)) {
    return stage.groupCount ? `分组循环（${stage.groupCount} 组）` : '分组循环'
  }
  const cycles = stage.roundRobinCycles ?? 1
  return cycles > 1 ? `单循环（${cycles} 轮）` : '单循环'
}

export function formatV4StageParticipantSource(stage: V4StageSummaryInput, allStages: V4StageSummaryInput[]): string {
  const order = stage.stageOrder || 0
  if (order <= 1) return '本赛区全部正式参赛队伍'
  const sorted = [...allStages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  const prev = sorted.find((s) => (s.stageOrder || 0) === order - 1) || sorted[order - 2]
  return prev?.stageName ? `「${prev.stageName}」晋级队伍` : '上一比赛阶段晋级队伍'
}

export function formatV4StagePairingRule(stage: V4StageSummaryInput): string {
  const format = (stage.competitionFormat || '').toUpperCase()
  if (format === 'SINGLE_ELIMINATION') return '单场对阵，败者淘汰'
  if (isV4GroupCompetitionStage(stage)) return '组内每两支队伍比赛 1 场'
  return '每两支队伍比赛 1 场'
}

export function formatV4StagePointsRule(stage: V4StageSummaryInput): string {
  if ((stage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') return '淘汰赛不计积分'
  if (stage.winPoints != null && stage.lossPoints != null) {
    return `胜 ${stage.winPoints} 分，负 ${stage.lossPoints} 分`
  }
  return '-'
}

export function formatV4StageAdvancementArrangement(
  stage: V4StageSummaryInput,
  allStages: V4StageSummaryInput[]
): string {
  const adv = (stage.advancementType || 'NONE').toUpperCase()
  if (adv === 'NONE' || !adv) return '本阶段为最终阶段'
  const nextStage = allStages.find((s) => s.stageCode === stage.nextStageCode)
  const nextLabel = nextStage?.stageName || '下一比赛阶段'
  const count = stage.advancementCount
  if (adv === 'GROUP_TOP_N') return count ? `每组前 ${count} 名晋级至「${nextLabel}」` : `每组晋级至「${nextLabel}」`
  if (adv === 'OVERALL_TOP_N') return count ? `总排名前 ${count} 名晋级至「${nextLabel}」` : `总排名晋级至「${nextLabel}」`
  if (adv === 'MANUAL') return `人工确认晋级至「${nextLabel}」`
  return v4AdvancementTypeLabel(stage.advancementType)
}

export function formatV4WizardPrimaryIssue(readiness: V4DivisionReadinessResult): string {
  if (readiness.openRegistrationReady) return '-'
  if (readiness.snapshotDraft) return '比赛赛制尚未确认，请返回上一步确认并启用赛制。'
  if (!readiness.hasValidStage) {
    if (readiness.reasons.some((r) => r.includes('至少需要'))) {
      return '尚未配置比赛阶段，请返回上一步完善赛制。'
    }
    return readiness.reasons.find((r) => r.includes('不支持')) || '赛制配置不完整，请返回上一步完善。'
  }
  if (!readiness.timeValid) return '赛区时间配置不完整或无效，请返回上一步完善。'
  if (!readiness.basicComplete) return '赛区基础信息不完整，请返回上一步完善。'
  return readiness.reasons[0] || '配置尚未完成'
}

export function formatV4WizardRegistrationReadiness(readiness: V4DivisionReadinessResult): string {
  if (readiness.openRegistrationReady) return '可开放报名'
  if (readiness.snapshotDraft || !readiness.snapshotLocked) return '确认赛制后可开放'
  return '暂不可开放'
}

export function formatV4WizardReadinessItem(
  kind: 'basic' | 'time' | 'format' | 'stageCount' | 'registration',
  readiness: V4DivisionReadinessResult,
  stageCount: number,
  snapshotStatus?: string | null
): string {
  switch (kind) {
    case 'basic':
      return readiness.basicComplete ? '已完成' : '未完成'
    case 'time':
      return readiness.timeValid ? '已完成' : '未完成'
    case 'format':
      return v4WizardFormatStatusLabel(snapshotStatus)
    case 'stageCount':
      return stageCount > 0 ? `${stageCount} 个` : '无'
    case 'registration':
      return formatV4WizardRegistrationReadiness(readiness)
    default:
      return '-'
  }
}

export function v4WizardAdvancementMode(advancementType?: string | null): 'FINAL' | 'NEXT' {
  const adv = (advancementType || 'NONE').toUpperCase()
  return adv === 'NONE' ? 'FINAL' : 'NEXT'
}

export function v4WizardAdvancementTypeOptions(isGroupStage: boolean) {
  const options = [
    { label: '总排名前 N 名', value: 'OVERALL_TOP_N' },
    { label: '人工确认晋级', value: 'MANUAL' }
  ]
  if (isGroupStage) {
    options.unshift({ label: '每组前 N 名', value: 'GROUP_TOP_N' })
  }
  return options
}

/** 向导赛制方案（快速创建） */
export type V4FormatSchemeId =
  | 'ROUND_ROBIN'
  | 'GROUP_ROUND_ROBIN'
  | 'SINGLE_ELIMINATION'
  | 'ROUND_ROBIN_TO_ELIM'
  | 'GROUP_TO_ELIM'
  | 'CUSTOM'

export interface V4FormatSchemeOption {
  id: V4FormatSchemeId
  name: string
  summary: string
  scene: string
  flowSteps: string[]
  category: 'single' | 'combo' | 'custom'
  baseTemplateCode?: string
}

export const V4_FORMAT_SCHEME_OPTIONS: V4FormatSchemeOption[] = [
  {
    id: 'ROUND_ROBIN',
    name: '单循环',
    summary: '全部参赛队伍进行单循环对阵，按积分排名决出名次。',
    scene: '适合联赛、常规赛等全员循环积分制赛事。',
    flowSteps: ['全部队伍', '单循环', '积分排名'],
    category: 'single',
    baseTemplateCode: 'ROUND_ROBIN_BASIC_V1'
  },
  {
    id: 'GROUP_ROUND_ROBIN',
    name: '分组循环',
    summary: '参赛队伍先分组，组内进行单循环，按小组积分排名。',
    scene: '适合队伍较多、需先分组再排积分榜的赛事。',
    flowSteps: ['全部队伍', '分组', '组内单循环', '小组排名'],
    category: 'single',
    baseTemplateCode: 'GROUP_STAGE_BASIC_V1'
  },
  {
    id: 'SINGLE_ELIMINATION',
    name: 'BO1 单败淘汰',
    summary: '全部参赛队伍直接进入 BO1 单败淘汰赛，败者出局。',
    scene: '适合杯赛、锦标赛等直接淘汰制赛事。',
    flowSteps: ['全部队伍', 'BO1 单败淘汰', '决出冠军'],
    category: 'single',
    baseTemplateCode: 'SINGLE_ELIM_BO1_V1'
  },
  {
    id: 'ROUND_ROBIN_TO_ELIM',
    name: '单循环 → BO1 单败淘汰',
    summary: '全部队伍先进行单循环赛，总排名前 N 名晋级 BO1 单败淘汰赛。',
    scene: '适合先积分筛选、再淘汰决冠的联赛季后赛模式。',
    flowSteps: ['全部队伍', '单循环', '前 N 名晋级', 'BO1 单败淘汰'],
    category: 'combo',
    baseTemplateCode: 'ROUND_ROBIN_BASIC_V1'
  },
  {
    id: 'GROUP_TO_ELIM',
    name: '小组循环 → BO1 单败淘汰',
    summary: '队伍先进行分组循环赛，每组达到条件的球队晋级 BO1 单败淘汰赛。',
    scene: '适合世界杯、杯赛小组赛+淘汰赛的经典赛制。',
    flowSteps: ['全部队伍', '分组循环', '每组前 N 名', 'BO1 单败淘汰'],
    category: 'combo',
    baseTemplateCode: 'GROUP_STAGE_BASIC_V1'
  },
  {
    id: 'CUSTOM',
    name: '自定义组合',
    summary: '自行添加、调整多个比赛阶段，并配置晋级关系。',
    scene: '适合有特殊赛制需求、需精细控制的赛事。',
    flowSteps: ['自定义阶段', '…', '…'],
    category: 'custom'
  }
]

export interface V4QuickCreateParams {
  expectedTeamCount: number
  groupCount?: number
  groupingMode?: string
  roundRobinCycles?: number
  advancePerGroup?: number
  overallAdvanceCount?: number
  pairingRule?: string
}

export interface V4SchemeStagePayload {
  stage: {
    stageCode: string
    stageName: string
    stageOrder: number
    stageType: string
    competitionFormat: string
    groupingMode: string
    groupCount?: number
    roundRobinCycles?: number
    bestOf: number
    winPoints?: number
    lossPoints?: number
    advancementType: string
    advancementCount?: number
    pairingRule: string
    nextStageCode?: string
    requiresManualConfirmation: number
  }
  tieBreakRules?: Array<{ ruleCode: string; priorityNo: number; enabled: number }>
}

export function getV4FormatSchemeOption(id: V4FormatSchemeId): V4FormatSchemeOption | undefined {
  return V4_FORMAT_SCHEME_OPTIONS.find((s) => s.id === id)
}

export function isV4PowerOfTwo(n: number): boolean {
  return n > 0 && (n & (n - 1)) === 0
}

export function formatV4EliminationRoundLabel(teamCount: number): string {
  if (teamCount <= 2) return '决赛'
  if (teamCount === 4) return '4 强'
  if (teamCount === 8) return '8 强'
  if (teamCount === 16) return '16 强'
  if (teamCount === 32) return '32 强'
  return `${teamCount} 支队伍`
}

export function calcV4TeamsPerGroup(expectedTeamCount: number, groupCount: number): number {
  if (groupCount <= 0) return expectedTeamCount
  return Math.ceil(expectedTeamCount / groupCount)
}

export function calcV4GroupStageMatchCount(
  expectedTeamCount: number,
  groupCount: number,
  cycles = 1
): number {
  if (groupCount <= 1 || expectedTeamCount < 2) return 0
  const base = Math.floor(expectedTeamCount / groupCount)
  const remainder = expectedTeamCount % groupCount
  let total = 0
  for (let i = 0; i < groupCount; i += 1) {
    const n = base + (i < remainder ? 1 : 0)
    if (n >= 2) total += calcRoundRobinMatchCount(n) * cycles
  }
  return total
}

export function calcV4EliminationMatchCount(teamCount: number): number {
  if (teamCount < 2) return 0
  return teamCount - 1
}

export function calcV4AdvancementTotal(params: V4QuickCreateParams, schemeId: V4FormatSchemeId): number {
  if (schemeId === 'GROUP_TO_ELIM' || schemeId === 'GROUP_ROUND_ROBIN') {
    return (params.groupCount || 0) * (params.advancePerGroup || 0)
  }
  if (schemeId === 'ROUND_ROBIN_TO_ELIM') {
    return params.overallAdvanceCount || 0
  }
  return params.expectedTeamCount
}

export function buildDefaultV4QuickCreateParams(
  schemeId: V4FormatSchemeId,
  expectedTeamCount: number
): V4QuickCreateParams {
  const base: V4QuickCreateParams = {
    expectedTeamCount,
    groupingMode: 'MANUAL',
    roundRobinCycles: 1,
    pairingRule: 'HIGH_VS_LOW'
  }
  if (schemeId === 'GROUP_ROUND_ROBIN' || schemeId === 'GROUP_TO_ELIM') {
    const groupCount = expectedTeamCount >= 16 ? 4 : 2
    return { ...base, groupCount, advancePerGroup: 2 }
  }
  if (schemeId === 'ROUND_ROBIN_TO_ELIM') {
    const overallAdvanceCount = expectedTeamCount >= 16 ? 8 : 4
    return { ...base, overallAdvanceCount }
  }
  return base
}

export function buildV4SchemeStagePayloads(
  schemeId: V4FormatSchemeId,
  params: V4QuickCreateParams
): V4SchemeStagePayload[] {
  const tieBreakRules = V4_WIZARD_TIE_BREAK_DEFAULTS.map((r) => ({ ...r }))
  if (schemeId === 'SINGLE_ELIMINATION') {
    return [{
      stage: {
        stageCode: 'KNOCKOUT',
        stageName: '淘汰赛',
        stageOrder: 1,
        stageType: 'KNOCKOUT',
        competitionFormat: 'SINGLE_ELIMINATION',
        groupingMode: 'NONE',
        bestOf: 1,
        advancementType: 'NONE',
        pairingRule: params.pairingRule || 'HIGH_VS_LOW',
        requiresManualConfirmation: 0
      }
    }]
  }
  if (schemeId === 'ROUND_ROBIN') {
    return [{
      stage: {
        stageCode: 'REGULAR',
        stageName: '循环赛',
        stageOrder: 1,
        stageType: 'REGULAR',
        competitionFormat: 'ROUND_ROBIN',
        groupingMode: 'NONE',
        roundRobinCycles: params.roundRobinCycles ?? 1,
        bestOf: 1,
        winPoints: 2,
        lossPoints: 1,
        advancementType: 'NONE',
        pairingRule: 'NONE',
        requiresManualConfirmation: 0
      },
      tieBreakRules
    }]
  }
  if (schemeId === 'GROUP_ROUND_ROBIN') {
    return [{
      stage: {
        stageCode: 'GROUP',
        stageName: '小组赛',
        stageOrder: 1,
        stageType: 'GROUP',
        competitionFormat: 'ROUND_ROBIN',
        groupingMode: params.groupingMode || 'MANUAL',
        groupCount: params.groupCount ?? 4,
        roundRobinCycles: params.roundRobinCycles ?? 1,
        bestOf: 1,
        winPoints: 2,
        lossPoints: 1,
        advancementType: 'NONE',
        pairingRule: 'NONE',
        requiresManualConfirmation: 0
      },
      tieBreakRules
    }]
  }
  if (schemeId === 'ROUND_ROBIN_TO_ELIM') {
    const advanceCount = params.overallAdvanceCount ?? 8
    return [
      {
        stage: {
          stageCode: 'REGULAR',
          stageName: '循环赛',
          stageOrder: 1,
          stageType: 'REGULAR',
          competitionFormat: 'ROUND_ROBIN',
          groupingMode: 'NONE',
          roundRobinCycles: params.roundRobinCycles ?? 1,
          bestOf: 1,
          winPoints: 2,
          lossPoints: 1,
          advancementType: 'OVERALL_TOP_N',
          advancementCount: advanceCount,
          pairingRule: 'NONE',
          nextStageCode: 'KNOCKOUT',
          requiresManualConfirmation: 0
        },
        tieBreakRules
      },
      {
        stage: {
          stageCode: 'KNOCKOUT',
          stageName: '淘汰赛',
          stageOrder: 2,
          stageType: 'KNOCKOUT',
          competitionFormat: 'SINGLE_ELIMINATION',
          groupingMode: 'NONE',
          bestOf: 1,
          advancementType: 'NONE',
          pairingRule: params.pairingRule || 'HIGH_VS_LOW',
          requiresManualConfirmation: 0
        }
      }
    ]
  }
  if (schemeId === 'GROUP_TO_ELIM') {
    const groupCount = params.groupCount ?? 4
    const advancePerGroup = params.advancePerGroup ?? 2
    return [
      {
        stage: {
          stageCode: 'GROUP',
          stageName: '小组赛',
          stageOrder: 1,
          stageType: 'GROUP',
          competitionFormat: 'ROUND_ROBIN',
          groupingMode: params.groupingMode || 'MANUAL',
          groupCount,
          roundRobinCycles: params.roundRobinCycles ?? 1,
          bestOf: 1,
          winPoints: 2,
          lossPoints: 1,
          advancementType: 'GROUP_TOP_N',
          advancementCount: advancePerGroup,
          pairingRule: 'NONE',
          nextStageCode: 'KNOCKOUT',
          requiresManualConfirmation: 0
        },
        tieBreakRules
      },
      {
        stage: {
          stageCode: 'KNOCKOUT',
          stageName: '淘汰赛',
          stageOrder: 2,
          stageType: 'KNOCKOUT',
          competitionFormat: 'SINGLE_ELIMINATION',
          groupingMode: 'NONE',
          bestOf: 1,
          advancementType: 'NONE',
          pairingRule: params.pairingRule || 'HIGH_VS_LOW',
          requiresManualConfirmation: 0
        }
      }
    ]
  }
  return []
}

export interface V4FormatValidationResult {
  valid: boolean
  messages: string[]
}

export function validateV4QuickCreateParams(
  schemeId: V4FormatSchemeId,
  params: V4QuickCreateParams
): V4FormatValidationResult {
  const messages: string[] = []
  const { expectedTeamCount } = params
  if (expectedTeamCount < 2) messages.push('预计参赛队伍至少需要 2 支')

  if (schemeId === 'GROUP_ROUND_ROBIN' || schemeId === 'GROUP_TO_ELIM') {
    const groupCount = params.groupCount ?? 0
    const advancePerGroup = params.advancePerGroup ?? 0
    const teamsPerGroup = calcV4TeamsPerGroup(expectedTeamCount, groupCount)
    if (groupCount <= 1) messages.push('分组数量必须大于 1')
    if (groupCount > expectedTeamCount) messages.push('分组数量不能大于预计参赛队伍数')
    if (schemeId === 'GROUP_TO_ELIM' && advancePerGroup <= 0) {
      messages.push('每组晋级数量必须大于 0')
    }
    if (schemeId === 'GROUP_TO_ELIM' && advancePerGroup > teamsPerGroup) {
      messages.push(`每组晋级数（${advancePerGroup}）不能大于预计每组队伍数（约 ${teamsPerGroup} 支）`)
    }
  }

  if (schemeId === 'ROUND_ROBIN_TO_ELIM') {
    const advanceCount = params.overallAdvanceCount ?? 0
    if (advanceCount <= 0) messages.push('晋级名额必须大于 0')
    if (advanceCount > expectedTeamCount) {
      messages.push('晋级名额不能大于预计参赛队伍数')
    }
    if (!isV4PowerOfTwo(advanceCount)) {
      messages.push(`当前将有 ${advanceCount} 支球队晋级，但系统暂不支持 ${advanceCount} 支队伍的淘汰赛轮空。请调整为 4 支或 8 支等 2 的幂次方晋级。`)
    }
  }

  if (schemeId === 'GROUP_TO_ELIM') {
    const advanceTotal = calcV4AdvancementTotal(params, schemeId)
    if (!isV4PowerOfTwo(advanceTotal)) {
      messages.push(`当前将有 ${advanceTotal} 支球队晋级，但系统暂不支持 ${advanceTotal} 支队伍的淘汰赛轮空。请调整为 4 支或 8 支等 2 的幂次方晋级。`)
    }
  }

  if (schemeId === 'SINGLE_ELIMINATION') {
    if (!isV4PowerOfTwo(expectedTeamCount)) {
      messages.push(`预计 ${expectedTeamCount} 支队伍直接淘汰赛，但系统暂不支持需要轮空的队伍规模。请调整队伍上限为 4、8、16 等 2 的幂次方。`)
    }
  }

  return { valid: messages.length === 0, messages }
}

export function validateV4StageCombination(
  stages: V4StageSummaryInput[],
  maxTeamCount?: number
): V4FormatValidationResult {
  const messages: string[] = []
  if (!stages.length) {
    messages.push('至少需要添加一个比赛阶段')
    return { valid: false, messages }
  }
  const sorted = [...stages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  const codeSet = new Set(sorted.map((s) => s.stageCode))

  for (let i = 0; i < sorted.length; i += 1) {
    const stage = sorted[i]
    const isLast = i === sorted.length - 1
    const adv = (stage.advancementType || 'NONE').toUpperCase()
    const hasNext = adv !== 'NONE' && !!adv

    if (isLast && stage.nextStageCode) {
      messages.push(`「${stage.stageName}」为最后阶段，不应再指向下一阶段`)
    }
    if (!isLast && !hasNext) {
      messages.push(`「${stage.stageName}」不是最后阶段，请配置晋级规则`)
    }
    if (hasNext && stage.nextStageCode && !codeSet.has(stage.nextStageCode)) {
      messages.push(`「${stage.stageName}」的下一阶段配置无效，请重新选择`)
    }
    if (hasNext && stage.nextStageCode) {
      const next = sorted.find((s) => s.stageCode === stage.nextStageCode)
      if (next && (next.stageOrder || 0) <= (stage.stageOrder || 0)) {
        messages.push('阶段顺序与晋级关系不一致，请调整阶段顺序')
      }
    }

    if (isV4GroupCompetitionStage(stage)) {
      const groupCount = stage.groupCount ?? 0
      const teamsPerGroup = calcV4TeamsPerGroup(maxTeamCount || 0, groupCount)
      if (groupCount <= 1) messages.push(`「${stage.stageName}」分组数量必须大于 1`)
      if (maxTeamCount && groupCount > maxTeamCount) {
        messages.push(`「${stage.stageName}」分组数量不能大于参赛队伍上限`)
      }
      if (adv === 'GROUP_TOP_N') {
        const count = stage.advancementCount ?? 0
        if (count <= 0) messages.push(`「${stage.stageName}」每组晋级数必须大于 0`)
        if (count > teamsPerGroup) {
          messages.push(`「${stage.stageName}」每组晋级数不能大于预计每组队伍数（约 ${teamsPerGroup} 支）`)
        }
      }
    }

    if (adv === 'GROUP_TOP_N' && stage.groupCount) {
      const advanceTotal = stage.groupCount * (stage.advancementCount || 0)
      const nextStage = sorted.find((s) => s.stageCode === stage.nextStageCode)
      if (nextStage && (nextStage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') {
        if (!isV4PowerOfTwo(advanceTotal)) {
          messages.push(`当前将有 ${advanceTotal} 支球队晋级，但系统暂不支持 ${advanceTotal} 支队伍的淘汰赛轮空。请调整为 4 支或 8 支晋级。`)
        }
      }
    }
    if (adv === 'OVERALL_TOP_N') {
      const advanceTotal = stage.advancementCount || 0
      const nextStage = sorted.find((s) => s.stageCode === stage.nextStageCode)
      if (nextStage && (nextStage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') {
        if (!isV4PowerOfTwo(advanceTotal)) {
          messages.push(`当前将有 ${advanceTotal} 支球队晋级，但系统暂不支持 ${advanceTotal} 支队伍的淘汰赛轮空。请调整为 4 支或 8 支晋级。`)
        }
      }
    }
  }

  const lastStage = sorted[sorted.length - 1]
  if ((lastStage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION' && sorted.length === 1) {
    const teamCount = maxTeamCount || 0
    if (teamCount >= 2 && !isV4PowerOfTwo(teamCount)) {
      messages.push(`预计 ${teamCount} 支队伍直接淘汰赛，但系统暂不支持需要轮空的队伍规模。请调整队伍上限为 4、8、16 等 2 的幂次方。`)
    }
  }

  return { valid: messages.length === 0, messages: [...new Set(messages)] }
}

export interface V4FormatEstimate {
  groupCount?: number
  teamsPerGroup?: number
  groupStageMatches?: number
  advanceTotal?: number
  eliminationStartRound?: string
  eliminationMatches?: number
  totalMatches?: number
}

export function calcV4FormatEstimateFromStages(
  stages: V4StageSummaryInput[],
  expectedTeamCount: number
): V4FormatEstimate {
  const sorted = [...stages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  const estimate: V4FormatEstimate = {}
  let total = 0

  for (const stage of sorted) {
    if (isV4GroupCompetitionStage(stage)) {
      const groupCount = stage.groupCount ?? 0
      estimate.groupCount = groupCount
      estimate.teamsPerGroup = calcV4TeamsPerGroup(expectedTeamCount, groupCount)
      const cycles = stage.roundRobinCycles ?? 1
      estimate.groupStageMatches = calcV4GroupStageMatchCount(expectedTeamCount, groupCount, cycles)
      total += estimate.groupStageMatches
      if ((stage.advancementType || '').toUpperCase() === 'GROUP_TOP_N') {
        estimate.advanceTotal = groupCount * (stage.advancementCount || 0)
      }
    } else if ((stage.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN') {
      const cycles = stage.roundRobinCycles ?? 1
      total += calcRoundRobinMatchCount(expectedTeamCount) * cycles
      if ((stage.advancementType || '').toUpperCase() === 'OVERALL_TOP_N') {
        estimate.advanceTotal = stage.advancementCount
      }
    } else if ((stage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') {
      const teamCount = estimate.advanceTotal || expectedTeamCount
      estimate.eliminationStartRound = formatV4EliminationRoundLabel(teamCount)
      estimate.eliminationMatches = calcV4EliminationMatchCount(teamCount)
      total += estimate.eliminationMatches
    }
  }
  estimate.totalMatches = total
  return estimate
}

export function calcV4FormatEstimateFromQuickParams(
  schemeId: V4FormatSchemeId,
  params: V4QuickCreateParams
): V4FormatEstimate {
  const payloads = buildV4SchemeStagePayloads(schemeId, params)
  const stages: V4StageSummaryInput[] = payloads.map((p) => p.stage)
  return calcV4FormatEstimateFromStages(stages, params.expectedTeamCount)
}

export function formatV4QuickCreateNaturalLanguage(
  schemeId: V4FormatSchemeId,
  params: V4QuickCreateParams
): string {
  const { expectedTeamCount } = params
  const estimate = calcV4FormatEstimateFromQuickParams(schemeId, params)
  if (schemeId === 'ROUND_ROBIN') {
    return `${expectedTeamCount} 支队伍进行单循环，按积分排名决出名次。`
  }
  if (schemeId === 'GROUP_ROUND_ROBIN') {
    const gc = params.groupCount ?? 4
    const tpg = estimate.teamsPerGroup ?? calcV4TeamsPerGroup(expectedTeamCount, gc)
    return `${expectedTeamCount} 支队伍分为 ${gc} 组，每组约 ${tpg} 支队伍，各组进行单循环，按小组积分排名。`
  }
  if (schemeId === 'SINGLE_ELIMINATION') {
    return `${expectedTeamCount} 支队伍直接进入 BO1 单败淘汰赛，最终决出冠军。`
  }
  if (schemeId === 'ROUND_ROBIN_TO_ELIM') {
    const adv = params.overallAdvanceCount ?? 8
    return `${expectedTeamCount} 支队伍进行单循环，总排名前 ${adv} 名晋级，共 ${adv} 支球队进入 BO1 单败淘汰赛，最终决出冠军。`
  }
  if (schemeId === 'GROUP_TO_ELIM') {
    const gc = params.groupCount ?? 4
    const tpg = estimate.teamsPerGroup ?? calcV4TeamsPerGroup(expectedTeamCount, gc)
    const adv = calcV4AdvancementTotal(params, schemeId)
    return `${expectedTeamCount} 支队伍分为 ${gc} 组，每组约 ${tpg} 支队伍。各组进行单循环，每组前 ${params.advancePerGroup ?? 2} 名晋级，共 ${adv} 支球队进入 BO1 单败淘汰赛，最终决出冠军。`
  }
  return ''
}

export function formatV4FormatFlowFromStages(stages: V4StageSummaryInput[]): string {
  if (!stages.length) return '-'
  const sorted = [...stages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  return sorted.map((s) => {
    if ((s.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') return 'BO1 单败淘汰赛'
    if (isV4GroupCompetitionStage(s)) return '小组循环赛'
    if ((s.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN') return '单循环赛'
    return s.stageName || '比赛阶段'
  }).join(' → ')
}

export function formatV4FormatNaturalLanguageFromStages(
  stages: V4StageSummaryInput[],
  expectedTeamCount: number
): string {
  if (!stages.length) return '尚未配置赛制'
  const sorted = [...stages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  if (sorted.length === 1) {
    const s = sorted[0]
    if ((s.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') {
      return `${expectedTeamCount} 支队伍直接进入 BO1 单败淘汰赛，最终决出冠军。`
    }
    if (isV4GroupCompetitionStage(s)) {
      const gc = s.groupCount ?? 0
      const tpg = calcV4TeamsPerGroup(expectedTeamCount, gc)
      return `${expectedTeamCount} 支队伍分为 ${gc} 组，每组约 ${tpg} 支队伍，各组进行单循环，按小组积分排名。`
    }
    return `${expectedTeamCount} 支队伍进行单循环，按积分排名决出名次。`
  }
  const first = sorted[0]
  const estimate = calcV4FormatEstimateFromStages(sorted, expectedTeamCount)
  if (isV4GroupCompetitionStage(first)) {
    const gc = first.groupCount ?? 0
    const adv = estimate.advanceTotal ?? 0
    return `${expectedTeamCount} 支队伍预计分为 ${gc} 组，每组前 ${first.advancementCount ?? 0} 名晋级，共 ${adv} 支队伍进入淘汰赛。`
  }
  const adv = first.advancementCount ?? estimate.advanceTotal ?? 0
  return `${expectedTeamCount} 支队伍进行单循环，总排名前 ${adv} 名晋级，共 ${adv} 支队伍进入 BO1 单败淘汰赛。`
}

export function formatV4StageFlowCardSummary(
  stage: V4StageSummaryInput,
  allStages: V4StageSummaryInput[],
  expectedTeamCount: number
): string {
  const parts: string[] = []
  if (isV4GroupCompetitionStage(stage)) {
    parts.push(`${stage.groupCount ?? 0} 组 · 每组单循环`)
    if ((stage.advancementType || '').toUpperCase() === 'GROUP_TOP_N') {
      parts.push(`每组前 ${stage.advancementCount ?? 0} 名晋级`)
    }
  } else if ((stage.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN') {
    parts.push('全部队伍单循环')
    if ((stage.advancementType || '').toUpperCase() === 'OVERALL_TOP_N') {
      parts.push(`总排名前 ${stage.advancementCount ?? 0} 名晋级`)
    }
  } else if ((stage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') {
    const prev = allStages.find((s) => s.nextStageCode === stage.stageCode)
    let teamCount = expectedTeamCount
    if (prev && (prev.advancementType || '').toUpperCase() === 'GROUP_TOP_N') {
      teamCount = (prev.groupCount || 0) * (prev.advancementCount || 0)
    } else if (prev && (prev.advancementType || '').toUpperCase() === 'OVERALL_TOP_N') {
      teamCount = prev.advancementCount || 0
    }
    parts.push(`${formatV4EliminationRoundLabel(teamCount)}开始 · 决出冠军`)
  }
  return parts.join(' · ') || formatV4StageCompetitionFormLabel(stage)
}

export function formatV4StageAdvanceCountLabel(
  stage: V4StageSummaryInput
): string | null {
  const adv = (stage.advancementType || 'NONE').toUpperCase()
  if (adv === 'NONE') return null
  if (adv === 'GROUP_TOP_N') {
    const total = (stage.groupCount || 0) * (stage.advancementCount || 0)
    return `${total} 支晋级`
  }
  if (adv === 'OVERALL_TOP_N') return `${stage.advancementCount ?? 0} 支晋级`
  return null
}

export function resolveV4WizardFormatDisplayStatus(
  snapshot?: V4DivisionSnapshotReadinessInput | null,
  readiness?: V4DivisionReadinessResult | null,
  stageCount = 0
): { label: string; tagType: TagType } {
  if (!snapshot?.snapshotStatus) return { label: '未配置', tagType: 'info' }
  const status = (snapshot.snapshotStatus || '').toUpperCase()
  if (status === 'LOCKED') return { label: '已确认', tagType: 'success' }
  if (status === 'IN_USE') return { label: '使用中', tagType: 'success' }
  if (status === 'ARCHIVED') return { label: '已归档', tagType: 'info' }
  if (status === 'DRAFT') {
    if (stageCount === 0) return { label: '配置中', tagType: 'warning' }
    if (readiness?.hasValidStage && readiness?.snapshotDraft) {
      return { label: '待确认', tagType: 'warning' }
    }
    return { label: '配置中', tagType: 'warning' }
  }
  return { label: '待确认', tagType: 'warning' }
}

export function inferV4FormatSchemeFromStages(stages: V4StageSummaryInput[]): V4FormatSchemeId | null {
  if (!stages.length) return null
  const sorted = [...stages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  if (sorted.length === 1) {
    const s = sorted[0]
    if ((s.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') return 'SINGLE_ELIMINATION'
    if (isV4GroupCompetitionStage(s)) return 'GROUP_ROUND_ROBIN'
    if ((s.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN') return 'ROUND_ROBIN'
  }
  if (sorted.length === 2) {
    const [first, second] = sorted
    if ((second.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') {
      if (isV4GroupCompetitionStage(first)) return 'GROUP_TO_ELIM'
      if ((first.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN') return 'ROUND_ROBIN_TO_ELIM'
    }
  }
  return 'CUSTOM'
}

export function formatV4LockConfirmPreview(
  div: { divisionName?: string; platformCode?: string; gameMode?: string; registrationMode?: string; maxTeamCount?: number },
  stages: V4StageSummaryInput[]
): string {
  const combo = formatV4DivisionComboLabel(div.platformCode, div.gameMode, div.registrationMode)
  const flow = formatV4FormatFlowFromStages(stages)
  const summary = formatV4FormatNaturalLanguageFromStages(stages, div.maxTeamCount || 0)
  return [
    `赛区：${div.divisionName || '未命名'} · ${combo}`,
    '',
    '赛制流程：',
    flow,
    '',
    '规则摘要：',
    summary,
    '',
    '确认后，该赛区将按照以上赛制运行。开放报名后，赛制不能随意修改。是否确认并启用？'
  ].join('\n')
}

export const V4_MATCH_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已排赛', value: 'SCHEDULED' },
  { label: '进行中', value: 'PLAYING' },
  { label: '已结束', value: 'FINISHED' },
  { label: '已取消', value: 'CANCELLED' }
] as const

export const V4_SUBMISSION_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已被新版本替代', value: 'SUPERSEDED' }
] as const
