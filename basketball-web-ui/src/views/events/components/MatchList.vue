<script setup lang="ts">
import type { Match, MatchStatus } from '../types/events'
import { matchAwayTeam, matchHomeTeam } from '../utils/mapPublic'
import { displayNumber } from '@/utils/publicDisplay'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import TeamLogo from './TeamLogo.vue'

defineProps<{
  matches: Match[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  openDetail: [match: Match]
  openStats: [match: Match]
}>()

const statusMap: Record<
  MatchStatus,
  { label: string; className: string; disabled?: boolean }
> = {
  finished: { label: '已结束', className: 'status-finished' },
  scheduled: { label: '未开始', className: 'status-scheduled' },
  live: { label: '进行中', className: 'status-live' },
  cancelled: { label: '已取消', className: 'status-cancelled', disabled: true },
}
</script>

<template>
  <div class="match-list">
    <div class="match-head">
      <span>时间</span>
      <span>客队</span>
      <span>比分</span>
      <span>主队</span>
      <span>比赛场馆</span>
      <span>比赛</span>
      <span>数据</span>
    </div>

    <div v-if="loading" class="match-empty">加载中...</div>
    <div v-else-if="error" class="match-empty is-error">{{ error }}</div>
    <div v-else-if="matches.length === 0" class="match-empty">当日暂无比赛</div>

    <template v-else>
      <article v-for="match in matches" :key="match.matchId" class="match-row">
        <span class="cell time">{{ match.startTime }}</span>

        <TeamEntryLink :team="matchAwayTeam(match)" class="cell team-cell">
          <TeamLogo :team="matchAwayTeam(match)" size="sm" />
          <span>{{ match.awayTeamName }}</span>
        </TeamEntryLink>

        <div class="cell score-cell sport-num">
          <template v-if="match.status === 'finished' || match.status === 'live'">
            {{ displayNumber(match.awayScore) }} : {{ displayNumber(match.homeScore) }}
          </template>
          <template v-else-if="match.status === 'cancelled'">-</template>
          <template v-else>VS</template>
        </div>

        <TeamEntryLink :team="matchHomeTeam(match)" class="cell team-cell">
          <TeamLogo :team="matchHomeTeam(match)" size="sm" />
          <span>{{ match.homeTeamName }}</span>
        </TeamEntryLink>

        <span class="cell venue">{{ match.venue }}</span>

        <span class="cell status" :class="statusMap[match.status].className">
          {{ statusMap[match.status].label }}
        </span>

        <div class="cell actions">
          <button
            type="button"
            class="action-btn"
            :disabled="statusMap[match.status].disabled"
            @click="emit('openDetail', match)"
          >
            比赛详情
          </button>
          <button
            type="button"
            class="action-btn"
            :disabled="statusMap[match.status].disabled"
            @click="emit('openStats', match)"
          >
            技术统计
          </button>
        </div>
      </article>
    </template>
  </div>
</template>

<style scoped lang="scss">
.match-list {
  border: 1px solid rgba(10, 10, 10, 0.12);
  background: #f1f0ed;
}

.match-head,
.match-row {
  display: grid;
  grid-template-columns: 72px 1.1fr 120px 1.1fr 1.4fr 80px 180px;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
}

.match-head {
  min-height: 48px;
  background: var(--cnpa-paper-surface);
  color: rgba(10, 10, 10, 0.55);
  font-size: 12px;
  font-weight: 800;
}

.match-row {
  min-height: 64px;
  border-top: 1px solid rgba(10, 10, 10, 0.08);
  background: #fff;
}

.match-empty {
  padding: 36px 16px;
  text-align: center;
  color: rgba(10, 10, 10, 0.45);
  font-weight: 600;

  &.is-error {
    color: #e10613;
  }
}

.cell {
  min-width: 0;
}

.team-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: inherit;
  text-decoration: none;
  font-weight: 700;
}

.score-cell {
  text-align: center;
  font-size: 18px;
  font-weight: 800;
}

.venue {
  color: rgba(10, 10, 10, 0.62);
  font-size: 13px;
}

.status {
  font-size: 12px;
  font-weight: 800;
}

.status-finished {
  color: rgba(10, 10, 10, 0.55);
}

.status-scheduled {
  color: #1565c0;
}

.status-live {
  color: #e10613;
}

.status-cancelled {
  color: rgba(10, 10, 10, 0.35);
}

.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  height: 32px;
  padding: 0 10px;
  border: 1px solid rgba(10, 10, 10, 0.16);
  background: #fff;
  color: #111;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

@media (max-width: 1100px) {
  .match-head,
  .match-row {
    grid-template-columns: 64px 1fr 90px 1fr 1fr 72px 1fr;
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .match-head {
    display: none;
  }

  .match-row {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    padding: 14px;
  }

  .actions {
    grid-column: 1 / -1;
  }
}
</style>
