import type { V4PublicSeasonItem, V4PublicStageItem } from '@/api/types/v4Public'

export const HOME_SERIES_CODES = ['LEAGUE', 'ARENA', 'CUP', 'OPEN'] as const

export type HomeSeriesCode = (typeof HOME_SERIES_CODES)[number]

const SEASON_STATUS_PRIORITY = [
  'PLAYING',
  'REGISTRATION_OPEN',
  'REGISTRATION_CLOSED',
  'NOT_STARTED',
  'FINISHED',
  'ARCHIVED',
] as const

function statusPriority(status?: string | null): number {
  if (!status?.trim()) return SEASON_STATUS_PRIORITY.length + 1
  const index = SEASON_STATUS_PRIORITY.indexOf(status.trim().toUpperCase() as (typeof SEASON_STATUS_PRIORITY)[number])
  return index >= 0 ? index : SEASON_STATUS_PRIORITY.length + 1
}

function timeValue(value?: string | null): number {
  if (!value) return Number.NEGATIVE_INFINITY
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? Number.NEGATIVE_INFINITY : parsed
}

export function pickCurrentSeason(seasons: V4PublicSeasonItem[]): V4PublicSeasonItem | null {
  if (!seasons.length) return null
  return [...seasons].sort((left, right) => {
    const statusDiff = statusPriority(left.seasonStatus) - statusPriority(right.seasonStatus)
    if (statusDiff !== 0) return statusDiff
    const timeDiff = timeValue(right.matchStartTime) - timeValue(left.matchStartTime)
    if (timeDiff !== 0) return timeDiff
    return Number(right.seasonId || 0) - Number(left.seasonId || 0)
  })[0] ?? null
}

export function pickCurrentSeasonBySeries(
  seasons: V4PublicSeasonItem[],
  seriesCodes: readonly string[] = HOME_SERIES_CODES,
): Record<string, V4PublicSeasonItem | null> {
  const grouped = new Map<string, V4PublicSeasonItem[]>()
  for (const season of seasons) {
    const code = season.seriesCode?.trim().toUpperCase()
    if (!code) continue
    const list = grouped.get(code) ?? []
    list.push(season)
    grouped.set(code, list)
  }
  const result: Record<string, V4PublicSeasonItem | null> = {}
  for (const code of seriesCodes) {
    result[code] = pickCurrentSeason(grouped.get(code) ?? [])
  }
  return result
}

export function pickCurrentStageName(stages: V4PublicStageItem[]): string | null {
  const current = stages.find((stage) => stage.currentStage)
  const name = current?.stageName?.trim()
  return name || null
}
