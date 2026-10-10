import type { Standing } from '../types/events'
import { translate } from '@/i18n'

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
      title: translate(qualify ? 'events.zone.qualifyTitle' : 'events.zone.playoffTitle', { count: qualifyCount }),
      hint: translate(qualify ? 'events.zone.qualifyHint' : 'events.zone.playoffHint', { count: qualifyCount }),
      en: qualify ? 'QUALIFY ZONE' : 'PLAYOFF ZONE',
    }
  }
  const end = Math.max(1, Math.min(splitIndex, rowCount))
  if (qualifyCount > 0 && qualifyCount >= rowCount) {
    return {
      title: translate(qualify ? 'events.zone.qualifyTitle' : 'events.zone.playoffTitle', { count: qualifyCount }),
      hint: translate(qualify ? 'events.zone.qualifyAll' : 'events.zone.playoffAll', { count: rowCount }),
      en: qualify ? 'QUALIFY ZONE' : 'PLAYOFF ZONE',
    }
  }
  return {
    title: translate(qualify ? 'events.zone.qualifyRange' : 'events.zone.playoffRange', { end }),
  }
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
      title: translate(qualify ? 'events.zone.qualifyAfter' : 'events.zone.playoffAfter', { start: qualifyCount + 1 }),
      en: qualify ? 'OUTSIDE QUALIFY' : 'OUTSIDE PLAYOFFS',
    }
  }
  const start = Math.min(splitIndex + 1, rowCount)
  if (qualifyCount > 0 && qualifyCount >= rowCount) {
    return {
      title: translate(qualify ? 'events.zone.qualifyAllAdvanced' : 'events.zone.playoffAllAdvanced'),
      en: qualify ? 'QUALIFY ZONE' : 'PLAYOFF ZONE',
    }
  }
  return { title: translate('events.zone.rest', { start }) }
}

export function splitLeftRows(rows: Standing[], index: number): Standing[] {
  return rows.slice(0, index)
}

export function splitRightRows(rows: Standing[], index: number): Standing[] {
  return rows.slice(index)
}
