function pad(value: number): string {
  return String(value).padStart(2, '0')
}

export function localToday(): Date {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

export function todayDateKey(): string {
  return formatDateKey(localToday())
}

export function formatDateKey(date: Date): string {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return ''
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function isValidDateKey(value: string | null | undefined): boolean {
  if (!value) return false
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return false
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) {
    return false
  }
  const date = new Date(year, month - 1, day)
  if (Number.isNaN(date.getTime())) return false
  return formatDateKey(date) === value
}

/**
 * Take the local calendar day YYYY-MM-DD only.
 * Do not parse datetimes with new Date(datetime string).
 */
export function toDateKey(value: string | null | undefined): string {
  if (value == null) return ''
  const normalized = String(value).replace('T', ' ').trim()
  if (!normalized) return ''
  const datePart = normalized.split(/\s+/)[0]
  return isValidDateKey(datePart) ? datePart : ''
}

export function parseDateKey(value: string | null | undefined): Date {
  if (isValidDateKey(value)) {
    const [year, month, day] = String(value).split('-').map(Number)
    return new Date(year, month - 1, day)
  }
  return localToday()
}

export function matchPassesDateFilter(
  itemDate: string | null | undefined,
  selectedDate: string | null | undefined,
): boolean {
  if (!isValidDateKey(selectedDate)) return true
  const key = toDateKey(itemDate)
  if (!key) return true
  return key === selectedDate
}

export function filterScheduleByDate<T extends { date?: string | null; scheduleDate?: string | null }>(
  items: T[],
  selectedDate: string | null | undefined,
): T[] {
  return items.filter((item) => matchPassesDateFilter(item.date || item.scheduleDate, selectedDate))
}

export function resolveCalendarAnchor(
  selectedDate?: string | null,
  fallbackDates: Array<string | null | undefined> = [],
): Date {
  if (isValidDateKey(selectedDate)) {
    return parseDateKey(selectedDate)
  }
  for (const candidate of fallbackDates) {
    const key = toDateKey(candidate)
    if (isValidDateKey(key)) {
      return parseDateKey(key)
    }
  }
  return localToday()
}

export function calendarMonthLabel(anchor: Date): string {
  const safe = Number.isNaN(anchor.getTime()) ? localToday() : anchor
  return `${safe.getFullYear()}年${safe.getMonth() + 1}月`
}

export function calendarRangeLabel(anchor: Date): string {
  const safe = Number.isNaN(anchor.getTime()) ? localToday() : anchor
  const start = new Date(safe.getFullYear(), safe.getMonth(), 1)
  const end = new Date(start.getFullYear(), start.getMonth() + 2, 1)
  return `${calendarMonthLabel(start)} - ${calendarMonthLabel(end)}`
}
