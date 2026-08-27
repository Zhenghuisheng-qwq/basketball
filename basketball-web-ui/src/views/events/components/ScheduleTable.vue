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
    <div class="schedule-header" aria-hidden="true">
      <div>时间</div>
      <div class="end">客队</div>
      <div class="center">比分</div>
      <div>主队</div>
      <div>赛事阶段</div>
      <div class="center">BO</div>
      <div class="center">状态</div>
      <div class="end">操作</div>
    </div>

    <p v-if="items.length === 0" class="schedule-table__empty">{{ emptyText || '当前筛选条件下暂无赛程' }}</p>

    <article
      v-for="item in items"
      :key="`${item.unitType}-${item.id}`"
      class="match-item"
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
  --schedule-columns: 84px minmax(0, 1.12fr) 168px minmax(0, 1.12fr) minmax(0, 0.78fr) 62px 76px minmax(104px, 0.52fr);
  margin-top: 8px;
  border: 1px solid #e8e6e3;
  background: #fff;
  overflow-x: hidden;
}

.schedule-header,
.match-item {
  display: grid;
  grid-template-columns: var(--schedule-columns);
  align-items: center;
}

.schedule-header {
  min-height: 40px;
  background: #111;
  color: #fff;
  font-size: 12px;
  font-weight: 700;

  > div {
    padding: 0 12px;
    box-sizing: border-box;
  }

  .center {
    text-align: center;
  }

  .end {
    text-align: right;
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
    padding: 8px 12px;
    box-sizing: border-box;
  }
}

.match-time {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;

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
.team-cell.team-cell--home {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
}

.team-cell--away {
  justify-content: flex-end;
  text-align: right;
}

.team-cell--home {
  justify-content: flex-start;
  text-align: left;
}

.team-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 800;
}

.team-cell :deep(.team-logo-schedule) {
  width: 46px;
  height: 46px;
}

.score-cell {
  text-align: center;
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
  display: flex;
  flex-direction: column;
  justify-content: center;
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

.bo-cell {
  text-align: center;
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

.status-cell {
  text-align: center;
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

.action-cell {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.match-action-btn {
  width: 100%;
  min-width: 0;
  height: 30px;
  padding: 0 8px;
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

@media (max-width: 1439px) {
  .schedule-table {
    --schedule-columns: 76px minmax(0, 1.08fr) 148px minmax(0, 1.08fr) minmax(0, 0.76fr) 58px 72px minmax(96px, 0.5fr);
  }
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

  .team-cell--away {
    flex-direction: column-reverse;
  }

  .action-cell {
    justify-content: stretch;
  }
}
</style>
