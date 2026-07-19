<script setup lang="ts">
import { ref } from 'vue'
import PaperSection from '@/components/common/PaperSection.vue'
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
  <div class="site-shell league-schedule-shell">
    <PaperSection edge="both" tone="gray" class="league-schedule">
      <div class="league-schedule__inner">
        <div class="schedule-head">
          <div>
            <h2 class="cnpa-paper-section__title">赛程</h2>
            <span class="cnpa-paper-section__subtitle">Schedule</span>
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
    </PaperSection>

    <MatchDetailModal :visible="detailVisible" :match="detailMatch" @close="closeDetail" />
    <StatsModal :visible="statsVisible" :match="statsMatch" @close="closeStats" />
  </div>
</template>

<style scoped lang="scss">
.league-schedule-shell {
  margin-top: 0;
}

.league-schedule {
  position: relative;
  z-index: 1;
}

.league-schedule__inner {
  padding:
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(32px, 2.8vw, 48px);
}

.schedule-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .league-schedule__inner {
    padding-left: 16px;
    padding-right: 16px;
  }

  .schedule-head {
    flex-direction: column;
  }
}
</style>
