import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import { isValidDateKey, todayDateKey } from '@/utils/scheduleDateKey'
import { groupScheduleByDate, scheduleItemDateKey } from './scheduleCalendar'

export const DEFAULT_VISIBLE_DATE_GROUPS = 3

export type ScheduleDateGroup = {
  date: string
  title: string
  items: V4PublicScheduleItem[]
}

export function uniqueSortedScheduleDates(items: Array<{ scheduleDate?: string | null; date?: string | null }>): string[] {
  return [...new Set(items.map((item) => scheduleItemDateKey(item)).filter((date) => isValidDateKey(date)))].sort()
}

export function resolveInitSelectedDate(
  items: Array<{ scheduleDate?: string | null; date?: string | null }>,
  seasonStatus?: string | null,
  today: string = todayDateKey(),
): string {
  const dates = uniqueSortedScheduleDates(items)
  if (!dates.length) return ''
  const status = String(seasonStatus || '').toUpperCase()
  if (['FINISHED', 'COMPLETED', 'CLOSED', 'ARCHIVED'].includes(status)) {
    return dates[dates.length - 1]
  }
  if (['NOT_STARTED', 'DRAFT', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'PENDING', 'REGISTRATION'].includes(status)) {
    return dates[0]
  }
  if (dates.includes(today)) return today
  const upcoming = dates.find((date) => date > today)
  if (upcoming) return upcoming
  return dates[dates.length - 1]
}

export function sliceDateGroups<T>(groups: T[], visibleCount: number): T[] {
  return groups.slice(0, Math.max(0, visibleCount))
}

export function remainingDateGroupCount(totalGroups: number, visibleCount: number): number {
  return Math.max(0, totalGroups - Math.max(0, visibleCount))
}

export function canLoadMoreDateGroups(totalGroups: number, visibleCount: number): boolean {
  return remainingDateGroupCount(totalGroups, visibleCount) > 0
}

export function nextVisibleDateGroupCount(
  visibleCount: number,
  totalGroups: number,
  step = DEFAULT_VISIBLE_DATE_GROUPS,
): number {
  return Math.min(Math.max(0, totalGroups), Math.max(0, visibleCount) + Math.max(1, step))
}

export function dateGroupsIntact(
  allGroups: ScheduleDateGroup[],
  visibleGroups: ScheduleDateGroup[],
): boolean {
  return visibleGroups.every((group) => {
    const source = allGroups.find((item) => item.date === group.date)
    return Boolean(source) && source!.items.length === group.items.length
  })
}

export function browseScheduleDateGroups(
  items: V4PublicScheduleItem[],
  selectedDate: string,
  visibleDateGroupCount: number,
): {
  allGroups: ScheduleDateGroup[]
  visibleGroups: ScheduleDateGroup[]
  remainingGroups: number
  canLoadMore: boolean
} {
  const allGroups = groupScheduleByDate(items)
  if (isValidDateKey(selectedDate)) {
    return {
      allGroups,
      visibleGroups: allGroups,
      remainingGroups: 0,
      canLoadMore: false,
    }
  }
  const visibleGroups = sliceDateGroups(allGroups, visibleDateGroupCount)
  const remainingGroups = remainingDateGroupCount(allGroups.length, visibleDateGroupCount)
  return {
    allGroups,
    visibleGroups,
    remainingGroups,
    canLoadMore: remainingGroups > 0,
  }
}
