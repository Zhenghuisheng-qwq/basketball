<script setup lang="ts">
import { computed, watch } from 'vue'
import { isValidDateKey, todayDateKey } from '@/utils/scheduleDateKey'
import { useCalendar } from '../composables/useCalendar'

const props = defineProps<{
  selectedDate: string
}>()

const emit = defineEmits<{
  select: [value: string]
  shift: [offset: number]
}>()

const initialDate = isValidDateKey(props.selectedDate) ? props.selectedDate : todayDateKey()
const { monthLabel, getThreeMonths, setAnchorFromDate, shiftMonths } = useCalendar(initialDate)

watch(
  () => props.selectedDate,
  (value) => {
    if (isValidDateKey(value)) setAnchorFromDate(value)
  },
)

const months = computed(() => getThreeMonths(props.selectedDate))

function handleShift(offset: number) {
  shiftMonths(offset)
  emit('shift', offset)
}
</script>
<template>
  <div
    class="calendar-panel schedule-panel cnpa-paper-section cnpa-paper-section--gray cnpa-paper-floating-panel"
    @click.stop
  >
    <div class="calendar-head">
      <button type="button" class="month-arrow" aria-label="上一组月份" @click="handleShift(-3)">
        ‹
      </button>
      <span>{{ monthLabel }}</span>
      <button type="button" class="month-arrow" aria-label="下一组月份" @click="handleShift(3)">
        ›
      </button>
    </div>

    <div class="months-grid">
      <div v-for="month in months" :key="`${month.year}-${month.month}`" class="month-block">
        <h4>{{ month.label }}</h4>
        <div class="weekday-row">
          <span v-for="day in ['日', '一', '二', '三', '四', '五', '六']" :key="day">{{ day }}</span>
        </div>
        <div class="days-grid">
          <span
            v-for="(day, index) in month.days"
            :key="`${month.label}-${index}`"
            class="day-cell"
            :class="{
              empty: !day.isCurrentMonth,
              weekend: day.isWeekend,
              selected: day.isSelected,
            }"
          >
            <button
              v-if="day.isCurrentMonth"
              type="button"
              @click="emit('select', day.date)"
            >
              {{ day.day }}
            </button>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped lang="scss">
.calendar-panel {
  width: min(760px, calc(100vw - 32px));
  padding: 18px;
}

.calendar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #111;
  font-weight: 800;
}

.month-arrow {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(10, 10, 10, 0.12);
  background: #fff;
  cursor: pointer;
}

.months-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.month-block h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #111;
}

.weekday-row,
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
}

.weekday-row span {
  text-align: center;
  font-size: 11px;
  color: rgba(10, 10, 10, 0.45);
}

.day-cell {
  aspect-ratio: 1;
  display: grid;
  place-items: center;

  &.weekend button {
    color: var(--cnpa-red);
  }

  &.selected button {
    background: var(--cnpa-red);
    color: #fff;
  }

  button {
    width: 100%;
    height: 100%;
    border: none;
    background: transparent;
    color: #111;
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
    font-family: inherit;
    border-radius: 2px;

    &:hover {
      background: rgba(225, 6, 19, 0.12);
    }
  }
}

@media (max-width: 992px) {
  .months-grid {
    grid-template-columns: 1fr;
    max-height: 420px;
    overflow-y: auto;
  }
}

@media (max-width: 768px) {
  .months-grid {
    display: flex;
    overflow-x: auto;
  }

  .month-block {
    min-width: 240px;
  }
}
</style>