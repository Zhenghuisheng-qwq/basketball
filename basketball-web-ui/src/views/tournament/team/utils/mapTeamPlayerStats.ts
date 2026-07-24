import type {
  PublicMatchDetailVo,
  PublicMatchPlayerStatVo,
  PublicMatchTeamStatVo,
  PublicPlayerSeasonStatVo,
} from '@/api/types/public'
import { displayText, formatDateTime, toNumber } from '@/utils/publicDisplay'
import type {
  TeamPlayerAverageStat,
  TeamPlayerTotalStat,
  TeamSingleMatchPlayerStat,
  TeamSingleMatchStats,
  TeamSingleMatchTotal,
} from '@/types/team'

function normalizePct(value: unknown): number {
  const num = toNumber(value, 0)
  return num <= 1 ? num * 100 : num
}

function formatShootingPct(value: unknown): string {
  const pct = normalizePct(value)
  return `${pct.toFixed(1)}%`
}

export function mapPublicPlayerSeasonStatToAverage(
  item: PublicPlayerSeasonStatVo,
): TeamPlayerAverageStat {
  return {
    playerId: String(item.playerId),
    nickname: displayText(item.playerName, '未知球员'),
    avatar: item.avatarUrl?.trim() || '',
    games: toNumber(item.games, 0),
    points: toNumber(item.avgPoints, 0),
    rebounds: toNumber(item.avgRebounds, 0),
    assists: toNumber(item.avgAssists, 0),
    steals: toNumber(item.avgSteals, 0),
    blocks: toNumber(item.avgBlocks, 0),
    fouls: 0,
    turnovers: 0,
    fgPct: normalizePct(item.fieldGoalPct),
    tpPct: normalizePct(item.threePointPct),
    ftPct: normalizePct(item.freeThrowPct),
  }
}

export function mapPublicPlayerSeasonStatToTotal(
  item: PublicPlayerSeasonStatVo,
): TeamPlayerTotalStat {
  return {
    playerId: String(item.playerId),
    nickname: displayText(item.playerName, '未知球员'),
    avatar: item.avatarUrl?.trim() || '',
    games: toNumber(item.games, 0),
    points: toNumber(item.points, 0),
    rebounds: toNumber(item.rebounds, 0),
    assists: toNumber(item.assists, 0),
    steals: toNumber(item.steals, 0),
    blocks: toNumber(item.blocks, 0),
    fouls: 0,
    turnovers: 0,
    fgPct: normalizePct(item.fieldGoalPct),
    tpPct: normalizePct(item.threePointPct),
    ftPct: normalizePct(item.freeThrowPct),
  }
}

function mapMatchPlayerStat(item: PublicMatchPlayerStatVo): TeamSingleMatchPlayerStat {
  return {
    playerId: String(item.playerId || ''),
    nickname: displayText(item.playerName, '未知球员'),
    avatar: item.avatarUrl?.trim() || '',
    grade: displayText(item.efficiency, '--'),
    points: toNumber(item.points, 0),
    rebounds: toNumber(item.rebounds, 0),
    assists: toNumber(item.assists, 0),
    steals: toNumber(item.steals, 0),
    blocks: toNumber(item.blocks, 0),
    fouls: toNumber(item.fouls, 0),
    turnovers: toNumber(item.turnovers, 0),
    fg: formatShootingPct(item.fieldGoalPct),
    tp: formatShootingPct(item.threePointPct),
    ft: formatShootingPct(item.freeThrowPct),
  }
}

function mapMatchTeamTotal(item: PublicMatchTeamStatVo): TeamSingleMatchTotal {
  return {
    grade: '--',
    points: toNumber(item.points, 0),
    rebounds: toNumber(item.rebounds, 0),
    assists: toNumber(item.assists, 0),
    steals: toNumber(item.steals, 0),
    blocks: toNumber(item.blocks, 0),
    fouls: toNumber(item.fouls, 0),
    turnovers: toNumber(item.turnovers, 0),
    fg: formatShootingPct(item.fieldGoalPct),
    tp: formatShootingPct(item.threePointPct),
    ft: formatShootingPct(item.freeThrowPct),
  }
}

export function mapPublicMatchDetailToSingleMatchStats(
  detail: PublicMatchDetailVo,
  teamId: string,
): TeamSingleMatchStats | null {
  if (!detail.statsAvailable) return null

  const players = (detail.playerStats ?? [])
    .filter((item) => String(item.teamId || '') === String(teamId))
    .map(mapMatchPlayerStat)

  if (!players.length) return null

  const teamStat = (detail.teamStats ?? []).find(
    (item) => String(item.teamId || '') === String(teamId),
  )
  const teamTotal = teamStat ? mapMatchTeamTotal(teamStat) : mapMatchTeamTotal({})

  const dt = formatDateTime(detail.scheduledStartTime)
  const label = displayText(detail.stageName || dt.full, '单场数据')

  return {
    matchId: String(detail.matchId),
    label,
    players,
    teamTotal,
  }
}
