import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicTournamentPage, getPublicTournamentSeasons, getPublicSeasonOverview } from '@/api/publicTournament'
import type {
  PublicSeasonItemVo,
  PublicSeasonOverviewVo,
  PublicTournamentItemVo,
} from '@/api/types/public'
import { displayText } from '@/utils/publicDisplay'
import { isSamePublicId, normalizePublicId } from '@/utils/publicId'

export type LoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error'

export function usePublicSeasonContext() {
  const route = useRoute()
  const router = useRouter()

  const tournaments = ref<PublicTournamentItemVo[]>([])
  const seasons = ref<PublicSeasonItemVo[]>([])
  const overview = ref<PublicSeasonOverviewVo | null>(null)

  const tournamentState = ref<LoadState>('idle')
  const seasonState = ref<LoadState>('idle')
  const overviewState = ref<LoadState>('idle')
  const errorMessage = ref('')

  let tournamentReq = 0
  let seasonReq = 0
  let overviewReq = 0

  const tournamentId = computed(() => normalizePublicId(route.query.competition))
  const seasonId = computed(() => normalizePublicId(route.query.seasonId))

  const currentTournament = computed(
    () =>
      tournaments.value.find((item) => isSamePublicId(item.tournamentId, tournamentId.value)) ??
      null,
  )

  const currentSeason = computed(
    () =>
      seasons.value.find((item) => isSamePublicId(item.seasonId, seasonId.value)) ?? null,
  )

  async function loadTournaments() {
    const reqId = ++tournamentReq
    tournamentState.value = 'loading'
    errorMessage.value = ''
    try {
      const res = await getPublicTournamentPage({ currentPage: 1, pageSize: 50 })
      if (reqId !== tournamentReq) return
      const records = res.data?.records ?? []
      tournaments.value = records
      tournamentState.value = records.length ? 'success' : 'empty'

      if (!records.length) return

      const exists = records.some((item) => isSamePublicId(item.tournamentId, tournamentId.value))
      if (!exists) {
        await router.replace({
          path: '/tournament',
          query: {
            ...route.query,
            competition: normalizePublicId(records[0]!.tournamentId),
          },
        })
      }
    } catch (err) {
      if (reqId !== tournamentReq) return
      tournaments.value = []
      tournamentState.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '加载赛事失败'
    }
  }

  async function loadSeasons(tid: string) {
    const reqId = ++seasonReq
    if (!tid) {
      seasons.value = []
      seasonState.value = 'empty'
      overview.value = null
      return
    }
    seasonState.value = 'loading'
    try {
      const res = await getPublicTournamentSeasons(tid)
      if (reqId !== seasonReq) return
      const list = res.data ?? []
      seasons.value = list
      seasonState.value = list.length ? 'success' : 'empty'

      if (!list.length) {
        overview.value = null
        return
      }

      const exists = list.some((item) => isSamePublicId(item.seasonId, seasonId.value))
      if (!exists) {
        await router.replace({
          path: '/tournament',
          query: {
            ...route.query,
            competition: tid,
            seasonId: normalizePublicId(list[0]!.seasonId),
          },
        })
      }
    } catch (err) {
      if (reqId !== seasonReq) return
      seasons.value = []
      seasonState.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '加载赛季失败'
    }
  }

  async function loadOverview(sid: string) {
    const reqId = ++overviewReq
    if (!sid) {
      overview.value = null
      overviewState.value = 'empty'
      return
    }
    overviewState.value = 'loading'
    try {
      const res = await getPublicSeasonOverview(sid)
      if (reqId !== overviewReq) return
      overview.value = res.data ?? null
      overviewState.value = res.data ? 'success' : 'empty'
    } catch (err) {
      if (reqId !== overviewReq) return
      overview.value = null
      overviewState.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '加载赛季概览失败'
    }
  }

  function selectTournament(id: string) {
    router.push({
      path: '/tournament',
      query: {
        ...route.query,
        competition: normalizePublicId(id),
        seasonId: undefined,
      },
    })
  }

  function selectSeason(id: string) {
    const normalized = normalizePublicId(id)
    if (!normalized) return
    router.push({
      path: '/tournament',
      query: {
        ...route.query,
        seasonId: normalized,
      },
    })
  }

  const heroMeta = computed(() => {
    const ov = overview.value
    const season = currentSeason.value
    const tournament = currentTournament.value
    return [
      {
        key: 'season' as const,
        icon: 'calendar' as const,
        title: displayText(
          tournament?.competitionSeasonName
            || ov?.competitionSeasonName
            || ov?.seasonName
            || season?.competitionSeasonName
            || season?.seasonName,
          '赛季',
        ),
        value: displayText(
          tournament?.competitionSeasonYear
            || ov?.competitionSeasonYear
            || (season?.seasonStartTime && season?.seasonEndTime
              ? `${String(season.seasonStartTime).slice(0, 10)} ~ ${String(season.seasonEndTime).slice(0, 10)}`
              : season?.seasonYear),
          '--',
        ),
      },
      {
        key: 'teams' as const,
        icon: 'trophy' as const,
        title: `${displayText(ov?.teamCount ?? 0, '0')} 支战队`,
        value: displayText(ov?.tournamentName || tournament?.tournamentName, '参赛球队'),
      },
      {
        key: 'stage' as const,
        icon: 'stage' as const,
        title: '公开赛程',
        value: `${displayText(ov?.publicMatchCount ?? 0, '0')} 场`,
      },
      {
        key: 'platform' as const,
        icon: 'gamepad' as const,
        title: '比赛平台',
        value: displayText(tournament?.gameTerminal, '--'),
      },
    ]
  })

  watch(
    () => route.path,
    (path) => {
      if (path.startsWith('/tournament') && !path.includes('/team')) {
        void loadTournaments()
      }
    },
    { immediate: true },
  )

  watch(
    tournamentId,
    (tid) => {
      void loadSeasons(tid)
    },
    { immediate: true },
  )

  watch(
    seasonId,
    (sid) => {
      void loadOverview(sid)
    },
    { immediate: true },
  )

  return {
    tournaments,
    seasons,
    overview,
    tournamentId,
    seasonId,
    currentTournament,
    currentSeason,
    tournamentState,
    seasonState,
    overviewState,
    errorMessage,
    heroMeta,
    selectTournament,
    selectSeason,
    reload: loadTournaments,
  }
}
