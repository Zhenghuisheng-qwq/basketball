import { computed, ref } from 'vue'
import type { CalendarDay, CalendarMonth } from '../types/events'

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

export function formatDateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function isValidDateKey(value: string | null | undefined): boolean {
  if (!value) return false
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return false
  const date = parseDateKey(value)
  return formatDateKey(date) === value
}

export function toDateKey(value: string | null | undefined): string {
  if (!value) return ''
  const normalized = value.replace('T', ' ').trim()
  const datePart = normalized.split(/\s+/)[0]
  if (isValidDateKey(datePart)) return datePart
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return ''
  return formatDateKey(parsed)
}

export function parseDateKey(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function buildMonth(year: number, month: number, selectedDate: string): CalendarMonth {
  const firstDay = new Date(year, month, 1)
  const startWeekday = firstDay.getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
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
    const date = `${year}-${pad(month + 1)}-${pad(day)}`
    const weekday = new Date(year, month, day).getDay()
    days.push({
      date,
      day,
      isCurrentMonth: true,
      isWeekend: weekday === 0 || weekday === 6,
      isSelected: date === selectedDate,
    })
  }

  return {
    year,
    month,
    label: `${year}年${month + 1}月`,
    days,
  }
}

export function useCalendar(initialDate: string) {
  const anchorDate = ref(parseDateKey(initialDate))

  function setAnchorFromDate(dateKey: string) {
    anchorDate.value = parseDateKey(dateKey)
  }

  function shiftMonths(offset: number) {
    const next = new Date(anchorDate.value)
    next.setMonth(next.getMonth() + offset)
    anchorDate.value = next
  }

  function getThreeMonths(selectedDate: string): CalendarMonth[] {
    const start = new Date(anchorDate.value.getFullYear(), anchorDate.value.getMonth(), 1)
    return [0, 1, 2].map((offset) => {
      const monthDate = new Date(start.getFullYear(), start.getMonth() + offset, 1)
      return buildMonth(monthDate.getFullYear(), monthDate.getMonth(), selectedDate)
    })
  }

  const monthLabel = computed(() => {
    const months = getThreeMonths(formatDateKey(anchorDate.value))
    return `${months[0]?.label} - ${months[2]?.label}`
  })

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
      label: `${['周日', '周一', '周二', '周三', '周四', '周五', '周六'][index]} ${current.getMonth() + 1}/${pad(current.getDate())}`,
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
