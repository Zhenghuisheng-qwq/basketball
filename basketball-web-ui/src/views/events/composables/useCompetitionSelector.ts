import { computed, inject, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicTournamentPage } from '@/api/publicTournament'
import type { PublicTournamentItemVo } from '@/api/types/public'
import type { CompetitionOption } from '../types/competition'
import type { usePublicSeasonContext } from './usePublicSeasonContext'
import { displayText } from '@/utils/publicDisplay'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

function toOption(item: PublicTournamentItemVo): CompetitionOption {
  return {
    id: item.tournamentId,
    code: displayText(item.tournamentNo || item.shortName || item.tournamentId, item.tournamentId),
    name: displayText(item.tournamentName, '未命名赛事'),
    shortName: displayText(item.shortName || item.tournamentName, '赛事'),
    region: 'domestic',
    logo: item.logoUrl?.trim() || '',
    enabled: true,
    defaultTab: 'overview',
  }
}

export function useCompetitionSelector() {
  const route = useRoute()
  const router = useRouter()
  const ctx = inject<SeasonContext | null>('publicSeasonContext', null)
  const fallbackTournaments = ref<PublicTournamentItemVo[]>([])

  onMounted(() => {
    if (ctx) return
    void getPublicTournamentPage({ currentPage: 1, pageSize: 50 })
      .then((res) => {
        fallbackTournaments.value = res.data?.records ?? []
      })
      .catch(() => {
        fallbackTournaments.value = []
      })
  })

  const competitionOptions = computed(() => {
    const list = ctx?.tournaments.value?.length
      ? ctx.tournaments.value
      : fallbackTournaments.value
    return list.map(toOption)
  })

  const currentCompetitionId = computed(() => {
    if (ctx?.tournamentId.value) return ctx.tournamentId.value
    const queryId = route.query.competition
    if (typeof queryId === 'string' && queryId.trim()) return queryId.trim()
    return competitionOptions.value[0]?.id ?? ''
  })

  const currentCompetition = computed<CompetitionOption | null>(() => {
    return (
      competitionOptions.value.find((item) => item.id === currentCompetitionId.value) ??
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
        competition: competition.id,
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
