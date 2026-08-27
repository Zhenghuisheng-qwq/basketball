import assert from 'node:assert/strict'
import { publicContestDetailPath } from '../../../utils/v4Display'
import {
  classifyPlayoffRound,
  countLeafTeams,
  countPlayoffParticipants,
  groupMatchItemsToRounds,
  playoffHalfSize,
  publicSeriesToCard,
  summarizeRoundBestOf,
  toMirroredBracket,
  type BracketRoundColumn,
} from './playoffBracketDisplay'
import type { PlayoffRoundKey, PlayoffSeries } from '../types/events'

function series(id: string, top: string, bottom: string, winner: string): PlayoffSeries {
  return {
    id,
    matchSeriesId: id,
    topTeamId: top,
    bottomTeamId: bottom,
    topTeamName: top,
    bottomTeamName: bottom,
    topTeamLogo: '',
    bottomTeamLogo: '',
    topScore: 1,
    bottomScore: 0,
    winnerTeamId: winner,
    seriesStatus: 'COMPLETED',
    round: 'other',
    roundLabel: '',
    roundLabelEn: '',
  }
}

function column(roundNo: number, seriesCount: number, items: PlayoffSeries[]): BracketRoundColumn {
  const meta = classifyPlayoffRound(seriesCount, seriesCount === 1, roundNo)
  return {
    key: meta.key + '-' + roundNo,
    roundNo,
    label: meta.label,
    labelEn: meta.labelEn,
    series: items.map((item) => ({ ...item, round: meta.key as PlayoffRoundKey })),
  }
}

const eight = [
  column(1, 4, [
    series('qf-1', 'T01', 'T08', 'T01'),
    series('qf-2', 'T04', 'T05', 'T04'),
    series('qf-3', 'T02', 'T07', 'T02'),
    series('qf-4', 'T03', 'T06', 'T03'),
  ]),
  column(2, 2, [
    series('sf-l', 'T01', 'T04', 'T01'),
    series('sf-r', 'T02', 'T03', 'T02'),
  ]),
  column(3, 1, [series('final', 'T01', 'T02', 'T01')]),
]

const eightLayout = toMirroredBracket(eight)
assert.equal(eightLayout.playoffParticipantCount, 8)
assert.equal(eightLayout.halfSize, 4)
assert.equal(countPlayoffParticipants(eight), 8)
assert.equal(playoffHalfSize(8), 4)
assert.equal(countLeafTeams(eightLayout.left), 4)
assert.equal(countLeafTeams(eightLayout.right), 4)
assert.deepEqual(
  eightLayout.left[0].series.map((item) => item.id),
  ['qf-1', 'qf-2'],
)
assert.deepEqual(
  eightLayout.right[eightLayout.right.length - 1].series.map((item) => item.id),
  ['qf-3', 'qf-4'],
)
assert.equal(eightLayout.final?.id, 'final')
assert.equal(eightLayout.left.length, 2)
assert.equal(eightLayout.right.length, 2)

const four = [
  column(1, 2, [
    series('sf-l', 'A', 'B', 'A'),
    series('sf-r', 'C', 'D', 'C'),
  ]),
  column(2, 1, [series('final', 'A', 'C', 'A')]),
]
const fourLayout = toMirroredBracket(four)
assert.equal(fourLayout.playoffParticipantCount, 4)
assert.equal(fourLayout.halfSize, 2)
assert.equal(countLeafTeams(fourLayout.left), 2)
assert.equal(countLeafTeams(fourLayout.right), 2)

const r16 = [
  column(1, 8, [
    series('r16-1', 'A1', 'A8', 'A1'),
    series('r16-2', 'A4', 'A5', 'A4'),
    series('r16-3', 'A2', 'A7', 'A2'),
    series('r16-4', 'A3', 'A6', 'A3'),
    series('r16-5', 'B1', 'B8', 'B1'),
    series('r16-6', 'B4', 'B5', 'B4'),
    series('r16-7', 'B2', 'B7', 'B2'),
    series('r16-8', 'B3', 'B6', 'B3'),
  ]),
  column(2, 4, [
    series('qf-l1', 'A1', 'A4', 'A1'),
    series('qf-l2', 'A2', 'A3', 'A2'),
    series('qf-r1', 'B1', 'B4', 'B1'),
    series('qf-r2', 'B2', 'B3', 'B2'),
  ]),
  column(3, 2, [
    series('sf-l', 'A1', 'A2', 'A1'),
    series('sf-r', 'B1', 'B2', 'B1'),
  ]),
  column(4, 1, [series('final', 'A1', 'B1', 'A1')]),
]
const sixteen = toMirroredBracket(r16)
assert.equal(sixteen.playoffParticipantCount, 16)
assert.equal(sixteen.halfSize, 8)
assert.equal(countLeafTeams(sixteen.left), 8)
assert.equal(countLeafTeams(sixteen.right), 8)
assert.equal(sixteen.left.length, 3)
assert.equal(sixteen.right.length, 3)
assert.equal(sixteen.left[0].series.length, 4)
assert.equal(sixteen.right[sixteen.right.length - 1].series.length, 4)
assert.deepEqual(
  sixteen.left[0].series.map((item) => item.id),
  ['r16-1', 'r16-2', 'r16-3', 'r16-4'],
)
assert.deepEqual(
  sixteen.right[sixteen.right.length - 1].series.map((item) => item.id),
  ['r16-5', 'r16-6', 'r16-7', 'r16-8'],
)

