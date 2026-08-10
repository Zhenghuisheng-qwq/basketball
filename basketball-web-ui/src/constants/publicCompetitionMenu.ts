/** 官网顶部「赛事 / 数据」Mega Menu 统一配置（桌面与移动端复用） */

export type MegaMenuType = 'event' | 'data'

/** @deprecated 使用 MegaMenuType */
export type MegaMenuTab = MegaMenuType

export type MenuEntryHighlight = 'primary' | 'secondary'

export interface PublicMenuEntry {
  id: string
  label: string
  abbrev: string
  to: {
    path: string
    query?: Record<string, string>
  }
}

export interface PublicMenuGroup {
  title: string
  entries: PublicMenuEntry[]
}

const SERIES_ITEMS = [
  { code: 'LEAGUE', label: 'League', abbrev: 'LE' },
  { code: 'ARENA', label: 'Arena', abbrev: 'AR' },
  { code: 'CUP', label: 'Cup', abbrev: 'CU' },
  { code: 'OPEN', label: 'Open', abbrev: 'OP' },
] as const

function eventsSeriesEntry(code: string, label: string, abbrev: string): PublicMenuEntry {
  return {
    id: `events-series-${code}`,
    label,
    abbrev,
    to: { path: '/tournament', query: { series: code } },
  }
}

function dataSeriesEntry(code: string, label: string, abbrev: string): PublicMenuEntry {
  return {
    id: `data-series-${code}`,
    label,
    abbrev,
    to: { path: '/data', query: { series: code } },
  }
}

export const competitionMenuGroups: Record<MegaMenuType, PublicMenuGroup[]> = {
  event: [
    {
      title: '赛事系列',
      entries: SERIES_ITEMS.map((item) => eventsSeriesEntry(item.code, item.label, item.abbrev)),
    },
  ],
  data: [
    {
      title: '数据分类',
      entries: [
        {
          id: 'data-standing',
          label: '积分榜',
          abbrev: '积',
          to: { path: '/data', query: { tab: 'standing' } },
        },
        {
          id: 'data-team',
          label: '球队统计',
          abbrev: '队',
          to: { path: '/data', query: { tab: 'team' } },
        },
        {
          id: 'data-player',
          label: '球员统计',
          abbrev: '员',
          to: { path: '/data', query: { tab: 'player' } },
        },
        {
          id: 'data-rank',
          label: '排行榜',
          abbrev: '榜',
          to: { path: '/data', query: { tab: 'rank' } },
        },
      ],
    },
    {
      title: '赛事系列',
      entries: SERIES_ITEMS.map((item) => dataSeriesEntry(item.code, item.label, item.abbrev)),
    },
  ],
}

export const DATA_TAB_VALUES = ['standing', 'team', 'player', 'rank'] as const

export type DataTabValue = (typeof DATA_TAB_VALUES)[number]
