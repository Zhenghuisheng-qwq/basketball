<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import type { Match, MatchStatus } from '../types/events'
import { matchAwayTeam, matchHomeTeam } from '../utils/mapPublic'
import { buildTeamLookup } from '../utils/teamLookup'
import TeamLogo from './TeamLogo.vue'

const props = defineProps<{
  matches: Match[]
  emptyText?: string
}>()

const emit = defineEmits<{
  openDetail: [match: Match]
  openStats: [match: Match]
}>()

const { t } = useI18n()

const statusMap = computed<Record<
  MatchStatus,
  { label: string; className: string; disabled?: boolean }
>>(() => ({
  finished: { label: t('match.finished'), className: 'status-finished' },
  scheduled: { label: t('eventsPage.schedule.comingSoon'), className: 'status-scheduled', disabled: true },
  live: { label: t('match.live'), className: 'status-live' },
  cancelled: { label: t('match.cancelled'), className: 'status-cancelled', disabled: true },
}))

const teamLookup = computed(() => {
  const teams = props.matches.flatMap((match) => [matchAwayTeam(match), matchHomeTeam(match)])
  return buildTeamLookup(teams)
})

function getTeamById(id: number | string) {
  return teamLookup.value(id)
}
</script>
<template>
  <div class="match-list">
    <div class="match-head">
      <span>{{ t('eventsPage.schedule.time') }}</span>
      <span>{{ t('eventsPage.schedule.away') }}</span>
      <span>{{ t('eventsPage.schedule.score') }}</span>
      <span>{{ t('eventsPage.schedule.home') }}</span>
      <span>{{ t('eventsPage.schedule.venue') }}</span>
      <span>{{ t('eventsPage.schedule.matchStatus') }}</span>
      <span>{{ t('eventsPage.schedule.related') }}</span>
    </div>

    <div v-if="matches.length === 0" class="match-empty">{{ emptyText || t('eventsPage.schedule.emptyFilter') }}</div>

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
        <template v-if="match.resultType === 'EVENT_DISQUALIFICATION' || match.resultType === 'FORFEIT'">
          <span>{{ match.awayResultLabel || t('match.forfeitLoss') }}</span>
          <span class="vs">vs</span>
          <span>{{ match.homeResultLabel || t('eventsPage.schedule.forfeitWin') }}</span>
          <small v-if="match.resultReason" class="dq-reason">{{ match.resultReason }}</small>
        </template>
        <template v-else-if="match.status === 'finished' || match.status === 'live'">
          <small v-if="match.status === 'finished'" class="score-label">{{ t('eventsPage.schedule.finalScore') }}</small>
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
          disabled
        >
          {{ t('eventsPage.schedule.replay') }}
        </button>
        <button
          type="button"
          class="action-btn"
          :disabled="statusMap[match.status].disabled"
          @click="emit('openDetail', match)"
        >
          {{ t('eventsPage.schedule.boxScore') }}
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
  background: rgba(255, 255, 255, 0.55);
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

.score-label {
  display: block;
  margin-bottom: 5px;
  color: rgba(10, 10, 10, 0.48);
  font-family: inherit;
  font-size: 10px;
  font-weight: 800;
}

.score-cell .vs {
  margin: 0 6px;
  font-size: 14px;
}

.dq-reason {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  font-weight: 700;
  color: #b91c1c;
}

.venue {
  font-size: 13px;
  color: rgba(10, 10, 10, 0.72);
}

.status-live {
  color: var(--cnpa-red);
  font-weight: 800;
}

.status-finished {
  color: #111;
  font-weight: 800;
}

.status-scheduled,
.status-cancelled {
  color: rgba(10, 10, 10, 0.42);
  font-weight: 800;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  height: 32px;
  border: 1px solid rgba(10, 10, 10, 0.16);
  background: #fff;
  color: #111;
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
