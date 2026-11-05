import type { V4PublicSeasonItem } from '@/api/types/v4Public'

export function platformTag(code?: string | null): string {
  const value = String(code || '').trim().toUpperCase()
  if (!value) return ''
  if (value === 'PC') return 'PC'
  if (value === 'PS_XBOX' || value === 'PS' || value === 'XBOX') return '主机'
  return value
}

/** 比赛周期覆盖当前时间的赛季视为执行中；没有周期时才看已写入的 PLAYING。 */
export function isSeasonInProgress(season: V4PublicSeasonItem, now = Date.now()): boolean {
  const start = Date.parse(season.matchStartTime || '')
  const end = Date.parse(season.matchEndTime || '')
  if (!Number.isNaN(start) && !Number.isNaN(end)) {
    return now >= start && now < end
  }
  return String(season.seasonStatus || '').toUpperCase() === 'PLAYING'
}

export interface HallPublishedRecord {
  seasonId?: string | null
  tournamentId?: string | null
  awardDate?: string | null
}

/** 默认展示最近发布且仍在比赛周期内的赛季；没有执行中的赛季时，退回最近发布的一届。 */
export function pickRunningSeason(
  seasons: V4PublicSeasonItem[],
  now = Date.now(),
): V4PublicSeasonItem | null {
  if (!seasons.length) return null
  const running = seasons.filter((season) => isSeasonInProgress(season, now))
  const pool = running.length ? running : seasons
  return [...pool].sort((left, right) => Number(right.seasonId || 0) - Number(left.seasonId || 0))[0] ?? null
}

/** 有已发布荣誉时，默认落在荣誉日期最新的那一届，避免停在还没有荣誉的执行中赛季。 */
export function pickLatestPublishedSeason(
  seasons: V4PublicSeasonItem[],
  records: HallPublishedRecord[],
): V4PublicSeasonItem | null {
  const ranked = [...records].sort((left, right) => {
    const dateDiff = String(right.awardDate || '').localeCompare(String(left.awardDate || ''))
    if (dateDiff !== 0) return dateDiff
    return Number(right.seasonId || 0) - Number(left.seasonId || 0)
  })
  for (const record of ranked) {
    const seasonId = String(record.seasonId || '')
    if (!seasonId) continue
    const season = seasons.find((item) => String(item.seasonId || '') === seasonId)
    if (season) return season
  }
  return pickRunningSeason(seasons)
}

export function seasonsNewestFirst(seasons: V4PublicSeasonItem[]): V4PublicSeasonItem[] {
  return [...seasons].sort((left, right) => Number(right.seasonId || 0) - Number(left.seasonId || 0))
}

export function seasonBelongsToSeries(
  seasons: V4PublicSeasonItem[],
  seasonId: string,
  seriesId: string,
): boolean {
  if (!seasonId || seasonId === 'ALL' || !seriesId || seriesId === 'ALL') return true
  const picked = seasons.find((item) => String(item.seasonId || '') === seasonId)
  if (!picked) return true
  return String(picked.seriesId || '') === seriesId
}
