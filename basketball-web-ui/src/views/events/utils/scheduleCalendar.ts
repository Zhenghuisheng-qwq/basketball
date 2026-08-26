import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import {
  formatDateKey,
  isValidDateKey,
  parseDateKey,
  toDateKey,
  todayDateKey,
} from '@/utils/scheduleDateKey'

const WEEKDAY_SHORT = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const WEEKDAY_FULL = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

export type CalendarDay = {
  date: string
  weekday: string
  dateText: string
  count: number
}

export type CalendarMonthDay = {
  date: string
  day: number
  inMonth: boolean
  weekend: boolean
  today: boolean
  selected: boolean
  hasMatch: boolean
}

export type CalendarMonthView = {
  year: number
  month: number
  label: string
  center: boolean
  days: CalendarMonthDay[]
}

export function scheduleItemDateKey(item: { scheduleDate?: string | null; date?: string | null }): string {
  return toDateKey(item.scheduleDate ?? item.date)
}

export function startOfSundayWeek(dateKey: string): string {
  const base = isValidDateKey(dateKey) ? parseDateKey(dateKey) : parseDateKey(todayDateKey())
  const sunday = new Date(base.getFullYear(), base.getMonth(), base.getDate() - base.getDay())
  return formatDateKey(sunday)
}

export function startOfMondayWeek(dateKey: string): string {
  return startOfSundayWeek(dateKey)
}

export function shiftSundayWeek(weekStart: string, direction: -1 | 1): string {
  const sunday = parseDateKey(startOfSundayWeek(weekStart))
  sunday.setDate(sunday.getDate() + direction * 7)
  return formatDateKey(sunday)
}

export function shiftMondayWeek(weekStart: string, direction: -1 | 1): string {
  return shiftSundayWeek(weekStart, direction)
}

function monthDayText(date: Date): string {
  return String(date.getMonth() + 1) + '/' + String(date.getDate())
}

export function sundayWeekDays(weekStart: string, counts: Record<string, number> = {}): CalendarDay[] {
  const sunday = parseDateKey(startOfSundayWeek(weekStart))
  return Array.from({ length: 7 }, (_, index) => {
    const current = new Date(sunday.getFullYear(), sunday.getMonth(), sunday.getDate() + index)
    const date = formatDateKey(current)
    return {
      date,
      weekday: WEEKDAY_SHORT[index],
      dateText: monthDayText(current),
      count: counts[date] || 0,
    }
  })
}

export function mondayWeekDays(weekStart: string, counts: Record<string, number> = {}): CalendarDay[] {
  return sundayWeekDays(weekStart, counts)
}

export function weekNumberOf(dateKey: string, seasonStart?: string): number {
  const weekStart = parseDateKey(startOfSundayWeek(isValidDateKey(dateKey) ? dateKey : todayDateKey()))
  const originKey = isValidDateKey(seasonStart || '') ? startOfSundayWeek(seasonStart as string) : ''
  if (originKey) {
    const origin = parseDateKey(originKey)
    return Math.max(1, Math.floor((weekStart.getTime() - origin.getTime()) / 86400000 / 7) + 1)
  }
  const yearStart = new Date(weekStart.getFullYear(), 0, 1)
  const firstSunday = new Date(yearStart.getFullYear(), yearStart.getMonth(), yearStart.getDate() - yearStart.getDay())
  return Math.floor((weekStart.getTime() - firstSunday.getTime()) / 86400000 / 7) + 1
}

export function startOfMonthKey(dateKey: string): string {
  const date = parseDateKey(isValidDateKey(dateKey) ? dateKey : todayDateKey())
  return formatDateKey(new Date(date.getFullYear(), date.getMonth(), 1))
}

export function shiftMonthKey(monthKey: string, offset: number): string {
  const date = parseDateKey(startOfMonthKey(monthKey))
  return formatDateKey(new Date(date.getFullYear(), date.getMonth() + offset, 1))
}

export function buildMonthView(
  year: number,
  month: number,
  selectedDate: string,
  counts: Record<string, number>,
  center: boolean,
): CalendarMonthView {
  const first = new Date(year, month, 1)
  const startWeekday = first.getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const today = todayDateKey()
  const days: CalendarMonthDay[] = []

  for (let i = 0; i < startWeekday; i += 1) {
    days.push({
      date: '',
      day: 0,
      inMonth: false,
      weekend: false,
      today: false,
      selected: false,
      hasMatch: false,
    })
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const current = new Date(year, month, day)
    const date = formatDateKey(current)
    const weekday = current.getDay()
    days.push({
      date,
      day,
      inMonth: true,
      weekend: weekday === 0 || weekday === 6,
      today: date === today,
      selected: Boolean(selectedDate) && date === selectedDate,
      hasMatch: (counts[date] || 0) > 0,
    })
  }

  return {
    year: first.getFullYear(),
    month: first.getMonth(),
    label: String(first.getFullYear()) + '年' + String(first.getMonth() + 1) + '月',
    center,
    days,
  }
}

