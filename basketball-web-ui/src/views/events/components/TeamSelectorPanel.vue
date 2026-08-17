<script setup lang="ts">
import type { ScheduleTeamOption } from '../utils/scheduleTeamFilter'
import type { Team } from '../types/events'
import TeamLogo from './TeamLogo.vue'

defineProps<{
  selectedTeamId: string | null
  teams: ScheduleTeamOption[]
  loading?: boolean
  error?: boolean
}>()

const emit = defineEmits<{
  select: [teamId: string | null]
}>()

function toTeam(option: ScheduleTeamOption): Team {
  return {
    id: 0,
    teamId: option.eventTeamId,
    code: option.shortName || option.name,
    name: option.name,
    shortName: option.shortName || option.name,
    logo: option.logoUrl || '',
    wins: 0,
    losses: 0,
  }
}

function displayName(option: ScheduleTeamOption): string {
  return option.shortName || option.name
}
</script>
<template>
  <div
    class="team-panel schedule-panel cnpa-paper-section cnpa-paper-section--gray cnpa-paper-floating-panel"
    @click.stop
  >
    <h3>选择参赛战队</h3>
    <p v-if="loading" class="team-hint">赛程加载中…</p>
    <p v-else-if="error" class="team-hint">赛程加载失败，请稍后重试</p>
    <div v-else class="team-list">
      <button
        type="button"
        class="team-item"
        :class="{ active: selectedTeamId == null || selectedTeamId === '' }"
        @click="emit('select', null)"
      >
        <span class="team-all">ALL</span>
        <span>全部战队</span>
        <span v-if="selectedTeamId == null || selectedTeamId === ''" class="check">●</span>
      </button>

      <p v-if="!teams.length" class="team-empty">当日暂无参赛球队</p>

      <button
        v-for="team in teams"
        :key="team.eventTeamId"
        type="button"
        class="team-item"
        :class="{ active: selectedTeamId === team.eventTeamId }"
        @click="emit('select', team.eventTeamId)"
      >
        <TeamLogo :team="toTeam(team)" size="sm" />
        <span>{{ displayName(team) }}</span>
        <span v-if="selectedTeamId === team.eventTeamId" class="check">●</span>
      </button>
    </div>
  </div>
</template>
<style scoped lang="scss">
.team-panel {
  width: min(920px, calc(100vw - 32px));
  padding: 20px;

  h3 {
    margin: 0 0 16px;
    font-size: 16px;
    color: #111;
  }
}

.team-hint,
.team-empty {
  margin: 0;
  padding: 12px 4px;
  color: rgba(10, 10, 10, 0.55);
  font-size: 13px;
  font-weight: 700;
}

.team-empty {
  grid-column: 1 / -1;
}

.team-list {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 10px;
}

.team-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-height: 92px;
  padding: 10px 8px;
  border: 1px solid rgba(10, 10, 10, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: #111;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover,
  &.active {
    border-color: var(--cnpa-red);
    box-shadow: inset 0 0 0 1px rgba(225, 6, 19, 0.18);
    background: rgba(225, 6, 19, 0.06);
  }
}

.team-all {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #111;
  color: #fff;
  font-size: 11px;
}

.check {
  position: absolute;
  top: 6px;
  right: 8px;
  color: var(--cnpa-red);
  font-size: 10px;
}

@media (max-width: 992px) {
  .team-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .team-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .team-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
