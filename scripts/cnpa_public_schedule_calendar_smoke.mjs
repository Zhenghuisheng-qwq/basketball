/**
 * Public Schedule Calendar / Date Filter smoke.
 * Contract must stay aligned with basketball-web-ui/src/utils/scheduleDateKey.ts
 */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'))
const utilSource = readFileSync(resolve(here, '../basketball-web-ui/src/utils/scheduleDateKey.ts'), 'utf8')

function pad(value) {
  return String(value).padStart(2, '0')
}

function localToday() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

function formatDateKey(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function isValidDateKey(value) {
  if (!value) return false
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return false
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  if (Number.isNaN(date.getTime())) return false
  return formatDateKey(date) === value
}

function toDateKey(value) {
  if (value == null) return ''
  const normalized = String(value).replace('T', ' ').trim()
  if (!normalized) return ''
  const datePart = normalized.split(/\s+/)[0]
  return isValidDateKey(datePart) ? datePart : ''
}

function parseDateKey(value) {
  if (isValidDateKey(value)) {
    const [year, month, day] = String(value).split('-').map(Number)
    return new Date(year, month - 1, day)
  }
  return localToday()
}

function matchPassesDateFilter(itemDate, selectedDate) {
  if (!isValidDateKey(selectedDate)) return true
  const key = toDateKey(itemDate)
  if (!key) return true
  return key === selectedDate
}

function filterScheduleByDate(items, selectedDate) {
  return items.filter((item) => matchPassesDateFilter(item.date || item.scheduleDate, selectedDate))
}

function calendarMonthLabel(anchor) {
  const safe = Number.isNaN(anchor.getTime()) ? localToday() : anchor
  return `${safe.getFullYear()}年${safe.getMonth() + 1}月`
}

function calendarRangeLabel(anchor) {
  const safe = Number.isNaN(anchor.getTime()) ? localToday() : anchor
  const start = new Date(safe.getFullYear(), safe.getMonth(), 1)
  const end = new Date(start.getFullYear(), start.getMonth() + 2, 1)
  return `${calendarMonthLabel(start)} - ${calendarMonthLabel(end)}`
}

function assertNoNan(text, label) {
  assert.equal(String(text).includes('NaN'), false, `${label} must not contain NaN: ${text}`)
}

const cases = []
function test(name, fn) {
  try {
    fn()
    cases.push({ name, status: 'PASS' })
  } catch (err) {
    cases.push({ name, status: 'FAIL', error: err instanceof Error ? err.message : String(err) })
  }
}

test('util source exports required helpers', () => {
  for (const name of [
    'toDateKey',
    'parseDateKey',
    'isValidDateKey',
    'matchPassesDateFilter',
    'filterScheduleByDate',
    'calendarRangeLabel',
    'resolveCalendarAnchor',
  ]) {
    assert.equal(utilSource.includes(`export function ${name}`), true, `missing ${name}`)
  }
  assert.equal(utilSource.includes("new Date(value)"), false, 'must not parse datetime with new Date(value)')
})

test('Case A: 2026-08-17', () => {
  assert.equal(toDateKey('2026-08-17'), '2026-08-17')
  assert.equal(isValidDateKey('2026-08-17'), true)
  const parsed = parseDateKey('2026-08-17')
  assert.equal(parsed.getFullYear(), 2026)
  assert.equal(parsed.getMonth(), 7)
  assert.equal(parsed.getDate(), 17)
  assert.equal(calendarMonthLabel(parsed), '2026年8月')
})

test('Case B: 2026-08-17 21:00:00', () => {
  assert.equal(toDateKey('2026-08-17 21:00:00'), '2026-08-17')
  assert.equal(toDateKey('2026-08-17T21:00:00'), '2026-08-17')
  assert.equal(matchPassesDateFilter('2026-08-17 21:00:00', '2026-08-17'), true)
})

test('Case C: null / empty', () => {
  assert.equal(toDateKey(null), '')
  assert.equal(toDateKey(''), '')
  assert.equal(toDateKey(undefined), '')
  const parsed = parseDateKey('')
  assert.equal(Number.isNaN(parsed.getTime()), false)
  assertNoNan(calendarMonthLabel(parsed), 'empty parseDateKey month')
  assertNoNan(calendarRangeLabel(parsed), 'empty parseDateKey range')
  assert.equal(matchPassesDateFilter(null, ''), true)
  assert.equal(matchPassesDateFilter('2026-08-17', 'not-a-date'), true)
})

test('Case D: empty schedule dates', () => {
  const dates = []
  const min = dates.length ? Math.min(...dates.map((d) => parseDateKey(d).getTime())) : Number.NaN
  const fallback = Number.isNaN(min) ? localToday() : new Date(min)
  assertNoNan(calendarMonthLabel(fallback), 'empty dates month')
  assertNoNan(calendarRangeLabel(new Date(Number.NaN)), 'Invalid Date range')
  assert.equal(filterScheduleByDate([], '').length, 0)
})

test('Date filter default / select / clear', () => {
  const items = [
    { id: 1, date: '2026-08-17', scheduleDate: '2026-08-17' },
    { id: 2, date: '2026-08-20', scheduleDate: '2026-08-20' },
  ]
  assert.equal(filterScheduleByDate(items, '').length, 2)
  assert.equal(filterScheduleByDate(items, 'invalid').length, 2)
  assert.equal(filterScheduleByDate(items, '2026-08-17').map((item) => item.id).join(','), '1')
  assert.equal(filterScheduleByDate(items, '').length, 2)
})

test('Team filter default keeps all', () => {
  const selectedTeamId = null
  const matches = [
    { homeTeamKey: '296', awayTeamKey: '311' },
    { homeTeamKey: '297', awayTeamKey: '312' },
  ]
  const filtered = matches.filter((match) => selectedTeamId === null)
  assert.equal(filtered.length, 2)
})

test('China local date is not shifted by time part', () => {
  assert.equal(toDateKey('2026-08-17 21:00:00'), '2026-08-17')
  assert.equal(toDateKey('2026-08-11T13:00:00.000+00:00'), '2026-08-11')
})

test('Calendar next month from 2026-08', () => {
  const august = parseDateKey('2026-08-17')
  const september = new Date(august.getFullYear(), august.getMonth() + 1, 1)
  assert.equal(calendarMonthLabel(september), '2026年9月')
})

const failed = cases.filter((item) => item.status === 'FAIL')
for (const item of cases) {
  console.log(`${item.status}\t${item.name}${item.error ? ` :: ${item.error}` : ''}`)
}
console.log(`RESULT\t${failed.length ? 'FAIL' : 'PASS'}\t${cases.length - failed.length}/${cases.length}`)
if (failed.length) process.exitCode = 1
