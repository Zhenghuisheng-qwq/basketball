import { computed, inject, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicSeriesList } from '@/api/v4Public'
import { getPublicTournamentPage } from '@/api/publicTournament'
import type { V4PublicSeriesItem } from '@/api/types/v4Public'
import type { PublicTournamentItemVo } from '@/api/types/public'
import type { CompetitionOption } from '../types/competition'
import { displayText } from '@/utils/publicDisplay'
import { isSamePublicId, normalizePublicId } from '@/utils/publicId'
import { v4SeriesLabel } from '@/utils/v4Display'

interface SeasonContext {
  tournaments: { value: PublicTournamentItemVo[] }
  tournamentId: { value: string }
  selectTournament: (id: string) => void
}

function toLegacyOption(item: PublicTournamentItemVo): CompetitionOption {
  return {
    id: normalizePublicId(item.tournamentId),
    code: displayText(item.tournamentNo || item.shortName || item.tournamentId, String(item.tournamentId)),
    name: displayText(item.tournamentName, '未命名赛事'),
    shortName: displayText(item.shortName || item.tournamentName, '赛事'),
    region: 'domestic',
    logo: item.logoUrl?.trim() || '',
    enabled: true,
    defaultTab: 'overview',
  }
}

function toV4Option(item: V4PublicSeriesItem): CompetitionOption {
  return {
    id: item.seriesCode,
    code: item.seriesCode,
    name: displayText(item.seriesName, v4SeriesLabel(item.seriesCode)),
    shortName: v4SeriesLabel(item.seriesCode),
    region: 'domestic',
    logo: item.iconUrl?.trim() || item.coverUrl?.trim() || '',
    enabled: item.enabled !== 0,
    defaultTab: 'overview',
  }
}

export function useCompetitionSelector() {
  const route = useRoute()
  const router = useRouter()
  const ctx = inject<SeasonContext | null>('publicSeasonContext', null)
  const fallbackTournaments = ref<PublicTournamentItemVo[]>([])
  const v4Series = ref<V4PublicSeriesItem[]>([])

  const isLegacyRoute = computed(() => route.path === '/tournament/legacy')

  onMounted(() => {
    if (ctx || isLegacyRoute.value) {
      void getPublicTournamentPage({ currentPage: 1, pageSize: 50 })
        .then((res) => {
          fallbackTournaments.value = res.data?.records ?? []
        })
        .catch(() => {
          fallbackTournaments.value = []
        })
      return
    }
    void getV4PublicSeriesList()
      .then((res) => {
        v4Series.value = (res.data ?? []).filter((item) => item.enabled !== 0)
      })
      .catch(() => {
        v4Series.value = []
      })
  })

  const competitionOptions = computed(() => {
    if (ctx?.tournaments.value?.length) {
      return ctx.tournaments.value.map(toLegacyOption)
    }
    if (isLegacyRoute.value) {
      return fallbackTournaments.value.map(toLegacyOption)
    }
    return v4Series.value.map(toV4Option)
  })

  const currentCompetitionId = computed(() => {
    if (ctx?.tournamentId.value) return ctx.tournamentId.value
    if (!isLegacyRoute.value) {
      const series = route.query.series
      if (typeof series === 'string' && series.trim()) return series.trim().toUpperCase()
      return normalizePublicId(competitionOptions.value[0]?.id)
    }
    const queryId = route.query.competition
    if (typeof queryId === 'string' && queryId.trim()) return queryId.trim()
    return normalizePublicId(competitionOptions.value[0]?.id)
  })

  const currentCompetition = computed<CompetitionOption | null>(() => {
    return (
      competitionOptions.value.find((item) => isSamePublicId(item.id, currentCompetitionId.value)) ??
      competitionOptions.value[0] ??
      null
    )
  })

  const domesticCompetitions = computed(() => competitionOptions.value)
  const internationalCompetitions = computed(() => [] as CompetitionOption[])

  function selectCompetition(competition: CompetitionOption) {
    if (ctx) {
      ctx.selectTournament(competition.id)
      return
    }
    if (isLegacyRoute.value) {
      router.push({
        path: '/tournament/legacy',
        query: {
          competition: competition.id,
          tab: competition.defaultTab,
        },
      })
      return
    }
    router.push({
      path: '/tournament',
      query: {
        series: competition.id,
      },
    })
  }

  return {
    competitionOptions,
    currentCompetitionId,
    currentCompetition,
    domesticCompetitions,
    internationalCompetitions,
    selectCompetition,
    DEFAULT_COMPETITION_ID: currentCompetitionId,
  }
}
