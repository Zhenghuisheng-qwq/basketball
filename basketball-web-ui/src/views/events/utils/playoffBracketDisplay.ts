import type { V4PublicMatchItem, V4PublicMatchSeriesDetail } from '@/api/types/v4Public'
import { translate } from '@/i18n'
import { toNumber } from '@/utils/publicDisplay'
import { v4BestOfLabel } from '@/utils/v4Display'
import type { PlayoffRoundKey, PlayoffSeries } from '../types/events'

export type RoundBestOfLine = {
  label: string
  bestOfLabel: string
}

export type BracketRoundColumn = {
  key: string
  roundNo: number
  label: string
  labelEn: string
  series: PlayoffSeries[]
}

export function classifyPlayoffRound(
  seriesCount: number,
  isLastRound: boolean,
  roundNo: number,
): { key: PlayoffRoundKey; label: string; labelEn: string } {
  if (seriesCount === 1 && isLastRound) {
    return { key: 'final', label: translate('events.round.FINAL'), labelEn: 'FINAL' }
  }
  if (seriesCount === 2) {
    return { key: 'semifinal', label: translate('events.round.SF'), labelEn: 'SEMIFINAL' }
  }
  if (seriesCount === 4) {
    return { key: 'quarterfinal', label: translate('events.round.R8'), labelEn: 'QUARTERFINAL' }
  }
  if (seriesCount === 8) {
    return { key: 'round16', label: translate('events.round.R16'), labelEn: 'ROUND OF 16' }
  }
  if (seriesCount === 16) {
    return { key: 'round32', label: translate('events.round.R32'), labelEn: 'ROUND OF 32' }
  }
  if (isLastRound) {
    return { key: 'final', label: translate('events.round.FINAL'), labelEn: 'FINAL' }
  }
  return { key: 'other', label: translate('events.round.OTHER', { round: roundNo }), labelEn: `ROUND ${roundNo}` }
}

function publicTeamId(id?: string | number | null): string {
  if (id == null || id === '') return ''
  const numeric = Number(id)
  if (!Number.isFinite(numeric) || numeric <= 0) return ''
  return String(id)
}

function teamName(name?: string | null, sourceType?: string | null, sourceDisplay?: string | null): string {
  const text = String(name || '').trim()
  if (text) return text
  const type = String(sourceType || '').toUpperCase()
  if (type === 'BYE') return translate('match.bye')
  if (type === 'WINNER_OF_SERIES') return translate('match.winnerOfPrevious')
  if (type === 'GROUP_RANK') return translate('match.groupRankPending')
  const display = String(sourceDisplay || '').trim()
  if (display) return display
  return translate('match.tbd')
}

export function isUnresolvedBracketSlot(teamId?: string | null, sourceType?: string | null): boolean {
  if (String(teamId || '').trim()) return false
  return String(sourceType || '').toUpperCase() !== 'BYE'
}

export function formatBracketSchedule(date?: string | null, time?: string | null): string {
  const rawDate = String(date || '').trim()
  if (!rawDate) return ''
  const datePart = rawDate.replace('T', ' ').split(/\s+/)[0] || ''
  const parts = datePart.split('-')
  const md = parts.length >= 3 ? `${parts[1]}.${parts[2]}` : datePart
  const hm = String(time || '').trim().slice(0, 5)
  return hm ? `${md} ${hm}` : md
}

function isStarted(status?: string | null): boolean {
  const code = String(status || '').toUpperCase()
  return code === 'RUNNING' || code === 'COMPLETED' || code === 'CANCELLED'
}

export function groupPublicSeriesToRounds(rows: V4PublicMatchSeriesDetail[]): BracketRoundColumn[] {
  const byRound = new Map<number, V4PublicMatchSeriesDetail[]>()
  for (const row of rows) {
    const roundNo = toNumber(row.roundNo, 0)
    const list = byRound.get(roundNo) || []
    list.push(row)
    byRound.set(roundNo, list)
  }
  const roundNos = [...byRound.keys()].sort((a, b) => a - b)
  const lastRoundNo = roundNos[roundNos.length - 1]
  return roundNos.map((roundNo) => {
    const list = byRound.get(roundNo) || []
    const meta = classifyPlayoffRound(list.length, roundNo === lastRoundNo, roundNo)
    return {
      key: `${meta.key}-${roundNo}`,
      roundNo,
      label: meta.label,
      labelEn: meta.labelEn,
      series: list.map((row) => publicSeriesToCard(row, meta)),
    }
  }).filter((col) => col.series.length)
}

