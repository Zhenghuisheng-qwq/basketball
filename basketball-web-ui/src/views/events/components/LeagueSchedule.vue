<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PaperSection from '@/components/common/PaperSection.vue'
import { getV4PublicMatchSeries } from '@/api/v4Public'
import type { V4PublicMatchSeriesDetail, V4PublicScheduleItem } from '@/api/types/v4Public'
import {
  formatScore,
  formatSeriesSetScore,
  v4BestOfLabel,
  v4PublicRoundLabel,
  v4ScheduleStatusLabel,
  v4ScheduleTeamLabel,
} from '@/utils/v4Display'
import { useScheduleFilters } from '../composables/useScheduleFilters'
import ScheduleFilters from './ScheduleFilters.vue'
import WeekDateNav from './WeekDateNav.vue'
import MatchList from './MatchList.vue'
import type { Match } from '../types/events'

const router = useRouter()

const {
  selectedSeason,
  selectedDate,
  selectedTeamId,
  currentWeek,
  isCalendarOpen,
  isTeamSelectorOpen,
  pagedMatches,
  scheduleByRound,
  hasSeriesItems,
  teamOptions,
  currentPage,
  pageSize,
  total,
  showPagination,
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

const seriesDetail = ref<V4PublicMatchSeriesDetail | null>(null)
const seriesDetailVisible = ref(false)
const seriesDetailLoading = ref(false)
const seriesDetailError = ref('')

function openDetail(match: Match) {
  if (!match.matchId || match.status === 'scheduled' || match.status === 'cancelled') return
  router.push(`/events/match/${match.matchId}`)
}


function seriesScoreText(item: V4PublicScheduleItem): string {
  if (item.seriesScore && item.seriesScore !== '0:0' && item.seriesScore !== '0 : 0') {
    return item.seriesScore.replace(':', ' : ')
  }
  const text = formatSeriesSetScore({
    homeWinCount: item.homeWinCount,
    awayWinCount: item.awayWinCount,
    bestOf: item.bestOf,
    seriesStatus: item.status,
    winnerEventTeamId: item.winnerEventTeamId,
    homeEventTeamId: item.homeEventTeamId,
    awayEventTeamId: item.awayEventTeamId,
    emptyText: '',
  })
  return text === '-- : --' ? '' : text
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

function seriesStatusCode(item: V4PublicScheduleItem): string {
  return String(item.status || '').toUpperCase()
}

function isSeriesScheduled(item: V4PublicScheduleItem): boolean {
  return ['READY', 'PENDING', 'SCHEDULED', 'NOT_STARTED', 'UPCOMING'].includes(seriesStatusCode(item))
}

function isSeriesFinished(item: V4PublicScheduleItem): boolean {
  return ['COMPLETED', 'FINISHED', 'CLOSED'].includes(seriesStatusCode(item))
}

function isSeriesOpenable(item: V4PublicScheduleItem): boolean {
  return item.unitType === 'MATCH_SERIES' && Boolean(item.id) && !isSeriesScheduled(item) && seriesStatusCode(item) !== 'CANCELLED'
}

function seriesStatusLabel(item: V4PublicScheduleItem): string {
  if (isSeriesScheduled(item)) return '敬请期待'
  if (['RUNNING', 'LIVE', 'IN_PROGRESS'].includes(seriesStatusCode(item))) return '进行中'
  if (isSeriesFinished(item)) return '已结束'
  if (seriesStatusCode(item) === 'CANCELLED') return '已取消'
  return v4ScheduleStatusLabel(item.status, item.statusName)
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
            :team-options="teamOptions"
            :loading="state === 'loading'"
            :error="state === 'error'"
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
              :class="{
                'series-card--disabled': !isSeriesOpenable(item),
                'series-card--finished': isSeriesFinished(item),
              }"
              role="button"
              :aria-disabled="!isSeriesOpenable(item)"
              :tabindex="isSeriesOpenable(item) ? 0 : -1"
              @click="isSeriesOpenable(item) && openSeries(item)"
              @keydown.enter="isSeriesOpenable(item) && openSeries(item)">
              <div class="series-card__meta">
                <span>{{ item.seriesCode || v4PublicRoundLabel(item.roundName, item.roundNo) }}</span>
                <span>{{ v4BestOfLabel(item.bestOf) }}</span>
                <span>{{ seriesStatusLabel(item) }}</span>
              </div>
              <div class="series-card__versus">
                <strong>{{ teamName(item, 'home') }}</strong>
                <span class="series-card__vs">VS</span>
                <strong>{{ teamName(item, 'away') }}</strong>
              </div>
              <div class="series-card__foot">
                <span>{{ formatScheduleTime(item) }}</span>
                <span v-if="seriesScoreText(item)" class="series-card__score">{{ isSeriesFinished(item) ? '最终比分 ' : '' }}{{ seriesScoreText(item) }}</span>
                <span v-if="item.resultReason" class="dq-reason">{{ item.resultReason }}</span>
              </div>
            </article>
          </section>
          <div v-if="!scheduleByRound.length" class="series-empty">当前筛选条件下暂无赛程</div>
        </div>

        <MatchList
          v-else
          :matches="pagedMatches"
          empty-text="当前筛选条件下暂无赛程"
          @open-detail="openDetail"
        />

        <div v-if="state === 'success' && showPagination" class="schedule-pagination">
          <span class="schedule-pagination__total">共 {{ total }} 场</span>
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            :pager-count="5"
            layout="prev, pager, next"
            prev-text="上一页"
            next-text="下一页"
            hide-on-single-page
          />
        </div>
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
            {{ seriesDetail.homeTeamName || '待定' }}
            <strong>{{ formatSeriesSetScore({
              homeWinCount: seriesDetail.homeWinCount,
              awayWinCount: seriesDetail.awayWinCount,
              bestOf: seriesDetail.bestOf,
              seriesStatus: seriesDetail.seriesStatus,
              resolutionType: seriesDetail.resolutionType,
              winnerEventTeamId: seriesDetail.winnerEventTeamId,
              homeEventTeamId: seriesDetail.homeEventTeamId,
              awayEventTeamId: seriesDetail.awayEventTeamId,
            }) }}</strong>
            {{ seriesDetail.awayTeamName || '待定' }}
          </p>
          <p class="series-modal__meta">
            {{ seriesDetail.seriesCode || '系列赛' }}
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
    clamp(28px, 2.4vw, 42px)
    var(--cnpa-public-content-gutter)
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
  position: relative;
  border: 1px solid rgba(10, 10, 10, 0.12);
  background: #fff;
  padding: 16px 18px;
  margin-bottom: 10px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(25, 26, 28, 0.06);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

  &:hover:not(.series-card--disabled) {
    border-color: rgba(225, 6, 19, 0.38);
    box-shadow: 0 14px 28px rgba(25, 26, 28, 0.1);
    transform: translateY(-2px);
  }

  &--disabled {
    cursor: not-allowed;
    opacity: 0.7;
  }

  &--finished .series-card__score {
    color: #111;
    font-weight: 800;
  }
}
.series-card__meta,
.series-card__foot {
  display: flex;
  gap: 12px;
  color: rgba(10, 10, 10, 0.55);
  font-size: 12px;
  font-weight: 700;
}

.series-card__meta span:last-child {
  margin-left: auto;
  color: var(--cnpa-red, #e10613);
}

.series-card__versus {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 12px;
  align-items: center;
  margin: 10px 0;
  font-size: 16px;
}

.series-card__versus strong:first-child {
  text-align: right;
}

.series-card__versus strong:last-child {
  text-align: left;
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

.schedule-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
}

.schedule-pagination__total {
  font-size: 13px;
  font-weight: 700;
  color: rgba(10, 10, 10, 0.55);
}

.schedule-pagination :deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.schedule-pagination :deep(.el-pager li) {
  font-weight: 700;
}

.schedule-pagination :deep(.el-pager li.is-active) {
  background: var(--cnpa-red);
  color: #fff;
}

.schedule-pagination :deep(.btn-prev),
.schedule-pagination :deep(.btn-next) {
  font-weight: 700;
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
  color: #b91c1c;
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
  border-top: 1px solid rgba(10, 10, 10, 0.08);
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

  .schedule-head {
    flex-direction: column;
  }

  .series-card__meta,
  .series-card__foot {
    flex-wrap: wrap;
  }

  .series-card__versus {
    grid-template-columns: minmax(0, 1fr) 32px minmax(0, 1fr);
    font-size: 14px;
  }
}
</style>
