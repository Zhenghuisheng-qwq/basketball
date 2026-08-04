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
  PERSONAL: '个人报名（暂未开放）'
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
