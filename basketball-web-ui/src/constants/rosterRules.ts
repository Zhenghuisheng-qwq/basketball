export const CNPA_MIN_ROSTER_COUNT = 8
export const CNPA_MAX_ROSTER_COUNT = 8
export const CNPA_TEAM_MAX_MEMBER_COUNT = 8
export const CNPA_MIN_BENCH_COUNT = 3
export const CNPA_REQUIRED_STARTER_COUNT = 5

export const STARTING_POSITION_CODES = ['PG', 'SG', 'SF', 'PF', 'C'] as const

export type StartingPositionCode = (typeof STARTING_POSITION_CODES)[number]

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
