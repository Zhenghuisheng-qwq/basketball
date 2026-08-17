/**
 * Public Schedule Team Filter + Pagination smoke.
 * Contract must stay aligned with:
 *   basketball-web-ui/src/views/events/utils/scheduleTeamFilter.ts
 *   basketball-web-ui/src/views/events/composables/useScheduleFilters.ts
 */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'))
const filterSource = readFileSync(
  resolve(here, '../basketball-web-ui/src/views/events/utils/scheduleTeamFilter.ts'),
  'utf8',
)
const composableSource = readFileSync(
  resolve(here, '../basketball-web-ui/src/views/events/composables/useScheduleFilters.ts'),
  'utf8',
)
const scheduleViewSource = readFileSync(
  resolve(here, '../basketball-web-ui/src/views/events/components/LeagueSchedule.vue'),
  'utf8',
)
const apiSource = readFileSync(
  resolve(here, '../basketball-web-ui/src/api/v4Public.ts'),
  'utf8',
)

const DEFAULT_SCHEDULE_PAGE_SIZE = 10
const PLACEHOLDER_IDS = new Set(['', 'TBD', 'BYE', 'NULL', 'ALL', 'UNDEFINED'])
const PLACEHOLDER_NAMES = new Set(['', 'TBD', 'BYE', '待定', '--', '轮空', '上一轮胜者', '小组名次待定'])

