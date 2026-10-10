import type {
  V4PublicEventTeamDetail,
  V4PublicMatchDetail,
  V4PublicMatchPlayerStatItem,
  V4PublicMatchSeriesDetail,
  V4PublicMatchSeriesGame,
  V4PublicMatchTeamStatItem,
} from '@/api/types/v4Public'
import type { Team } from '@/views/events/types/events'
import { translate } from '@/i18n'
import { formatSeriesSetScore, v4MatchStatusLabel } from '@/utils/v4Display'

export const SERIES_TOTAL_TAB = 'SERIES_TOTAL'

export type ScorePair = {
  kind: 'vs' | 'score'
  left: number | null
  right: number | null
}

export type PeriodColumn = {
  key: string
  label: string
  periodNo?: number
}

export type BoxScoreRow = {
  eventPlayerId: string
  sourcePlayerId?: string
  playerName: string
  points: number | null
  rebound: number | null
  assist: number | null
  steal: number | null
  block: number | null
  foul: number | null
  turnover: number | null
  fieldGoalMade: number | null
  fieldGoalAttempt: number | null
  threePointMade: number | null
  threePointAttempt: number | null
  freeThrowMade: number | null
  freeThrowAttempt: number | null
  total?: boolean
}

export function isPresent(value: number | null | undefined): value is number {
  return value != null && Number.isFinite(Number(value))
}

export function formatExactScore(value: number | null | undefined, empty = '--'): string {
  return isPresent(value) ? String(Number(value)) : empty
}

export function formatShooting(made?: number | null, attempt?: number | null): string {
  if (!isPresent(made) || !isPresent(attempt)) return '--'
  return `${Number(made)}/${Number(attempt)}`
}

export function formatMatchWhen(value?: string | null, date?: string | null, time?: string | null): string {
  const raw = String(value || '').trim()
  const datePart = raw ? raw.replace('T', ' ').split(/\s+/)[0] : String(date || '').trim()
  const timePart = raw ? (raw.replace('T', ' ').split(/\s+/)[1] || '').slice(0, 5) : String(time || '').slice(0, 5)
  if (!datePart) return timePart || ''
  const [, month, day] = datePart.split('-')
  const dt = new Date(datePart.replace(/-/g, '/'))
  const week = Number.isNaN(dt.getTime()) ? '' : translate(`eventsPage.weekdayFull.${dt.getDay()}`)
  const shortDate = month && day ? `${month}.${day}` : datePart
  const clock = timePart && timePart !== '00:00' ? timePart : ''
  return [shortDate, week, clock].filter(Boolean).join(' · ')
}

export function toDetailTeam(
  eventTeamId?: string | null,
  name?: string | null,
  logoUrl?: string | null,
  shortName?: string | null,
): Team {
  const id = String(eventTeamId || '')
  const teamName = String(name || '').trim() || translate('match.tbd')
  const code = String(shortName || '').trim()
  return {
    id: Number(id) || 0,
    teamId: id,
    code: code || teamName,
    name: teamName,
    shortName: code || teamName,
    logo: String(logoUrl || '').trim(),
    wins: 0,
    losses: 0,
  }
}

export function teamFromMatchSide(match: V4PublicMatchDetail, side: 'home' | 'away'): Team {
  const team: V4PublicEventTeamDetail | undefined = side === 'home' ? match.homeTeam : match.awayTeam
  return toDetailTeam(
    team?.eventTeamId,
    team?.teamName,
    team?.teamLogoUrl,
    team?.teamShortName || team?.eventTeamNo,
  )
}

export function matchBasketballScore(match: V4PublicMatchDetail | null | undefined): ScorePair {
  if (!match) return { kind: 'vs', left: null, right: null }
  if (isPresent(match.awayScore) && isPresent(match.homeScore)) {
    return { kind: 'score', left: Number(match.awayScore), right: Number(match.homeScore) }
  }
  if (isPresent(match.awayScore) || isPresent(match.homeScore)) {
    return {
      kind: 'score',
      left: isPresent(match.awayScore) ? Number(match.awayScore) : null,
      right: isPresent(match.homeScore) ? Number(match.homeScore) : null,
    }
  }
  return { kind: 'vs', left: null, right: null }
}

