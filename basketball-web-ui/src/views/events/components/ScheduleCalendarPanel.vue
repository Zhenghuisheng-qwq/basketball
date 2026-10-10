<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
const months = computed(() => buildThreeMonths(props.monthAnchor, props.selectedDate, props.dateCounts || {}))
const weekdays = computed(() => [0, 1, 2, 3, 4, 5, 6].map((day) => t(`eventsPage.weekdayChar.${day}`)))
</script>

<template>
  <section class="calendar-panel" :aria-label="t('eventsPage.aria.monthCalendar')">
    <div class="calendar-nav">
      <button type="button" class="month-arrow" :aria-label="t('eventsPage.aria.prevMonth')" @click="emit('prevMonth')">‹</button>
      <span>{{ months[1]?.label || '' }}</span>
      <button type="button" class="month-arrow" :aria-label="t('eventsPage.aria.nextMonth')" @click="emit('nextMonth')">›</button>
    </div>

    <div class="calendar-months">
      <div
        v-for="month in months"
        :key="month.label"
        class="month-card"
        :class="{ 'month-card--center': month.center }"
      >
        <div class="month-title">{{ month.label }}</div>
        <div class="month-weekdays">
          <span v-for="(day, index) in weekdays" :key="index">{{ day }}</span>
        </div>
        <div class="month-days">
          <button
            v-for="(day, index) in month.days"
            :key="month.label + '-' + index"
            type="button"
            class="calendar-date"
            :class="{
              empty: !day.inMonth,
              weekend: day.weekend,
              selected: day.selected,
              'has-match': day.hasMatch,
            }"
            :disabled="!day.inMonth"
            @click="day.inMonth && emit('selectDate', day.date)"
          >
            {{ day.inMonth ? day.day : '' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.calendar-panel {
  padding: 24px;
  background: #fff;
  border: 1px solid var(--schedule-border, #dfddda);
  border-top: none;
}

.calendar-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #151515;
  font-size: 15px;
  font-weight: 800;
}

.month-arrow {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: #fff;
  color: #151515;
  font-size: 20px;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    border-color: var(--cnpa-red, #e51422);
    color: var(--cnpa-red, #e51422);
  }
}

.calendar-months {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.month-card {
  min-height: 325px;
  padding: 20px;
  background: #f7f6f4;
  border: 1px solid #eceae7;
}

.month-title {
  margin-bottom: 18px;
  text-align: center;
  font-size: 17px;
  font-weight: 800;
}

.month-weekdays,
.month-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
}

.month-weekdays span {
  padding: 8px 0;
  color: #999;
  font-size: 12px;
}

.calendar-date {
  min-height: 40px;
  display: grid;
  place-items: center;
  margin: 2px;
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  position: relative;

  &:hover:not(.empty) {
    background: #eee;
  }

  &.weekend {
    color: #e64550;
  }

  &.has-match::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 3px;
    transform: translateX(-50%);
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--cnpa-red, #e51422);
  }

  &.selected {
    background: var(--cnpa-red, #e51422);
    color: #fff;
    font-weight: 800;
  }

  &.selected::after {
    background: #fff;
  }

  &.empty {
    pointer-events: none;
    cursor: default;
  }
}

@media (max-width: 900px) {
  .calendar-months {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .month-card:nth-child(3) {
    display: none;
  }
}

@media (max-width: 640px) {
  .calendar-months {
    grid-template-columns: 1fr;
  }

  .month-card:not(.month-card--center) {
    display: none;
  }
}
</style>