export function buildThreeMonths(
  centerDateKey: string,
  selectedDate: string,
  counts: Record<string, number> = {},
): CalendarMonthView[] {
  const center = parseDateKey(isValidDateKey(centerDateKey) ? centerDateKey : todayDateKey())
  return [-1, 0, 1].map((offset) =>
    buildMonthView(center.getFullYear(), center.getMonth() + offset, selectedDate, counts, offset === 0),
  )
}

export function formatDateGroupTitle(dateKey: string): string {
  if (!isValidDateKey(dateKey)) return '待定日期'
  const date = parseDateKey(dateKey)
  const weekday = WEEKDAY_FULL[date.getDay()]
  return String(date.getMonth() + 1) + '月' + String(date.getDate()) + '日 · ' + weekday
}

export function formatDateHint(dateKey: string): string {
  if (!isValidDateKey(dateKey)) return '全部日期'
  const date = parseDateKey(dateKey)
  return String(date.getMonth() + 1) + '月' + String(date.getDate()) + '日'
}

export function countScheduleByDate(items: V4PublicScheduleItem[]): Record<string, number> {
  const counts: Record<string, number> = {}
  for (const item of items) {
    const key = scheduleItemDateKey(item)
    if (!key) continue
    counts[key] = (counts[key] || 0) + 1
  }
  return counts
}

function timeRank(startTime?: string | null): number {
  const match = String(startTime || '').trim().match(/^(\d{1,2}):(\d{2})/)
  if (!match) return 24 * 60
  return Number(match[1]) * 60 + Number(match[2])
}

export function sortScheduleItems(items: V4PublicScheduleItem[]): V4PublicScheduleItem[] {
  return [...items].sort((left, right) => {
    const leftDate = scheduleItemDateKey(left)
    const rightDate = scheduleItemDateKey(right)
    if (leftDate && rightDate && leftDate !== rightDate) return leftDate.localeCompare(rightDate)
    if (leftDate && !rightDate) return -1
    if (!leftDate && rightDate) return 1
    const timeDiff = timeRank(left.startTime) - timeRank(right.startTime)
    if (timeDiff !== 0) return timeDiff
    return String(left.id || '').localeCompare(String(right.id || ''), undefined, { numeric: true })
  })
}

export function groupScheduleByDate(items: V4PublicScheduleItem[]): Array<{ date: string; title: string; items: V4PublicScheduleItem[] }> {
  const groups = new Map<string, V4PublicScheduleItem[]>()
  for (const item of sortScheduleItems(items)) {
    const key = scheduleItemDateKey(item) || 'UNSCHEDULED'
    const list = groups.get(key) ?? []
    list.push(item)
    groups.set(key, list)
  }
  return Array.from(groups.entries()).map(([date, rows]) => ({
    date,
    title: date === 'UNSCHEDULED' ? '待定日期' : formatDateGroupTitle(date),
    items: rows,
  }))
}

export function resolveInitWeekStart(items: V4PublicScheduleItem[], seasonStatus?: string | null): string {
  const dates = items.map((item) => scheduleItemDateKey(item)).filter((item) => isValidDateKey(item)).sort()
  if (!dates.length) return startOfSundayWeek(todayDateKey())
  const status = String(seasonStatus || '').toUpperCase()
  if (['FINISHED', 'COMPLETED', 'CLOSED', 'ARCHIVED'].includes(status)) {
    return startOfSundayWeek(dates[0])
  }
  if (['NOT_STARTED', 'DRAFT', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'PENDING', 'REGISTRATION'].includes(status)) {
    return startOfSundayWeek(dates[0])
  }
  const today = todayDateKey()
  const todayWeek = startOfSundayWeek(today)
  if (dates.some((date) => startOfSundayWeek(date) === todayWeek)) return todayWeek
  const nearest = dates.reduce((best, date) => {
    const bestDiff = Math.abs(parseDateKey(best).getTime() - parseDateKey(today).getTime())
    const nextDiff = Math.abs(parseDateKey(date).getTime() - parseDateKey(today).getTime())
    return nextDiff < bestDiff ? date : best
  }, dates[0])
  return startOfSundayWeek(nearest)
}