export function seriesWinScore(series: V4PublicMatchSeriesDetail | null | undefined): ScorePair {
  if (!series) return { kind: 'vs', left: null, right: null }
  const official = formatSeriesSetScore({
    homeWinCount: series.homeWinCount,
    awayWinCount: series.awayWinCount,
    bestOf: series.bestOf,
    seriesStatus: series.seriesStatus,
    resolutionType: series.resolutionType,
    winnerEventTeamId: series.winnerEventTeamId,
    homeEventTeamId: series.homeEventTeamId,
    awayEventTeamId: series.awayEventTeamId,
    emptyText: 'VS',
  })
  if (!official || official === 'VS' || official === '-- : --') {
    return { kind: 'vs', left: null, right: null }
  }
  const [home, away] = official.split(':').map((part) => part.trim())
  const homeWins = home === '' ? null : Number(home)
  const awayWins = away === '' ? null : Number(away)
  if (!isPresent(homeWins) && !isPresent(awayWins)) {
    return { kind: 'vs', left: null, right: null }
  }
  return {
    kind: 'score',
    left: isPresent(awayWins) ? awayWins : null,
    right: isPresent(homeWins) ? homeWins : null,
  }
}

export function publicSeriesGames(series: V4PublicMatchSeriesDetail | null | undefined): V4PublicMatchSeriesGame[] {
  return (series?.games || [])
    .filter((game) => game && !game.skipped)
    .slice()
    .sort((a, b) => (a.seriesGameNo ?? 0) - (b.seriesGameNo ?? 0))
}

export function gameTabLabel(game: V4PublicMatchSeriesGame): string {
  return `G${game.seriesGameNo}`
}

export function gameCaption(game: V4PublicMatchSeriesGame | null | undefined): string {
  if (!game) return ''
  const status = game.matchStatus
    ? v4MatchStatusLabel(game.matchStatus)
    : (game.matchStatusName || '')
  return `${gameTabLabel(game)} · ${status}`
}

export function periodColumns(
  periods: Array<{ periodNo?: number; homeScore?: number; awayScore?: number }>,
  includeTotal = true,
): PeriodColumn[] {
  const cols: PeriodColumn[] = periods
    .filter((row) => row.periodNo != null)
    .sort((a, b) => Number(a.periodNo) - Number(b.periodNo))
    .map((row) => {
      const no = Number(row.periodNo)
      return {
        key: `p-${no}`,
        label: no <= 4 ? `Q${no}` : no === 5 ? 'OT' : `OT${no - 4}`,
        periodNo: no,
      }
    })
  if (includeTotal) {
    cols.push({ key: 'total', label: 'TOTAL' })
  }
  return cols
}

export function periodValue(
  periods: Array<{ periodNo?: number; homeScore?: number; awayScore?: number }>,
  periodNo: number,
  side: 'home' | 'away',
): number | null {
  const row = periods.find((item) => Number(item.periodNo) === periodNo)
  if (!row) return null
  const value = side === 'home' ? row.homeScore : row.awayScore
  return isPresent(value) ? Number(value) : null
}

export function periodTotal(
  periods: Array<{ periodNo?: number; homeScore?: number; awayScore?: number }>,
  side: 'home' | 'away',
): number | null {
  let total = 0
  let seen = false
  for (const row of periods) {
    const value = side === 'home' ? row.homeScore : row.awayScore
    if (isPresent(value)) {
      total += Number(value)
      seen = true
    }
  }
  return seen ? total : null
}

export function playersOfTeam(
  players: V4PublicMatchPlayerStatItem[] | undefined,
  eventTeamId?: string | null,
): V4PublicMatchPlayerStatItem[] {
  const teamId = String(eventTeamId || '')
  return (players || []).filter((row) => String(row.eventTeamId || '') === teamId)
}

