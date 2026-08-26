<script setup lang="ts">
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import TeamLogo from './TeamLogo.vue'
import {
  formatMatchNo,
  formatScheduleClock,
  formatScheduleDateShort,
  formatStageRoundBlock,
  hasOfficialStats,
  isScheduleSeries,
  isWinningSide,
  scheduleBestOfLabel,
  scheduleReplayUrl,
  scheduleScoreView,
  scheduleStatusLabel,
  scheduleStatusTone,
  seriesProgressDots,
  toScheduleTeam,
  type ScheduleScoreView,
} from '../utils/scheduleRowDisplay'

const props = defineProps<{
  items: V4PublicScheduleItem[]
  hideDateOnRow?: boolean
  compactActions?: boolean
  emptyText?: string
}>()

const emit = defineEmits<{
  openMatch: [item: V4PublicScheduleItem]
  openSeries: [item: V4PublicScheduleItem]
  openStats: [item: V4PublicScheduleItem]
  openReplay: [item: V4PublicScheduleItem]
}>()

function clockText(item: V4PublicScheduleItem): string {
  return formatScheduleClock(item.startTime) || '待定'
}

function dateText(item: V4PublicScheduleItem): string {
  if (props.hideDateOnRow) return ''
  return formatScheduleDateShort(item.scheduleDate) || ''
}

function matchNoText(item: V4PublicScheduleItem): string {
  return formatMatchNo(item)
}

function scoreView(item: V4PublicScheduleItem): ScheduleScoreView {
  return scheduleScoreView(item)
}

function scoreText(view: ScheduleScoreView): string {
  if (view.kind === 'vs') return 'VS'
  if (view.left != null && view.right != null) return `${view.left} : ${view.right}`
  if (view.leftLabel || view.rightLabel) {
    return [view.leftLabel, view.rightLabel].filter(Boolean).join(' / ')
  }
  return view.caption || 'VS'
}

function hasSplitScore(view: ScheduleScoreView): boolean {
  return view.left != null && view.right != null
}

function actionMode(item: V4PublicScheduleItem): 'upcoming' | 'live' | 'finished' {
  const tone = scheduleStatusTone(item)
  if (tone === 'live') return 'live'
  if (tone === 'finished' || tone === 'forfeit') return 'finished'
  return 'upcoming'
}

function canReplay(item: V4PublicScheduleItem): boolean {
  return Boolean(scheduleReplayUrl(item))
}

function canStats(item: V4PublicScheduleItem): boolean {
  if (isScheduleSeries(item)) return true
  return hasOfficialStats(item)
}

function liveActionLabel(item: V4PublicScheduleItem): string {
  if (isScheduleSeries(item)) return '系列详情'
  return '比赛详情'
}

function onLiveAction(item: V4PublicScheduleItem) {
  if (isScheduleSeries(item)) {
    emit('openSeries', item)
    return
  }
  emit('openMatch', item)
}

function onStats(item: V4PublicScheduleItem) {
  if (!canStats(item)) return
  if (isScheduleSeries(item)) {
    emit('openSeries', item)
    return
  }
  emit('openStats', item)
}

</script>

