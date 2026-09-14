import type { Standing } from '../types/events'

/**
 * Qualification Split ≠ Playoff Bracket Half Split
 *
 * This file only splits Regular Season standings:
 *   qualificationCount / qualifyCount → who advances.
 *
 * Playoff left/right half size is NOT computed here.
 * See `playoffBracketDisplay.ts`: halfSize = playoffParticipantCount / 2.
 */

export type StageProgressLayout = 'cutoff' | 'all-advance'

export type ZoneCopy = {
  title: string
  hint?: string
  en?: string
}

export type StandingZoneTone = 'playoff' | 'qualify'

export function resolveQualifyCount(
  qualifyCount?: number | null,
  fallback?: number | null,
): number {
  const primary = Number(qualifyCount)
  if (Number.isFinite(primary) && primary > 0) return primary
  const secondary = Number(fallback)
  if (Number.isFinite(secondary) && secondary > 0) return secondary
  return 0
}

export function stageProgressLayout(rowCount: number, qualifyCount: number): StageProgressLayout {
  if (qualifyCount > 0 && qualifyCount < rowCount) return 'cutoff'
  return 'all-advance'
}

export function splitAt(rowCount: number, qualifyCount: number, layout: StageProgressLayout): number {
  if (layout === 'cutoff') return qualifyCount
  if (rowCount <= 1) return rowCount
  return Math.ceil(rowCount / 2)
}

export function leftZoneCopy(
  layout: StageProgressLayout,
  qualifyCount: number,
  rowCount: number,
  splitIndex: number,
  tone: StandingZoneTone = 'playoff',
): ZoneCopy {
  const qualify = tone === 'qualify'
  if (layout === 'cutoff' && qualifyCount > 0) {
    return {
      title: qualify ? `晋级区（前${qualifyCount}名晋级）` : `季后赛区（前${qualifyCount}名晋级）`,
      hint: qualify ? `前${qualifyCount}名晋级下一阶段` : `前${qualifyCount}名晋级季后赛`,
      en: qualify ? 'QUALIFY ZONE' : 'PLAYOFF ZONE',
    }
  }
  const end = Math.max(1, Math.min(splitIndex, rowCount))
  if (qualifyCount > 0 && qualifyCount >= rowCount) {
    return {
      title: qualify ? `晋级区（前${qualifyCount}名晋级）` : `季后赛区（前${qualifyCount}名晋级）`,
      hint: qualify ? `全部${rowCount}支晋级下一阶段` : `全部${rowCount}支晋级季后赛`,
      en: qualify ? 'QUALIFY ZONE' : 'PLAYOFF ZONE',
    }
  }
  return { title: qualify ? `晋级区（1-${end}名）` : `季后赛区（1-${end}名）` }
}

export function rightZoneCopy(
  layout: StageProgressLayout,
  qualifyCount: number,
  rowCount: number,
  splitIndex: number,
  tone: StandingZoneTone = 'playoff',
): ZoneCopy {
  const qualify = tone === 'qualify'
  if (layout === 'cutoff' && qualifyCount > 0) {
    return {
      title: qualify ? `非晋级区（${qualifyCount + 1}名及以后）` : `非季后赛区（${qualifyCount + 1}名及以后）`,
      en: qualify ? 'OUTSIDE QUALIFY' : 'OUTSIDE PLAYOFFS',
    }
  }
  const start = Math.min(splitIndex + 1, rowCount)
  if (qualifyCount > 0 && qualifyCount >= rowCount) {
    return {
      title: qualify ? `晋级区（全部已晋级）` : `季后赛区（全部已晋级）`,
      en: qualify ? 'QUALIFY ZONE' : 'PLAYOFF ZONE',
    }
  }
  return { title: `其余排名（${start}名及以后）` }
}

export function splitLeftRows(rows: Standing[], index: number): Standing[] {
  return rows.slice(0, index)
}

export function splitRightRows(rows: Standing[], index: number): Standing[] {
  return rows.slice(index)
}
