import assert from 'node:assert/strict'
import {
  browseScheduleDateGroups,
  canLoadMoreDateGroups,
  dateGroupsIntact,
  DEFAULT_VISIBLE_DATE_GROUPS,
  nextVisibleDateGroupCount,
  remainingDateGroupCount,
  resolveInitSelectedDate,
  sliceDateGroups,
} from './scheduleDateBrowse'
import type { V4PublicScheduleItem } from '@/api/types/v4Public'

function match(id: string, date: string): V4PublicScheduleItem {
  return {
    id,
    unitType: 'MATCH',
    scheduleDate: date,
    startTime: '19:00',
  }
}

const items: V4PublicScheduleItem[] = [
  ...Array.from({ length: 13 }, (_, index) => match(`a${index}`, '2026-08-26')),
  ...Array.from({ length: 12 }, (_, index) => match(`b${index}`, '2026-08-27')),
  ...Array.from({ length: 4 }, (_, index) => match(`c${index}`, '2026-08-28')),
  ...Array.from({ length: 25 }, (_, index) => match(`d${index}`, '2026-08-29')),
]

const playingToday = resolveInitSelectedDate(items, 'PLAYING', '2026-08-26')
assert.equal(playingToday, '2026-08-26')

const playingGap = resolveInitSelectedDate(items, 'PLAYING', '2026-08-25')
assert.equal(playingGap, '2026-08-26')

const playingPast = resolveInitSelectedDate(items, 'PLAYING', '2026-09-01')
assert.equal(playingPast, '2026-08-29')

assert.equal(resolveInitSelectedDate(items, 'FINISHED', '2026-08-27'), '2026-08-29')
assert.equal(resolveInitSelectedDate(items, 'NOT_STARTED', '2026-08-01'), '2026-08-26')
assert.equal(resolveInitSelectedDate([], 'PLAYING', '2026-08-27'), '')

const allBrowse = browseScheduleDateGroups(items, '', DEFAULT_VISIBLE_DATE_GROUPS)
assert.equal(allBrowse.allGroups.length, 4)
assert.equal(allBrowse.visibleGroups.length, 3)
assert.equal(allBrowse.remainingGroups, 1)
assert.equal(allBrowse.canLoadMore, true)
assert.ok(dateGroupsIntact(allBrowse.allGroups, allBrowse.visibleGroups))
assert.equal(allBrowse.visibleGroups[0].items.length, 13)
assert.equal(allBrowse.visibleGroups[1].items.length, 12)
assert.equal(allBrowse.visibleGroups.every((group) => {
  const source = allBrowse.allGroups.find((item) => item.date === group.date)
  return source?.items.length === group.items.length
}), true)

const nextCount = nextVisibleDateGroupCount(3, 4)
assert.equal(nextCount, 4)
const more = browseScheduleDateGroups(items, '', nextCount)
assert.equal(more.visibleGroups.length, 4)
assert.equal(more.canLoadMore, false)
assert.equal(more.visibleGroups[3].items.length, 25)

const single = browseScheduleDateGroups(
  items.filter((item) => item.scheduleDate === '2026-08-29'),
  '2026-08-29',
  3,
)
assert.equal(single.visibleGroups.length, 1)
assert.equal(single.visibleGroups[0].items.length, 25)
assert.equal(single.canLoadMore, false)

const sliced = sliceDateGroups(allBrowse.allGroups, 2)
assert.equal(sliced[0].items.length, allBrowse.allGroups[0].items.length)
assert.equal(remainingDateGroupCount(4, 3), 1)
assert.equal(canLoadMoreDateGroups(4, 4), false)

console.log('scheduleDateBrowse.test.ts PASS')
