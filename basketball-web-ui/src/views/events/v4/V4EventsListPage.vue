<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import PublicPageLayout from '@/components/layout/PublicPageLayout.vue'
import { getV4PublicSeasonList } from '@/api/v4Public'
import type { V4PublicSeasonItem } from '@/api/types/v4Public'
import V4EventsHero from './components/V4EventsHero.vue'
import V4SeasonCard from './components/V4SeasonCard.vue'
import './styles/v4-events-list.scss'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const loading = ref(true)
const error = ref('')
const seasons = ref<V4PublicSeasonItem[]>([])
const activeSeries = ref('')

type ListState = 'loading' | 'success' | 'empty' | 'error' | 'filter-empty'

const listState = ref<ListState>('loading')

const filteredSeasons = computed(() => {
  if (!activeSeries.value) return seasons.value
  return seasons.value.filter((item) => item.seriesCode === activeSeries.value)
})

const seasonCount = computed(() => seasons.value.length)

const registrationOpenDivisionCount = computed(() =>
  seasons.value.reduce(
    (total, item) => total + (item.registrationOpenDivisionCount ?? 0),
    0,
  ),
)

const sparseGrid = computed(() => filteredSeasons.value.length > 0 && filteredSeasons.value.length <= 2)

const emptyText = computed(() => {
  if (listState.value === 'error') return error.value || t('eventsPage.list.loadFailed')
  if (listState.value === 'filter-empty') return t('eventsPage.list.filterEmpty')
  return t('eventsPage.list.empty')
})

async function loadSeasons() {
  loading.value = true
  error.value = ''
  listState.value = 'loading'
  try {
    const res = await getV4PublicSeasonList(
      activeSeries.value ? { seriesCode: activeSeries.value } : undefined,
    )
    seasons.value = res.data ?? []
    if (!seasons.value.length) {
      listState.value = activeSeries.value ? 'filter-empty' : 'empty'
    } else if (!filteredSeasons.value.length) {
      listState.value = 'filter-empty'
    } else {
      listState.value = 'success'
    }
  } catch (err) {
    seasons.value = []
    error.value = err instanceof Error ? err.message : t('eventsPage.list.loadFailedShort')
    listState.value = 'error'
  } finally {
    loading.value = false
  }
}

function selectSeries(code: string) {
  activeSeries.value = code
  router.replace({
    path: '/tournament',
    query: code ? { series: code } : {},
  })
}

function goDetail(season: V4PublicSeasonItem) {
  router.push(`/events/${season.seasonId}`)
}

watch(
  () => route.query.series,
  (value) => {
    activeSeries.value = typeof value === 'string' ? value.toUpperCase() : ''
    void loadSeasons()
  },
  { immediate: true },
)

onMounted(() => {
  if (typeof route.query.series === 'string') {
    activeSeries.value = route.query.series.toUpperCase()
  }
})
</script>

<template>
  <PublicPageLayout>
    <template #hero>
      <V4EventsHero
        :active-series="activeSeries"
        :season-count="seasonCount"
        :registration-open-division-count="registrationOpenDivisionCount"
        @select-series="selectSeries"
      />
    </template>

    <div class="v4-events-list-page__shell">
      <header class="v4-events-list-page__heading">
        <div>
          <h2 class="v4-events-list-page__title">{{ t('eventsPage.list.publicSeasons') }}</h2>
          <p class="v4-events-list-page__subtitle">OPEN SEASONS</p>
          <p class="v4-events-list-page__desc">{{ t('eventsPage.list.publicSeasonsDesc') }}</p>
        </div>
        <p v-if="listState === 'success'" class="v4-events-list-page__count">
          {{ t('eventsPage.count.seasons', { count: filteredSeasons.length }) }}
        </p>
      </header>

      <div v-if="listState === 'loading'" class="v4-events-list-page__skeleton" aria-hidden="true">
        <div v-for="idx in 6" :key="idx" class="v4-events-list-page__skeleton-card" />
      </div>

      <div v-else-if="listState === 'error'" class="v4-events-list-page__state v4-events-list-page__state--error">
        <p>{{ emptyText }}</p>
        <button type="button" class="v4-events-list-page__retry" @click="loadSeasons">{{ t('eventsPage.list.retry') }}</button>
      </div>

      <div
        v-else-if="listState === 'empty' || listState === 'filter-empty'"
        class="v4-events-list-page__state"
      >
        <p>{{ emptyText }}</p>
        <button
          v-if="listState === 'filter-empty'"
          type="button"
          class="v4-events-list-page__retry v4-events-list-page__retry--ghost"
          @click="selectSeries('')"
        >
          {{ t('eventsPage.list.viewAllSeries') }}
        </button>
      </div>

      <div
        v-else
        class="v4-events-list-page__grid"
        :class="{ 'v4-events-list-page__grid--sparse': sparseGrid }"
      >
        <V4SeasonCard
          v-for="season in filteredSeasons"
          :key="season.seasonId"
          :season="season"
          @select="goDetail"
        />
      </div>
    </div>
  </PublicPageLayout>
</template>
