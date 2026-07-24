<script setup lang="ts">
import { computed } from 'vue'
import type { Team } from '../types/events'
import CalendarPanel from './CalendarPanel.vue'
import TeamSelectorPanel from './TeamSelectorPanel.vue'

const props = defineProps<{
  selectedSeason: string
  seasonOptions: Array<{ id: string; label: string }>
  selectedDate: string
  selectedTeamId: string | null
  teams: Team[]
  isCalendarOpen: boolean
  isTeamSelectorOpen: boolean
}>()

const emit = defineEmits<{
  'update:selectedSeason': [value: string]
  toggleCalendar: []
  toggleTeamSelector: []
  selectDate: [value: string]
  selectTeam: [value: string | null]
  shiftMonths: [offset: number]
}>()

const selectedTeamLabel = computed(() => {
  if (props.selectedTeamId === null) return '选择球队'
  return props.teams.find((team) => team.teamId === props.selectedTeamId)?.name || '选择球队'
})
</script>

<template>
  <div class="schedule-filters">
    <div class="filter-row">
      <label class="filter-item">
        <select
          :value="selectedSeason"
          @change="emit('update:selectedSeason', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="season in seasonOptions" :key="season.id" :value="season.id">
            {{ season.label }}
          </option>
        </select>
      </label>

      <button
        type="button"
        class="filter-btn"
        :class="{ active: isCalendarOpen }"
        @click.stop="emit('toggleCalendar')"
      >
        日期日历
        <span class="arrow" :class="{ up: isCalendarOpen }">▼</span>
      </button>

      <button
        type="button"
        class="filter-btn"
        :class="{ active: isTeamSelectorOpen }"
        @click.stop="emit('toggleTeamSelector')"
      >
        {{ selectedTeamLabel }}
        <span class="arrow" :class="{ up: isTeamSelectorOpen }">▼</span>
      </button>
    </div>

    <CalendarPanel
      v-if="isCalendarOpen"
      class="schedule-panel"
      :selected-date="selectedDate"
      @select="emit('selectDate', $event)"
      @shift="emit('shiftMonths', $event)"
    />

    <TeamSelectorPanel
      v-if="isTeamSelectorOpen"
      class="schedule-panel"
      :teams="teams"
      :selected-team-id="selectedTeamId"
      @select="emit('selectTeam', $event)"
    />
  </div>
</template>

<style scoped lang="scss">
.schedule-filters {
  position: relative;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-item select,
.filter-btn {
  min-width: 120px;
  height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(17, 17, 17, 0.14);
  border-radius: 4px;
  background: #fff;
  color: #111;
  font-size: 13px;
  font-weight: 700;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  &.active {
    border-color: #e10613;
    color: #e10613;
  }
}

.arrow {
  font-size: 10px;
  transition: transform 0.15s ease;

  &.up {
    transform: rotate(180deg);
  }
}

.schedule-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 20;
}
</style>
