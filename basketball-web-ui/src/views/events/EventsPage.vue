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

    <div
      v-if="seasonContext.tournamentState.value === 'error' || seasonContext.seasonState.value === 'error'"
      class="events-page__error site-shell"
    >
      {{ seasonContext.errorMessage.value || '加载赛事失败' }}
      <button type="button" class="events-page__retry" @click="seasonContext.reload()">重试</button>
    </div>

    <section v-else class="events-content-section">
      <LeagueOverview v-if="activeTab === 'overview'" />
      <LeagueSchedule v-else-if="activeTab === 'schedule'" />
      <LeagueStage v-else />
    </section>
  </div>
</template>

<style scoped lang="scss">
.events-page__error {
  padding: 48px 16px;
  text-align: center;
  color: #e10613;
  font-weight: 700;
}

.events-page__retry {
  display: inline-block;
  margin-top: 12px;
  margin-left: 12px;
  padding: 8px 16px;
  border: 0;
  border-radius: 4px;
  background: #e10613;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}
</style>
