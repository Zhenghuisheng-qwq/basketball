<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import HallHero from './components/HallHero.vue'
import HallTabs from './components/HallTabs.vue'
import WeeklyBestPanel from './components/WeeklyBestPanel.vue'
import EventHonorsPanel from './components/EventHonorsPanel.vue'
import PastChampionsPanel from './components/PastChampionsPanel.vue'
import {
  getDefaultEventTournamentId,
  getDefaultWeeklyTournamentId,
  getLatestPublishedWeekNo,
  resolveTournamentId,
  resolveWeekNo,
} from './mock/hallOfFame'
import type { HallTab } from './types'
import { normalizeHallTab, parsePositiveInt, parseQueryString } from '@/utils/hallNavigate'

const route = useRoute()
const router = useRouter()

const activeTab = computed<HallTab>(() => normalizeHallTab(route.query.tab))

const weeklyTournamentId = computed(() =>
  resolveTournamentId(
    typeof route.query.tournamentId === 'string' ? route.query.tournamentId : undefined,
    getDefaultWeeklyTournamentId(),
  ),
)

const weeklyWeekNo = computed(() => {
  const week = parsePositiveInt(route.query.week, getLatestPublishedWeekNo(weeklyTournamentId.value))
  return resolveWeekNo(weeklyTournamentId.value, week)
})

const eventTournamentId = computed(() =>
  resolveTournamentId(
    typeof route.query.tournamentId === 'string' ? route.query.tournamentId : undefined,
    getDefaultEventTournamentId(),
  ),
)

const rosterExpanded = computed(() => route.query.showRoster === '1')

const championYear = computed(() => parseQueryString(route.query.year, 'ALL'))
const championPlatform = computed(() => parseQueryString(route.query.platform, 'ALL'))
const championSeries = computed(() => parseQueryString(route.query.series, 'ALL'))
const championPage = computed(() => parsePositiveInt(route.query.page, 1))

function replaceQuery(next: Record<string, string | undefined>) {
  const query: Record<string, string> = {
    tab: next.tab ?? activeTab.value,
  }

  Object.entries(next).forEach(([key, value]) => {
    if (key === 'tab') return
    if (value !== undefined && value !== '') {
      query[key] = value
    }
  })

  router.replace({ path: '/hall-of-fame', query })
}

function setTab(tab: HallTab) {
  if (tab === 'weekly') {
    replaceQuery({
      tab,
      tournamentId: weeklyTournamentId.value,
      week: String(weeklyWeekNo.value),
    })
    return
  }

  if (tab === 'event') {
    replaceQuery({
      tab,
      tournamentId: eventTournamentId.value,
      showRoster: rosterExpanded.value ? '1' : undefined,
    })
    return
  }

  replaceQuery({
    tab,
    year: championYear.value,
    platform: championPlatform.value,
    series: championSeries.value,
    page: String(championPage.value),
  })
}

function updateWeeklyTournamentId(value: string) {
  replaceQuery({
    tab: 'weekly',
    tournamentId: value,
    week: String(getLatestPublishedWeekNo(value)),
  })
}

function updateWeeklyWeekNo(value: number) {
  replaceQuery({
    tab: 'weekly',
    tournamentId: weeklyTournamentId.value,
    week: String(value),
  })
}

function updateEventTournamentId(value: string) {
  replaceQuery({
    tab: 'event',
    tournamentId: value,
    showRoster: undefined,
  })
}

function updateRosterExpanded(value: boolean) {
  replaceQuery({
    tab: 'event',
    tournamentId: eventTournamentId.value,
    showRoster: value ? '1' : undefined,
  })
}

function updateChampionYear(value: string) {
  replaceQuery({
    tab: 'champions',
    year: value,
    platform: championPlatform.value,
    series: championSeries.value,
    page: '1',
  })
}

function updateChampionPlatform(value: string) {
  replaceQuery({
    tab: 'champions',
    year: championYear.value,
    platform: value,
    series: championSeries.value,
    page: '1',
  })
}

function updateChampionSeries(value: string) {
  replaceQuery({
    tab: 'champions',
    year: championYear.value,
    platform: championPlatform.value,
    series: value,
    page: '1',
  })
}

function updateChampionPage(value: number) {
  replaceQuery({
    tab: 'champions',
    year: championYear.value,
    platform: championPlatform.value,
    series: championSeries.value,
    page: String(value),
  })
}

watch(
  () => [route.path, route.query.tab] as const,
  () => {
    if (route.path !== '/hall-of-fame') return

    const tab = normalizeHallTab(route.query.tab)
    if (route.query.tab !== undefined && route.query.tab !== tab) {
      replaceQuery({
        tab: 'weekly',
        tournamentId: getDefaultWeeklyTournamentId(),
        week: String(getLatestPublishedWeekNo(getDefaultWeeklyTournamentId())),
      })
      return
    }

    if (!route.query.tab) {
      replaceQuery({
        tab: 'weekly',
        tournamentId: getDefaultWeeklyTournamentId(),
        week: String(getLatestPublishedWeekNo(getDefaultWeeklyTournamentId())),
      })
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="hall-of-fame-page">
    <HallHero />

    <div class="site-shell">
      <HallTabs :model-value="activeTab" @update:model-value="setTab" />
    </div>

    <section class="hall-of-fame-page__content">
      <div class="site-shell">
        <div class="cnpa-paper-section cnpa-paper-section--light">
          <PaperTear position="top" tone="light" />
          <div class="cnpa-paper-section__content hall-of-fame-page__paper-inner">
          <WeeklyBestPanel
            v-if="activeTab === 'weekly'"
            :tournament-id="weeklyTournamentId"
            :week-no="weeklyWeekNo"
            @update:tournament-id="updateWeeklyTournamentId"
            @update:week-no="updateWeeklyWeekNo"
          />
          <EventHonorsPanel
            v-else-if="activeTab === 'event'"
            :tournament-id="eventTournamentId"
            :roster-expanded="rosterExpanded"
            @update:tournament-id="updateEventTournamentId"
            @update:roster-expanded="updateRosterExpanded"
          />
          <PastChampionsPanel
            v-else
            :year="championYear"
            :platform="championPlatform"
            :series="championSeries"
            :page="championPage"
            @update:year="updateChampionYear"
            @update:platform="updateChampionPlatform"
            @update:series="updateChampionSeries"
            @update:page="updateChampionPage"
          />
          </div>
          <PaperTear position="bottom" tone="light" />
        </div>
      </div>
    </section>
  </div>
</template>

<style lang="scss">
@use './styles/hall-shared.scss';
</style>

<style scoped lang="scss">
.hall-of-fame-page {
  width: 100%;
  background: #000;
  overflow-x: clip;
}

.hall-of-fame-page__content {
  margin: 0;
}

.hall-of-fame-page__paper-inner {
  padding: 34px 32px 42px;
  background: var(--cnpa-paper-bg, #e9e7e7);
}

@media (max-width: 767px) {
  .hall-of-fame-page__paper-inner {
    padding: 24px 16px 32px;
  }
}
</style>
