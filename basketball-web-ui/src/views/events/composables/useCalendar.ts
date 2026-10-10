import { computed, ref } from 'vue'
import type { CalendarDay, CalendarMonth } from '../types/events'
import { translate } from '@/i18n'
import {
  calendarRangeLabel,
  formatDateKey,
  isValidDateKey,
  parseDateKey,
  resolveCalendarAnchor,
  toDateKey,
} from '@/utils/scheduleDateKey'

export { formatDateKey, isValidDateKey, parseDateKey, toDateKey }

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function buildMonth(year: number, month: number, selectedDate: string): CalendarMonth {
  const safeYear = Number.isFinite(year) ? year : parseDateKey('').getFullYear()
  const safeMonth = Number.isFinite(month) ? month : parseDateKey('').getMonth()
  const firstDay = new Date(safeYear, safeMonth, 1)
  const startWeekday = firstDay.getDay()
  const daysInMonth = new Date(safeYear, safeMonth + 1, 0).getDate()
  const days: CalendarDay[] = []

  for (let i = 0; i < startWeekday; i += 1) {
    days.push({
      date: '',
      day: 0,
      isCurrentMonth: false,
      isWeekend: false,
      isSelected: false,
    })
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = `${safeYear}-${pad(safeMonth + 1)}-${pad(day)}`
    const weekday = new Date(safeYear, safeMonth, day).getDay()
    days.push({
      date,
      day,
      isCurrentMonth: true,
      isWeekend: weekday === 0 || weekday === 6,
      isSelected: date === selectedDate,
    })
  }

  return {
    year: safeYear,
    month: safeMonth,
    label: translate('eventsPage.date.yearMonth', { year: safeYear, month: safeMonth + 1 }),
    days,
  }
}

export function useCalendar(initialDate: string) {
  const anchorDate = ref(resolveCalendarAnchor(initialDate))

  function setAnchorFromDate(dateKey: string) {
    if (!isValidDateKey(dateKey)) return
    anchorDate.value = parseDateKey(dateKey)
  }

  function shiftMonths(offset: number) {
    const base = Number.isNaN(anchorDate.value.getTime()) ? parseDateKey('') : anchorDate.value
    const next = new Date(base.getFullYear(), base.getMonth() + offset, 1)
    anchorDate.value = next
  }

  function getThreeMonths(selectedDate: string): CalendarMonth[] {
    const base = Number.isNaN(anchorDate.value.getTime()) ? parseDateKey('') : anchorDate.value
    const start = new Date(base.getFullYear(), base.getMonth(), 1)
    const highlight = isValidDateKey(selectedDate) ? selectedDate : ''
    return [0, 1, 2].map((offset) => {
      const monthDate = new Date(start.getFullYear(), start.getMonth() + offset, 1)
      return buildMonth(monthDate.getFullYear(), monthDate.getMonth(), highlight)
    })
  }

  const monthLabel = computed(() => calendarRangeLabel(anchorDate.value))

  return {
    anchorDate,
    monthLabel,
    setAnchorFromDate,
    shiftMonths,
    getThreeMonths,
    formatDateKey,
    parseDateKey,
  }
}

export function getWeekDates(baseDate: string, weekNumber: number) {
  const date = parseDateKey(baseDate)
  const weekday = date.getDay()
  const sunday = new Date(date)
  sunday.setDate(date.getDate() - weekday)

  const weekDays = Array.from({ length: 7 }, (_, index) => {
    const current = new Date(sunday)
    current.setDate(sunday.getDate() + index)
    return {
      date: formatDateKey(current),
      weekday: index,
      label: `${translate(`match.weekday.${index}`)} ${current.getMonth() + 1}/${pad(current.getDate())}`,
    }
  })

  return { weekNumber, weekDays }
}

export function shiftWeek(baseDate: string, direction: -1 | 1): string {
  const date = parseDateKey(baseDate)
  date.setDate(date.getDate() + direction * 7)
  return formatDateKey(date)
}

export function getWeekNumber(dateKey: string): number {
  const date = parseDateKey(dateKey)
  const start = new Date(date.getFullYear(), 0, 1)
  const diff = date.getTime() - start.getTime()
  return Math.ceil((diff / 86400000 + start.getDay() + 1) / 7)
}
