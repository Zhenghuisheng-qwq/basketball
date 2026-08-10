import { computed, inject, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicSeriesList } from '@/api/v4Public'
import type { V4PublicSeriesItem } from '@/api/types/v4Public'
import type { CompetitionOption } from '../types/competition'
import type { usePublicSeasonContext } from './usePublicSeasonContext'
import { displayText } from '@/utils/publicDisplay'
import { v4SeriesLabel } from '@/utils/v4Display'
import { isSamePublicId, normalizePublicId } from '@/utils/publicId'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

function toOption(item: V4PublicSeriesItem): CompetitionOption {
  return {
    id: normalizePublicId(item.seriesId),
    code: displayText(item.seriesCode, String(item.seriesId)),
    name: displayText(item.seriesName, v4SeriesLabel(item.seriesCode)),
    shortName: v4SeriesLabel(item.seriesCode),
    region: 'domestic',
    logo: item.iconUrl?.trim() || item.coverUrl?.trim() || '',
    enabled: true,
    defaultTab: 'overview',
  }
}

export function useCompetitionSelector() {
  const route = useRoute()
  const router = useRouter()
  const ctx = inject<SeasonContext | null>('publicSeasonContext', null)
  const fallbackSeries = ref<V4PublicSeriesItem[]>([])

  onMounted(() => {
    if (ctx) return
    void getV4PublicSeriesList()
      .then((res) => {
        fallbackSeries.value = res.data ?? []
      })
      .catch(() => {
        fallbackSeries.value = []
      })
  })

  const competitionOptions = computed(() => {
    if (ctx?.tournaments.value?.length) {
      return ctx.tournaments.value.map((item) => ({
        id: normalizePublicId(item.tournamentId),
        code: displayText(item.tournamentNo || item.shortName || item.tournamentId, String(item.tournamentId)),
        name: displayText(item.tournamentName, '未命名赛事'),
        shortName: displayText(item.shortName || item.tournamentName, '赛事'),
        region: 'domestic' as const,
        logo: item.logoUrl?.trim() || '',
        enabled: true,
        defaultTab: 'overview' as const,
      }))
    }
    return fallbackSeries.value.map(toOption)
  })

  const currentCompetitionId = computed(() => {
    if (ctx?.tournamentId.value) return ctx.tournamentId.value
    const queryId = route.query.competition
    if (typeof queryId === 'string' && queryId.trim()) return queryId.trim()
    const series = route.query.series
    if (typeof series === 'string' && series.trim()) {
      const matched = competitionOptions.value.find(
        (item) => item.code.toUpperCase() === series.trim().toUpperCase(),
      )
      if (matched) return matched.id
    }
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
    router.push({
      path: '/tournament',
      query: {
        series: competition.code,
        tab: competition.defaultTab,
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
