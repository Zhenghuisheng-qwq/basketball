import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getV4PublicDivisionDetail,
  getV4PublicCurrentSeason,
  getV4PublicSeasonDetail,
  getV4PublicSeasonList,
  getV4PublicSeriesList,
  getV4PublicStanding,
} from '@/api/v4Public'
import type {
  V4PublicDivisionDetail,
  V4PublicSeasonDetail,
  V4PublicSeasonItem,
  V4PublicSeriesItem,
} from '@/api/types/v4Public'
import type { PublicSeasonItemVo, PublicSeasonOverviewVo, PublicTournamentItemVo } from '@/api/types/public'
import { translate } from '@/i18n'
import { displayText, localizedText } from '@/utils/publicDisplay'
import { v4PlatformLabel, v4PublicLifecycleLabel, v4SeriesLabel } from '@/utils/v4Display'
import type { LeagueHeroMetaItem } from '../types/events'

export type LoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error'

/** master UI 兼容：Series 伪装成 Tournament */
function seriesToTournament(series: V4PublicSeriesItem): PublicTournamentItemVo {
  return {
    tournamentId: series.seriesId,
    tournamentNo: series.seriesCode,
    tournamentName: series.seriesName || v4SeriesLabel(series.seriesCode),
    shortName: v4SeriesLabel(series.seriesCode),
    gameTerminal: '',
    logoUrl: series.iconUrl || series.coverUrl || '',
    coverUrl: series.bannerUrl || series.coverUrl || '',
  }
}

function seasonToPublicItem(season: V4PublicSeasonItem): PublicSeasonItemVo {
  return {
    seasonId: season.seasonId,
    tournamentId: season.seriesId,
    tournamentName: season.seriesName || v4SeriesLabel(season.seriesCode),
    seasonName: localizedText(season.displayTitle, season.displayTitleEn, season.seasonName || ''),
    seasonYear: season.seasonYear != null ? String(season.seasonYear) : '',
    seasonStartTime: season.matchStartTime || season.registrationStartTime || '',
    seasonEndTime: season.matchEndTime || season.registrationEndTime || '',
    competitionSeasonName: localizedText(season.displayTitle, season.displayTitleEn, season.seasonName || ''),
    competitionSeasonYear: season.seasonYear ?? null,
    maxTeamCount: season.divisions?.[0]?.maxTeamCount ?? null,
  }
}

