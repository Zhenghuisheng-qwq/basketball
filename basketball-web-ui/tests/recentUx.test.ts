import assert from 'node:assert/strict'
import test from 'node:test'
import { buildCurrentEventMenuEntries } from '../src/constants/publicCompetitionMenu.ts'
import {
  goToNewTeamRegistration,
  goToRegistrationEditFlow,
} from '../src/utils/registrationNavigation.ts'

const menuOptions = {
  locale: 'zh-CN',
  seriesLabel: (code: string) => code,
  statusLabel: (status?: string | null) => status || '',
  emptyHint: '暂无赛事',
}

test('current event menu prefers a playing season over a newer finished one', () => {
  const entries = buildCurrentEventMenuEntries([
    {
      seasonId: '300',
      seriesCode: 'cup',
      displayTitle: '已结束的新赛季',
      seasonStatus: 'FINISHED',
    },
    {
      seasonId: '184',
      seriesCode: 'CUP',
      displayTitle: 'CNPA Cup 2026 S1',
      seasonStatus: 'PLAYING',
    },
  ], menuOptions)
  const cup = entries.find((entry) => entry.seriesCode === 'CUP')
  assert.equal(cup?.label, 'CNPA Cup 2026 S1')
  assert.equal(cup?.seasonId, '184')
  assert.deepEqual(cup?.to, { path: '/events/184' })
  assert.equal(cup?.hint, 'CUP · PLAYING')
})

test('missing season keeps the series page and does not throw on a numeric id', () => {
  const entries = buildCurrentEventMenuEntries([
    {
      seasonId: 91 as unknown as string,
      seriesCode: 'LEAGUE',
      displayTitle: '联赛 S1',
      seasonStatus: 'REGISTRATION_OPEN',
    },
  ], menuOptions)
  const league = entries.find((entry) => entry.seriesCode === 'LEAGUE')
  const arena = entries.find((entry) => entry.seriesCode === 'ARENA')
  assert.equal(league?.seasonId, '91')
  assert.deepEqual(league?.to, { path: '/events/91' })
  assert.equal(arena?.label, 'Arena')
  assert.equal(arena?.hint, '暂无赛事')
  assert.deepEqual(arena?.to, { path: '/tournament', query: { series: 'ARENA' } })
})

test('english menu uses the english title when it exists', () => {
  const [league] = buildCurrentEventMenuEntries([
    {
      seasonId: '12',
      seriesCode: 'LEAGUE',
      displayTitle: '联赛',
      displayTitleEn: 'League 2026',
      seasonStatus: 'NOT_STARTED',
    },
  ], { ...menuOptions, locale: 'en-US' })
  assert.equal(league?.label, 'League 2026')
})

test('registration opens the division form, not the season id', () => {
  const pushes: unknown[] = []
  const router = { push(target: unknown) { pushes.push(target) } }
  goToNewTeamRegistration(router as never, {
    seasonId: '184',
    divisionId: '213',
    platformCode: 'PS',
  })
  assert.deepEqual(pushes[0], {
    name: 'AccountV4Registration',
    params: { divisionId: '213' },
    query: { platformCode: 'PS' },
  })
})

test('an existing registration still opens that division with its registration id', () => {
  const pushes: unknown[] = []
  const router = { push(target: unknown) { pushes.push(target) } }
  goToRegistrationEditFlow(router as never, {
    registrationId: '88',
    seasonId: '213',
    platformCode: 'XBOX',
  })
  assert.deepEqual(pushes[0], {
    name: 'AccountV4Registration',
    params: { divisionId: '213' },
    query: { registrationId: '88', platformCode: 'XBOX' },
  })
})
