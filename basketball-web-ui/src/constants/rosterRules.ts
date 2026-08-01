export const CNPA_MIN_ROSTER_COUNT = 3
export const CNPA_MAX_ROSTER_COUNT = 8
export const CNPA_TEAM_MAX_MEMBER_COUNT = 8
export const CNPA_REQUIRED_STARTER_COUNT = 5

/** 5 人阵容无替补；8 人阵容最多 3 替补 */
export const CNPA_MIN_BENCH_COUNT = 0
export const CNPA_MAX_BENCH_COUNT = CNPA_MAX_ROSTER_COUNT - CNPA_REQUIRED_STARTER_COUNT

function defaultMaxForMin(min: number) {
  if (min <= 3) return 5
  if (min <= 4) return 6
  return 8
}

/** 将赛季/赛事返回的阵容限额归一化（支持 3v3 / 4v4 / 5v5） */
export function resolveSeasonRosterLimits(
  min?: number | null,
  max?: number | null,
  teamPlayCount?: number | null,
) {
  const baseMin = teamPlayCount && teamPlayCount > 0 ? teamPlayCount : CNPA_REQUIRED_STARTER_COUNT
  const normalizedMax =
    max == null
      ? defaultMaxForMin(min ?? baseMin)
      : Math.min(Math.max(max, CNPA_MIN_ROSTER_COUNT), CNPA_MAX_ROSTER_COUNT)
  const normalizedMin =
    min == null
      ? Math.min(baseMin, normalizedMax)
      : Math.max(CNPA_MIN_ROSTER_COUNT, Math.min(min, normalizedMax))
  return {
    minRosterCount: normalizedMin,
    maxRosterCount: normalizedMax,
    teamPlayCount: teamPlayCount && teamPlayCount > 0 ? teamPlayCount : normalizedMin,
  }
}

export const STARTING_POSITION_CODES = ['PG', 'SG', 'SF', 'PF', 'C'] as const

export type StartingPositionCode = (typeof STARTING_POSITION_CODES)[number]

/** 按赛制场上人数取首发位置集合（3v3 取前 3 个，以此类推） */
export function resolveStartingPositionCodes(teamPlayCount: number): readonly string[] {
  const count = Math.max(1, Math.min(teamPlayCount, STARTING_POSITION_CODES.length))
  return STARTING_POSITION_CODES.slice(0, count)
}

export const POSITION_LABELS: Record<string, string> = {
  PG: '控球后卫',
  SG: '得分后卫',
  SF: '小前锋',
  PF: '大前锋',
  C: '中锋',
}

export function formatPositionLabel(code?: string | null): string {
  if (!code) return '未设置'
  const normalized = code.trim().toUpperCase()
  const label = POSITION_LABELS[normalized]
  return label ? `${normalized} ${label}` : normalized
}

export function formatFormatDisplay(formatCode?: string | null, teamPlayCount?: number | null): string {
  if (teamPlayCount && teamPlayCount > 0) {
    return `${teamPlayCount}v${teamPlayCount}`
  }
  if (!formatCode) return '5v5'
  const normalized = formatCode.trim().toUpperCase()
  if (normalized.includes('V')) {
    return normalized.toLowerCase()
  }
  const count = normalized.startsWith('3') ? 3 : normalized.startsWith('4') ? 4 : 5
  return `${count}v${count}`
}