const roundLines = summarizeRoundBestOf([
  { id: '15', roundNo: 1, bestOf: 3, homeEventTeamId: '49', awayEventTeamId: '56' },
  { id: '16', roundNo: 1, bestOf: 3, homeEventTeamId: '52', awayEventTeamId: '53' },
  { id: '17', roundNo: 1, bestOf: 3, homeEventTeamId: '50', awayEventTeamId: '55' },
  { id: '18', roundNo: 1, bestOf: 3, homeEventTeamId: '51', awayEventTeamId: '54' },
  { id: '19', roundNo: 2, bestOf: 5, homeEventTeamId: '49', awayEventTeamId: '52' },
  { id: '20', roundNo: 2, bestOf: 5, homeEventTeamId: '50', awayEventTeamId: '51' },
  { id: '21', roundNo: 3, bestOf: 7, homeEventTeamId: '49', awayEventTeamId: '50' },
])
assert.deepEqual(roundLines, [
  { label: '八强', bestOfLabel: 'BO3' },
  { label: '半决赛', bestOfLabel: 'BO5' },
  { label: '总决赛', bestOfLabel: 'BO7' },
])

console.log('playoff half-size rules: 4/8/16 PASS')
console.log('round bestOf summary: QF/SF/Final PASS')

const bo1Card = publicSeriesToCard(
  { id: 's-bo1', bestOf: 1, games: [{ matchId: 'm-bo1' }] },
  { key: 'quarterfinal', label: '八强', labelEn: 'QUARTERFINAL' },
)
const bo5Card = publicSeriesToCard(
  { id: 's-bo5', bestOf: 5, games: [{ matchId: 'm-g1' }, { matchId: 'm-g2' }] },
  { key: 'semifinal', label: '半决赛', labelEn: 'SEMIFINAL' },
)
assert.equal(publicContestDetailPath({
  unitType: 'MATCH_SERIES',
  matchId: bo1Card.matchId,
  matchSeriesId: bo1Card.matchSeriesId,
}), '/events/match-series/s-bo1')
assert.equal(publicContestDetailPath({
  unitType: 'MATCH_SERIES',
  matchId: bo5Card.matchId,
  matchSeriesId: bo5Card.matchSeriesId,
}), '/events/match-series/s-bo5')

const fallback = groupMatchItemsToRounds([
  { matchId: 'm-bo1-only', seasonId: '3', divisionId: '3', bestOf: 1, roundNo: 1, homeEventTeamId: '1', awayEventTeamId: '2' },
  { matchId: 'm-bo5-g1', seasonId: '3', divisionId: '3', matchSeriesId: 's-bo5', bestOf: 5, seriesGameNo: 1, roundNo: 2, homeEventTeamId: '1', awayEventTeamId: '2' },
])
const fallbackBo1 = fallback[0].series[0]
const fallbackBo5 = fallback[1].series[0]
assert.equal(publicContestDetailPath({
  unitType: fallbackBo1.matchSeriesId ? 'MATCH_SERIES' : 'MATCH',
  matchId: fallbackBo1.matchId || fallbackBo1.id,
  matchSeriesId: fallbackBo1.matchSeriesId,
}), '/events/match/m-bo1-only')
assert.equal(publicContestDetailPath({
  unitType: fallbackBo5.matchSeriesId ? 'MATCH_SERIES' : 'MATCH',
  matchId: fallbackBo5.matchId,
  matchSeriesId: fallbackBo5.matchSeriesId,
}), '/events/match-series/s-bo5')
assert.equal(publicContestDetailPath({
  unitType: 'MATCH',
  matchId: 'g1',
  matchSeriesId: 's-bo3',
}), '/events/match/g1')
console.log('public contest route targets: object type PASS')
