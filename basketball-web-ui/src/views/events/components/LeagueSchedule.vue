<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useRouter } from 'vue-router'
import PaperSection from '@/components/common/PaperSection.vue'
import { getV4PublicMatchSeries } from '@/api/v4Public'
import type { V4PublicMatchSeriesDetail, V4PublicScheduleItem } from '@/api/types/v4Public'
import { formatScore, formatSeriesSetScore } from '@/utils/v4Display'
import { useScheduleFilters } from '../composables/useScheduleFilters'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { formatDateHint } from '../utils/scheduleCalendar'
import { isSafeHttpUrl, scheduleReplayUrl } from '../utils/scheduleRowDisplay'
import WeekDateNav from './WeekDateNav.vue'
import ScheduleCalendarPanel from './ScheduleCalendarPanel.vue'
import ScheduleTeamFilter from './ScheduleTeamFilter.vue'
import ScheduleTable from './ScheduleTable.vue'

const router = useRouter()
const ctx = inject<ReturnType<typeof usePublicSeasonContext>>('publicSeasonContext')!

const {
  selectedDate,
  selectedTeamId,
  selectedTeam,
  calendarWeekStart,
  calendarMonthAnchor,
  calendarExpanded,
  teamFilterExpanded,
  teamOptions,
  dateCounts,
  groupedScheduleByDate,
  total,
  selectDate,
  clearDate,
  selectTeam,
  toggleCalendar,
  toggleTeamFilter,
  goPrevWeek,
  goNextWeek,
  goPrevMonth,
  goNextMonth,
  state,
  errorMessage,
} = useScheduleFilters()

const filterDateLabel = computed(() => selectedDate.value ? formatDateHint(selectedDate.value) : '全部日期')
const filterTeamLabel = computed(() => selectedTeam.value?.name || '全部球队')

function groupHead(group: { date: string; title: string; items: unknown[] }) {
  const [dateLabel, weekday] = group.title.split(' · ')
  return {
    dateLabel: dateLabel || group.title,
    weekday: weekday || '',
    count: group.items.length,
  }
}

const emptyFilterText = computed(() => {
  const hasDate = Boolean(selectedDate.value)
  const hasTeam = Boolean(selectedTeamId.value)
  if (hasDate && hasTeam) {
    return `${selectedTeam.value?.name || '该球队'}在所选日期暂无比赛`
  }
  if (hasDate) return `${formatDateHint(selectedDate.value)}暂无比赛`
  if (hasTeam) return '该球队暂无符合条件的比赛'
  return '当前筛选条件下暂无赛程'
})

const seriesDetail = ref<V4PublicMatchSeriesDetail | null>(null)
const seriesDetailVisible = ref(false)
const seriesDetailLoading = ref(false)
const seriesDetailError = ref('')

const seasonOptions = computed(() =>
  (ctx.seasons.value || []).map((item) => ({
    id: String(item.seasonId),
    label: item.competitionSeasonName || item.seasonName || String(item.seasonId),
  })),
)

const currentSeasonLabel = computed(() => {
  const current = seasonOptions.value.find((item) => item.id === String(ctx.seasonId.value))
  return current?.label || ctx.currentSeason.value?.seasonName || ''
})

function onSeasonChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  if (value) ctx.selectSeason(value)
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

