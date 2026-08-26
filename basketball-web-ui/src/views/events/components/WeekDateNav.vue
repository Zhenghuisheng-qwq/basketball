<script setup lang="ts">
import { computed } from 'vue'
import { sundayWeekDays, weekNumberOf } from '../utils/scheduleCalendar'
import { todayDateKey } from '@/utils/scheduleDateKey'

const props = defineProps<{
  selectedDate: string
  weekStart: string
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
const weekLabel = computed(() => '第' + weekNumberOf(props.weekStart) + '周')
const teamButtonLabel = computed(() => props.selectedTeamName || '选择球队')

function onSelect(date: string) {
  if (date && date === props.selectedDate) {
    emit('clearDate')
    return
  }
  emit('selectDate', date)
}
</script>
<template>
  <div class="schedule-weekbar" role="tablist" aria-label="赛程周日历">
    <button type="button" class="cal-arrow" aria-label="上一周" @click="emit('prevWeek')">‹</button>
    <span class="cal-week-label">{{ weekLabel }}</span>

    <div class="cal-days">
      <button
        type="button"
        class="cal-all"
        :class="{ active: !selectedDate }"
        :aria-pressed="!selectedDate"
        @click="emit('clearDate')"
      >
        全部
      </button>
      <button
        v-for="day in weekDays"
        :key="day.date"
        type="button"
        class="cal-day"
        :class="{ active: day.date === selectedDate, today: day.date === today }"
        :aria-pressed="day.date === selectedDate"
        @click="onSelect(day.date)"
      >
        <span v-if="day.count > 0" class="cal-day__badge">{{ day.count }}</span>
        <span class="cal-day__week">{{ day.weekday }}</span>
        <strong class="cal-day__date">{{ day.dateText }}</strong>
      </button>
    </div>

    <button type="button" class="cal-arrow" aria-label="下一周" @click="emit('nextWeek')">›</button>

    <div class="week-tools">
      <button
        type="button"
        class="tool-btn tool-btn--calendar"
        :class="{ expanded: calendarExpanded }"
        :aria-expanded="calendarExpanded"
        @click="emit('toggleCalendar')"
      >
        日历 {{ calendarExpanded ? '∧' : '▾' }}
      </button>
      <button
        type="button"
        class="tool-btn tool-btn--team"
        :class="{ expanded: teamFilterExpanded, filtered: Boolean(selectedTeamName) }"
        :aria-expanded="teamFilterExpanded"
        @click="emit('toggleTeamFilter')"
      >
        {{ teamButtonLabel }} {{ teamFilterExpanded ? '∧' : '▾' }}
      </button>
    </div>
  </div>
</template>
<style scoped lang="scss">
.schedule-weekbar {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 72px;
  margin-bottom: 16px;
  padding: 0 10px 0 8px;
  background: #111;
  color: #fff;
  border-radius: 3px;
}

.cal-arrow {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: #1a1a1a;
  color: #fff;
  font-size: 20px;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    border-color: var(--cnpa-red, #e10613);
    color: var(--cnpa-red, #e10613);
  }
}

.cal-week-label {
  flex: 0 0 auto;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.cal-days {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(8, minmax(72px, 92px));
  justify-content: start;
  gap: 4px;
}

.cal-all,
.cal-day {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  min-height: 52px;
  padding: 4px 4px 6px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: rgba(255, 255, 255, 0.64);
  font-family: inherit;
  cursor: pointer;
}

.cal-all {
  color: #fff;
  font-size: 13px;
  font-weight: 800;

  &.active {
    background: rgba(225, 6, 19, 0.12);
    color: var(--cnpa-red, #e10613);
    border-bottom-color: var(--cnpa-red, #e10613);
  }
}

.cal-day {
  &.today:not(.active) {
    box-shadow: inset 0 0 0 1px var(--cnpa-red, #e10613);
    color: #fff;
  }

  &.active {
    background: var(--cnpa-red, #e10613);
    color: #fff;
  }

  &:hover:not(.active) {
    background: rgba(255, 255, 255, 0.08);
  }
}

.cal-day__week {
  font-size: 11px;
  font-weight: 700;
}

.cal-day__date {
  font-size: 13px;
  font-weight: 800;
  color: inherit;
}

.cal-day__badge {
  position: absolute;
  top: 3px;
  right: 4px;
  min-width: 16px;
  height: 14px;
  padding: 0 4px;
  border-radius: 7px;
  background: var(--cnpa-red, #e10613);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  line-height: 14px;
}

.cal-day.active .cal-day__badge {
  background: #fff;
  color: var(--cnpa-red, #e10613);
}

.week-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  margin-left: auto;
}

.tool-btn {
  height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: transparent;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
}

.tool-btn--calendar.expanded,
.tool-btn--team.expanded {
  background: var(--cnpa-red, #e10613);
  border-color: var(--cnpa-red, #e10613);
}

.tool-btn--team.filtered:not(.expanded) {
  border-color: var(--cnpa-red, #e10613);
  color: #ffd4d7;
}

@media (max-width: 1199px) {
  .cal-days {
    overflow-x: auto;
    grid-template-columns: repeat(8, minmax(64px, 76px));
  }
}

@media (max-width: 767px) {
  .schedule-weekbar {
    flex-wrap: wrap;
    height: auto;
    min-height: 72px;
    padding: 8px;
  }

  .cal-days {
    order: 3;
    flex: 1 1 100%;
    grid-template-columns: repeat(8, minmax(64px, 1fr));
  }

  .week-tools {
    margin-left: 0;
  }

  .tool-btn {
    flex: 1;
  }
}
</style>
