import assert from 'node:assert/strict'
import test from 'node:test'
import { businessClock, businessDateTime, businessKickoffLabel } from '../src/utils/businessDateTime.ts'

test('UTC offset 13:00 is Shanghai 21:00', () => {
  const parsed = businessDateTime('2026-10-20T13:00:00.000+00:00')
  assert.equal(parsed?.date, '2026-10-20')
  assert.equal(parsed?.time, '21:00')
})

test('Shanghai offset stays 21:00', () => {
  const parsed = businessDateTime('2026-10-20T21:00:00.000+08:00')
  assert.equal(parsed?.date, '2026-10-20')
  assert.equal(parsed?.time, '21:00')
})

test('wall clock without offset is not shifted', () => {
  const parsed = businessDateTime('2026-10-20 19:00')
  assert.equal(parsed?.date, '2026-10-20')
  assert.equal(parsed?.time, '19:00')
  assert.equal(businessClock('21:00:00'), '21:00')
})

test('series kickoff keeps the business date and clock', () => {
  assert.equal(businessKickoffLabel('2026-10-20', '21:00:00'), '10月20日 21:00')
  assert.equal(businessKickoffLabel('2026-10-20T13:00:00.000+00:00', '21:00'), '10月20日 21:00')
})

test('empty and invalid values do not invent a clock', () => {
  assert.equal(businessDateTime(null), null)
  assert.equal(businessDateTime(''), null)
  assert.equal(businessDateTime('not-a-date'), null)
  assert.equal(businessClock(null), '')
  assert.equal(businessKickoffLabel(null, null), '')
})

test('BO1 wall clock 19:00 is unchanged', () => {
  const parsed = businessDateTime('2026-10-20 19:00:00')
  assert.equal(parsed?.time, '19:00')
  assert.equal(businessDateTime('2026-10-20T19:00:00.000+08:00')?.time, '19:00')
})
