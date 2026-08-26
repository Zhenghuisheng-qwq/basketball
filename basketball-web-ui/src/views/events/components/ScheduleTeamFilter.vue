<script setup lang="ts">
import type { ScheduleTeamOption } from '../utils/scheduleTeamFilter'
import TeamLogo from './TeamLogo.vue'
import type { Team } from '../types/events'

const props = defineProps<{
  teams: ScheduleTeamOption[]
  selectedTeamId: string | null
}>()

const emit = defineEmits<{
  selectTeam: [value: string | null]
}>()

function toTeam(option: ScheduleTeamOption): Team {
  return {
    id: Number(option.eventTeamId) || 0,
    teamId: option.eventTeamId,
    code: (option.shortName || option.name || '').slice(0, 2).toUpperCase(),
    name: option.name,
    shortName: option.name,
    logo: option.logoUrl || '',
    wins: 0,
    losses: 0,
  }
}

function isSelected(teamId: string | null): boolean {
  if (teamId == null) return props.selectedTeamId == null || props.selectedTeamId === ''
  return props.selectedTeamId === teamId
}
</script>
<template>
  <section class="team-filter" aria-label="选择参赛球队">
    <div class="team-filter__head">
      <span class="team-filter__rule" />
      <h3>选择参赛球队</h3>
      <span class="team-filter__rule" />
    </div>

    <div class="team-filter__grid">
      <button
        type="button"
        class="team-chip team-chip--all"
        :class="{ selected: isSelected(null) }"
        :aria-pressed="isSelected(null)"
        @click="emit('selectTeam', null)"
      >
        <span v-if="isSelected(null)" class="team-chip__check" aria-hidden="true">✓</span>
        <span class="team-chip__all-mark">全部</span>
        <span class="team-chip__name">全部球队</span>
      </button>

      <button
        v-for="team in teams"
        :key="team.eventTeamId"
        type="button"
        class="team-chip"
        :class="{ selected: isSelected(team.eventTeamId) }"
        :aria-pressed="isSelected(team.eventTeamId)"
        :title="team.name"
        @click="emit('selectTeam', team.eventTeamId)"
      >
        <span v-if="isSelected(team.eventTeamId)" class="team-chip__check" aria-hidden="true">✓</span>
        <TeamLogo :team="toTeam(team)" size="schedule" />
        <span class="team-chip__name">{{ team.name }}</span>
      </button>
    </div>
  </section>
</template>
<style scoped lang="scss">
.team-filter {
  margin: 0 0 20px;
  padding: 14px 4px 4px;
}

.team-filter__head {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.team-filter__head h3 {
  margin: 0;
  color: #5a5754;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.team-filter__rule {
  height: 1px;
  background: #e5e5e5;
}

.team-filter__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 10px;
}

.team-chip {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-height: 110px;
  padding: 12px 8px 10px;
  border: 2px solid transparent;
  background: #fff;
  color: #111;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;

  &:hover {
    background: #f7f5f2;
  }

  &.selected {
    border-color: var(--cnpa-red, #e10613);
    background: rgba(225, 6, 19, 0.06);
  }
}

.team-chip--all {
  justify-content: center;
}

.team-chip__all-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  background: #151515;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.team-chip__name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
}

.team-chip__check {
  position: absolute;
  top: 4px;
  right: 6px;
  color: var(--cnpa-red, #e10613);
  font-size: 12px;
  font-weight: 900;
}

@media (max-width: 1023px) {
  .team-filter__grid {
    grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  }
}

@media (max-width: 767px) {
  .team-filter__grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .team-chip {
    min-height: 92px;
    padding: 8px 4px;
  }
}

@media (max-width: 479px) {
  .team-filter__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
