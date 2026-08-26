<script setup lang="ts">
import { computed } from 'vue'
import { sundayWeekDays, weekNumberOf } from '../utils/scheduleCalendar'
import { todayDateKey } from '@/utils/scheduleDateKey'

const props = defineProps<{
  selectedDate: string
  weekStart: string
  weekNumber?: number
  dateCounts?: Record<string, number>
  calendarExpanded?: boolean
  teamFilterExpanded?: boolean
  selectedTeamName?: string
}>()

const emit = defineEmits<{
  selectDate: [value: string]
  clearDate: []
  prevWeek: []
  nextWeek: []
  toggleCalendar: []
  toggleTeamFilter: []
}>()

const today = computed(() => todayDateKey())
const weekDays = computed(() => sundayWeekDays(props.weekStart, props.dateCounts || {}))
const weekLabel = computed(() => `第${props.weekNumber || weekNumberOf(props.weekStart)}周`)
const teamButtonLabel = computed(() => props.selectedTeamName || '选择球队')

function onSelect(date: string) {
  if (date && date === props.selectedDate) {
    emit('clearDate')
    return
  }
  emit('selectDate', date)
}

function countText(count: number) {
  return count > 0 ? `${count}场` : ''
}
</script>

<template>
  <section class="calendar-toolbar" aria-label="赛程周日历">
    <div class="week-control">
      <button type="button" class="arrow-btn" aria-label="上一周" @click="emit('prevWeek')">‹</button>
      <strong class="week-number">{{ weekLabel }}</strong>
      <button type="button" class="arrow-btn" aria-label="下一周" @click="emit('nextWeek')">›</button>
    </div>

    <div class="week-days" role="tablist">
      <button
        type="button"
        class="day-item all"
        :class="{ active: !selectedDate }"
        :aria-pressed="!selectedDate"
        @click="emit('clearDate')"
      >
        <span class="day-main">全部 ALL</span>
      </button>

      <button
        v-for="day in weekDays"
        :key="day.date"
        type="button"
        class="day-item"
        :class="{
          active: day.date === selectedDate,
          today: day.date === today,
          'has-match': day.count > 0,
        }"
        :aria-pressed="day.date === selectedDate"
        @click="onSelect(day.date)"
      >
        <span class="day-main">{{ day.weekday }} {{ day.dateText }}</span>
        <span v-if="countText(day.count)" class="match-count">{{ countText(day.count) }}</span>
      </button>
    </div>

    <div class="calendar-tools">
      <button
        type="button"
        class="toolbar-btn calendar-btn"
        :class="{ active: calendarExpanded }"
        :aria-expanded="calendarExpanded"
        @click="emit('toggleCalendar')"
      >
        <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
          <rect x="1.5" y="2.5" width="13" height="12" rx="1.2" fill="none" stroke="currentColor" stroke-width="1.4" />
          <path d="M1.5 6h13M5 1.2v3.2M11 1.2v3.2" fill="none" stroke="currentColor" stroke-width="1.4" />
        </svg>
        <span>日历</span>
      </button>
      <button
        type="button"
        class="toolbar-btn team-btn"
        :class="{ active: teamFilterExpanded }"
        :aria-expanded="teamFilterExpanded"
        @click="emit('toggleTeamFilter')"
      >
        <span>{{ teamButtonLabel }}</span>
        <svg viewBox="0 0 12 8" width="10" height="7" aria-hidden="true">
          <path d="M1 1.5 L6 6.5 L11 1.5" fill="none" stroke="currentColor" stroke-width="1.6" />
        </svg>
      </button>
    </div>
  </section>
</template>

<style scoped lang="scss">
.calendar-toolbar {
  min-height: 68px;
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto;
  align-items: stretch;
  padding: 0 12px;
  color: #fff;
  background: #111;
}

.week-control {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
}

.week-number {
  min-width: 46px;
  text-align: center;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.4px;
}

.arrow-btn,
.toolbar-btn {
  border: 1px solid #3a3a3a;
  background: #1a1a1a;
  color: #fff;
  font-family: inherit;
  cursor: pointer;
}

.arrow-btn {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-color: #4a4a4a;
  background: transparent;
  font-size: 20px;
  line-height: 1;

  &:hover {
    border-color: #fff;
  }
}

.week-days {
  display: grid;
  grid-template-columns: 80px repeat(7, minmax(72px, 1fr));
  min-height: 68px;
}

.day-item {
  min-width: 0;
  padding: 10px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border: 0;
  background: transparent;
  color: #d8d8d8;
  font-family: inherit;
  cursor: pointer;
  position: relative;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .day-main {
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
  }

  .match-count {
    font-size: 11px;
    color: #888;
    font-weight: 600;
  }

  &.all .day-main {
    color: #d8d8d8;
    font-weight: 800;
  }

  &.active {
    background: var(--cnpa-red, #e51422);
    color: #fff;

    .day-main,
    .match-count {
      color: #fff;
    }
  }

  &.all.active {
    background: rgba(229, 20, 34, 0.16);

    .day-main {
      color: #ff5a63;
    }

    &::after {
      content: '';
      position: absolute;
      left: 18%;
      right: 18%;
      bottom: 0;
      height: 2px;
      border-radius: 0;
      background: var(--cnpa-red, #e51422);
    }
  }

  &.has-match:not(.active)::after {
    content: '';
    position: absolute;
    bottom: 6px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--cnpa-red, #e51422);
  }
}

.calendar-tools {
  margin-left: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-btn {
  height: 36px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 700;

  &:hover,
  &.active {
    background: var(--cnpa-red, #e51422);
    border-color: var(--cnpa-red, #e51422);
  }
}

.calendar-btn {
  min-width: 78px;
}

.team-btn {
  min-width: 108px;
}

@media (max-width: 1200px) {
  .calendar-toolbar {
    grid-template-columns: 132px minmax(0, 1fr);
    padding-top: 8px;
  }

  .calendar-tools {
    grid-column: 1 / -1;
    margin: 0;
    padding: 8px 0 12px;
    justify-content: flex-end;
  }

  .week-days {
    grid-template-columns: 72px repeat(7, minmax(68px, 1fr));
  }
}

@media (max-width: 900px) {
  .calendar-toolbar {
    display: block;
    padding: 10px 12px 12px;
  }

  .week-control {
    margin-bottom: 8px;
  }

  .week-days {
    overflow-x: auto;
    grid-template-columns: 78px repeat(7, 86px);
    min-height: 64px;
  }

  .calendar-tools {
    margin-top: 10px;
  }
}
</style>
