import type { PublicTeamCompareVo, PublicTeamSeasonStatVo } from '@/api/types/public'
import { toNumber } from '@/utils/publicDisplay'
import type { TeamCompareBarStat, TeamCompareData } from '@/types/team'

function normalizeWinRate(value: unknown): number {
  const num = toNumber(value, 0)
  return num <= 1 ? num * 100 : num
}

function normalizePct(value: unknown): number {
  const num = toNumber(value, 0)
  return num <= 1 ? num * 100 : num
}

function statOverview(stat?: PublicTeamSeasonStatVo | null) {
  const avgPoints = toNumber(stat?.avgPoints, 0)
  const avgPointsAgainst = toNumber(stat?.avgPointsAgainst, 0)
  return {
    winRate: normalizeWinRate(stat?.winRate),
    avgPoints,
    avgPointsAgainst,
    avgPointDiff: avgPoints - avgPointsAgainst,
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

export function mapPublicTeamCompareVo(vo: PublicTeamCompareVo): TeamCompareData {
  const left = vo.leftStat
  const right = vo.rightStat

  return {
    overview: statOverview(left),
    basics: [
      createBarStat(
        'rebounds',
        '场均篮板',
        toNumber(left?.avgRebounds, 0),
        toNumber(right?.avgRebounds, 0),
        true,
      ),
      createBarStat(
        'assists',
        '场均助攻',
        toNumber(left?.avgAssists, 0),
        toNumber(right?.avgAssists, 0),
        true,
      ),
      createBarStat(
        'steals',
        '场均抢断',
        toNumber(left?.avgSteals, 0),
        toNumber(right?.avgSteals, 0),
        true,
      ),
      createBarStat(
        'blocks',
        '场均盖帽',
        toNumber(left?.avgBlocks, 0),
        toNumber(right?.avgBlocks, 0),
        true,
      ),
      createBarStat(
        'turnovers',
        '场均失误',
        toNumber(left?.avgTurnovers, 0),
        toNumber(right?.avgTurnovers, 0),
        false,
      ),
      createBarStat('fouls', '场均犯规', 0, 0, false),
    ],
    shooting: [
      createBarStat(
        'fgPct',
        '投篮命中率',
        normalizePct(left?.fieldGoalPct),
        normalizePct(right?.fieldGoalPct),
        true,
      ),
      createBarStat(
        'tpPct',
        '三分命中率',
        normalizePct(left?.threePointPct),
        normalizePct(right?.threePointPct),
        true,
      ),
      createBarStat(
        'ftPct',
        '罚球命中率',
        normalizePct(left?.freeThrowPct),
        normalizePct(right?.freeThrowPct),
        true,
      ),
    ],
  }
}

export function compareOverviewValues(vo: PublicTeamCompareVo) {
  return {
    left: statOverview(vo.leftStat),
    right: statOverview(vo.rightStat),
  }
}
