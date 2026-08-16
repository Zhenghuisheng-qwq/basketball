<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PaperSection from '@/components/common/PaperSection.vue'
import { getV4PublicMatchSeries } from '@/api/v4Public'
import type { V4PublicMatchSeriesDetail, V4PublicScheduleItem } from '@/api/types/v4Public'
import {
  v4BestOfLabel,
  v4PublicRoundLabel,
  v4ScheduleStatusLabel,
  v4ScheduleTeamLabel,
} from '@/utils/v4Display'
import { useScheduleFilters } from '../composables/useScheduleFilters'
import ScheduleFilters from './ScheduleFilters.vue'
import WeekDateNav from './WeekDateNav.vue'
import MatchList from './MatchList.vue'
import MatchDetailModal from './MatchDetailModal.vue'
import StatsModal from './StatsModal.vue'
import type { Match } from '../types/events'

const router = useRouter()

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
  state,
  errorMessage,
} = useScheduleFilters()

const detailMatch = ref<Match | null>(null)
const statsMatch = ref<Match | null>(null)
const detailVisible = ref(false)
const statsVisible = ref(false)

const seriesDetail = ref<V4PublicMatchSeriesDetail | null>(null)
const seriesDetailVisible = ref(false)
const seriesDetailLoading = ref(false)
const seriesDetailError = ref('')

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

function formatScheduleTime(item: V4PublicScheduleItem): string {
  if (item.scheduleDate && item.startTime) {
    return `${item.scheduleDate} ${String(item.startTime).slice(0, 5)}`
  }
  if (item.scheduleDate) return item.scheduleDate
  return '待安排'
}

function teamName(item: V4PublicScheduleItem, side: 'home' | 'away'): string {
  const name = side === 'home' ? item.homeTeam : item.awayTeam
  return v4ScheduleTeamLabel(name, item.placeholderSource)
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
  router.push(`/events/match/${matchId}`)
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
            :hide-date-filter="hasSeriesItems"
            @update:selected-season="selectSeason"
            @toggle-calendar="toggleCalendar"
            @toggle-team-selector="toggleTeamSelector"
            @select-date="selectDate"
            @select-team="selectTeam"
            @shift-months="() => {}"
          />
        </div>

        <WeekDateNav
          v-if="state === 'success' && !hasSeriesItems && selectedDate"
          :selected-date="selectedDate"
          :week-number="currentWeek"
          @select-date="selectDate"
          @prev-week="goPrevWeek"
          @next-week="goNextWeek"
        />

        <p v-if="state === 'loading'" class="series-empty">赛程加载中…</p>
        <p v-else-if="state === 'error'" class="series-empty">{{ errorMessage || '加载赛程失败' }}</p>
        <p v-else-if="state === 'empty'" class="series-empty">暂无已发布赛程</p>

        <div v-else-if="hasSeriesItems" class="series-schedule">
          <section v-for="[roundName, items] in scheduleByRound" :key="roundName" class="series-round">
            <h3 class="series-round__title">{{ v4PublicRoundLabel(roundName, items[0]?.roundNo) }}</h3>
            <article
              v-for="item in items"
              :key="`${item.unitType}-${item.id}`"
              class="series-card"
              role="button"
              tabindex="0"
              @click="openSeries(item)"
              @keydown.enter="openSeries(item)"
            >
              <div class="series-card__meta">
                <span>{{ item.seriesCode || v4PublicRoundLabel(item.roundName, item.roundNo) }}</span>
                <span>{{ v4BestOfLabel(item.bestOf) }}</span>
                <span>{{ v4ScheduleStatusLabel(item.status, item.statusName) }}</span>
              </div>
              <div class="series-card__versus">
                <strong>{{ teamName(item, 'home') }}</strong>
                <span class="series-card__vs">VS</span>
                <strong>{{ teamName(item, 'away') }}</strong>
              </div>
              <div class="series-card__foot">
                <span>{{ formatScheduleTime(item) }}</span>
                <span v-if="item.seriesScore">{{ item.seriesScore }}</span>
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
            {{ seriesDetail.homeTeamName || '待定' }}
            <strong>{{ seriesDetail.homeWinCount ?? 0 }} : {{ seriesDetail.awayWinCount ?? 0 }}</strong>
            {{ seriesDetail.awayTeamName || '待定' }}
          </p>
          <p class="series-modal__meta">
            {{ seriesDetail.seriesCode || '系列赛' }}
            · BO{{ seriesDetail.bestOf || 1 }}
            · {{ seriesDetail.seriesStatusName || seriesDetail.seriesStatus || '' }}
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
                <span v-if="game.homeScore != null && game.awayScore != null">
                  {{ game.homeScore }} : {{ game.awayScore }}
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
  cursor: pointer;
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

.series-card__vs {
  font-size: 13px;
  font-weight: 800;
  color: rgba(10, 10, 10, 0.45);
}

.series-empty {
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
  border-top: 1px solid rgba(10, 10, 10, 0.08);
  font-size: 13px;
  font-weight: 700;

  &.clickable {
    cursor: pointer;
  }
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