export function toBoxScoreRow(row: V4PublicMatchPlayerStatItem): BoxScoreRow {
  return {
    eventPlayerId: String(row.eventPlayerId),
    sourcePlayerId: row.sourcePlayerId,
    playerName: row.playerName || '--',
    points: isPresent(row.points) ? Number(row.points) : null,
    rebound: isPresent(row.rebound) ? Number(row.rebound) : null,
    assist: isPresent(row.assist) ? Number(row.assist) : null,
    steal: isPresent(row.steal) ? Number(row.steal) : null,
    block: isPresent(row.block) ? Number(row.block) : null,
    foul: isPresent(row.foul) ? Number(row.foul) : null,
    turnover: isPresent(row.turnover) ? Number(row.turnover) : null,
    fieldGoalMade: isPresent(row.fieldGoalMade) ? Number(row.fieldGoalMade) : null,
    fieldGoalAttempt: isPresent(row.fieldGoalAttempt) ? Number(row.fieldGoalAttempt) : null,
    threePointMade: isPresent(row.threePointMade) ? Number(row.threePointMade) : null,
    threePointAttempt: isPresent(row.threePointAttempt) ? Number(row.threePointAttempt) : null,
    freeThrowMade: isPresent(row.freeThrowMade) ? Number(row.freeThrowMade) : null,
    freeThrowAttempt: isPresent(row.freeThrowAttempt) ? Number(row.freeThrowAttempt) : null,
  }
}

function sumNullable(values: Array<number | null>): number | null {
  let total = 0
  let seen = false
  for (const value of values) {
    if (isPresent(value)) {
      total += value
      seen = true
    }
  }
  return seen ? total : null
}

export function officialTeamTotal(
  players: BoxScoreRow[],
  teamStat?: V4PublicMatchTeamStatItem | null,
): BoxScoreRow {
  const summed: BoxScoreRow = {
    eventPlayerId: 'team-total',
    playerName: translate('eventsPage.match.teamTotal'),
    points: sumNullable(players.map((row) => row.points)),
    rebound: sumNullable(players.map((row) => row.rebound)),
    assist: sumNullable(players.map((row) => row.assist)),
    steal: sumNullable(players.map((row) => row.steal)),
    block: sumNullable(players.map((row) => row.block)),
    foul: sumNullable(players.map((row) => row.foul)),
    turnover: sumNullable(players.map((row) => row.turnover)),
    fieldGoalMade: sumNullable(players.map((row) => row.fieldGoalMade)),
    fieldGoalAttempt: sumNullable(players.map((row) => row.fieldGoalAttempt)),
    threePointMade: sumNullable(players.map((row) => row.threePointMade)),
    threePointAttempt: sumNullable(players.map((row) => row.threePointAttempt)),
    freeThrowMade: sumNullable(players.map((row) => row.freeThrowMade)),
    freeThrowAttempt: sumNullable(players.map((row) => row.freeThrowAttempt)),
    total: true,
  }
  if (!teamStat) return summed
  return {
    ...summed,
    points: isPresent(teamStat.score) ? Number(teamStat.score)
      : isPresent(teamStat.pointsFor) ? Number(teamStat.pointsFor) : summed.points,
    rebound: isPresent(teamStat.rebound) ? Number(teamStat.rebound) : summed.rebound,
    assist: isPresent(teamStat.assist) ? Number(teamStat.assist) : summed.assist,
    steal: isPresent(teamStat.steal) ? Number(teamStat.steal) : summed.steal,
    block: isPresent(teamStat.block) ? Number(teamStat.block) : summed.block,
    foul: isPresent(teamStat.foul) ? Number(teamStat.foul) : summed.foul,
    turnover: isPresent(teamStat.turnover) ? Number(teamStat.turnover) : summed.turnover,
    fieldGoalMade: isPresent(teamStat.fieldGoalMade) ? Number(teamStat.fieldGoalMade) : summed.fieldGoalMade,
    fieldGoalAttempt: isPresent(teamStat.fieldGoalAttempt) ? Number(teamStat.fieldGoalAttempt) : summed.fieldGoalAttempt,
    threePointMade: isPresent(teamStat.threePointMade) ? Number(teamStat.threePointMade) : summed.threePointMade,
    threePointAttempt: isPresent(teamStat.threePointAttempt) ? Number(teamStat.threePointAttempt) : summed.threePointAttempt,
    freeThrowMade: isPresent(teamStat.freeThrowMade) ? Number(teamStat.freeThrowMade) : summed.freeThrowMade,
    freeThrowAttempt: isPresent(teamStat.freeThrowAttempt) ? Number(teamStat.freeThrowAttempt) : summed.freeThrowAttempt,
  }
}

export function formatStatCell(value: number | null | undefined): string {
  return isPresent(value) ? String(Number(value)) : '--'
}
