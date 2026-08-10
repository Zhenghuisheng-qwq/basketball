<script setup lang="ts">
import { provide } from 'vue'
import { useEventTab } from './composables/useEventTab'
import { usePublicSeasonContext } from './composables/usePublicSeasonContext'
import LeagueHero from './components/LeagueHero.vue'
import LeagueTabs from './components/LeagueTabs.vue'
import LeagueOverview from './components/LeagueOverview.vue'
import LeagueSchedule from './components/LeagueSchedule.vue'
import LeagueStage from './components/LeagueStage.vue'
import './styles/events.scss'

const { activeTab, setTab } = useEventTab()
const seasonContext = usePublicSeasonContext()

provide('publicSeasonContext', seasonContext)
</script>
<template>
  <div class="events-page">
    <section class="events-hero-section">
      <div class="site-shell">
        <LeagueHero />
        <LeagueTabs :active-tab="activeTab" @change="setTab" />
      </div>
    </section>

    <section class="events-content-section">
      <LeagueOverview v-if="activeTab === 'overview'" />
      <LeagueSchedule v-else-if="activeTab === 'schedule'" />
      <LeagueStage v-else />
    </section>
  </div>
</template>