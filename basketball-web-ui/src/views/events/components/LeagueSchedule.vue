<script setup lang="ts">
import { computed, inject, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import { useScheduleFilters } from '../composables/useScheduleFilters'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { formatDateGroupTitle } from '../utils/scheduleCalendar'
import PaperSection from '@/components/common/PaperSection.vue'
import WeekDateNav from './WeekDateNav.vue'
import ScheduleCalendarPanel from './ScheduleCalendarPanel.vue'
import ScheduleTeamFilter from './ScheduleTeamFilter.vue'
import ScheduleTable from './ScheduleTable.vue'
import ScheduleLoadMore from './ScheduleLoadMore.vue'

const router = useRouter()
const { t } = useI18n()
const ctx = inject<ReturnType<typeof usePublicSeasonContext>>('publicSeasonContext')!

const {
  selectedDate,
  selectedTeamId,
  selectedTeam,
  calendarWeekStart,
  weekNumber,
  calendarMonthAnchor,
  calendarExpanded,
  teamFilterExpanded,
  teamOptions,
  dateCounts,
  groupedScheduleByDate,
  total,
  visibleItemCount,
  remainingDateGroups,
  canLoadMoreDateGroups,
  loadMoreDateGroups,
  allDateGroupCount,
  selectDate,
  clearDate,
  selectTeam,
  toggleCalendar,
  goPrevWeek,
  goNextWeek,
  goPrevMonth,
  goNextMonth,
  state,
  errorMessage,
} = useScheduleFilters()

const filterDateLabel = computed(() => selectedDate.value ? formatDateGroupTitle(selectedDate.value) : t('eventsPage.date.all'))
const filterTeamLabel = computed(() => {
  if (!selectedTeam.value) return t('eventsPage.schedule.allTeams')
  const name = selectedTeam.value.name
  const code = String(selectedTeam.value.shortName || selectedTeam.value.code || '').trim()
  if (!code || code === name || /^CNPA-T-\d+$/i.test(code)) return name
  return `${name} · ${code}`
})

function groupHead(group: { date: string; title: string; items: unknown[] }) {
  return {
    title: group.title,
    count: group.items.length,
  }
}

const emptyFilterText = computed(() => {
  const hasDate = Boolean(selectedDate.value)
  const hasTeam = Boolean(selectedTeamId.value)
  if (hasDate && hasTeam) {
    return t('eventsPage.schedule.noMatchForTeamOnDate', {
      team: selectedTeam.value?.name || t('eventsPage.teams.thisTeam'),
    })
  }
  if (hasDate) return t('eventsPage.schedule.noMatchOnDate', { date: formatDateGroupTitle(selectedDate.value) })
  if (hasTeam) return t('eventsPage.schedule.noMatchForTeam')
  return t('eventsPage.schedule.noMatch')
})

const teamStripRef = ref<HTMLElement | null>(null)

const showAllLoadedHint = computed(() =>
  !selectedDate.value
  && !canLoadMoreDateGroups.value
  && allDateGroupCount.value > 3
  && total.value > 0,
)

const currentSeasonLabel = computed(() => {
  const detail = ctx.seasonDetail.value
  const current = ctx.currentSeason.value
  return (
    detail?.displayTitle
    || detail?.seasonName
    || current?.competitionSeasonName
    || current?.seasonName
    || ''
  )
})

function scrollToTeamStrip() {
  teamFilterExpanded.value = true
  nextTick(() => {
    teamStripRef.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function matchLocation(matchId: string) {
  return {
    path: `/events/match/${matchId}`,
    query: {
      ...(ctx.seasonId.value ? { seasonId: ctx.seasonId.value } : {}),
      ...(ctx.divisionId.value ? { divisionId: ctx.divisionId.value } : {}),
    },
  }
}

function openMatch(item: V4PublicScheduleItem) {
  if (!item.id || item.unitType !== 'MATCH') return
  router.push(matchLocation(String(item.id)))
}

function seriesLocation(seriesId: string) {
  return {
    path: `/events/match-series/${seriesId}`,
    query: {
      ...(ctx.seasonId.value ? { seasonId: ctx.seasonId.value } : {}),
      ...(ctx.divisionId.value ? { divisionId: ctx.divisionId.value } : {}),
    },
  }
}

function openSeries(item: V4PublicScheduleItem) {
  if (item.unitType !== 'MATCH_SERIES' || !item.id) return
  router.push(seriesLocation(String(item.id)))
}
</script>

<template>
  <div class="league-schedule-page">
    <div class="site-shell">
      <PaperSection edge="none" tone="gray" class="league-schedule__paper">
        <div class="league-schedule__inner">
          <section class="schedule-heading">
            <div class="schedule-heading-copy">
              <div class="schedule-title-row">
                <h1 class="schedule-title">{{ t('events.schedule') }}</h1>
                <span class="schedule-slash">/</span>
                <span class="schedule-en">SCHEDULE</span>
              </div>
              <p class="schedule-timezone">{{ t('events.timezone') }}</p>
              <p v-if="currentSeasonLabel" class="schedule-subtitle">{{ currentSeasonLabel }}</p>
            </div>
          </section>

          <div v-if="state !== 'error'" class="schedule-board">
            <WeekDateNav
              :selected-date="selectedDate"
              :week-start="calendarWeekStart"
              :week-number="weekNumber"
              :date-counts="dateCounts"
              :calendar-expanded="calendarExpanded"
              :team-filter-expanded="teamFilterExpanded"
              :selected-team-name="selectedTeam?.name || ''"
              @select-date="selectDate"
              @clear-date="clearDate"
              @prev-week="goPrevWeek"
              @next-week="goNextWeek"
              @toggle-calendar="toggleCalendar"
              @toggle-team-filter="scrollToTeamStrip"
            />

            <section class="filter-summary">
              <div class="summary-left">
                <span>{{ filterDateLabel }}</span>
                <span class="summary-divider" />
                <span>{{ filterTeamLabel }}</span>
              </div>
              <span class="summary-total">{{ t('eventsPage.count.games', { count: total }) }}</span>
            </section>

            <ScheduleCalendarPanel
              v-if="calendarExpanded"
              :selected-date="selectedDate"
              :month-anchor="calendarMonthAnchor"
              :date-counts="dateCounts"
              @select-date="selectDate"
              @prev-month="goPrevMonth"
              @next-month="goNextMonth"
            />

            <div v-if="state === 'success'" ref="teamStripRef">
              <ScheduleTeamFilter
                :teams="teamOptions"
                :selected-team-id="selectedTeamId"
                @select-team="selectTeam"
              />
            </div>
          </div>

          <p v-if="state === 'loading'" class="schedule-empty show">{{ t('eventsPage.schedule.loading') }}</p>
          <p v-else-if="state === 'error'" class="schedule-empty show">{{ errorMessage || t('eventsPage.schedule.loadFailed') }}</p>
          <div v-else-if="state === 'empty'" class="schedule-empty show">
            <strong>{{ t('eventsPage.schedule.emptyTitle') }}</strong>
            <span>{{ t('eventsPage.schedule.emptyHint') }}</span>
          </div>

          <template v-else>
            <section v-if="groupedScheduleByDate.length" class="match-content">
              <section v-for="group in groupedScheduleByDate" :key="group.date" class="date-group">
                <div class="date-group-title">
                  <strong>{{ groupHead(group).title }}</strong>
                  <span>{{ t('eventsPage.count.gamesOnDay', { count: groupHead(group).count }) }}</span>
                </div>
                <ScheduleTable
                  :items="group.items"
                  hide-date-on-row
                  @open-match="openMatch"
                  @open-series="openSeries"
                  @open-stats="openMatch"
                />
              </section>
              <ScheduleLoadMore
                :can-load-more="canLoadMoreDateGroups"
                :remaining-groups="remainingDateGroups"
                :visible-count="visibleItemCount"
                :total="total"
                :all-loaded="showAllLoadedHint"
                @load-more="loadMoreDateGroups"
              />
            </section>
            <div v-else class="schedule-empty show">
              <strong>{{ emptyFilterText }}</strong>
              <span>{{ t('eventsPage.schedule.tryFilters') }}</span>
            </div>
          </template>
        </div>
      </PaperSection>
    </div>
  </div>
</template>

<style scoped lang="scss">
.league-schedule-page {
  --cnpa-red: #e51422;
  --schedule-border: #dfddda;
  --schedule-border-light: #eceae7;

  width: 100%;
  color: #151515;
}

.league-schedule__paper {
  margin-top: 0;
  min-height: calc(100vh - 140px);
}

.league-schedule__inner {
  padding:
    32px
    var(--cnpa-public-content-gutter)
    70px;
}

.schedule-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

.schedule-heading-copy {
  min-width: 0;
}

.schedule-title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.schedule-title {
  margin: 0;
  font-size: clamp(34px, 3vw, 46px);
  line-height: 1;
  font-weight: 900;
  letter-spacing: -1px;
}

.schedule-slash {
  font-size: 24px;
  font-weight: 300;
  color: #8a8a8a;
}

.schedule-en {
  color: var(--cnpa-red);
  font-size: 14px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 2px;
}

.schedule-timezone {
  margin: 8px 0 0;
  color: #8a8a8a;
  font-size: 12px;
  font-weight: 600;
}

.schedule-subtitle {
  margin: 10px 0 0;
  color: #3d3d3d;
  font-size: 15px;
  font-weight: 700;
}

.filter-summary {
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
  background: #f3f3f3;
  border: 1px solid #eceae7;
  border-top: none;
  color: #555;
  font-size: 13px;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.summary-total {
  flex-shrink: 0;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 7px;
}

.summary-icon {
  color: #999;
}

.summary-divider {
  width: 1px;
  height: 18px;
  background: #ddd;
}

.match-content {
  margin-top: 26px;
  scroll-margin-top: 16px;
}

.date-group + .date-group {
  margin-top: 32px;
}

.date-group-title {
  min-height: 40px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 0 8px 0 14px;
  border-left: 4px solid var(--cnpa-red);
  background: transparent;

  strong {
    font-size: 20px;
    font-weight: 800;
    letter-spacing: 0.2px;
  }

  span {
    color: #999;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;
  }
}

.schedule-empty {
  display: none;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  margin: 24px 0 0;
  background: #fff;
  border: 1px solid var(--schedule-border-light);
  color: #888;

  &.show {
    display: flex;
  }

  strong {
    color: #333;
    font-size: 18px;
  }
}

@media (max-width: 900px) {
  .schedule-heading {
    align-items: center;
    gap: 18px;
  }

  .schedule-title-row {
    flex-wrap: wrap;
  }
}

@media (max-width: 640px) {
  .league-schedule__inner {
    padding-top: 22px;
  }
}
</style>
