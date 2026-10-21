import type { V4PublicMatchDetail, V4PublicPlayerStatItem } from '@/api/types/v4Public'
import { translate } from '@/i18n'
import { displayText, formatDateTime, toNumber } from '@/utils/publicDisplay'
import { calculateAverage } from '@/utils/statAverage'
import type {
  TeamPlayerAverageStat,
  TeamPlayerTotalStat,
  TeamSingleMatchPlayerStat,
  TeamSingleMatchStats,
  TeamSingleMatchTotal,
} from '@/types/team'

function publicPlayerRouteId(item: { sourcePlayerId?: string | null; eventPlayerId?: string | null }): string {
  return String(item.sourcePlayerId || '').trim()
}

function shootingPct(
  made?: number | string | null,
  attempt?: number | string | null,
  pct?: number | string | null,
): number | null {
  if (pct != null && pct !== '' && Number.isFinite(Number(pct))) {
    const num = Number(pct)
    return num <= 1 ? num * 100 : num
  }
  if (attempt == null || attempt === '' || !Number.isFinite(Number(attempt))) return null
  const attempts = Number(attempt)
  if (attempts === 0) return null
  return (toNumber(made, 0) / attempts) * 100
}

function formatMakesAttempts(made?: number | null, attempt?: number | null): string {
  if (attempt == null || !Number.isFinite(Number(attempt))) return '—'
  const attempts = Number(attempt)
  if (attempts === 0) return '—'
  return `${toNumber(made, 0)}/${attempts}`
}

export function mapV4PlayerStatToAverage(item: V4PublicPlayerStatItem): TeamPlayerAverageStat {
  const games = toNumber(item.matchCount, 0)
  return {
    playerId: publicPlayerRouteId(item),
    nickname: displayText(item.playerName, translate('publicPages.team.unknownPlayer')),
    avatar: item.avatarUrl?.trim() || '',
    games,
    points: item.avgPoints != null ? toNumber(item.avgPoints, 0) : calculateAverage(item.points, games),
    rebounds: item.avgRebound != null ? toNumber(item.avgRebound, 0) : calculateAverage(item.rebound, games),
    assists: item.avgAssist != null ? toNumber(item.avgAssist, 0) : calculateAverage(item.assist, games),
    steals: calculateAverage(item.steal, games),
    blocks: calculateAverage(item.block, games),
    fouls: item.foul != null ? calculateAverage(item.foul, games) : null,
    turnovers: calculateAverage(item.turnover, games),
    fgPct: shootingPct(item.fieldGoalMade, item.fieldGoalAttempt, item.fieldGoalPct),
    tpPct: shootingPct(item.threePointMade, item.threePointAttempt, item.threePointPct),
    ftPct: shootingPct(item.freeThrowMade, item.freeThrowAttempt, item.freeThrowPct),
  }
}

export function mapV4PlayerStatToTotal(item: V4PublicPlayerStatItem): TeamPlayerTotalStat {
  return {
    playerId: publicPlayerRouteId(item),
    nickname: displayText(item.playerName, translate('publicPages.team.unknownPlayer')),
    avatar: item.avatarUrl?.trim() || '',
    games: toNumber(item.matchCount, 0),
    points: toNumber(item.points, 0),
    rebounds: toNumber(item.rebound, 0),
    assists: toNumber(item.assist, 0),
    steals: toNumber(item.steal, 0),
    blocks: toNumber(item.block, 0),
    fouls: item.foul != null ? toNumber(item.foul, 0) : null,
    turnovers: toNumber(item.turnover, 0),
    fgPct: shootingPct(item.fieldGoalMade, item.fieldGoalAttempt, item.fieldGoalPct),
    tpPct: shootingPct(item.threePointMade, item.threePointAttempt, item.threePointPct),
    ftPct: shootingPct(item.freeThrowMade, item.freeThrowAttempt, item.freeThrowPct),
  }
}

function mapV4MatchPlayerStat(item: V4PublicMatchDetail['playerStats'][number]): TeamSingleMatchPlayerStat {
  return {
    playerId: publicPlayerRouteId(item),
    nickname: displayText(item.playerName, translate('publicPages.team.unknownPlayer')),
    avatar: item.avatarUrl?.trim() || '',
    grade: '—',
    points: toNumber(item.points, 0),
    rebounds: toNumber(item.rebound, 0),
    assists: toNumber(item.assist, 0),
    steals: toNumber(item.steal, 0),
    blocks: toNumber(item.block, 0),
    fouls: item.foul != null ? toNumber(item.foul, 0) : null,
    turnovers: toNumber(item.turnover, 0),
    fg: formatMakesAttempts(item.fieldGoalMade, item.fieldGoalAttempt),
    tp: formatMakesAttempts(item.threePointMade, item.threePointAttempt),
    ft: formatMakesAttempts(item.freeThrowMade, item.freeThrowAttempt),
  }
}

function mapV4MatchTeamTotal(item: V4PublicMatchDetail['teamStats'][number]): TeamSingleMatchTotal {
  return {
    grade: '—',
    points: toNumber(item.pointsFor ?? item.score, 0),
    rebounds: toNumber(item.rebound, 0),
    assists: toNumber(item.assist, 0),
    steals: toNumber(item.steal, 0),
    blocks: toNumber(item.block, 0),
    fouls: item.foul != null ? toNumber(item.foul, 0) : null,
    turnovers: toNumber(item.turnover, 0),
    fg: formatMakesAttempts(item.fieldGoalMade, item.fieldGoalAttempt),
    tp: formatMakesAttempts(item.threePointMade, item.threePointAttempt),
    ft: formatMakesAttempts(item.freeThrowMade, item.freeThrowAttempt),
  }
}

export function mapV4MatchDetailToSingleMatchStats(
  detail: V4PublicMatchDetail,
  eventTeamId: string,
): TeamSingleMatchStats | null {
  const players = (detail.playerStats ?? [])
    .filter((item) => String(item.eventTeamId || '') === String(eventTeamId))
    .map(mapV4MatchPlayerStat)

  if (!players.length) return null

  const teamStat = (detail.teamStats ?? []).find(
    (item) => String(item.eventTeamId || '') === String(eventTeamId),
  )
  const teamTotal = teamStat ? mapV4MatchTeamTotal(teamStat) : mapV4MatchTeamTotal({
    eventTeamId,
    score: 0,
    pointsFor: 0,
  })

  const dt = formatDateTime(detail.matchTime)
  const label = displayText(dt.full, translate('publicPages.team.singleGame'))

  return {
    matchId: String(detail.matchId),
    label,
    players,
    teamTotal,
  }
}

/** @deprecated use mapV4MatchDetailToSingleMatchStats */
export function mapPublicMatchDetailToSingleMatchStats(
  detail: V4PublicMatchDetail,
  teamId: string,
): TeamSingleMatchStats | null {
  return mapV4MatchDetailToSingleMatchStats(detail, teamId)
}

/** @deprecated use mapV4PlayerStatToAverage */
export function mapPublicPlayerSeasonStatToAverage(item: V4PublicPlayerStatItem): TeamPlayerAverageStat {
  return mapV4PlayerStatToAverage(item)
}

/** @deprecated use mapV4PlayerStatToTotal */
export function mapPublicPlayerSeasonStatToTotal(item: V4PublicPlayerStatItem): TeamPlayerTotalStat {
  return mapV4PlayerStatToTotal(item)
}
