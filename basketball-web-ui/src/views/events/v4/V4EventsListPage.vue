<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicSeasonList } from '@/api/v4Public'
import type { V4PublicSeasonItem } from '@/api/types/v4Public'
import SafeImage from '@/components/common/SafeImage.vue'
import { displayText, formatDateTime, initials } from '@/utils/publicDisplay'
import {
  V4_SERIES_FILTERS,
  v4PlatformLabel,
  v4SeasonStatusLabel,
  v4SeriesLabel,
} from '@/utils/v4Display'
import '../styles/v4-events.scss'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const seasons = ref<V4PublicSeasonItem[]>([])
const activeSeries = ref('')

const filteredSeasons = computed(() => {
  if (!activeSeries.value) return seasons.value
  return seasons.value.filter((item) => item.seriesCode === activeSeries.value)
})

async function loadSeasons() {
  loading.value = true
  error.value = ''
  try {
    const res = await getV4PublicSeasonList(
      activeSeries.value ? { seriesCode: activeSeries.value } : undefined,
    )
    seasons.value = res.data ?? []
  } catch (err) {
    seasons.value = []
    error.value = err instanceof Error ? err.message : '加载赛事失败'
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
  <div class="v4-events-page">
    <section class="v4-events-hero">
      <div class="site-shell">
        <p class="v4-events-hero__eyebrow">CNPA 赛事中心</p>
        <h1 class="v4-events-hero__title">V4 赛事赛季</h1>
        <p class="v4-events-hero__desc">Series → Season → Division，按平台赛区浏览公开赛事数据。</p>
        <div class="v4-series-filter">
          <button
            v-for="item in V4_SERIES_FILTERS"
            :key="item.code || 'all'"
            type="button"
            class="v4-series-filter__btn"
            :class="{ 'v4-series-filter__btn--active': activeSeries === item.code }"
            @click="selectSeries(item.code)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </section>

    <section class="v4-events-list-section">
      <div class="site-shell">
        <div v-if="loading" class="v4-state">加载中...</div>
        <div v-else-if="error" class="v4-state v4-state--error">
          {{ error }}
          <button type="button" class="v4-btn v4-btn--primary" @click="loadSeasons">重试</button>
        </div>
        <div v-else-if="!filteredSeasons.length" class="v4-state">暂无已发布赛季</div>
        <div v-else class="v4-season-grid">
          <article v-for="season in filteredSeasons" :key="season.seasonId" class="v4-season-card">
            <div class="v4-season-card__banner">
              <SafeImage
                v-if="season.bannerUrl || season.coverUrl"
                :src="season.bannerUrl || season.coverUrl"
                :alt="season.displayTitle"
                :initials="initials(season.displayTitle, 2)"
                :size="120"
                :rounded="false"
              />
              <div v-else class="v4-season-card__banner-placeholder">
                {{ initials(season.displayTitle, 2) }}
              </div>
            </div>
            <div class="v4-season-card__body">
              <div class="v4-season-card__tags">
                <span class="v4-tag v4-tag--series">{{ v4SeriesLabel(season.seriesCode) }}</span>
                <span class="v4-tag">{{ v4SeasonStatusLabel(season.seasonStatus) }}</span>
              </div>
              <h2 class="v4-season-card__title">{{ displayText(season.displayTitle, '未命名赛季') }}</h2>
              <p class="v4-season-card__meta">
                {{ displayText(season.seriesName) }} · {{ season.seasonYear || '--' }}
              </p>
              <div class="v4-season-card__platforms">
                <span
                  v-for="division in season.divisions"
                  :key="division.divisionId"
                  class="v4-platform-tag"
                >
                  {{ v4PlatformLabel(division.platformCode) }}
                </span>
              </div>
              <dl class="v4-season-card__times">
                <div>
                  <dt>报名时间</dt>
                  <dd>{{ formatDateTime(season.registrationStartTime).full }} — {{ formatDateTime(season.registrationEndTime).full }}</dd>
                </div>
                <div>
                  <dt>比赛时间</dt>
                  <dd>{{ formatDateTime(season.matchStartTime).full }} — {{ formatDateTime(season.matchEndTime).full }}</dd>
                </div>
              </dl>
              <button type="button" class="v4-btn v4-btn--primary v4-season-card__action" @click="goDetail(season)">
                查看详情
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>
