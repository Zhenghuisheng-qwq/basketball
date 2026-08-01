import { computed, provide, ref, watch, type InjectionKey, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  getPublicTeamDetail,
  getPublicTeamRanking,
  getPublicTeamRoster,
  getPublicTeamStat,
} from '@/api/publicTeam'
import type {
  PublicTeamDetailVo,
  PublicTeamRankItemVo,
  PublicTeamRosterItemVo,
  PublicTeamSeasonStatVo,
} from '@/api/types/public'
import { descriptionToText, displayText, toNumber } from '@/utils/publicDisplay'
import type { TeamBase } from '@/types/team'

export interface PublicTeamPageContext {
  teamId: Ref<string>
  detail: Ref<PublicTeamDetailVo | null>
  roster: Ref<PublicTeamRosterItemVo[]>
  stats: Ref<PublicTeamSeasonStatVo[]>
  ranking: Ref<PublicTeamRankItemVo[]>
  teamView: Ref<TeamBase | null>
  state: Ref<'loading' | 'success' | 'empty' | 'error'>
  errorMessage: Ref<string>
  reload: () => Promise<void>
}

export const PUBLIC_TEAM_PAGE_KEY: InjectionKey<PublicTeamPageContext> = Symbol('publicTeamPage')

export function usePublicTeamPage() {
  const route = useRoute()
  const teamId = computed(() => String(route.params.teamId || ''))

  const detail = ref<PublicTeamDetailVo | null>(null)
  const roster = ref<PublicTeamRosterItemVo[]>([])
  const stats = ref<PublicTeamSeasonStatVo[]>([])
  const ranking = ref<PublicTeamRankItemVo[]>([])
  const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
  const errorMessage = ref('')
  let reqId = 0

  const teamView = computed<TeamBase | null>(() => {
    if (!detail.value) return null
    const d = detail.value
    const stat = stats.value[0]
    const rank = ranking.value[0]
    const name = displayText(d.teamName, '未知球队')
    return {
      id: d.teamId,
      code: displayText(d.shortName || d.teamNo || name, name),
      name,
      shortName: displayText(d.shortName || name.slice(0, 3), name.slice(0, 3)),
      logoUrl: d.logoUrl?.trim() || '',
      largeLogoUrl: d.logoUrl?.trim() || '',
      captainName:
        roster.value.find((item) => (item.roleType || '').toUpperCase().includes('CAPTAIN'))
          ?.playerName || '--',
      platform: '',
      currentStage: 'regular',
      rosterCount: toNumber(d.currentMemberCount, roster.value.length),
      verified: toNumber(d.teamStatus, 0) === 1,
      seasonName: displayText(stat?.seasonId ? `赛季 ${stat.seasonId}` : '公开赛季', '公开赛季'),
      seasonYear: new Date().getFullYear(),
      description: descriptionToText(d.description) || '暂无球队简介',
      wins: toNumber(stat?.wins, 0),
      losses: toNumber(stat?.losses, 0),
      ranking: toNumber(rank?.rankNo, 0),
      themeColor: '#c9a360',
      secondaryColor: '#111111',
    }
  })

  async function load() {
    const id = teamId.value
    const current = ++reqId
    if (!id || !/^\d+$/.test(id)) {
      detail.value = null
      state.value = 'error'
      errorMessage.value = '无效的球队 ID'
      return
    }
    state.value = 'loading'
    errorMessage.value = ''
    try {
      const [detailRes, rosterRes, statRes, rankRes] = await Promise.all([
        getPublicTeamDetail(id),
        getPublicTeamRoster(id),
        getPublicTeamStat(id),
        getPublicTeamRanking(id),
      ])
      if (current !== reqId) return
      detail.value = detailRes.data ?? null
      roster.value = rosterRes.data ?? []
      stats.value = statRes.data ?? []
      ranking.value = rankRes.data ?? []
      state.value = detail.value ? 'success' : 'empty'
      if (!detail.value) errorMessage.value = '球队不存在或未公开'
    } catch (err) {
      if (current !== reqId) return
      detail.value = null
      roster.value = []
      stats.value = []
      ranking.value = []
      state.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '加载球队失败'
    }
  }

  watch(teamId, () => {
    void load()
  }, { immediate: true })

  const ctx: PublicTeamPageContext = {
    teamId: teamId as unknown as Ref<string>,
    detail,
    roster,
    stats,
    ranking,
    teamView,
    state,
    errorMessage,
    reload: load,
  }

  provide(PUBLIC_TEAM_PAGE_KEY, ctx)
  return ctx
}
