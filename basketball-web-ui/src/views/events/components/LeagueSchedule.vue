<script setup lang="ts">
import { ref } from 'vue'
import { useScheduleFilters } from '../composables/useScheduleFilters'
import ScheduleFilters from './ScheduleFilters.vue'
import WeekDateNav from './WeekDateNav.vue'
import MatchList from './MatchList.vue'
import MatchDetailModal from './MatchDetailModal.vue'
import StatsModal from './StatsModal.vue'
import type { Match } from '../types/events'

const {
  selectedSeason,
  selectedDate,
  selectedTeamId,
  currentWeek,
  isCalendarOpen,
  isTeamSelectorOpen,
  filteredMatches,
  toggleCalendar,
  toggleTeamSelector,
  selectDate,
  selectTeam,
  selectSeason,
  goPrevWeek,
  goNextWeek,
} = useScheduleFilters()

const detailMatch = ref<Match | null>(null)
const statsMatch = ref<Match | null>(null)
const detailVisible = ref(false)
const statsVisible = ref(false)

function openDetail(match: Match) {
  detailMatch.value = match
  detailVisible.value = true
}

function openStats(match: Match) {
  statsMatch.value = match
  statsVisible.value = true
}

function closeDetail() {
  detailVisible.value = false
}

function closeStats() {
  statsVisible.value = false
}
</script>

<template>
  <section class="league-schedule events-section events-section-paper">
    <div class="events-inner">
      <div class="schedule-head">
        <div>
          <h2 class="section-head-title schedule-title">赛程</h2>
          <span class="section-head-sub">Schedule</span>
        </div>

        <ScheduleFilters
          :selected-season="selectedSeason"
          :selected-date="selectedDate"
          :selected-team-id="selectedTeamId"
          :is-calendar-open="isCalendarOpen"
          :is-team-selector-open="isTeamSelectorOpen"
          @update:selected-season="selectSeason"
          @toggle-calendar="toggleCalendar"
          @toggle-team-selector="toggleTeamSelector"
          @select-date="selectDate"
          @select-team="selectTeam"
          @shift-months="() => {}"
        />
      </div>

      <WeekDateNav
        :selected-date="selectedDate"
        :week-number="currentWeek"
        @select-date="selectDate"
        @prev-week="goPrevWeek"
        @next-week="goNextWeek"
      />

      <MatchList
        :matches="filteredMatches"
        @open-detail="openDetail"
        @open-stats="openStats"
      />
    </div>

    <MatchDetailModal :visible="detailVisible" :match="detailMatch" @close="closeDetail" />
    <StatsModal :visible="statsVisible" :match="statsMatch" @close="closeStats" />
  </section>
</template>

<style scoped lang="scss">
.league-schedule {
  position: relative;
  z-index: 1;
  padding: 48px 0 56px;
}

.schedule-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.schedule-title {
  color: #111;
}

.section-head-sub {
  color: rgba(10, 10, 10, 0.45);
}

@media (max-width: 768px) {
  .schedule-head {
    flex-direction: column;
  }
}
</style>
