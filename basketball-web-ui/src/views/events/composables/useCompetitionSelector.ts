import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { CompetitionOption } from '../types/competition'
import {
  competitionMap,
  competitionOptions,
  defaultCompetition,
  DEFAULT_COMPETITION_ID,
} from '../mock/competitions.mock'

export function useCompetitionSelector() {
  const route = useRoute()
  const router = useRouter()

  const currentCompetitionId = computed(() => {
    const queryId = route.query.competition

    if (typeof queryId === 'string' && competitionMap.has(queryId)) {
      return queryId
    }

    return DEFAULT_COMPETITION_ID
  })

  const currentCompetition = computed<CompetitionOption>(
    () => competitionMap.get(currentCompetitionId.value) ?? defaultCompetition,
  )

  const domesticCompetitions = computed(() =>
    competitionOptions.filter((item) => item.region === 'domestic'),
  )

  const internationalCompetitions = computed(() =>
    competitionOptions.filter((item) => item.region === 'international'),
  )

  function selectCompetition(competition: CompetitionOption) {
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
    DEFAULT_COMPETITION_ID,
  }
}
