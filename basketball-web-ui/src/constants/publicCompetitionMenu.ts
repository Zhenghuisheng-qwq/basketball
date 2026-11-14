/** 官网顶部「赛事」Mega Menu 统一配置（桌面与移动端复用） */

import { iconAssets } from '@/constants/iconAssets'

const iconLeague = iconAssets['home/league.png']
const iconArena = iconAssets['home/arena.png']
const iconCup = iconAssets['home/cup.png']
const iconOpen = iconAssets['home/open.png']

export type MegaMenuType = 'event'

/** @deprecated 使用 MegaMenuType */
export type MegaMenuTab = MegaMenuType

export type MenuEntryHighlight = 'primary' | 'secondary'

export interface PublicMenuEntry {
  id: string
  label: string
  hint?: string
  abbrev: string
  icon?: string
  seriesCode?: string
  seasonId?: string
  to: {
    path: string
    query?: Record<string, string>
  }
}

export interface PublicMenuGroup {
  /** vue-i18n key; consumers translate this when the menu renders */
  titleKey: string
  entries: PublicMenuEntry[]
}

const SERIES_ITEMS = [
  { code: 'LEAGUE', label: 'League', abbrev: 'LE', icon: iconLeague },
  { code: 'ARENA', label: 'Arena', abbrev: 'AR', icon: iconArena },
  { code: 'CUP', label: 'Cup', abbrev: 'CU', icon: iconCup },
  { code: 'OPEN', label: 'Open', abbrev: 'OP', icon: iconOpen },
] as const

function eventsSeriesEntry(
  code: string,
  label: string,
  abbrev: string,
  icon: string,
): PublicMenuEntry {
  return {
    id: `events-series-${code}`,
    label,
    abbrev,
    icon,
    seriesCode: code,
    to: { path: '/tournament', query: { series: code } },
  }
}

export function buildCurrentEventMenuEntries(
  seasons: Array<{
    seasonId?: string
    seriesCode?: string
    seasonName?: string | null
    displayTitle?: string | null
    displayTitleEn?: string | null
    seasonStatus?: string | null
  }>,
  options: {
    locale: string
    seriesLabel: (code: string) => string
    statusLabel: (status?: string | null) => string
    emptyHint: string
  },
): PublicMenuEntry[] {
  const grouped = new Map<string, typeof seasons>()
  for (const season of seasons) {
    const code = season.seriesCode?.trim().toUpperCase()
    if (!code) continue
    const list = grouped.get(code) ?? []
    list.push(season)
    grouped.set(code, list)
  }
  const preferEnglish = options.locale.toLowerCase().startsWith('en')
  return SERIES_ITEMS.map((item) => {
    const season = pickMenuSeason(grouped.get(item.code) ?? [])
    const title = seasonTitle(season, preferEnglish)
    const status = season?.seasonStatus ? options.statusLabel(season.seasonStatus) : ''
    const seriesName = options.seriesLabel(item.code)
    const hint = title
      ? [seriesName, status].filter(Boolean).join(' · ')
      : options.emptyHint
    const seasonId = text(season?.seasonId)
    return {
      id: `events-series-${item.code}`,
      label: title || item.label,
      hint,
      abbrev: item.abbrev,
      icon: item.icon,
      seriesCode: item.code,
      seasonId: seasonId || undefined,
      to: seasonId
        ? { path: `/events/${seasonId}` }
        : { path: '/tournament', query: { series: item.code } },
    }
  })
}

function text(value: unknown) {
  return String(value ?? '').trim()
}

function seasonTitle(
  season: { displayTitle?: string | null, displayTitleEn?: string | null, seasonName?: string | null } | null,
  preferEnglish: boolean,
) {
  if (!season) return ''
  const localized = preferEnglish ? text(season.displayTitleEn) : ''
  return localized || text(season.displayTitle) || text(season.seasonName)
}

function pickMenuSeason<T extends { seasonStatus?: string | null, seasonId?: string }>(seasons: T[]): T | null {
  if (!seasons.length) return null
  const rank = [
    'PLAYING',
    'REGISTRATION_OPEN',
    'REGISTRATION_CLOSED',
    'NOT_STARTED',
    'FINISHED',
    'ARCHIVED',
  ]
  return [...seasons].sort((left, right) => {
    const leftRank = rank.indexOf((left.seasonStatus || '').toUpperCase())
    const rightRank = rank.indexOf((right.seasonStatus || '').toUpperCase())
    const statusDiff = (leftRank < 0 ? rank.length : leftRank) - (rightRank < 0 ? rank.length : rightRank)
    if (statusDiff !== 0) return statusDiff
    return Number(right.seasonId || 0) - Number(left.seasonId || 0)
  })[0] ?? null
}

export const competitionMenuGroups: Record<MegaMenuType, PublicMenuGroup[]> = {
  event: [
    {
      titleKey: 'home.seriesMenu',
      entries: SERIES_ITEMS.map((item) => eventsSeriesEntry(item.code, item.label, item.abbrev, item.icon)),
    },
  ],
}
