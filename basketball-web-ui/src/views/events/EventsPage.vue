<script setup lang="ts">
import { computed, provide } from 'vue'
import { useEventTab } from './composables/useEventTab'
import { usePublicSeasonContext } from './composables/usePublicSeasonContext'
import LeagueHero from './components/LeagueHero.vue'
import LeagueTabs from './components/LeagueTabs.vue'
import LeagueOverview from './components/LeagueOverview.vue'
import LeagueSchedule from './components/LeagueSchedule.vue'
import LeagueStage from './components/LeagueStage.vue'
import { v4PlatformLabel } from '@/utils/v4Display'
import './styles/events.scss'

const { activeTab, setTab } = useEventTab()
const seasonContext = usePublicSeasonContext()

provide('publicSeasonContext', seasonContext)

const divisions = computed(() => seasonContext.seasonDetail.value?.divisions || [])
const activeDivisionId = computed(() => seasonContext.divisionId.value)

function divisionLabel(division: { platformCode?: string; divisionName?: string; lineupMaxCount?: number }) {
  const platform = v4PlatformLabel(division.platformCode)
  const mode = division.lineupMaxCount === 3 ? '3V3' : division.lineupMaxCount === 5 ? '5V5' : ''
  const name = division.divisionName || ''
  return [platform, mode, name].filter(Boolean).join(' · ')
}
</script>
<template>
  <div class="events-page">
    <section class="events-hero-section">
      <div class="site-shell">
        <LeagueHero />
        <LeagueTabs :active-tab="activeTab" @change="setTab" />
        <div v-if="divisions.length > 1" class="division-switch" aria-label="赛区">
          <button
            v-for="division in divisions"
            :key="division.divisionId"
            type="button"
            class="division-switch__btn"
            :class="{ active: activeDivisionId === String(division.divisionId) }"
            @click="seasonContext.selectDivision(String(division.divisionId))"
          >
            {{ divisionLabel(division) }}
          </button>
        </div>
      </div>
    </section>

    <section class="events-content-section">
      <LeagueOverview v-if="activeTab === 'overview'" />
      <LeagueSchedule v-else-if="activeTab === 'schedule'" />
      <LeagueStage v-else />
    </section>
  </div>
</template>