<template>
  <div class="schedule-table">
    <div class="schedule-grid-header" aria-hidden="true">
      <span>时间</span>
      <span>客队</span>
      <span>比分</span>
      <span>主队</span>
      <span class="col-stage">赛事阶段</span>
      <span class="col-bo">赛制</span>
      <span>状态</span>
    </div>

    <p v-if="items.length === 0" class="schedule-table__empty">{{ emptyText || '当前筛选条件下暂无赛程' }}</p>

    <article
      v-for="item in items"
      :key="`${item.unitType}-${item.id}`"
      class="schedule-row"
    >
      <div class="schedule-main-row">
      <div class="cell time-cell">
        <strong>{{ clockText(item) }}</strong>
        <small v-if="dateText(item)">{{ dateText(item) }}</small>
        <small v-if="matchNoText(item)" class="match-no">{{ matchNoText(item) }}</small>
      </div>

      <TeamEntryLink :team="toScheduleTeam(item, 'away')" class="cell team-cell team-cell--away">
        <TeamLogo :team="toScheduleTeam(item, 'away')" size="schedule" />
        <span
          class="team-name"
          :class="{ 'is-winner': isWinningSide(item, 'away') }"
          :title="toScheduleTeam(item, 'away').name"
        >
          {{ toScheduleTeam(item, 'away').name }}
        </span>
      </TeamEntryLink>

      <div
        class="cell score-cell"
        :class="[
          `score-cell--${scoreView(item).kind}`,
          { 'score-cell--live': scheduleStatusTone(item) === 'live' },
        ]"
      >
        <small v-if="scoreView(item).kind === 'forfeit'" class="score-caption">
          {{ scoreView(item).caption || '弃权' }}
        </small>
        <strong
          v-if="hasSplitScore(scoreView(item))"
          class="score-main"
        >
          <span :class="{ win: isWinningSide(item, 'away'), lose: isWinningSide(item, 'home') }">
            {{ scoreView(item).left }}
          </span>
          <i>:</i>
          <span :class="{ win: isWinningSide(item, 'home'), lose: isWinningSide(item, 'away') }">
            {{ scoreView(item).right }}
          </span>
        </strong>
        <strong
          v-else
          class="score-main"
          :class="{ 'score-main--vs': scoreView(item).kind === 'vs' }"
        >
          {{ scoreText(scoreView(item)) }}
        </strong>
        <small v-if="scoreView(item).kind !== 'forfeit' && scoreView(item).caption" class="score-live">
          {{ scoreView(item).caption }}
        </small>
        <span v-if="seriesProgressDots(item).length" class="series-dots" aria-hidden="true">
          <i
            v-for="(filled, index) in seriesProgressDots(item)"
            :key="index"
            :class="{ filled }"
          />
        </span>
      </div>

      <TeamEntryLink :team="toScheduleTeam(item, 'home')" class="cell team-cell team-cell--home">
        <TeamLogo :team="toScheduleTeam(item, 'home')" size="schedule" />
        <span
          class="team-name"
          :class="{ 'is-winner': isWinningSide(item, 'home') }"
          :title="toScheduleTeam(item, 'home').name"
        >
          {{ toScheduleTeam(item, 'home').name }}
        </span>
      </TeamEntryLink>

      <div class="cell stage-cell">
        <strong v-if="formatStageRoundBlock(item).stage">{{ formatStageRoundBlock(item).stage }}</strong>
        <span v-if="formatStageRoundBlock(item).round">{{ formatStageRoundBlock(item).round }}</span>
        <span class="stage-bo">{{ scheduleBestOfLabel(item) }}</span>
      </div>

      <div class="cell bo-cell"><span class="bo-badge">{{ scheduleBestOfLabel(item) }}</span></div>

      <div class="cell status-cell">
        <span class="status-badge" :class="`status-badge--${scheduleStatusTone(item)}`">
          {{ scheduleStatusLabel(item) }}
        </span>
      </div>
      </div>

      <div class="schedule-actions-row">
        <button
          v-if="actionMode(item) === 'upcoming'"
          type="button"
          class="action-btn action-btn--disabled"
          disabled
        >
          敬请期待
        </button>
        <button
          v-else-if="actionMode(item) === 'live'"
          type="button"
          class="action-btn action-btn--primary"
          @click="onLiveAction(item)"
        >
          {{ liveActionLabel(item) }}
        </button>
        <template v-else-if="isScheduleSeries(item)">
          <a
            v-if="canReplay(item)"
            class="action-btn"
            :href="scheduleReplayUrl(item)"
            target="_blank"
            rel="noopener noreferrer"
          >比赛录像</a>
          <button v-else type="button" class="action-btn action-btn--disabled" disabled>比赛录像</button>
          <button type="button" class="action-btn action-btn--emphasis" @click="onLiveAction(item)">系列详情</button>
        </template>
        <template v-else>
          <a
            v-if="canReplay(item)"
            class="action-btn"
            :href="scheduleReplayUrl(item)"
            target="_blank"
            rel="noopener noreferrer"
          >比赛录像</a>
          <button v-else type="button" class="action-btn action-btn--disabled" disabled>比赛录像</button>
          <button
            type="button"
            class="action-btn"
            :class="{ 'action-btn--emphasis': canStats(item), 'action-btn--disabled': !canStats(item) }"
            :disabled="!canStats(item)"
            @click="onStats(item)"
          >技术统计</button>
        </template>
      </div>
    </article>
  </div>
</template>

<style scoped lang="scss">
.schedule-table {
  --schedule-columns: 90px minmax(180px, 1fr) 130px minmax(180px, 1fr) 150px 80px 100px;
  border: 1px solid #e5e5e5;
  background: #f7f5f2;
}

.schedule-grid-header,
.schedule-main-row {
  display: grid;
  grid-template-columns: var(--schedule-columns);
  align-items: center;
  gap: 10px;
  padding: 0 20px;
}

.schedule-grid-header {
  min-height: 36px;
  background: #eceae7;
  color: #5a5754;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.schedule-row {
  background: #fff;
  border-top: 1px solid #eee;

  &:nth-child(odd) {
    background: #faf9f7;
  }

  &:hover {
    background: #f4f1ed;
  }
}

.cell {
  min-width: 0;
}

.schedule-main-row {
  min-height: 62px;
}

.cell {
  min-width: 0;
}

.time-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #111;

  strong {
    font-size: 18px;
    font-weight: 800;
    line-height: 1.1;
  }

  small {
    color: rgba(17, 17, 17, 0.48);
    font-size: 11px;
    font-weight: 700;
  }
}