export function summarizeRoundBestOf(rows: V4PublicMatchSeriesDetail[]): RoundBestOfLine[] {
  return groupPublicSeriesToRounds(rows).flatMap((column) => {
    const values = [...new Set(
      column.series
        .map((item) => item.bestOf)
        .filter((bestOf): bestOf is number => Number(bestOf) > 0),
    )]
    if (!values.length) return []
    return [{
      label: column.label,
      bestOfLabel: values.map((bestOf) => v4BestOfLabel(bestOf)).join(' / '),
    }]
  })
}

export function countSeriesParticipants(rows: V4PublicMatchSeriesDetail[]): number {
  const ids = new Set<string>()
  for (const row of rows) {
    for (const id of [row.homeEventTeamId, row.awayEventTeamId]) {
      const text = id == null ? '' : String(id).trim()
      if (text) ids.add(text)
    }
  }
  return ids.size
}

export function publicSeriesToCard(
  row: V4PublicMatchSeriesDetail,
  meta: { key: PlayoffRoundKey; label: string; labelEn: string },
): PlayoffSeries {
  const started = isStarted(row.seriesStatus)
  const firstGame = (row.games || []).find((game) => String(game.matchId || '').trim())
  return {
    id: String(row.id || firstGame?.matchId || ''),
    matchSeriesId: row.id ? String(row.id) : undefined,
    matchId: firstGame?.matchId ? String(firstGame.matchId) : undefined,
    bestOf: row.bestOf,
    round: meta.key,
    roundLabel: meta.label,
    roundLabelEn: meta.labelEn,
    topTeamId: publicTeamId(row.homeEventTeamId),
    bottomTeamId: publicTeamId(row.awayEventTeamId),
    topSourceType: row.homeSourceType,
    bottomSourceType: row.awaySourceType,
    topTeamName: teamName(row.homeTeamName, row.homeSourceType, row.homeSourceDisplay),
    bottomTeamName: teamName(row.awayTeamName, row.awaySourceType, row.awaySourceDisplay),
    topTeamLogo: row.homeTeamLogoUrl || '',
    bottomTeamLogo: row.awayTeamLogoUrl || '',
    topScore: started ? toNumber(row.homeWinCount, 0) : null,
    bottomScore: started ? toNumber(row.awayWinCount, 0) : null,
    winnerTeamId: publicTeamId(row.winnerEventTeamId),
    seriesStatus: row.seriesStatus,
    scheduledLabel: formatBracketSchedule(row.scheduledDate, row.scheduledStartTime),
  }
}

function dedupeMatchesToSeries(matches: V4PublicMatchItem[]): V4PublicMatchItem[] {
  const bySeries = new Map<string, V4PublicMatchItem>()
  const sorted = [...matches].sort((a, b) => {
    const ga = toNumber(a.seriesGameNo, 0) - toNumber(b.seriesGameNo, 0)
    if (ga !== 0) return ga
    return String(a.matchId).localeCompare(String(b.matchId))
  })
  for (const item of sorted) {
    const key = String(item.matchSeriesId || '').trim() || `match:${item.matchId}`
    const prev = bySeries.get(key)
    if (!prev) {
      bySeries.set(key, item)
      continue
    }
    const prevDone = String(prev.seriesStatus || '').toUpperCase() === 'COMPLETED' || Boolean(prev.winnerEventTeamId)
    const nextDone = String(item.seriesStatus || '').toUpperCase() === 'COMPLETED' || Boolean(item.winnerEventTeamId)
    if (!prevDone && nextDone) bySeries.set(key, item)
  }
  return [...bySeries.values()]
}

