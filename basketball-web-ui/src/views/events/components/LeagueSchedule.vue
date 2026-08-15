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
  scheduleByRound,
  hasSeriesItems,
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
          v-if="!hasSeriesItems"
          :selected-date="selectedDate"
          :week-number="currentWeek"
          @select-date="selectDate"
          @prev-week="goPrevWeek"
          @next-week="goNextWeek"
        />

        <div v-if="hasSeriesItems" class="series-schedule">
          <section v-for="[roundName, items] in scheduleByRound" :key="roundName" class="series-round">
            <h3 class="series-round__title">{{ roundName }}</h3>
            <article v-for="item in items" :key="`${item.unitType}-${item.id}`" class="series-card">
              <div class="series-card__meta">
                <span>{{ item.seriesCode || item.roundName }}</span>
                <span v-if="item.bestOf">BO{{ item.bestOf }}</span>
                <span>{{ item.statusName || item.status || '待比赛' }}</span>
              </div>
              <div class="series-card__versus">
                <strong>{{ item.homeTeam || '待定' }}</strong>
                <span class="series-card__score">{{ item.seriesScore || '0:0' }}</span>
                <strong>{{ item.awayTeam || '待定' }}</strong>
              </div>
              <div class="series-card__foot">
                <span>{{ item.scheduleDate && item.startTime ? `${item.scheduleDate} ${String(item.startTime).slice(0, 5)}` : '待安排' }}</span>
                <span v-if="item.gameProgress">进度 {{ item.gameProgress }}</span>
              </div>
            </article>
          </section>
          <div v-if="!scheduleByRound.length" class="series-empty">暂无已发布赛程</div>
        </div>

        <MatchList
          v-else
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

.series-schedule {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.series-round__title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 800;
}

.series-card {
  border: 1px solid rgba(10, 10, 10, 0.12);
  background: #f7f6f3;
  padding: 16px 18px;
  margin-bottom: 10px;
}

.series-card__meta,
.series-card__foot {
  display: flex;
  gap: 12px;
  color: rgba(10, 10, 10, 0.55);
  font-size: 12px;
  font-weight: 700;
}

.series-card__versus {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 12px;
  align-items: center;
  margin: 10px 0;
  font-size: 16px;
}

.series-card__score {
  font-size: 22px;
  font-weight: 900;
}

.series-empty {
  padding: 48px 16px;
  text-align: center;
  color: rgba(10, 10, 10, 0.55);
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