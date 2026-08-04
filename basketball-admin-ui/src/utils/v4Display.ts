import { parseTime } from '@/utils/ruoyi'

/** V4 后台赛事发布页展示辅助（纯展示，不改业务值） */

const PUBLISH_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下线'
}

const SEASON_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '未开始',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名结束',
  SCHEDULED: '已排赛',
  PLAYING: '进行中',
  FINISHED: '已结束'
}

const DIVISION_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名结束',
  SCHEDULED: '已排赛',
  PLAYING: '进行中',
  FINISHED: '已结束'
}

const REGISTRATION_MODE_LABELS: Record<string, string> = {
  TEAM: '球队报名',
  PERSONAL: '个人报名'
}

const FORMAT_TYPE_LABELS: Record<string, string> = {
  REGULAR_ROUND_ROBIN: '常规赛单循环',
  GROUP_KNOCKOUT: '小组赛+淘汰赛',
  KNOCKOUT: '单淘汰'
}

export function v4PublishStatusLabel(status?: string | null): string {
  return PUBLISH_STATUS_LABELS[(status || '').toUpperCase()] || status || '-'
}

export function v4SeasonStatusLabel(status?: string | null): string {
  return SEASON_STATUS_LABELS[(status || '').toUpperCase()] || status || '-'
}

export function v4DivisionStatusLabel(status?: string | null): string {
  return DIVISION_STATUS_LABELS[(status || '').toUpperCase()] || status || '-'
}

export function v4RegistrationModeLabel(mode?: string | null): string {
  return REGISTRATION_MODE_LABELS[(mode || '').toUpperCase()] || mode || '-'
}

export function v4FormatTypeLabel(type?: string | null): string {
  return FORMAT_TYPE_LABELS[(type || '').toUpperCase()] || type || '-'
}

export function v4PublishStatusTagType(status?: string | null): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch ((status || '').toUpperCase()) {
    case 'PUBLISHED':
      return 'success'
    case 'OFFLINE':
      return 'info'
    case 'DRAFT':
    default:
      return 'warning'
  }
}

export function v4SeasonStatusTagType(status?: string | null): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch ((status || '').toUpperCase()) {
    case 'REGISTRATION_OPEN':
      return 'success'
    case 'PLAYING':
      return 'danger'
    case 'SCHEDULED':
      return ''
    case 'REGISTRATION_CLOSED':
      return 'warning'
    case 'FINISHED':
      return 'info'
    case 'NOT_STARTED':
    default:
      return 'info'
  }
}

export function v4DivisionStatusTagType(status?: string | null): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return 'warning'
    case 'REGISTRATION_OPEN':
      return 'success'
    case 'PLAYING':
      return 'danger'
    case 'SCHEDULED':
      return ''
    case 'REGISTRATION_CLOSED':
      return 'warning'
    case 'FINISHED':
      return 'info'
    default:
      return 'info'
  }
}

export function v4RegistrationModeTagType(mode?: string | null): '' | 'success' | 'warning' | 'info' | 'danger' {
  return (mode || '').toUpperCase() === 'TEAM' ? '' : 'info'
}

export function v4FormatTypeTagType(_type?: string | null): '' | 'success' | 'warning' | 'info' | 'danger' {
  return 'info'
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

export const V4_PUBLISH_STATUS_FILTER_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' }
] as const
