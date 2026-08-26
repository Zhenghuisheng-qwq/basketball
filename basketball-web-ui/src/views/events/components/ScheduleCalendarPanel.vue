<script setup lang="ts">
import { computed } from 'vue'
import { buildThreeMonths } from '../utils/scheduleCalendar'

const props = defineProps<{
  selectedDate: string
  monthAnchor: string
  dateCounts?: Record<string, number>
}>()

const emit = defineEmits<{
  selectDate: [value: string]
  prevMonth: []
  nextMonth: []
}>()

const months = computed(() => buildThreeMonths(props.monthAnchor, props.selectedDate, props.dateCounts || {}))
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
</script>
<template>
  <section class="month-panel" aria-label="赛程月历">
    <div class="month-panel__head">
      <button type="button" class="month-arrow" aria-label="上一月" @click="emit('prevMonth')">‹</button>
      <span>{{ months[1]?.label || '' }}</span>
      <button type="button" class="month-arrow" aria-label="下一月" @click="emit('nextMonth')">›</button>
    </div>

    <div class="months-grid">
      <div
        v-for="month in months"
        :key="month.label"
        class="month-block"
        :class="{ 'month-block--center': month.center }"
      >
        <h4>{{ month.label }}</h4>
        <div class="weekday-row">
          <span v-for="day in weekdays" :key="day">{{ day }}</span>
        </div>
        <div class="days-grid">
          <span
            v-for="(day, index) in month.days"
            :key="month.label + '-' + index"
            class="day-cell"
            :class="{
              empty: !day.inMonth,
              weekend: day.weekend,
              today: day.today,
              selected: day.selected,
              match: day.hasMatch,
            }"
          >
            <button
              v-if="day.inMonth"
              type="button"
              @click="emit('selectDate', day.date)"
            >
              {{ day.day }}
              <i v-if="day.hasMatch" class="day-dot" aria-hidden="true" />
            </button>
          </span>
        </div>
      </div>
    </div>
  </section>
</template>
<style scoped lang="scss">
.month-panel {
  width: 100%;
  margin: 0 0 16px;
  padding: 14px 16px 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
}

.month-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  color: #111;
  font-size: 15px;
  font-weight: 800;
}

.month-arrow {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(10, 10, 10, 0.12);
  background: #fff;
  color: #111;
  font-size: 18px;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    border-color: var(--cnpa-red, #e10613);
    color: var(--cnpa-red, #e10613);
  }
}

.months-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.month-block {
  padding: 12px 12px 10px;
  background: #f8f7f5;
}

.month-block h4 {
  margin: 0 0 10px;
  color: #111;
  font-size: 14px;
  font-weight: 800;
}

.weekday-row,
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 2px;
}

.weekday-row span {
  text-align: center;
  color: #8a8682;
  font-size: 11px;
  font-weight: 700;
}

.day-cell {
  aspect-ratio: 1;
  display: grid;
  place-items: center;

  button {
    position: relative;
    width: 100%;
    height: 100%;
    border: 0;
    background: transparent;
    color: #111;
    font-size: 12px;
    font-weight: 700;
    font-family: inherit;
    cursor: pointer;
  }

  &.weekend button {
    color: var(--cnpa-red, #e10613);
  }

  &.today:not(.selected) button {
    box-shadow: inset 0 0 0 1px var(--cnpa-red, #e10613);
  }

  &.selected button {
    background: var(--cnpa-red, #e10613);
    color: #fff;
  }
}

.day-dot {
  position: absolute;
  left: 50%;
  bottom: 3px;
  width: 4px;
  height: 4px;
  margin-left: -2px;
  border-radius: 50%;
  background: var(--cnpa-red, #e10613);
}

.day-cell.selected .day-dot {
  background: #fff;
}

@media (max-width: 1023px) {
  .months-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .month-block:not(.month-block--center):last-child {
    display: none;
  }
}

@media (max-width: 767px) {
  .months-grid {
    grid-template-columns: 1fr;
  }

  .month-block:not(.month-block--center) {
    display: none;
  }
}
</style>
