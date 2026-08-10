import { computed, provide, ref, watch, type InjectionKey, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  getV4PublicDivisionDetail,
  getV4PublicSeasonDetail,
  getV4PublicStanding,
  getV4PublicStageList,
  getV4PublicTeamDetail,
  getV4PublicTeamStat,
} from '@/api/v4Public'
import type {
  PublicTeamDetailVo,
  PublicTeamRankItemVo,
  PublicTeamRosterItemVo,
  PublicTeamSeasonStatVo,
} from '@/api/types/public'
import type { V4PublicDivisionDetail } from '@/api/types/v4Public'
import { descriptionToText, displayText, toNumber } from '@/utils/publicDisplay'
import { v4PlatformLabel } from '@/utils/v4Display'
import type { TeamBase, TeamStage } from '@/types/team'
import {
  mapV4PlayerToRoster,
  mapV4StandingToRank,
  mapV4TeamDetailToPublic,
  mapV4TeamStatToSeasonStat,
} from '../utils/mapV4PublicTeam'
import { resolveEventTeamDivisionId } from '../utils/resolveTeamDivision'

export interface PublicTeamPageContext {
  teamId: Ref<string>
  divisionId: Ref<string>
  seasonId: Ref<string>
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

function resolveCurrentStage(stageName?: string | null): TeamStage {
  const text = (stageName || '').toLowerCase()
  if (text.includes('playoff') || text.includes('淘汰') || text.includes('季后')) {
    return 'playoffs'
  }
  return 'regular'
}

export function usePublicTeamPage() {
  const route = useRoute()
  const teamId = computed(() => String(route.params.teamId || ''))
  const queryDivisionId = computed(() => String(route.query.divisionId || ''))
  const querySeasonId = computed(() => String(route.query.seasonId || ''))

  const divisionId = ref('')
  const seasonId = ref('')
  const divisionDetail = ref<V4PublicDivisionDetail | null>(null)
  const seasonTitle = ref('')

  const detail = ref<PublicTeamDetailVo | null>(null)
  const roster = ref<PublicTeamRosterItemVo[]>([])
  const stats = ref<PublicTeamSeasonStatVo[]>([])
  const ranking = ref<PublicTeamRankItemVo[]>([])
  const currentStageName = ref('')
  const captainName = ref('--')
  const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
  const errorMessage = ref('')
  let reqId = 0

  const teamView = computed<TeamBase | null>(() => {
    if (!detail.value) return null
    const d = detail.value
    const stat = stats.value[0]
    const rank = ranking.value[0]
    const name = displayText(d.teamName, '未知球队')
    const captainFromRoster =
      roster.value.find((item) => (item.roleType || '').toUpperCase().includes('CAPTAIN'))
        ?.playerName || ''
    const wins = toNumber(stat?.wins, 0)
    const losses = toNumber(stat?.losses, 0)

    return {
      id: d.teamId,
      code: displayText(d.shortName || name, name),
      name,
      shortName: displayText(d.shortName || name.slice(0, 3), name.slice(0, 3)),
      logoUrl: d.logoUrl?.trim() || '',
      largeLogoUrl: d.logoUrl?.trim() || '',
      captainName: displayText(captainName.value || captainFromRoster, '--'),
      platform: v4PlatformLabel(d.platformCode),
      currentStage: resolveCurrentStage(currentStageName.value),
      rosterCount: toNumber(d.currentMemberCount, roster.value.length),
      verified: toNumber(d.teamStatus, 0) === 1,
      seasonName: displayText(
        seasonTitle.value || d.seasonName || divisionDetail.value?.seasonTitle,
        '公开赛季',
      ),
      seasonYear: new Date().getFullYear(),
      description: descriptionToText(d.description) || '暂无球队简介',
      wins,
      losses,
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
      divisionId.value = ''
      seasonId.value = ''
      state.value = 'error'
      errorMessage.value = '无效的球队 ID'
      return
    }

    state.value = 'loading'
    errorMessage.value = ''
    divisionId.value = ''
    seasonId.value = ''
    divisionDetail.value = null
    seasonTitle.value = ''
    currentStageName.value = ''

    try {
      const detailRes = await getV4PublicTeamDetail(id)
      if (current !== reqId) return

      const teamDetail = detailRes.data
      captainName.value = displayText(teamDetail?.captainName, '--')
      if (!teamDetail) {
        detail.value = null
        roster.value = []
        stats.value = []
        ranking.value = []
        state.value = 'empty'
        errorMessage.value = '球队不存在或未公开'
        return
      }

      const resolvedDivisionId = await resolveEventTeamDivisionId(id, {
        divisionId: queryDivisionId.value,
        seasonId: querySeasonId.value,
      })
      if (current !== reqId) return

      if (!resolvedDivisionId) {
        detail.value = mapV4TeamDetailToPublic(teamDetail)
        roster.value = (teamDetail.players ?? []).map(mapV4PlayerToRoster)
        stats.value = []
        ranking.value = []
        state.value = 'success'
        return
      }

      divisionId.value = resolvedDivisionId

      const [divisionRes, standingRes, teamStatRes, stageRes] = await Promise.all([
        getV4PublicDivisionDetail(resolvedDivisionId),
        getV4PublicStanding(resolvedDivisionId).catch(() => ({ data: [] })),
        getV4PublicTeamStat(resolvedDivisionId).catch(() => ({ data: [] })),
        getV4PublicStageList(resolvedDivisionId).catch(() => ({ data: [] })),
      ])
      if (current !== reqId) return

      divisionDetail.value = divisionRes.data ?? null
      seasonId.value = String(divisionDetail.value?.seasonId || '')
      seasonTitle.value = displayText(
        divisionDetail.value?.seasonTitle,
        divisionDetail.value?.divisionName || '',
      )

      if (seasonId.value) {
        try {
          const seasonRes = await getV4PublicSeasonDetail(seasonId.value)
          if (current !== reqId) return
          seasonTitle.value = displayText(
            seasonRes.data?.displayTitle || seasonRes.data?.seasonName,
            seasonTitle.value,
          )
        } catch {
          // keep division title fallback
        }
      }

      const standingRows = standingRes.data ?? []
      const teamStatRows = teamStatRes.data ?? []
      const teamStanding = standingRows.find(
        (row) => String(row.eventTeamId) === String(id),
      )
      const teamStat = teamStatRows.find((row) => String(row.eventTeamId) === String(id))

      const currentStage = (stageRes.data ?? []).find((stage) => stage.currentStage)
      currentStageName.value = displayText(currentStage?.stageName, '')

      detail.value = mapV4TeamDetailToPublic(teamDetail, {
        divisionId: resolvedDivisionId,
        divisionName: divisionDetail.value?.divisionName,
        seasonId: seasonId.value,
        seasonTitle: seasonTitle.value,
      })
      roster.value = (teamDetail.players ?? []).map(mapV4PlayerToRoster)
      stats.value = teamStat
        ? [mapV4TeamStatToSeasonStat(teamStat, { seasonId: seasonId.value })]
        : teamStanding
          ? [{
              seasonId: seasonId.value,
              teamId: id,
              teamName: teamStanding.teamName,
              teamLogoUrl: teamStanding.teamLogoUrl,
              games: toNumber(teamStanding.winCount, 0) + toNumber(teamStanding.loseCount, 0),
              wins: teamStanding.winCount,
              losses: teamStanding.loseCount,
              winRate:
                toNumber(teamStanding.winCount, 0) + toNumber(teamStanding.loseCount, 0) > 0
                  ? toNumber(teamStanding.winCount, 0)
                    / (toNumber(teamStanding.winCount, 0) + toNumber(teamStanding.loseCount, 0))
                  : 0,
            }]
          : []
      ranking.value = teamStanding ? [mapV4StandingToRank(teamStanding, seasonId.value)] : []
      state.value = 'success'
    } catch (err) {
      if (current !== reqId) return
      detail.value = null
      roster.value = []
      stats.value = []
      ranking.value = []
      divisionId.value = ''
      seasonId.value = ''
      state.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '加载球队失败'
    }
  }

  watch([teamId, queryDivisionId, querySeasonId], () => {
    void load()
  }, { immediate: true })

  const ctx: PublicTeamPageContext = {
    teamId: teamId as unknown as Ref<string>,
    divisionId,
    seasonId,
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
