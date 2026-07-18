<script setup lang="ts">
import { computed } from 'vue'
import { seasonOptions, teams } from '../mock'
import CalendarPanel from './CalendarPanel.vue'
import TeamSelectorPanel from './TeamSelectorPanel.vue'

const props = defineProps<{
  selectedSeason: string
  selectedDate: string
  selectedTeamId: number | null
  isCalendarOpen: boolean
  isTeamSelectorOpen: boolean
}>()

const emit = defineEmits<{
  'update:selectedSeason': [value: string]
  toggleCalendar: []
  toggleTeamSelector: []
  selectDate: [value: string]
  selectTeam: [value: number | null]
  shiftMonths: [offset: number]
}>()

const selectedTeamLabel = computed(() => {
  if (props.selectedTeamId === null) return '选择球队'
  return teams.find((team) => team.id === props.selectedTeamId)?.name || '选择球队'
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
          <option v-for="season in seasonOptions" :key="season" :value="season">
            {{ season }}
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
      :selected-team-id="selectedTeamId"
      @select="emit('selectTeam', $event)"
    />
  </div>
</template>

<style scoped lang="scss">
.schedule-filters {
  position: relative;
  z-index: 20;
}

.filter-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-item select,
.filter-btn {
  min-width: 140px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(10, 10, 10, 0.16);
  border-radius: 2px;
  background: #fff;
  color: #111;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  &.active {
    background: var(--cnpa-red);
    border-color: var(--cnpa-red);
    color: #fff;
  }
}

.arrow {
  font-size: 10px;
  transition: transform 0.2s ease;

  &.up {
    transform: rotate(180deg);
  }
}

:deep(.schedule-panel) {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 30;
}
</style>
