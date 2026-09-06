<script setup lang="ts">
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import TeamLogo from './TeamLogo.vue'
import { publicContestDetailLabel } from '@/utils/v4Display'
import {
  formatMatchNo,
  formatScheduleClock,
  formatStageRoundBlock,
  isScheduleSeries,
  isWinningSide,
  scheduleBestOfLabel,
  scheduleScoreView,
  scheduleStatusLabel,
  scheduleStatusTone,
  toScheduleTeam,
  type ScheduleScoreView,
} from '../utils/scheduleRowDisplay'

defineProps<{
  items: V4PublicScheduleItem[]
  hideDateOnRow?: boolean
  compactActions?: boolean
  emptyText?: string
}>()

const emit = defineEmits<{
  openMatch: [item: V4PublicScheduleItem]
  openSeries: [item: V4PublicScheduleItem]
  openStats: [item: V4PublicScheduleItem]
}>()

function clockText(item: V4PublicScheduleItem): string {
  return formatScheduleClock(item.startTime) || '待定'
}

function matchNoText(item: V4PublicScheduleItem): string {
  return formatMatchNo(item)
}

function scoreView(item: V4PublicScheduleItem): ScheduleScoreView {
  return scheduleScoreView(item)
}

function hasSplitScore(view: ScheduleScoreView): boolean {
  return view.left != null && view.right != null
}

function isUpcoming(item: V4PublicScheduleItem): boolean {
  return scheduleStatusTone(item) === 'scheduled' || scheduleStatusTone(item) === 'cancelled'
}

function canOpenDetail(item: V4PublicScheduleItem): boolean {
  if (!item.id) return false
  if (isScheduleSeries(item)) return true
  return !isUpcoming(item)
}

function onOpenDetail(item: V4PublicScheduleItem) {
  if (!canOpenDetail(item)) return
  if (isScheduleSeries(item)) {
    emit('openSeries', item)
    return
  }
  emit('openMatch', item)
}

function detailLabel(item: V4PublicScheduleItem): string {
  return publicContestDetailLabel(item.unitType)
}

function teamName(item: V4PublicScheduleItem, side: 'home' | 'away'): string {
  return toScheduleTeam(item, side).name
}

function stageText(item: V4PublicScheduleItem): string {
  return formatStageRoundBlock(item).stage || '常规赛'
}

function roundText(item: V4PublicScheduleItem): string {
  return formatStageRoundBlock(item).round
}
</script>

<template>
  <div class="schedule-table">
    <div class="schedule-grid schedule-header" aria-hidden="true">
      <div>时间</div>
      <div>客队</div>
      <div>比分</div>
      <div>主队</div>
      <div>赛事阶段</div>
      <div>BO</div>
      <div>状态</div>
      <div>操作</div>
    </div>

    <p v-if="items.length === 0" class="schedule-table__empty">{{ emptyText || '当前筛选条件下暂无赛程' }}</p>

    <article
      v-for="item in items"
      :key="`${item.unitType}-${item.id}`"
      class="schedule-grid match-item"
    >
      <div class="match-time">
        <strong>{{ clockText(item) }}</strong>
        <span v-if="matchNoText(item)" class="match-no">{{ matchNoText(item) }}</span>
      </div>

      <TeamEntryLink :team="toScheduleTeam(item, 'away')" class="team-cell team-cell--away">
        <span class="team-name" :title="teamName(item, 'away')">{{ teamName(item, 'away') }}</span>
        <TeamLogo :team="toScheduleTeam(item, 'away')" size="schedule" />
      </TeamEntryLink>

      <div class="score-cell">
        <template v-if="hasSplitScore(scoreView(item))">
          <div class="score">
            <span :class="{ loser: !isWinningSide(item, 'away') && isWinningSide(item, 'home') }">
              {{ scoreView(item).left }}
            </span>
            <span class="score-colon">:</span>
            <span :class="{ loser: !isWinningSide(item, 'home') && isWinningSide(item, 'away') }">
              {{ scoreView(item).right }}
            </span>
          </div>
        </template>
        <div v-else-if="scoreView(item).kind === 'forfeit'" class="score score--forfeit">
          {{ [scoreView(item).leftLabel, scoreView(item).rightLabel].filter(Boolean).join(' / ') || scoreView(item).caption || '弃权' }}
        </div>
        <div v-else class="score-vs">VS</div>
      </div>

      <TeamEntryLink :team="toScheduleTeam(item, 'home')" class="team-cell team-cell--home">
        <TeamLogo :team="toScheduleTeam(item, 'home')" size="schedule" />
        <span class="team-name" :title="teamName(item, 'home')">{{ teamName(item, 'home') }}</span>
      </TeamEntryLink>

      <div class="stage-cell">
        <span class="stage-name">{{ stageText(item) }}</span>
        <span v-if="roundText(item)" class="round-name">{{ roundText(item) }}</span>
      </div>

      <div class="bo-cell">
        <span class="bo-badge">{{ scheduleBestOfLabel(item) }}</span>
      </div>

      <div class="status-cell">
        <span class="status-badge" :class="scheduleStatusTone(item)">
          {{ scheduleStatusLabel(item) }}
        </span>
      </div>

      <div class="action-cell">
        <button
          v-if="!canOpenDetail(item)"
          type="button"
          class="match-action-btn waiting-btn"
          disabled
        >
          <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
            <rect x="3.2" y="7" width="9.6" height="7" rx="1.2" fill="none" stroke="currentColor" stroke-width="1.4" />
            <path d="M5.2 7V5.3a2.8 2.8 0 0 1 5.6 0V7" fill="none" stroke="currentColor" stroke-width="1.4" />
          </svg>
          敬请期待
        </button>
        <button
          v-else
          type="button"
          class="match-action-btn"
          @click="onOpenDetail(item)"
        >
          {{ detailLabel(item) }}
        </button>
      </div>
    </article>
  </div>