export function usePublicSeasonContext() {
  const route = useRoute()
  const router = useRouter()

  const tournaments = ref<PublicTournamentItemVo[]>([])
  const seriesList = ref<V4PublicSeriesItem[]>([])
  const rawSeasons = ref<V4PublicSeasonItem[]>([])
  const seasons = computed(() => rawSeasons.value.map(seasonToPublicItem))
  const seasonDetail = ref<V4PublicSeasonDetail | null>(null)
  const divisionDetail = ref<V4PublicDivisionDetail | null>(null)
  const overview = ref<PublicSeasonOverviewVo | null>(null)

  const tournamentState = ref<LoadState>('idle')
  const seasonState = ref<LoadState>('idle')
  const overviewState = ref<LoadState>('idle')
  const errorMessage = ref('')

  let seriesReq = 0
  let seasonListReq = 0
  let detailReq = 0

  const paramSeasonId = computed(() => {
    const raw = route.params.seasonId
    return typeof raw === 'string' ? raw.trim() : ''
  })

  const querySeriesCode = computed(() => {
    const raw = route.query.series ?? route.query.seriesCode
    return typeof raw === 'string' ? raw.trim().toUpperCase() : ''
  })

  const divisionId = computed(() => {
    const raw = route.query.divisionId
    if (typeof raw === 'string' && raw.trim()) return raw.trim()
    return (
      seasonDetail.value?.defaultDivisionId
      || seasonDetail.value?.divisions?.[0]?.divisionId
      || ''
    )
  })

  const seasonId = computed(() => paramSeasonId.value || (typeof route.query.seasonId === 'string' ? route.query.seasonId : ''))

  const currentSeasonRaw = computed(
    () =>
      rawSeasons.value.find((item) => item.seasonId === seasonId.value)
      || seasonDetail.value
      || null,
  )

  const tournamentId = computed(() => {
    const sid = currentSeasonRaw.value?.seriesId
    if (sid) return sid
    const code = querySeriesCode.value || currentSeasonRaw.value?.seriesCode
    if (!code) return ''
    return seriesList.value.find((item) => (item.seriesCode || '').toUpperCase() === code)?.seriesId || ''
  })

  const currentTournament = computed(
    () => tournaments.value.find((item) => item.tournamentId === tournamentId.value) ?? null,
  )

  const currentSeason = computed(
    () => seasons.value.find((item) => item.seasonId === seasonId.value) ?? null,
  )

  const heroMeta = computed<LeagueHeroMetaItem[]>(() => {
    const season = seasonDetail.value || currentSeasonRaw.value
    const division = divisionDetail.value
    const detailCounts = seasonDetail.value
    const platforms = season?.platformCodes?.length
      ? season.platformCodes.map((code) => v4PlatformLabel(code)).join(' / ')
      : v4PlatformLabel(division?.platformCode)
    return [
      {
        key: 'season',
        icon: 'calendar',
        title: translate('eventsPage.hero.season'),
        value: localizedText(
          season?.displayTitle || season?.seasonName || season?.seasonYear,
          season?.displayTitleEn,
          '--',
        ),
      },
      {
        key: 'teams',
        icon: 'trophy',
        title: translate('eventsPage.hero.teamCount'),
        value: String(division?.eventTeamCount ?? detailCounts?.eventTeamCount ?? 0),
      },
      {
        key: 'stage',
        icon: 'stage',
        title: translate('eventsPage.hero.currentStage'),
        value: v4PublicLifecycleLabel(divisionDetail.value?.divisionStatus, season?.seasonStatus, translate('events.regularSeason')),
      },
      {
        key: 'platform',
        icon: 'gamepad',
        title: translate('eventsPage.hero.platform'),
        value: displayText(platforms, '--'),
      },
    ]
  })

  async function loadSeries() {
    const reqId = ++seriesReq
    tournamentState.value = 'loading'
    errorMessage.value = ''
    try {
      const res = await getV4PublicSeriesList()
      if (reqId !== seriesReq) return
      seriesList.value = res.data ?? []
      tournaments.value = seriesList.value.map(seriesToTournament)
      tournamentState.value = tournaments.value.length ? 'success' : 'empty'
    } catch (err) {
      if (reqId !== seriesReq) return
      seriesList.value = []
      tournaments.value = []
      tournamentState.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : translate('eventsPage.hero.loadSeriesFailed')
    }
  }

  async function loadSeasonsBySeriesCode(seriesCode: string) {
    const reqId = ++seasonListReq
    if (!seriesCode) {
      rawSeasons.value = []
      seasonState.value = 'empty'
      return
    }
    seasonState.value = 'loading'
    try {
      const res = await getV4PublicSeasonList({ seriesCode })
      if (reqId !== seasonListReq) return
      rawSeasons.value = res.data ?? []
      seasonState.value = rawSeasons.value.length ? 'success' : 'empty'
    } catch (err) {
      if (reqId !== seasonListReq) return
      rawSeasons.value = []
      seasonState.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : translate('eventsPage.hero.loadSeasonsFailed')
    }
  }

  async function loadSeasonDetail(sid: string) {
    const reqId = ++detailReq
    if (!sid) {
      seasonDetail.value = null
      divisionDetail.value = null
      overview.value = null
      overviewState.value = 'empty'
      return
    }
    overviewState.value = 'loading'
    errorMessage.value = ''
    try {
      const seasonRes = await getV4PublicSeasonDetail(sid)
      if (reqId !== detailReq) return
      const detail = seasonRes.data
      if (!detail) {
        seasonDetail.value = null
        divisionDetail.value = null
        overview.value = null
        overviewState.value = 'empty'
        return
      }
      seasonDetail.value = detail

      // 同步 series 下赛季列表，供赛程筛选
      if (detail.seriesCode) {
        void loadSeasonsBySeriesCode(detail.seriesCode)
      }

      const nextDivisionId =
        (typeof route.query.divisionId === 'string' && route.query.divisionId.trim())
        || detail.defaultDivisionId
        || detail.divisions?.[0]?.divisionId
        || ''

      let division: V4PublicDivisionDetail | null = null
      let standingCount = 0
      if (nextDivisionId) {
        const [divisionRes, standingRes] = await Promise.all([
          getV4PublicDivisionDetail(nextDivisionId),
          getV4PublicStanding(nextDivisionId).catch(() => ({ data: [] })),
        ])
        if (reqId !== detailReq) return
        division = divisionRes.data ?? null
        standingCount = standingRes.data?.length ?? 0
        if (
          nextDivisionId
          && route.query.divisionId !== nextDivisionId
          && !route.query.divisionId
        ) {
          await router.replace({
            path: route.path,
            query: { ...route.query, divisionId: nextDivisionId },
          })
        }
      }
      divisionDetail.value = division

      overview.value = {
        seasonId: detail.seasonId,
        seasonName: detail.displayTitle || detail.seasonName,
        tournamentId: detail.seriesId,
        tournamentName: detail.seriesName || v4SeriesLabel(detail.seriesCode),
        teamCount: division?.eventTeamCount ?? detail.eventTeamCount ?? 0,
        finishedMatchCount: division?.finishedMatchCount ?? detail.finishedMatchCount ?? 0,
        publicMatchCount: division?.matchCount ?? detail.matchCount ?? 0,
        standingCount,
        competitionSeasonName: detail.displayTitle,
        competitionSeasonYear: detail.seasonYear ?? null,
      }
      overviewState.value = 'success'
    } catch (err) {
      if (reqId !== detailReq) return
      seasonDetail.value = null
      divisionDetail.value = null
      overview.value = null
      overviewState.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : translate('eventsPage.hero.systemError')
    }
  }

  function selectTournament(seriesId: string) {
    const series = seriesList.value.find((item) => item.seriesId === seriesId)
    const code = (series?.seriesCode || '').toUpperCase()
    router.push({
      path: '/tournament',
      query: code ? { series: code } : {},
    })
  }

  function selectSeason(nextSeasonId: string) {
    if (!nextSeasonId) return
    const series =
      querySeriesCode.value
      || (currentSeasonRaw.value?.seriesCode || '').toUpperCase()
      || 'LEAGUE'
    // 历史 /tournament 停留在赛事中心；深链详情仍可用 /events/:seasonId
    if (route.path === '/tournament' || route.path === '/events') {
      router.replace({
        path: '/tournament',
        query: {
          series,
          seasonId: nextSeasonId,
          ...(route.query.divisionId ? { divisionId: String(route.query.divisionId) } : {}),
        },
      })
      return
    }
    router.push({
      path: `/events/${nextSeasonId}`,
      query: route.query.divisionId
        ? { divisionId: String(route.query.divisionId) }
        : {},
    })
  }

  function selectDivision(nextDivisionId: string) {
    if (!nextDivisionId || !seasonId.value) return
    if (route.path === '/tournament' || route.path === '/events') {
      router.replace({
        path: '/tournament',
        query: {
          ...route.query,
          seasonId: seasonId.value,
          divisionId: nextDivisionId,
        },
      })
      return
    }
    router.replace({
      path: `/events/${seasonId.value}`,
      query: { ...route.query, divisionId: nextDivisionId },
    })
  }

  watch(
    () => route.path,
    () => {
      void loadSeries()
    },
    { immediate: true },
  )

  /** 历史 /tournament = EventsPage；无 seasonId 时按 Series 自动选最新公开赛季 */
  let resolvingSeason = false
  watch(
    () => [route.path, querySeriesCode.value, seasonId.value, seriesList.value.length] as const,
    async ([path, seriesCode, sid]) => {
      if (resolvingSeason) return
      if (path !== '/tournament' && path !== '/events') return
      if (sid) return
      const code = (seriesCode || 'LEAGUE').toUpperCase()
      resolvingSeason = true
      try {
        const currentRes = await getV4PublicCurrentSeason(code)
        const current = currentRes.data
        if (!current?.seasonId) {
          seasonDetail.value = null
          divisionDetail.value = null
          overview.value = null
          overviewState.value = 'empty'
          return
        }
        await router.replace({
          path: '/tournament',
          query: {
            ...route.query,
            series: code,
            seasonId: current.seasonId,
          },
        })
      } finally {
        resolvingSeason = false
      }
    },
    { immediate: true },
  )

  watch(
    () => [seasonId.value, typeof route.query.divisionId === 'string' ? route.query.divisionId : ''] as const,
    ([sid]) => {
      void loadSeasonDetail(sid)
    },
    { immediate: true },
  )

  return {
    tournaments,
    seasons,
    overview,
    tournamentId,
    seasonId,
    divisionId,
    currentTournament,
    currentSeason,
    seasonDetail,
    divisionDetail,
    tournamentState,
    seasonState,
    overviewState,
    errorMessage,
    heroMeta,
    selectTournament,
    selectSeason,
    selectDivision,
    reload: () => {
      void loadSeries()
      if (seasonId.value) void loadSeasonDetail(seasonId.value)
    },
  }
}
