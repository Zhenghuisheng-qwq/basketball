/** CNPA 赛事业务时区。展示层一律换算到此时区，不跟随浏览器本地时区。 */
export const BUSINESS_TIME_ZONE = 'Asia/Shanghai'

export type BusinessDateTime = {
  date: string
  time: string
  week: number
}

const OFFSET_TAIL = /(?:Z|[+-]\d{2}:?\d{2})$/i

function calendarWeekday(year: number, month: number, day: number): number {
  return new Date(Date.UTC(year, month - 1, day)).getUTCDay()
}

function padClock(hour: string, minute: string): string {
  const normalizedHour = hour === '24' ? '00' : hour
  return `${normalizedHour.padStart(2, '0')}:${minute.padStart(2, '0')}`
}

function fromZonedInstant(value: string): BusinessDateTime | null {
  const dt = new Date(value)
  if (Number.isNaN(dt.getTime())) return null
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-GB', {
      timeZone: BUSINESS_TIME_ZONE,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(dt).map((part) => [part.type, part.value]),
  )
  const year = Number(parts.year)
  const month = Number(parts.month)
  const day = Number(parts.day)
  if (!year || !month || !day || !parts.hour || !parts.minute) return null
  return {
    date: `${parts.year}-${parts.month}-${parts.day}`,
    time: padClock(parts.hour, parts.minute),
    week: calendarWeekday(year, month, day),
  }
}

function fromBusinessWallClock(value: string): BusinessDateTime | null {
  const normalized = value.replace('T', ' ').trim()
  const [datePart = '', timePart = ''] = normalized.split(/\s+/)
  const dateMatch = datePart.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!dateMatch) return null
  const clock = timePart.match(/^(\d{1,2}):(\d{2})/)
  const year = Number(dateMatch[1])
  const month = Number(dateMatch[2])
  const day = Number(dateMatch[3])
  return {
    date: `${dateMatch[1]}-${dateMatch[2]}-${dateMatch[3]}`,
    time: clock ? padClock(clock[1], clock[2]) : '--',
    week: calendarWeekday(year, month, day),
  }
}

/**
 * 带偏移的时间按 Asia/Shanghai 换算。
 * 没有偏移的日期时间已经是业务墙钟，原样读取，不再加 8 小时。
 */
export function businessDateTime(value?: string | null): BusinessDateTime | null {
  const raw = String(value || '').trim()
  if (!raw) return null
  if (OFFSET_TAIL.test(raw)) return fromZonedInstant(raw)
  return fromBusinessWallClock(raw)
}

/** 系列赛开赛钟面。只接受 HH:mm，避免把整段 ISO 截成错误小时。 */
export function businessClock(value?: string | null): string {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (OFFSET_TAIL.test(raw)) return businessDateTime(raw)?.time || ''
  const clock = raw.match(/^(\d{1,2}):(\d{2})/)
  return clock ? padClock(clock[1], clock[2]) : ''
}

/** 业务墙钟标签，例如 10月20日 21:00。日期和钟面都按赛事时区，不跟随浏览器时区。 */
export function businessKickoffLabel(date?: string | null, time?: string | null): string {
  const clock = businessClock(time)
  const parsed = businessDateTime(date)
  if (parsed?.date && clock) {
    const [, month, day] = parsed.date.split('-')
    return `${Number(month)}月${Number(day)}日 ${clock}`
  }
  if (clock) return clock
  if (!parsed?.date) return ''
  const [, month, day] = parsed.date.split('-')
  return `${Number(month)}月${Number(day)}日`
}