export function groupMatchItemsToRounds(matches: V4PublicMatchItem[]): BracketRoundColumn[] {
  const seriesList = dedupeMatchesToSeries(matches)
  const fake: V4PublicMatchSeriesDetail[] = seriesList.map((item) => ({
    id: item.matchSeriesId ? String(item.matchSeriesId) : undefined,
    roundNo: item.roundNo,
    bestOf: item.bestOf,
    homeEventTeamId: item.homeEventTeamId,
    awayEventTeamId: item.awayEventTeamId,
    homeSourceType: item.homeSourceType,
    awaySourceType: item.awaySourceType,
    homeSourceDisplay: item.homeSourceDisplay,
    awaySourceDisplay: item.awaySourceDisplay,
    homeTeamName: item.homeTeamName,
    awayTeamName: item.awayTeamName,
    homeTeamLogoUrl: item.homeTeamLogoUrl,
    awayTeamLogoUrl: item.awayTeamLogoUrl,
    homeWinCount: item.seriesHomeWinCount,
    awayWinCount: item.seriesAwayWinCount,
    seriesStatus: item.seriesStatus,
    winnerEventTeamId: item.winnerEventTeamId,
    scheduledDate: item.matchTime,
    scheduledStartTime: item.matchTime,
    games: item.matchId ? [{ matchId: String(item.matchId) }] : [],
  }))
  return groupPublicSeriesToRounds(fake)
}

export function connectorCount(leftCount: number, rightCount: number): number {
  if (rightCount > 0) return rightCount
  return Math.floor(leftCount / 2)
}

export type MirrorSideColumn = {
  key: string
  label: string
  labelEn: string
  series: PlayoffSeries[]
}

export type MirroredBracket = {
  left: MirrorSideColumn[]
  final: PlayoffSeries | null
  right: MirrorSideColumn[]
  playoffParticipantCount: number
  halfSize: number
}

/**
 * Qualification Split ≠ Playoff Bracket Half Split
 *
 * Regular season (`stageProgressDisplay`):
 *   qualificationCount / qualifyCount decides who advances into playoffs.
 *
 * Playoff bracket (this file):
 *   halfSize = playoffParticipantCount / 2
 *   4 → 2+2, 8 → 4+4, 16 → 8+8, 32 → 16+16
 *
 * Never use qualificationCount or the regular-season roster as halfSize.
 * Never slice a team list to invent 1v8 pairings.
 * Left/right membership comes from existing MatchSeries + winner-advance links.
 */
export function countPlayoffParticipants(columns: BracketRoundColumn[]): number {
  const first = columns[0]
  if (!first?.series.length) return 0
  const ids = new Set<string>()
  for (const series of first.series) {
    for (const id of seriesTeamIds(series)) ids.add(id)
  }
  if (ids.size > 0) return ids.size
  return first.series.length * 2
}

export function playoffHalfSize(participantCount: number): number {
  if (!Number.isFinite(participantCount) || participantCount <= 0) return 0
  return participantCount / 2
}

export function countLeafTeams(side: MirrorSideColumn[]): number {
  if (!side.length) return 0
  const leaf = side.reduce((best, column) => (
    column.series.length > best.series.length ? column : best
  ), side[0])
  const ids = new Set<string>()
  for (const series of leaf.series) {
    for (const id of seriesTeamIds(series)) ids.add(id)
  }
  return ids.size
}

function emptyBracket(): MirroredBracket {
  return { left: [], final: null, right: [], playoffParticipantCount: 0, halfSize: 0 }
}

function seriesTeamIds(series: PlayoffSeries): string[] {
  return [series.topTeamId, series.bottomTeamId].filter(Boolean)
}

function feedsParent(child: PlayoffSeries, parent: PlayoffSeries): boolean {
  const parentTeams = new Set(seriesTeamIds(parent))
  if (!parentTeams.size) return false
  if (child.winnerTeamId && parentTeams.has(child.winnerTeamId)) return true
  return seriesTeamIds(child).some((id) => parentTeams.has(id))
}

function expectedLeftSeriesCount(
  previousCount: number,
  leftParents: number,
  rightParents: number,
): number {
  if (leftParents > 0 && rightParents > 0) {
    return Math.round(previousCount * (leftParents / (leftParents + rightParents)))
  }
  // Keep a balanced half even if the opposite Final/SF slot is still TBD.
  // halfSize is playoffParticipantCount / 2, never qualificationCount.
  return previousCount / 2
}