function isValidDateKey(value) {
  if (!value) return false
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return false
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) return false
  const date = new Date(year, month - 1, day)
  if (Number.isNaN(date.getTime())) return false
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` === value
}

function toDateKey(value) {
  if (value == null) return ''
  const normalized = String(value).replace('T', ' ').trim()
  if (!normalized) return ''
  const datePart = normalized.split(/[\s/]/)[0]
  return isValidDateKey(datePart) ? datePart : ''
}

function matchPassesDateFilter(itemDate, selectedDate) {
  if (!isValidDateKey(selectedDate)) return true
  const key = toDateKey(itemDate)
  if (!key) return true
  return key === selectedDate
}

function isAllTeamsSelected(selectedTeamId) {
  return selectedTeamId == null || selectedTeamId === ''
}

function isRealEventTeamId(value) {
  const id = String(value ?? '').trim()
  if (!id) return false
  return !PLACEHOLDER_IDS.has(id.toUpperCase())
}

function isPlaceholderName(value) {
  const name = String(value ?? '').trim()
  if (!name) return true
  return PLACEHOLDER_NAMES.has(name) || PLACEHOLDER_NAMES.has(name.toUpperCase())
}

function buildScheduleTeamOptions(items) {
  const map = new Map()
  const add = (eventTeamId, name, logoUrl) => {
    const id = String(eventTeamId || '').trim()
    const displayName = String(name || '').trim()
    if (!isRealEventTeamId(id)) return
    if (isPlaceholderName(displayName)) return
    const next = { eventTeamId: id, name: displayName, shortName: displayName, logoUrl: logoUrl || undefined }
    const existing = map.get(id)
    if (existing) {
      if (!existing.logoUrl && next.logoUrl) existing.logoUrl = next.logoUrl
      if ((!existing.name || existing.name === id) && next.name) existing.name = next.name
    } else {
      map.set(id, next)
    }
  }
  for (const item of items) {
    add(item.homeEventTeamId, item.homeTeam || item.homeTeamName, item.homeTeamLogo)
    add(item.awayEventTeamId, item.awayTeam || item.awayTeamName, item.awayTeamLogo)
  }
  return Array.from(map.values())
}

function filterScheduleByDate(items, selectedDate) {
  return items.filter((item) => matchPassesDateFilter(item.scheduleDate || item.date, selectedDate))
}

function matchIncludesTeam(item, selectedTeamId) {
  if (isAllTeamsSelected(selectedTeamId)) return true
  return String(item.homeEventTeamId || '') === selectedTeamId || String(item.awayEventTeamId || '') === selectedTeamId
}

function filterScheduleByTeam(items, selectedTeamId) {
  if (isAllTeamsSelected(selectedTeamId)) return items
  return items.filter((item) => matchIncludesTeam(item, selectedTeamId))
}

function resolveSelectedTeamId(selectedTeamId, teamOptions) {
  if (isAllTeamsSelected(selectedTeamId)) return null
  return teamOptions.some((team) => team.eventTeamId === selectedTeamId) ? selectedTeamId : null
}

function schedulePageCount(total, pageSize = DEFAULT_SCHEDULE_PAGE_SIZE) {
  return Math.max(1, Math.ceil(Math.max(0, total) / Math.max(1, pageSize)))
}

function clampSchedulePage(currentPage, pageCount) {
  return Math.min(Math.max(1, currentPage || 1), Math.max(1, pageCount))
}

function paginateScheduleItems(items, currentPage, pageSize = DEFAULT_SCHEDULE_PAGE_SIZE) {
  const size = Math.max(1, pageSize)
  const page = clampSchedulePage(currentPage, schedulePageCount(items.length, size))
  const start = (page - 1) * size
  return items.slice(start, start + size)
}

function applyScheduleFilterPipeline(input) {
  const dateMatches = filterScheduleByDate(input.allPublishedMatches, input.selectedDate)
  const contextMatches = dateMatches
  const teamOptions = buildScheduleTeamOptions(contextMatches)
  const selectedTeamId = resolveSelectedTeamId(input.selectedTeamId ?? null, teamOptions)
  const visibleMatches = filterScheduleByTeam(contextMatches, selectedTeamId)
  const pageSize = input.pageSize ?? DEFAULT_SCHEDULE_PAGE_SIZE
  const total = visibleMatches.length
  const pageCount = schedulePageCount(total, pageSize)
  const currentPage = clampSchedulePage(input.currentPage ?? 1, pageCount)
  const pagedMatches = paginateScheduleItems(visibleMatches, currentPage, pageSize)
  return {
    dateMatches,
    contextMatches,
    teamOptions,
    selectedTeamId,
    visibleMatches,
    total,
    pageCount,
    currentPage,
    pageSize,
    pagedMatches,
  }
}

function match(id, home, away, date, extra = {}) {
  return {
    id,
    homeEventTeamId: home.id,
    homeTeam: home.name,
    homeTeamLogo: home.logo,
    awayEventTeamId: away.id,
    awayTeam: away.name,
    awayTeamLogo: away.logo,
    scheduleDate: date,
    ...extra,
  }
}

const A = { id: 'A', name: 'A' }
const B = { id: 'B', name: 'B' }
const C = { id: 'C', name: 'C' }
const D = { id: 'D', name: 'D' }
const E = { id: 'E', name: 'E' }
const F = { id: 'F', name: 'F' }
const TBD = { id: '', name: 'TBD' }
const BYE = { id: '', name: 'BYE' }
const SNOW = { id: '2086877274199658500', name: 'SNOW' }

const cases = []
function test(name, fn) {
  try {
    fn()
    cases.push({ name, status: 'PASS' })
  } catch (err) {
    cases.push({
      name,
      status: 'FAIL',
      error: err instanceof Error ? err.message : String(err),
    })
  }
}

test('source contract: pipeline after filters, options not from paged', () => {
  for (const name of [
    'buildScheduleTeamOptions',
    'filterScheduleByDate',
    'filterScheduleByTeam',
    'paginateScheduleItems',
    'applyScheduleFilterPipeline',
    'DEFAULT_SCHEDULE_PAGE_SIZE',
  ]) {
    assert.equal(
      filterSource.includes(name),
      true,
      `missing ${name}`,
    )
  }
  assert.equal(filterSource.includes('buildScheduleTeamOptions(paged'), false)
  assert.equal(filterSource.includes('filterScheduleByTeam(paged'), false)
  assert.equal(composableSource.includes('buildScheduleTeamOptions(source)'), true)
  assert.equal(composableSource.includes('paginateScheduleItems(visibleMatches'), true)
  assert.equal(composableSource.includes('teamPanelExpanded'), true)
  assert.equal(composableSource.includes('selectedTeamId.value'), true)
  assert.equal(/watch\(\s*teamPanelExpanded/.test(composableSource), false)
  assert.equal(scheduleViewSource.includes(':matches="pagedMatches"'), true)
  assert.equal(scheduleViewSource.includes(':matches="visibleMatches"'), false)
  assert.equal(scheduleViewSource.includes('el-pagination'), true)
  assert.equal(scheduleViewSource.includes('hide-on-single-page'), true)
  const scheduleApiStart = apiSource.indexOf('export function getV4PublicDivisionSchedule')
  const scheduleApiEnd = apiSource.indexOf('export function', scheduleApiStart + 1)
  const scheduleApi = apiSource.slice(scheduleApiStart, scheduleApiEnd)
  assert.equal(scheduleApi.includes('pageNum'), false)
  assert.equal(scheduleApi.includes('pageSize'), false)
})

test('Case 1 date options A B C D E F', () => {
  const items = [
    match(1, A, B, '2026-05-06'),
    match(2, C, D, '2026-05-06'),
    match(3, E, F, '2026-05-06'),
  ]
  const result = applyScheduleFilterPipeline({ allPublishedMatches: items, selectedDate: '2026-05-06' })
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['A', 'B', 'C', 'D', 'E', 'F'])
})

test('Case 2 dedupe A vs B, A vs C', () => {
  const items = [match(1, A, B, '2026-05-06'), match(2, A, C, '2026-05-06')]
  const result = applyScheduleFilterPipeline({ allPublishedMatches: items, selectedDate: '2026-05-06' })
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['A', 'B', 'C'])
})

test('Case 3 team filter does not shrink options', () => {
  const items = [match(1, A, B, '2026-05-06'), match(2, C, D, '2026-05-06')]
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-05-06',
    selectedTeamId: 'A',
  })
  assert.equal(result.visibleMatches.length, 1)
  assert.equal(result.visibleMatches[0].id, 1)
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['A', 'B', 'C', 'D'])
})

test('invariant: selected team in options has visible matches', () => {
  const items = [match(1, A, B, '2026-05-06'), match(2, C, D, '2026-05-06')]
  const options = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-05-06',
  }).teamOptions
  for (const team of options) {
    const result = applyScheduleFilterPipeline({
      allPublishedMatches: items,
      selectedDate: '2026-05-06',
      selectedTeamId: team.eventTeamId,
    })
    assert.equal(result.teamOptions.some((item) => item.eventTeamId === team.eventTeamId), true)
    assert.equal(result.visibleMatches.length > 0, true, `${team.eventTeamId} in options but no matches`)
  }
})

test('Case 4 date switch resets missing team', () => {
  const items = [match(1, A, B, '2026-05-06'), match(2, C, D, '2026-05-07')]
  const next = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-05-07',
    selectedTeamId: 'A',
  })
  assert.equal(next.selectedTeamId, null)
  assert.deepEqual(next.teamOptions.map((t) => t.eventTeamId), ['C', 'D'])
  assert.equal(next.visibleMatches.length, 1)
  assert.equal(next.visibleMatches[0].id, 2)
})

test('Case 5 date switch keeps existing team', () => {
  const items = [match(1, A, B, '2026-05-06'), match(2, A, C, '2026-05-07')]
  const next = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-05-07',
    selectedTeamId: 'A',
  })
  assert.equal(next.selectedTeamId, 'A')
  assert.equal(next.visibleMatches.length, 1)
  assert.equal(next.visibleMatches[0].id, 2)
})

test('Case 6 empty date uses all published teams', () => {
  const items = [match(1, A, B, '2026-05-06'), match(2, C, D, '2026-05-07')]
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '',
    selectedTeamId: 'C',
  })
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['A', 'B', 'C', 'D'])
  assert.equal(result.visibleMatches.length, 1)
  assert.equal(result.visibleMatches[0].id, 2)
})

test('Case 7 TBD excluded', () => {
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: [match(1, A, TBD, '2026-05-06')],
    selectedDate: '2026-05-06',
  })
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['A'])
})

test('Case 8 BYE excluded', () => {
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: [match(1, A, BYE, '2026-05-06')],
    selectedDate: '2026-05-06',
  })
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['A'])
})

test('Case 9 forfeit still filterable', () => {
  const items = [match(1, A, B, '2026-05-06', { status: 'FORFEITED', homeScore: null, awayScore: null })]
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-05-06',
    selectedTeamId: 'A',
  })
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['A', 'B'])
  assert.equal(result.visibleMatches.length, 1)
  const away = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-05-06',
    selectedTeamId: 'B',
  })
  assert.equal(away.visibleMatches.length, 1)
})

test('Case 10 empty date matches', () => {
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: [match(1, A, B, '2026-05-06')],
    selectedDate: '2026-05-10',
  })
  assert.equal(result.dateMatches.length, 0)
  assert.equal(result.teamOptions.length, 0)
  assert.equal(result.visibleMatches.length, 0)
  assert.equal(result.pageCount, 1)
  assert.equal(result.pagedMatches.length, 0)
})

test('Case 11 division switch cannot keep old team', () => {
  const div2 = [match(1, { id: 'X', name: 'X' }, { id: 'Y', name: 'Y' }, '2026-05-06')]
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: div2,
    selectedDate: '2026-05-06',
    selectedTeamId: 'A',
  })
  assert.equal(result.selectedTeamId, null)
  assert.deepEqual(result.teamOptions.map((t) => t.eventTeamId), ['X', 'Y'])
})

test('Case 12 snowflake id stays string', () => {
  const items = [match(1, SNOW, B, '2026-05-06')]
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-05-06',
    selectedTeamId: '2086877274199658500',
  })
  assert.equal(typeof result.teamOptions[0].eventTeamId, 'string')
  assert.equal(result.teamOptions[0].eventTeamId, '2086877274199658500')
  assert.equal(result.selectedTeamId, '2086877274199658500')
  assert.equal(result.visibleMatches.length, 1)
})

test('Pagination Case 1: 25 matches pageSize 10', () => {
  const items = Array.from({ length: 25 }, (_, index) =>
    match(index + 1, A, B, '2026-08-17'),
  )
  const page1 = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-08-17',
    currentPage: 1,
    pageSize: 10,
  })
  const page2 = applyScheduleFilterPipeline({ ...page1, allPublishedMatches: items, currentPage: 2 })
  const page3 = applyScheduleFilterPipeline({ ...page1, allPublishedMatches: items, currentPage: 3 })
  assert.equal(page1.total, 25)
  assert.equal(page1.pageCount, 3)
  assert.equal(page1.pagedMatches.length, 10)
  assert.equal(page2.pagedMatches.length, 10)
  assert.equal(page3.pagedMatches.length, 5)
  assert.deepEqual(page1.pagedMatches.map((item) => item.id), [1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
  assert.deepEqual(page3.pagedMatches.map((item) => item.id), [21, 22, 23, 24, 25])
})

test('Pagination Case 2: team filter resets to page 1', () => {
  const items = Array.from({ length: 25 }, (_, index) =>
    match(index + 1, index === 0 || index === 16 ? A : C, B, '2026-08-17'),
  )
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-08-17',
    selectedTeamId: 'A',
    currentPage: 3,
    pageSize: 10,
  })
  assert.equal(result.visibleMatches.length, 2)
  assert.equal(result.currentPage, 1)
  assert.equal(result.pagedMatches.length, 2)
})

test('Pagination Case 3: team on page 2 still in options on page 1', () => {
  const items = Array.from({ length: 20 }, (_, index) =>
    match(index + 1, index === 14 ? A : C, B, '2026-08-17'),
  )
  const page1 = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-08-17',
    currentPage: 1,
    pageSize: 10,
  })
  assert.equal(page1.pagedMatches.some((item) => item.homeEventTeamId === 'A'), false)
  assert.equal(page1.teamOptions.some((team) => team.eventTeamId === 'A'), true)
})

test('Pagination Case 4: team filter uses full dateMatches not current page', () => {
  const items = Array.from({ length: 20 }, (_, index) =>
    match(index + 1, index === 14 ? A : C, B, '2026-08-17'),
  )
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-08-17',
    selectedTeamId: 'A',
    currentPage: 1,
    pageSize: 10,
  })
  assert.equal(result.visibleMatches.length, 1)
  assert.equal(result.visibleMatches[0].id, 15)
  assert.equal(result.pagedMatches.length, 1)
})

test('Pagination Case 5: date switch resets page', () => {
  const items = [
    ...Array.from({ length: 25 }, (_, index) => match(index + 1, A, B, '2026-08-17')),
    ...Array.from({ length: 5 }, (_, index) => match(100 + index, C, D, '2026-08-18')),
  ]
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-08-18',
    currentPage: 3,
    pageSize: 10,
  })
  assert.equal(result.visibleMatches.length, 5)
  assert.equal(result.currentPage, 1)
  assert.equal(result.pageCount, 1)
})

test('Pagination Case 6: empty hides pagination', () => {
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: [match(1, A, B, '2026-08-17')],
    selectedDate: '2026-08-19',
  })
  const showPagination = result.total > 0 && result.pageCount > 1
  assert.equal(result.visibleMatches.length, 0)
  assert.equal(showPagination, false)
})

test('Pagination Case 7: clamp out-of-range page, no fake empty', () => {
  const items = Array.from({ length: 20 }, (_, index) => match(index + 1, A, B, '2026-08-17'))
  const result = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-08-17',
    currentPage: 4,
    pageSize: 10,
  })
  assert.equal(result.pageCount, 2)
  assert.equal(result.currentPage, 2)
  assert.equal(result.pagedMatches.length, 10)
  assert.equal(result.pagedMatches.length === 0, false)
})

test('clear team returns all date matches and page 1', () => {
  const items = Array.from({ length: 12 }, (_, index) =>
    match(index + 1, index < 2 ? A : C, B, '2026-08-17'),
  )
  const cleared = applyScheduleFilterPipeline({
    allPublishedMatches: items,
    selectedDate: '2026-08-17',
    selectedTeamId: null,
    currentPage: 1,
    pageSize: 10,
  })
  assert.equal(cleared.visibleMatches.length, 12)
  assert.equal(cleared.currentPage, 1)
  assert.equal(cleared.pageCount, 2)
})

test('logo merge keeps first appearance order', () => {
  const items = [
    match(1, { id: 'A', name: 'A', logo: '' }, B, '2026-05-06'),
    match(2, { id: 'A', name: 'A', logo: 'logo-a.png' }, C, '2026-05-06'),
    match(3, D, { id: 'A', name: 'A' }, '2026-05-06'),
  ]
  const options = buildScheduleTeamOptions(items)
  assert.deepEqual(options.map((t) => t.eventTeamId), ['A', 'B', 'C', 'D'])
  assert.equal(options[0].logoUrl, 'logo-a.png')
})

const realSmoke = {
  divisionId: '177',
  selectedDate: '',
  dateMatchCount: 0,
  uniqueTeamCount: 0,
  selectedTeam: null,
  visibleMatchCount: 0,
  pageSize: DEFAULT_SCHEDULE_PAGE_SIZE,
  pageCount: 0,
  paginationVisible: false,
}

try {
  const res = await fetch('http://111.229.241.29:8881/basket/v4/public/division/177/schedule', {
    signal: AbortSignal.timeout(20000),
  })
  const body = await res.json()
  const all = (body.data || []).filter((item) => item.unitType === 'MATCH')
  const counts = new Map()
  for (const item of all) {
    const key = toDateKey(item.scheduleDate) || ''
    counts.set(key, (counts.get(key) || 0) + 1)
  }
  const selectedDate = [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || ''
  const dateResult = applyScheduleFilterPipeline({
    allPublishedMatches: all,
    selectedDate,
    pageSize: DEFAULT_SCHEDULE_PAGE_SIZE,
  })
  const team = dateResult.teamOptions[0]
  const teamResult = applyScheduleFilterPipeline({
    allPublishedMatches: all,
    selectedDate,
    selectedTeamId: team?.eventTeamId || null,
    currentPage: 3,
    pageSize: DEFAULT_SCHEDULE_PAGE_SIZE,
  })
  realSmoke.selectedDate = selectedDate
  realSmoke.dateMatchCount = dateResult.dateMatches.length
  realSmoke.uniqueTeamCount = dateResult.teamOptions.length
  realSmoke.selectedTeam = team
    ? { eventTeamId: team.eventTeamId, name: team.name, visibleMatchCount: teamResult.visibleMatches.length }
    : null
  realSmoke.visibleMatchCount = teamResult.visibleMatches.length
  realSmoke.pageCount = dateResult.pageCount
  realSmoke.paginationVisible = dateResult.total > dateResult.pageSize
  assert.equal(all.length > 0, true, 'division 177 should have published matches')
  assert.equal(dateResult.teamOptions.length > 0, true, 'date team options should not be empty')
  if (team) {
    assert.equal(teamResult.currentPage, 1)
    assert.equal(teamResult.visibleMatches.length > 0, true)
    assert.equal(
      teamResult.teamOptions.length,
      dateResult.teamOptions.length,
      'team options must not shrink after team filter',
    )
  }
  cases.push({ name: 'Real smoke division 177', status: 'PASS' })
} catch (err) {
  cases.push({
    name: 'Real smoke division 177',
    status: 'FAIL',
    error: err instanceof Error ? err.message : String(err),
  })
}

const failed = cases.filter((item) => item.status === 'FAIL')
for (const item of cases) {
  console.log(`${item.status}\t${item.name}${item.error ? ` :: ${item.error}` : ''}`)
}
console.log(`RESULT\t${failed.length ? 'FAIL' : 'PASS'}\t${cases.length - failed.length}/${cases.length}`)
console.log('REAL_SMOKE\t' + JSON.stringify(realSmoke))
if (failed.length) process.exitCode = 1
