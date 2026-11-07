import type { Router } from 'vue-router'

export type PlayerDetailTab = 'overview' | 'matches' | 'data' | 'honors' | 'career'
export type PlayerDataMode = 'career' | 'season'

const VALID_TABS: PlayerDetailTab[] = ['overview', 'matches', 'data', 'honors', 'career']
const VALID_DATA_MODES: PlayerDataMode[] = ['career', 'season']

export function normalizePlayerTab(tab: unknown): PlayerDetailTab {
  if (typeof tab === 'string' && VALID_TABS.includes(tab as PlayerDetailTab)) {
    return tab as PlayerDetailTab
  }
  return 'overview'
}

export function normalizeDataMode(mode: unknown): PlayerDataMode {
  if (typeof mode === 'string' && VALID_DATA_MODES.includes(mode as PlayerDataMode)) {
    return mode as PlayerDataMode
  }
  return 'career'
}

/** 路由球员 ID：直接使用后端 Long 字符串，兼容纯数字 */
export function resolvePlayerId(raw: string | number | undefined | null): string | null {
  if (raw === undefined || raw === null || raw === '') return null
  const key = String(raw).trim()
  if (!key) return null
  if (/^\d+$/.test(key)) return key
  return null
}

export function goToPlayerDetail(
  router: Router,
  playerId: string | number,
  tab: PlayerDetailTab = 'overview',
  extraQuery: Record<string, string> = {},
) {
  const resolved = resolvePlayerId(playerId)
  if (!resolved) return
  router.push({
    name: 'PlayerDetail',
    params: { playerId: resolved },
    query: { tab, ...extraQuery },
  })
}

export function replacePlayerDetailTab(
  router: Router,
  tab: PlayerDetailTab,
  extraQuery: Record<string, string> = {},
) {
  router.replace({
    ...router.currentRoute.value,
    query: {
      ...router.currentRoute.value.query,
      tab,
      ...extraQuery,
    },
  })
}
