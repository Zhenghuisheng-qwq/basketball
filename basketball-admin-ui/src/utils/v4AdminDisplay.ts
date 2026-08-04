import { parseTime } from '@/utils/ruoyi'

/** V4 后台运营展示辅助（纯展示，不改业务值） */

const PUBLISH_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下线'
}

const SEASON_DIVISION_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '未开始',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名结束',
  SCHEDULED: '已排赛',
  PLAYING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消',
  DRAFT: '草稿'
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

const COMPETITION_FORMAT_LABELS: Record<string, string> = {
  ROUND_ROBIN: '循环赛',
  SINGLE_ELIMINATION: '单败淘汰',
  MANUAL: '人工安排'
}

const GROUPING_MODE_LABELS: Record<string, string> = {
  NONE: '无分组',
  OFFLINE_RANDOM_RESULT: '线下抽签结果',
  MANUAL: '人工分组',
  SEEDED: '种子分组'
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
  return lookup(SEASON_DIVISION_STATUS_LABELS, status)
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
  if (mode === 'THREE_V_THREE') return '阵容人数 3～8 人'
  return '阵容人数 5～8 人'
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
  { label: '线下抽签结果', value: 'OFFLINE_RANDOM_RESULT' },
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

export const V4_TIE_BREAK_RULE_OPTIONS = [
  { label: '相互战绩', value: 'HEAD_TO_HEAD' },
  { label: '胜场数', value: 'WIN_COUNT' },
  { label: '净胜分', value: 'POINT_DIFFERENCE' },
  { label: '总得分', value: 'TOTAL_POINTS' },
  { label: '人工裁定', value: 'MANUAL' }
] as const

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
