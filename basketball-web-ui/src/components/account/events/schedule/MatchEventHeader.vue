<script setup lang="ts">
import type { ScheduleEventHeaderView } from '@/types/v4ScheduleCard'

defineProps<{
  header: ScheduleEventHeaderView
}>()
</script>

<template>
  <header class="schedule-event-header">
    <div class="schedule-event-header__main">
      <h3 class="schedule-event-header__season">{{ header.seasonTitle }}</h3>
      <p class="schedule-event-header__sub">
        <span>{{ header.divisionLine }}</span>
        <span>{{ header.roundLine }}</span>
      </p>
    </div>
    <span
      class="schedule-event-header__status"
      :class="`schedule-event-header__status--${header.matchStatusTone}`"
    >
      {{ header.matchStatusLabel }}
    </span>
  </header>
</template>

<style scoped lang="scss">
@import './schedule-match-card.scss';
</style>
