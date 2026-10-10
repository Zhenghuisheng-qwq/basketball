/** 官网顶部「赛事」Mega Menu 统一配置（桌面与移动端复用） */

import iconLeague from '@/assets/icon/home/league.png'
import iconArena from '@/assets/icon/home/arena.png'
import iconCup from '@/assets/icon/home/cup.png'
import iconOpen from '@/assets/icon/home/open.png'

export type MegaMenuType = 'event'

/** @deprecated 使用 MegaMenuType */
export type MegaMenuTab = MegaMenuType

export type MenuEntryHighlight = 'primary' | 'secondary'

export interface PublicMenuEntry {
  id: string
  label: string
  abbrev: string
  icon?: string
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
    to: { path: '/tournament', query: { series: code } },
  }
}

export const competitionMenuGroups: Record<MegaMenuType, PublicMenuGroup[]> = {
  event: [
    {
      titleKey: 'home.seriesMenu',
      entries: SERIES_ITEMS.map((item) => eventsSeriesEntry(item.code, item.label, item.abbrev, item.icon)),
    },
  ],
}