function assignChildren(
  previous: PlayoffSeries[],
  leftParents: PlayoffSeries[],
  rightParents: PlayoffSeries[],
): { left: PlayoffSeries[]; right: PlayoffSeries[] } {
  const left: PlayoffSeries[] = []
  const right: PlayoffSeries[] = []
  const leftover: PlayoffSeries[] = []

  for (const series of previous) {
    const toLeft = leftParents.some((parent) => feedsParent(series, parent))
    const toRight = rightParents.some((parent) => feedsParent(series, parent))
    if (toLeft && !toRight) left.push(series)
    else if (toRight && !toLeft) right.push(series)
    else leftover.push(series)
  }

  // Leftover series still exist in the real bracket; only their visual half
  // is unresolved (e.g. next round TBD). Fill the half-series quota.
  // This does not invent pairings or touch qualificationCount.
  const expectedLeft = expectedLeftSeriesCount(
    previous.length,
    leftParents.length,
    rightParents.length,
  )

  for (const series of leftover) {
    if (left.length < expectedLeft) left.push(series)
    else right.push(series)
  }

  const order = new Map(previous.map((series, index) => [series.id, index]))
  left.sort((a, b) => (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0))
  right.sort((a, b) => (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0))
  return { left, right }
}

function asSlot(id: string, key: string): PlayoffSeries {
  return {
    id: key,
    topTeamId: id,
    bottomTeamId: '',
    topTeamName: '',
    bottomTeamName: '',
    topTeamLogo: '',
    bottomTeamLogo: '',
    topScore: null,
    bottomScore: null,
    winnerTeamId: '',
    round: 'final',
    roundLabel: '',
    roundLabelEn: '',
  }
}

function splitInnerByFinal(
  innerSeries: PlayoffSeries[],
  final: PlayoffSeries | null,
): { left: PlayoffSeries[]; right: PlayoffSeries[] } {
  if (innerSeries.length <= 1) {
    return { left: innerSeries, right: [] }
  }
  if (final) {
    const topId = final.topTeamId
    const bottomId = final.bottomTeamId
    if (topId || bottomId) {
      return assignChildren(
        innerSeries,
        topId ? [asSlot(topId, 'final-top')] : [],
        bottomId ? [asSlot(bottomId, 'final-bottom')] : [],
      )
    }
  }
  const mid = innerSeries.length / 2
  return {
    left: innerSeries.slice(0, mid),
    right: innerSeries.slice(mid),
  }
}

function toSideColumn(column: BracketRoundColumn, series: PlayoffSeries[], side: 'left' | 'right'): MirrorSideColumn {
  return {
    key: `${column.key}-${side}`,
    label: column.label,
    labelEn: column.labelEn,
    series,
  }
}

export function toMirroredBracket(columns: BracketRoundColumn[]): MirroredBracket {
  const playoffParticipantCount = countPlayoffParticipants(columns)
  const halfSize = playoffHalfSize(playoffParticipantCount)

  if (!columns.length) return emptyBracket()

  const last = columns[columns.length - 1]
  const hasFinal = last.series.length === 1
  const final = hasFinal ? last.series[0] ?? null : null
  const early = hasFinal ? columns.slice(0, -1) : columns
  if (!early.length) {
    return { left: [], final, right: [], playoffParticipantCount, halfSize }
  }

  const inner = early[early.length - 1]
  const innerSplit = splitInnerByFinal(inner.series, final)
  let leftParents = innerSplit.left
  let rightParents = innerSplit.right

  const left: MirrorSideColumn[] = [toSideColumn(inner, leftParents, 'left')]
  const right: MirrorSideColumn[] = [toSideColumn(inner, rightParents, 'right')]

  for (let index = early.length - 2; index >= 0; index -= 1) {
    const column = early[index]
    const split = assignChildren(column.series, leftParents, rightParents)
    left.unshift(toSideColumn(column, split.left, 'left'))
    right.unshift(toSideColumn(column, split.right, 'right'))
    leftParents = split.left
    rightParents = split.right
  }

  return {
    left,
    final,
    right: [...right].reverse(),
    playoffParticipantCount,
    halfSize,
  }
}