</template>

<style scoped lang="scss">
.schedule-table {
  --schedule-columns:
    minmax(72px, 0.9fr)
    minmax(140px, 1.35fr)
    minmax(84px, 0.85fr)
    minmax(140px, 1.35fr)
    minmax(88px, 1.15fr)
    minmax(48px, 0.65fr)
    minmax(60px, 0.75fr)
    minmax(88px, 1.1fr);
  margin-top: 8px;
  border: 1px solid #e8e6e3;
  background: #fff;
  overflow-x: auto;
}

.schedule-grid {
  display: grid;
  grid-template-columns: var(--schedule-columns);
  align-items: center;
  justify-items: stretch;
}

@media (min-width: 1024px) {
  .schedule-grid {
    min-width: 1040px;
  }
}

.schedule-header {
  min-height: 40px;
  background: #111;
  color: #fff;
  font-size: 12px;
  font-weight: 700;

  > div {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 0;
    padding: 0 8px;
    box-sizing: border-box;
    text-align: center;
  }
}

.match-item {
  min-height: 70px;
  border-bottom: 1px solid #eee;

  &:last-child {
    border-bottom: 0;
  }

  &:hover {
    background: #fafafa;
  }

  > :deep(*) {
    min-width: 0;
    padding: 8px;
    box-sizing: border-box;
  }
}

.match-time,
.score-cell,
.stage-cell,
.bo-cell,
.status-cell,
.action-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.match-time,
.stage-cell {
  flex-direction: column;
  gap: 4px;
}

.match-time {
  strong {
    font-size: 18px;
    line-height: 1;
    font-weight: 800;
    white-space: nowrap;
  }
}

.match-no {
  color: #aaa;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

.team-cell.team-cell--away,
.team-cell.team-cell--home,
:deep(.team-entry-link.team-cell) {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: inherit;
  text-align: center;
  text-decoration: none;
}

.team-name {
  flex: 0 1 auto;
  min-width: 0;
  max-width: calc(100% - 58px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 800;
}

.team-cell :deep(.team-logo),
.team-cell :deep(.team-logo-schedule) {
  flex-shrink: 0;
}

.team-cell :deep(.team-logo-schedule) {
  width: 46px;
  height: 46px;
}

.score {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0.5px;
  color: #111;
  white-space: nowrap;

  .loser {
    color: #888;
  }
}

.score-colon {
  margin: 0 8px;
  color: #aaa;
}

.score--forfeit {
  font-size: 14px;
}

.score-vs {
  font-size: 22px;
  font-weight: 900;
  color: #6f6f6f;
  letter-spacing: 2px;
}

.stage-cell {
  gap: 2px;
}

.stage-name {
  font-size: 14px;
  font-weight: 700;
}

.round-name {
  color: #888;
  font-size: 12px;
}

.bo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  height: 24px;
  padding: 4px 8px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  color: #222;
  font-size: 12px;
  font-weight: 800;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 2px;
  font-size: 12px;
  font-weight: 700;

  &.finished {
    background: #ececec;
    color: #444;
  }

  &.scheduled {
    background: #f5f5f5;
    color: #888;
  }

  &.live {
    background: #fdecee;
    color: var(--cnpa-red, #e51422);
  }

  &.forfeit {
    background: #fdece6;
    color: #c24a16;
  }

  &.cancelled {
    background: #ededed;
    color: #777;
  }
}

.match-action-btn {
  width: auto;
  min-width: 88px;
  height: 30px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  background: #fff;
  border: 1px solid #ddd;
  color: #333;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  text-decoration: none;
  cursor: pointer;

  &:not(:disabled):hover {
    border-color: var(--cnpa-red, #e51422);
    color: var(--cnpa-red, #e51422);
  }

  &:disabled {
    background: #f5f5f5;
    border-color: #e5e5e5;
    color: #bbb;
    cursor: not-allowed;
  }
}

.schedule-table__empty {
  margin: 0;
  padding: 48px 16px;
  text-align: center;
  color: #888;
  background: #fff;
}

@media (max-width: 1023px) {
  .schedule-header {
    display: none;
  }

  .match-item {
    grid-template-columns: 70px 1fr 88px 1fr;
    grid-template-areas:
      "time away score home"
      "stage stage bo status"
      "action action action action";
  }

  .match-time { grid-area: time; }
  .team-cell--away { grid-area: away; }
  .score-cell { grid-area: score; }
  .team-cell--home { grid-area: home; }
  .stage-cell { grid-area: stage; }
  .bo-cell { grid-area: bo; }
  .status-cell { grid-area: status; }
  .action-cell {
    grid-area: action;
    justify-content: flex-start;
    padding-top: 0;
  }

  .match-action-btn {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .match-item {
    display: grid;
    grid-template-columns: 1fr 80px 1fr;
    grid-template-areas:
      "time time time"
      "away score home"
      "stage stage stage"
      "bo status status"
      "action action action";
    padding-bottom: 8px;
  }

  .team-cell--away,
  .team-cell--home {
    flex-direction: column;
    text-align: center;
  }

  .team-name {
    max-width: 100%;
  }

  .team-cell--away {
    flex-direction: column-reverse;
  }

  .action-cell {
    justify-content: stretch;
  }
}
</style>
