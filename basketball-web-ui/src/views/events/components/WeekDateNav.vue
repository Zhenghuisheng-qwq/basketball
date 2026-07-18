<script setup lang="ts">
import { computed } from 'vue'
import { getWeekDates } from '../composables/useCalendar'

const props = defineProps<{
  selectedDate: string
  weekNumber: number
}>()

const emit = defineEmits<{
  selectDate: [value: string]
  prevWeek: []
  nextWeek: []
}>()

const weekInfo = computed(() => getWeekDates(props.selectedDate, props.weekNumber))
</script>

<template>
  <div class="week-nav">
    <button type="button" class="week-arrow" aria-label="上一周" @click="emit('prevWeek')">‹</button>

    <div class="week-label">第{{ weekNumber }}周</div>

    <div class="week-days">
      <button
        v-for="day in weekInfo.weekDays"
        :key="day.date"
        type="button"
        class="week-day"
        :class="{ active: day.date === selectedDate }"
        @click="emit('selectDate', day.date)"
      >
        {{ day.label }}
      </button>
    </div>

    <button type="button" class="week-arrow" aria-label="下一周" @click="emit('nextWeek')">›</button>
  </div>
</template>

<style scoped lang="scss">
.week-nav {
  display: grid;
  grid-template-columns: 40px auto 1fr 40px;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  background: #111;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.week-arrow {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.week-label {
  min-width: 72px;
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.week-days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.week-day {
  min-height: 48px;
  padding: 6px 8px;
  border: 1px solid transparent;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba(225, 6, 19, 0.35);
    color: #fff;
  }

  &.active {
    background: var(--cnpa-red);
    color: #fff;
  }
}

@media (max-width: 992px) {
  .week-nav {
    grid-template-columns: 32px 1fr 32px;
  }

  .week-label {
    grid-column: 1 / -1;
    text-align: center;
  }

  .week-days {
    grid-column: 1 / -1;
    overflow-x: auto;
    grid-template-columns: repeat(7, minmax(88px, 1fr));
  }
}
</style>
