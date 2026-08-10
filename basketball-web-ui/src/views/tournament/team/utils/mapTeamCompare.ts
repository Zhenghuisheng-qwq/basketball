import type { V4PublicTeamStatItem } from '@/api/types/v4Public'
import { toNumber } from '@/utils/publicDisplay'
import type { TeamCompareBarStat, TeamCompareData } from '@/types/team'

function normalizeWinRate(wins: number, games: number): number {
  if (games <= 0) return 0
  return (wins / games) * 100
}

function statOverview(stat?: V4PublicTeamStatItem | null) {
  const games = toNumber(stat?.matchCount, 0)
  const wins = toNumber(stat?.winCount, 0)
  const avgPoints = toNumber(stat?.avgPointsFor, 0)
  const avgPointsAgainst = toNumber(stat?.avgPointsAgainst, 0)
  return {
    winRate: normalizeWinRate(wins, games),
    avgPoints,
    avgPointsAgainst,
    avgPointDiff: avgPoints - avgPointsAgainst,
    games,
    wins,
    losses: toNumber(stat?.loseCount, 0),
  }
}

function createBarStat(
  key: string,
  label: string,
  left: number,
  right: number,
  higherIsBetter: boolean,
): TeamCompareBarStat {
  return { key, label, left, right, higherIsBetter }
}

function avgPerGame(total: unknown, games: number): number {
  if (games <= 0) return 0
  return toNumber(total, 0) / games
}

export function buildTeamCompareFromV4Stats(
  left?: V4PublicTeamStatItem | null,
  right?: V4PublicTeamStatItem | null,
): TeamCompareData {
  const leftGames = toNumber(left?.matchCount, 0)
  const rightGames = toNumber(right?.matchCount, 0)

  return {
    overview: statOverview(left),
    basics: [
      createBarStat(
        'rebounds',
        '场均篮板',
        avgPerGame(left?.rebound, leftGames),
        avgPerGame(right?.rebound, rightGames),
        true,
      ),
      createBarStat(
        'assists',
        '场均助攻',
        avgPerGame(left?.assist, leftGames),
        avgPerGame(right?.assist, rightGames),
        true,
      ),
      createBarStat(
        'steals',
        '场均抢断',
        avgPerGame(left?.steal, leftGames),
        avgPerGame(right?.steal, rightGames),
        true,
      ),
      createBarStat(
        'blocks',
        '场均盖帽',
        avgPerGame(left?.block, leftGames),
        avgPerGame(right?.block, rightGames),
        true,
      ),
      createBarStat(
        'turnovers',
        '场均失误',
        avgPerGame(left?.turnover, leftGames),
        avgPerGame(right?.turnover, rightGames),
        false,
      ),
      createBarStat('fouls', '场均犯规', 0, 0, false),
    ],
    shooting: [
      createBarStat('fgPct', '投篮命中率', 0, 0, true),
      createBarStat('tpPct', '三分命中率', 0, 0, true),
      createBarStat('ftPct', '罚球命中率', 0, 0, true),
    ],
  }
}

export function compareOverviewValuesFromV4Stats(
  left?: V4PublicTeamStatItem | null,
  right?: V4PublicTeamStatItem | null,
) {
  return {
    left: statOverview(left),
    right: statOverview(right),
  }
}

/** @deprecated use buildTeamCompareFromV4Stats */
export function mapPublicTeamCompareVo(vo: {
  leftStat?: { avgPoints?: unknown; avgPointsAgainst?: unknown; winRate?: unknown; avgRebounds?: unknown; avgAssists?: unknown; avgSteals?: unknown; avgBlocks?: unknown; avgTurnovers?: unknown; fieldGoalPct?: unknown; threePointPct?: unknown; freeThrowPct?: unknown } | null
  rightStat?: { avgPoints?: unknown; avgPointsAgainst?: unknown; winRate?: unknown; avgRebounds?: unknown; avgAssists?: unknown; avgSteals?: unknown; avgBlocks?: unknown; avgTurnovers?: unknown; fieldGoalPct?: unknown; threePointPct?: unknown; freeThrowPct?: unknown } | null
}): TeamCompareData {
  const left = vo.leftStat
  const right = vo.rightStat
  return {
    overview: {
      winRate: toNumber(left?.winRate, 0) <= 1 ? toNumber(left?.winRate, 0) * 100 : toNumber(left?.winRate, 0),
      avgPoints: toNumber(left?.avgPoints, 0),
      avgPointsAgainst: toNumber(left?.avgPointsAgainst, 0),
      avgPointDiff: toNumber(left?.avgPoints, 0) - toNumber(left?.avgPointsAgainst, 0),
    },
    basics: [
      createBarStat('rebounds', '场均篮板', toNumber(left?.avgRebounds, 0), toNumber(right?.avgRebounds, 0), true),
      createBarStat('assists', '场均助攻', toNumber(left?.avgAssists, 0), toNumber(right?.avgAssists, 0), true),
      createBarStat('steals', '场均抢断', toNumber(left?.avgSteals, 0), toNumber(right?.avgSteals, 0), true),
      createBarStat('blocks', '场均盖帽', toNumber(left?.avgBlocks, 0), toNumber(right?.avgBlocks, 0), true),
      createBarStat('turnovers', '场均失误', toNumber(left?.avgTurnovers, 0), toNumber(right?.avgTurnovers, 0), false),
      createBarStat('fouls', '场均犯规', 0, 0, false),
    ],
    shooting: [
      createBarStat('fgPct', '投篮命中率', 0, 0, true),
      createBarStat('tpPct', '三分命中率', 0, 0, true),
      createBarStat('ftPct', '罚球命中率', 0, 0, true),
    ],
  }
}

/** @deprecated use compareOverviewValuesFromV4Stats */
export function compareOverviewValues(vo: {
  leftStat?: { avgPoints?: unknown; avgPointsAgainst?: unknown; winRate?: unknown } | null
  rightStat?: { avgPoints?: unknown; avgPointsAgainst?: unknown; winRate?: unknown } | null
}) {
  const left = vo.leftStat
  const right = vo.rightStat
  return {
    left: {
      winRate: toNumber(left?.winRate, 0) <= 1 ? toNumber(left?.winRate, 0) * 100 : toNumber(left?.winRate, 0),
      avgPoints: toNumber(left?.avgPoints, 0),
      avgPointsAgainst: toNumber(left?.avgPointsAgainst, 0),
      avgPointDiff: toNumber(left?.avgPoints, 0) - toNumber(left?.avgPointsAgainst, 0),
    },
    right: {
      winRate: toNumber(right?.winRate, 0) <= 1 ? toNumber(right?.winRate, 0) * 100 : toNumber(right?.winRate, 0),
      avgPoints: toNumber(right?.avgPoints, 0),
      avgPointsAgainst: toNumber(right?.avgPointsAgainst, 0),
      avgPointDiff: toNumber(right?.avgPoints, 0) - toNumber(right?.avgPointsAgainst, 0),
    },
  }
}

export function v4CompareSideMeta(stat?: V4PublicTeamStatItem | null) {
  const overview = statOverview(stat)
  return {
    games: overview.games,
    wins: overview.wins,
    losses: overview.losses,
  }
}
