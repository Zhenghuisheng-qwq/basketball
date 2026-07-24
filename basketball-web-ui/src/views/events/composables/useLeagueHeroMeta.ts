import { computed, inject, type ComputedRef, type Ref } from 'vue'
import type { LeagueHeroMetaItem } from '../types/events'
import type { usePublicSeasonContext } from './usePublicSeasonContext'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

export function useLeagueHeroMeta(): {
  heroMetaItems: ComputedRef<LeagueHeroMetaItem[]>
  loading: Ref<boolean>
} {
  const ctx = inject<SeasonContext>('publicSeasonContext')

  const heroMetaItems = computed(() => ctx?.heroMeta.value ?? [])
  const loading = computed(
    () =>
      ctx?.overviewState.value === 'loading' ||
      ctx?.seasonState.value === 'loading' ||
      ctx?.tournamentState.value === 'loading',
  ) as unknown as Ref<boolean>

  return {
    heroMetaItems,
    loading: loading as Ref<boolean>,
  }
}
