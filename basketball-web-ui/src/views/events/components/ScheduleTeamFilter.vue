<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ScheduleTeamOption } from '../utils/scheduleTeamFilter'
import TeamLogo from './TeamLogo.vue'
import type { Team } from '../types/events'

const COLLAPSE_AFTER = 16

const props = defineProps<{
  teams: ScheduleTeamOption[]
  selectedTeamId: string | null
}>()

const emit = defineEmits<{
  selectTeam: [value: string | null]
}>()

const expanded = ref(false)

const visibleTeams = computed(() => {
  if (expanded.value || props.teams.length <= COLLAPSE_AFTER) return props.teams
  return props.teams.slice(0, COLLAPSE_AFTER)
})

const canExpand = computed(() => props.teams.length > COLLAPSE_AFTER && !expanded.value)

function toTeam(option: ScheduleTeamOption): Team {
  const shortName = option.shortName || option.name
  return {
    id: Number(option.eventTeamId) || 0,
    teamId: option.eventTeamId,
    code: shortName || option.name,
    name: option.name,
    shortName: shortName || option.name,
    logo: option.logoUrl || '',
    wins: 0,
    losses: 0,
  }
}

function displayName(option: ScheduleTeamOption): string {
  return String(option.name || option.shortName || '球队').trim()
}

function isSelected(teamId: string | null): boolean {
  if (teamId == null) return props.selectedTeamId == null || props.selectedTeamId === ''
  return props.selectedTeamId === teamId
}
</script>

<template>
  <section class="team-filter" aria-label="选择参赛球队">
    <div class="team-filter-head">
      <button
        type="button"
        class="all-btn"
        :class="{ active: isSelected(null) }"
        :aria-pressed="isSelected(null)"
        @click="emit('selectTeam', null)"
      >
        全部球队
      </button>
      <span class="team-count">{{ teams.length }} 支参赛球队</span>
    </div>

    <div class="team-grid">
      <button
        v-for="team in visibleTeams"
        :key="team.eventTeamId"
        type="button"
        class="team-chip"
        :class="{ active: isSelected(team.eventTeamId) }"
        :aria-pressed="isSelected(team.eventTeamId)"
        :title="team.name"
        @click="emit('selectTeam', team.eventTeamId)"
      >
        <TeamLogo :team="toTeam(team)" size="schedule" />
        <span class="team-chip-name">{{ displayName(team) }}</span>
      </button>
    </div>

    <button
      v-if="canExpand"
      type="button"
      class="more-teams"
      @click="expanded = true"
    >
      更多球队
      <span aria-hidden="true">›</span>
    </button>
  </section>
</template>

<style scoped lang="scss">
.team-filter {
  padding: 12px 12px 10px;
  background: #faf9f7;
  border: 1px solid #eceae7;
  border-top: none;
  overflow-x: hidden;
}

.team-filter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.all-btn {
  min-height: 32px;
  padding: 0 12px;
  border: 2px solid transparent;
  background: #fff;
  color: #333;
  font-size: 13px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;

  &.active {
    border-color: var(--cnpa-red, #e51422);
    background: rgba(229, 20, 34, 0.04);
    color: var(--cnpa-red, #e51422);
  }
}

.team-count {
  color: #888;
  font-size: 12px;
  font-weight: 600;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  column-gap: 6px;
  row-gap: 7px;
  overflow-x: hidden;
}

.team-chip {
  position: relative;
  min-width: 0;
  height: 46px;
  padding: 0 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 2px solid transparent;
  background: #fff;
  color: #151515;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    background: #fff;
    border-color: #eceae7;
  }

  &.active {
    border-color: var(--cnpa-red, #e51422);
    background: rgba(229, 20, 34, 0.04);
  }

  &.active::after {
    content: '';
    position: absolute;
    top: 5px;
    right: 5px;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--cnpa-red, #e51422);
  }

  :deep(.team-logo-schedule) {
    width: 34px;
    height: 34px;
  }

  :deep(.team-logo-schedule .team-logo-fallback) {
    font-size: 11px;
  }
}

.team-chip-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
}

.more-teams {
  width: 100%;
  height: 34px;
  border: 0;
  border-top: 1px solid #eceae7;
  background: transparent;
  color: #666;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    color: var(--cnpa-red, #e51422);
  }
}

@media (max-width: 1440px) {
  .team-grid {
    grid-template-columns: repeat(8, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .team-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .team-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .team-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
