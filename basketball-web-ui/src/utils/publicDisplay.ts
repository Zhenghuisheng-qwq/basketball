/** 官网公开数据展示辅助：空值、数字、比赛状态等 */

export function displayText(value: unknown, fallback = '--'): string {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  return text === '' ? fallback : text
}

export function displayNumber(value: unknown, fallback = '0'): string {
  if (value === null || value === undefined || value === '') return fallback
  const num = Number(value)
  if (Number.isNaN(num)) return fallback
  return String(num)
}

export function toNumber(value: unknown, fallback = 0): number {
  if (value === null || value === undefined || value === '') return fallback
  const num = Number(value)
  return Number.isNaN(num) ? fallback : num
}

export function formatPercent(value: unknown, digits = 1): string {
  if (value === null || value === undefined || value === '') return '0%'
  const num = Number(value)
  if (Number.isNaN(num)) return '0%'
  // 后端可能返回 0-1 或 0-100
  const pct = num <= 1 ? num * 100 : num
  return `${pct.toFixed(digits)}%`
}

export function formatDecimal(value: unknown, digits = 1): string {
  if (value === null || value === undefined || value === '') return '0'
  const num = Number(value)
  if (Number.isNaN(num)) return '0'
  return Number.isInteger(num) ? String(num) : num.toFixed(digits)
}

export function formatStatValue(value: unknown, unit?: string): string {
  const text = formatDecimal(value, 1)
  return unit ? `${text} ${unit}` : text
}

/** 比赛状态：V4 字符串优先；兼容旧整型 0草稿 1已编排 2进行中 3已结束 4已延期 5已取消 */
export type UiMatchStatus = 'scheduled' | 'live' | 'finished' | 'cancelled' | 'postponed'

export function mapMatchStatus(status: number | string | null | undefined): UiMatchStatus {
  if (typeof status === 'string') {
    switch (status.trim().toUpperCase()) {
      case 'PLAYING':
      case 'LIVE':
        return 'live'
      case 'FINISHED':
      case 'FORFEITED':
        return 'finished'
      case 'CANCELLED':
        return 'cancelled'
      case 'POSTPONED':
        return 'postponed'
      default:
        return 'scheduled'
    }
  }
  switch (status) {
    case 2:
      return 'live'
    case 3:
      return 'finished'
    case 4:
      return 'postponed'
    case 5:
      return 'cancelled'
    default:
      return 'scheduled'
  }
}

export function matchStatusLabel(status: number | string | null | undefined): string {
  switch (mapMatchStatus(status)) {
    case 'live':
      return '进行中'
    case 'finished':
      return '已结束'
    case 'postponed':
      return '已延期'
    case 'cancelled':
      return '已取消'
    default:
      return '未开始'
  }
}

export function formatDateTime(value: string | null | undefined): { date: string; time: string; full: string } {
  if (!value) return { date: '--', time: '--', full: '--' }
  const normalized = value.replace('T', ' ').trim()
  const [datePart = '', timePart = ''] = normalized.split(/\s+/)
  const time = timePart.slice(0, 5) || '--'
  if (!datePart) return { date: '--', time, full: displayText(value) }
  const [, m, d] = datePart.split('-')
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dt = new Date(datePart.replace(/-/g, '/'))
  const week = Number.isNaN(dt.getTime()) ? '' : weekDays[dt.getDay()]
  const shortDate = m && d ? `${m}.${d}` : datePart
  const full = week ? `${shortDate} ${week} ${time}` : `${shortDate} ${time}`
  return { date: datePart, time, full }
}

export function descriptionToText(value: unknown): string {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return ''
    }
  }
  return String(value)
}

export function initials(name: string | null | undefined, size = 2): string {
  const text = (name || '').replace(/\s+/g, '')
  if (!text) return '--'
  return text.slice(0, size).toUpperCase()
}

export function formatStreak(streakType?: string | null, streakCount?: number | null): string {
  const count = toNumber(streakCount, 0)
  if (!count) return '--'
  const type = (streakType || '').toUpperCase()
  if (type === 'W' || type === 'WIN') return `${count}连胜`
  if (type === 'L' || type === 'LOSS') return `${count}连败`
  return `${count}`
}