function openReplay(item: V4PublicScheduleItem) {
  const url = scheduleReplayUrl(item)
  if (!isSafeHttpUrl(url)) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

async function openSeries(item: V4PublicScheduleItem) {
  if (item.unitType !== 'MATCH_SERIES' || !item.id) return
  seriesDetailVisible.value = true
  seriesDetail.value = null
  seriesDetailError.value = ''
  seriesDetailLoading.value = true
  try {
    const res = await getV4PublicMatchSeries(String(item.id))
    seriesDetail.value = res.data ?? null
    if (!seriesDetail.value) {
      seriesDetailError.value = '暂无系列赛详情'
    }
  } catch (err) {
    seriesDetailError.value = err instanceof Error ? err.message : '加载系列赛详情失败'
  } finally {
    seriesDetailLoading.value = false
  }
}

function closeSeries() {
  seriesDetailVisible.value = false
}

function openSeriesGame(matchId?: string) {
  if (!matchId) return
  closeSeries()
  router.push(matchLocation(matchId))
}

function seriesModalScore(detail: V4PublicMatchSeriesDetail): string {
  const official = formatSeriesSetScore({
    homeWinCount: detail.homeWinCount,
    awayWinCount: detail.awayWinCount,
    bestOf: detail.bestOf,
    seriesStatus: detail.seriesStatus,
    resolutionType: detail.resolutionType,
    winnerEventTeamId: detail.winnerEventTeamId,
    homeEventTeamId: detail.homeEventTeamId,
    awayEventTeamId: detail.awayEventTeamId,
  })
  const [home, away] = official.split(':').map((part) => part.trim())
  if (!away) return official
  return `${away} : ${home}`
}
</script>

<template>
  <div class="site-shell league-schedule-shell">
    <PaperSection edge="none" tone="gray" class="league-schedule">
      <div class="league-schedule__inner">
        <div class="schedule-head">
          <div class="schedule-head__title">
            <h2 class="cnpa-paper-section__title">赛程</h2>
            <span class="cnpa-paper-section__subtitle">SCHEDULE</span>
          </div>
          <label class="season-switch">
            <select :value="ctx.seasonId.value" @change="onSeasonChange">
              <option v-for="season in seasonOptions" :key="season.id" :value="season.id">
                {{ season.label }}
              </option>
              <option v-if="!seasonOptions.length" :value="ctx.seasonId.value">
                {{ currentSeasonLabel || '当前赛季' }}
              </option>
            </select>
          </label>
        </div>

        <WeekDateNav
          v-if="state !== 'error'"
          :selected-date="selectedDate"
          :week-start="calendarWeekStart"
          :date-counts="dateCounts"
          :calendar-expanded="calendarExpanded"
          :team-filter-expanded="teamFilterExpanded"
          :selected-team-name="selectedTeam?.name || ''"
          @select-date="selectDate"
          @clear-date="clearDate"
          @prev-week="goPrevWeek"
          @next-week="goNextWeek"
          @toggle-calendar="toggleCalendar"
          @toggle-team-filter="toggleTeamFilter"
        />

        <ScheduleCalendarPanel
          v-if="calendarExpanded && state !== 'error'"
          :selected-date="selectedDate"
          :month-anchor="calendarMonthAnchor"
          :date-counts="dateCounts"
          @select-date="selectDate"
          @prev-month="goPrevMonth"
          @next-month="goNextMonth"
        />

        <ScheduleTeamFilter
          v-if="teamFilterExpanded && state === 'success'"
          :teams="teamOptions"
          :selected-team-id="selectedTeamId"
          @select-team="selectTeam"
        />

        <div v-if="state === 'success'" class="filter-summary">
          <span class="filter-chip">{{ filterDateLabel }}</span>
          <span class="filter-chip">{{ filterTeamLabel }}</span>
          <strong class="filter-count">共 {{ total }} 场</strong>
        </div>
        <p v-if="state === 'loading'" class="schedule-empty">赛程加载中…</p>
        <p v-else-if="state === 'error'" class="schedule-empty">{{ errorMessage || '赛程加载失败' }}</p>
        <p v-else-if="state === 'empty'" class="schedule-empty">暂无赛程</p>

        <template v-else>
          <section v-for="group in groupedScheduleByDate" :key="group.date" class="schedule-round">
            <header class="schedule-round__head">
              <div>
                <h3>{{ groupHead(group).dateLabel }}</h3>
                <span v-if="groupHead(group).weekday">{{ groupHead(group).weekday }}</span>
              </div>
              <em>{{ groupHead(group).count }} 场</em>
            </header>
            <ScheduleTable
              :items="group.items"
              hide-date-on-row
              :empty-text="emptyFilterText"
              @open-match="openMatch"
              @open-series="openSeries"
              @open-stats="openMatch"
              @open-replay="openReplay"
            />
          </section>
          <div v-if="!groupedScheduleByDate.length" class="schedule-empty">{{ emptyFilterText }}</div>
        </template>
      </div>
    </PaperSection>

    <div v-if="seriesDetailVisible" class="series-modal" @click.self="closeSeries">
      <div class="series-modal__panel" role="dialog" aria-label="系列赛详情">
        <header class="series-modal__head">
          <h3>{{ seriesDetail?.seriesCode || '系列赛' }}</h3>
          <button type="button" class="series-modal__close" @click="closeSeries">关闭</button>
        </header>
        <p v-if="seriesDetailLoading">加载中…</p>
        <p v-else-if="seriesDetailError">{{ seriesDetailError }}</p>
        <template v-else-if="seriesDetail">
          <p class="series-modal__meta">
            {{ seriesDetail.awayTeamName || '待定' }}
            <strong>{{ seriesModalScore(seriesDetail) }}</strong>
            {{ seriesDetail.homeTeamName || '待定' }}
          </p>
          <p class="series-modal__meta">
            {{ seriesDetail.stageName || '系列赛' }}
            · BO{{ seriesDetail.bestOf || 1 }}
            · {{ seriesDetail.seriesStatusName || seriesDetail.seriesStatus || '' }}
          </p>
          <p v-if="seriesDetail.resultReason || seriesDetail.resultSummary" class="series-modal__meta dq-reason">
            {{ seriesDetail.resultReason || seriesDetail.resultSummary }}
          </p>
          <ul class="series-modal__games">
            <li
              v-for="game in seriesDetail.games || []"
              :key="game.matchId || String(game.seriesGameNo)"
              class="series-modal__game"
              :class="{ clickable: Boolean(game.matchId) && !game.skipped }"
              @click="!game.skipped && openSeriesGame(game.matchId)"
            >
              <span>Game {{ game.seriesGameNo }}</span>
              <span v-if="game.skipped">{{ game.skippedReason || '无需进行' }}</span>
              <template v-else>
                <span>{{ game.matchStatusName || game.matchStatus || '待安排' }}</span>
                <span v-if="game.homeScore != null && game.awayScore != null || String(game.matchStatus || '').toUpperCase() === 'FORFEITED'">
                  {{ formatScore(game.homeScore, game.awayScore, game.matchStatus) }}
                </span>
                <span v-else>{{ game.scheduledTime || '待安排' }}</span>
              </template>
            </li>
          </ul>
        </template>
      </div>
    </div>
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
    clamp(24px, 2vw, 32px)
    clamp(24px, 2.4vw, 32px)
    clamp(32px, 2.8vw, 48px);
}