.match-no {
  color: #aaa;
  font-size: 10px;
  font-weight: 600;
}

.team-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.team-cell--away {
  justify-content: flex-end;
}

.team-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 700;
  color: #1a1918;

  &.is-winner {
    font-weight: 800;
    color: #111;
  }
}

.score-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 2px;
}

.score-main {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  font-size: 26px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 1px;
  color: #111;
  font-variant-numeric: tabular-nums;

  i {
    font-style: normal;
    font-weight: 800;
    color: #bbb;
  }

  .lose {
    color: #666;
    font-weight: 700;
  }

  .win {
    color: #111;
  }

  &--vs {
    font-size: 22px;
    font-weight: 800;
    color: rgba(17, 17, 17, 0.42);
    letter-spacing: 0.12em;
  }
}

.score-caption,
.score-live {
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
}

.score-caption {
  color: #c2410c;
}

.score-live,
.score-cell--live .score-live {
  color: var(--cnpa-red, #e10613);
}

.series-dots {
  display: flex;
  gap: 4px;
  margin-top: 2px;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #d4d0cc;

    &.filled {
      background: #111;
    }
  }
}

.stage-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #6b6763;
  font-size: 12px;
  font-weight: 700;

  strong {
    color: #111;
    font-size: 14px;
    font-weight: 800;
  }
}

.stage-bo,
.col-bo {
  display: none;
}

.bo-cell {
  display: flex;
  align-items: center;
}

.bo-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border: 1px solid #ddd;
  background: #f2f2f2;
  color: #333;
  font-size: 12px;
  font-weight: 700;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 800;

  &--scheduled {
    background: #f2f2f2;
    color: #8a8682;
  }

  &--live {
    background: rgba(225, 6, 19, 0.1);
    color: var(--cnpa-red, #e10613);
  }

  &--finished {
    background: #eceae7;
    color: #5a5754;
  }

  &--forfeit {
    background: #fff1e8;
    color: #c2410c;
  }

  &--cancelled {
    background: #f2f2f2;
    color: #b0aba6;
  }
}

.schedule-actions-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 20px;
  border-top: 1px solid #eee;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 12px;
  border: 1px solid #bbb;
  background: #fff;
  color: #171717;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  text-decoration: none;
  cursor: pointer;

  &:hover:not(:disabled) {
    border-color: var(--cnpa-red, #e10613);
    color: var(--cnpa-red, #e10613);
  }

  &--primary {
    border-color: var(--cnpa-red, #e10613);
    color: var(--cnpa-red, #e10613);
  }

  &--emphasis {
    background: #111;
    border-color: #111;
    color: #fff;

    &:hover:not(:disabled) {
      background: #fff;
      border-color: var(--cnpa-red, #e10613);
      color: var(--cnpa-red, #e10613);
    }
  }

  &--disabled,
  &:disabled {
    color: #aaa;
    background: #f2f2f2;
    border-color: #e5e5e5;
    cursor: not-allowed;
  }
}

.schedule-table__empty {
  margin: 0;
  padding: 48px 16px;
  text-align: center;
  color: rgba(17, 17, 17, 0.5);
  font-size: 14px;
  background: #fff;
}

@media (max-width: 1439px) {
  .schedule-table {
    --schedule-columns: 84px minmax(160px, 1fr) 120px minmax(160px, 1fr) 140px 72px 92px;
  }

  .team-name {
    font-size: 15px;
  }
}

@media (max-width: 1199px) {
  .schedule-table {
    --schedule-columns: 80px minmax(0, 1fr) 112px minmax(0, 1fr) 168px 88px;
  }

  .col-bo,
  .bo-cell {
    display: none;
  }

  .stage-bo {
    display: inline;
    margin-top: 2px;
    color: #8a8682;
  }

  .score-main {
    font-size: 22px;
  }
}

@media (max-width: 767px) {
  .schedule-grid-header {
    display: none;
  }

  .schedule-main-row {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'time status'
      'away away'
      'score score'
      'home home'
      'stage stage';
    gap: 6px;
    padding: 12px 12px 8px;
    min-height: 0;
  }

  .time-cell { grid-area: time; }
  .status-cell { grid-area: status; justify-self: end; }
  .team-cell--away { grid-area: away; justify-content: flex-start; }
  .score-cell { grid-area: score; padding: 6px 0; }
  .team-cell--home { grid-area: home; }
  .stage-cell { grid-area: stage; }

  .bo-cell {
    display: none;
  }

  .stage-bo {
    display: inline;
  }

  .schedule-actions-row {
    justify-content: stretch;
    padding: 8px 12px 10px;
  }

  .schedule-actions-row .action-btn {
    flex: 1;
  }

  .score-main {
    font-size: 28px;
  }
}
</style>
