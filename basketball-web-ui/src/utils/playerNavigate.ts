import type { Router } from 'vue-router'

export type PlayerDetailTab = 'overview' | 'matches' | 'data' | 'career'
export type PlayerDataMode = 'career' | 'season'

const VALID_TABS: PlayerDetailTab[] = ['overview', 'matches', 'data', 'career']
const VALID_DATA_MODES: PlayerDataMode[] = ['career', 'season']

/** 数据中心 10 名球员 canonical id：10001 - 10010 */
export const CORE_PLAYER_IDS = [10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 10009, 10010] as const

const aliasToPlayerId: Record<string, number> = {
  '10001': 10001,
  '10002': 10002,
  '10003': 10003,
  '10004': 10004,
  '10005': 10005,
  '10006': 10006,
  '10007': 10007,
  '10008': 10008,
  '10009': 10009,
  '10010': 10010,
  'rng-p01': 10001,
  'rng_titanx': 10001,
  'rng_01': 10001,
  'titan-p01': 10002,
  'titan_king': 10002,
  'phx_fire': 10003,
  'phoenix_fire': 10003,
  'drg_lion': 10004,
  'dragon_lion': 10004,
  'leg_zeus': 10005,
  'legend_zeus': 10005,
  'wolves_ace': 10006,
  'allstar_melo': 10007,
  'all-star_melo': 10007,
  'omg_nova': 10008,
  'omega_nova': 10008,
  'beast_hawk': 10009,
  'unity_jay': 10010,
  knight: 10002,
  elk: 10006,
}

;([
  ['RNG_TitanX', 10001],
  ['TITAN_King', 10002],
  ['PHX_Fire', 10003],
  ['DRG_Lion', 10004],
  ['LEG_Zeus', 10005],
  ['WOLVES_Ace', 10006],
  ['ALLSTAR_Melo', 10007],
  ['OMG_Nova', 10008],
  ['BEAST_Hawk', 10009],
  ['UNITY_Jay', 10010],
  ['RNG_01', 10001],
] as Array<[string, number]>).forEach(([name, id]) => {
  aliasToPlayerId[String(name).toLowerCase()] = id
})

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

export function resolvePlayerId(raw: string | number | undefined | null): number | null {
  if (raw === undefined || raw === null || raw === '') return null
  const key = String(raw).trim().toLowerCase()
  if (aliasToPlayerId[key] !== undefined) return aliasToPlayerId[key]
  const numeric = Number(key)
  if (Number.isInteger(numeric) && numeric >= 10001 && numeric <= 10010) return numeric
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
    params: { playerId: String(resolved) },
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
