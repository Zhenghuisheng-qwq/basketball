<script setup lang="ts">
import { getTeamById } from '../mock/teams.mock'
import type { Match, MatchStatus } from '../types/events'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import TeamLogo from './TeamLogo.vue'

defineProps<{
  matches: Match[]
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

    <div v-if="matches.length === 0" class="match-empty">当日暂无比赛</div>

    <article v-for="match in matches" :key="match.id" class="match-row">
      <span class="cell time">{{ match.startTime }}</span>

      <TeamEntryLink
        v-if="getTeamById(match.awayTeamId)"
        :team="getTeamById(match.awayTeamId)!"
        class="cell team-cell"
      >
        <TeamLogo :team="getTeamById(match.awayTeamId)!" size="sm" />
        <span>{{ getTeamById(match.awayTeamId)!.name }}</span>
      </TeamEntryLink>

      <div class="cell score-cell sport-num">
        <template v-if="match.status === 'finished' || match.status === 'live'">
          {{ match.awayScore }} : {{ match.homeScore }}
        </template>
        <template v-else-if="match.status === 'cancelled'">-</template>
        <template v-else>VS</template>
      </div>

      <TeamEntryLink
        v-if="getTeamById(match.homeTeamId)"
        :team="getTeamById(match.homeTeamId)!"
        class="cell team-cell"
      >
        <TeamLogo :team="getTeamById(match.homeTeamId)!" size="sm" />
        <span>{{ getTeamById(match.homeTeamId)!.name }}</span>
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
  min-height: 108px;
  border-top: 1px solid rgba(10, 10, 10, 0.1);
  transition: background 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.42);
  }
}

.team-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  color: inherit;
}

.score-cell {
  text-align: center;
  font-size: clamp(22px, 2.2vw, 30px);
  font-weight: 900;
}

.venue {
  font-size: 13px;
  color: rgba(10, 10, 10, 0.72);
}

.status-live {
  color: var(--cnpa-red);
  font-weight: 800;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  height: 32px;
  border: 1px solid var(--cnpa-paper-border);
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.match-empty {
  padding: 48px 16px;
  text-align: center;
  color: rgba(10, 10, 10, 0.55);
  font-size: 14px;
}

@media (max-width: 992px) {
  .match-head {
    display: none;
  }

  .match-row {
    grid-template-columns: 1fr;
    padding: 16px;
    gap: 10px;
  }

  .actions {
    flex-direction: row;
  }
}
</style>