.schedule-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.schedule-head__title .cnpa-paper-section__title {
  font-size: clamp(28px, 2.4vw, 36px);
}

.schedule-head__title .cnpa-paper-section__subtitle {
  color: var(--cnpa-red, #e10613);
  font-style: italic;
  letter-spacing: 0.16em;
}

.season-switch select {
  min-width: 220px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(10, 10, 10, 0.16);
  background: #fff;
  color: #111;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
}

.filter-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px;
}

.filter-chip {
  height: 28px;
  padding: 0 10px;
  background: #eceae7;
  color: #4d4a47;
  font-size: 12px;
  font-weight: 700;
  line-height: 28px;
}

.filter-count {
  margin-left: auto;
  color: #111;
  font-size: 13px;
  font-weight: 800;
}

.schedule-round + .schedule-round {
  margin-top: 28px;
}

.schedule-round__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 10px;
  padding: 10px 14px;
  background: #f3f1ee;
  border-left: 3px solid var(--cnpa-red, #e10613);

  h3 {
    margin: 0;
    color: #111;
    font-size: clamp(18px, 1.6vw, 22px);
    font-weight: 800;
    line-height: 1.1;
  }

  span {
    color: #6b6763;
    font-size: 12px;
    font-weight: 700;
  }

  em {
    color: #8a8682;
    font-size: 13px;
    font-style: normal;
    font-weight: 800;
  }
}

.schedule-empty {
  padding: 48px 16px;
  text-align: center;
  color: rgba(10, 10, 10, 0.55);
}

.series-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(8, 8, 8, 0.46);
}

.series-modal__panel {
  width: min(560px, calc(100vw - 32px));
  max-height: 80vh;
  overflow: auto;
  padding: 20px 22px;
  background: #fff;
}

.series-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.series-modal__head h3,
.series-modal__meta {
  margin: 0 0 12px;
}

.dq-reason {
  color: #c2410c;
  font-weight: 700;
}

.series-modal__close {
  border: 0;
  background: transparent;
  font-weight: 800;
  cursor: pointer;
}

.series-modal__games {
  margin: 0;
  padding: 0;
  list-style: none;
}

.series-modal__game {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid #e5e5e5;
  font-size: 13px;
  font-weight: 700;

  &.clickable {
    cursor: pointer;
  }
}

@media (max-width: 768px) {
  .league-schedule__inner {
    padding-left: var(--cnpa-public-content-gutter);
    padding-right: var(--cnpa-public-content-gutter);
  }

}
</style>
