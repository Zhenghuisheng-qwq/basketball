<script setup lang="ts">
import { computed } from 'vue'
import type { TeamCompareBarStat } from '@/types/team'
import {
  formatMetricValue,
  getMetricFormatType,
  getMetricPercent,
  getWinnerSide,
} from '@/utils/compareMetrics'

const props = defineProps<{
  stat: TeamCompareBarStat
}>()

const formatType = computed(() => getMetricFormatType(props.stat.key))

const winner = computed(() =>
  getWinnerSide(props.stat.left, props.stat.right, props.stat.higherIsBetter),
)

const leftDisplay = computed(() => formatMetricValue(props.stat.left, formatType.value))
const rightDisplay = computed(() => formatMetricValue(props.stat.right, formatType.value))

const leftPercent = computed(() =>
  getMetricPercent(props.stat.left, props.stat.left, props.stat.right),
)

const rightPercent = computed(() =>
  getMetricPercent(props.stat.right, props.stat.left, props.stat.right),
)

const leftAriaLabel = computed(() => {
  if (winner.value === 'left') return `${leftDisplay.value}，领先`
  return leftDisplay.value
})

const rightAriaLabel = computed(() => {
  if (winner.value === 'right') return `${rightDisplay.value}，领先`
  return rightDisplay.value
})
</script>

<template>
  <div class="metric-row">
    <div
      class="metric-side metric-side--left"
      :class="{ 'is-leading': winner === 'left' }"
    >
      <span class="metric-value" :aria-label="leftAriaLabel">{{ leftDisplay }}</span>

      <div class="metric-track" aria-hidden="true">
        <div class="metric-bar" :style="{ width: `${leftPercent}%` }" />
      </div>
    </div>

    <div class="metric-name">{{ stat.label }}</div>

    <div
      class="metric-side metric-side--right"
      :class="{ 'is-leading': winner === 'right' }"
    >
      <div class="metric-track" aria-hidden="true">
        <div class="metric-bar" :style="{ width: `${rightPercent}%` }" />
      </div>

      <span class="metric-value" :aria-label="rightAriaLabel">{{ rightDisplay }}</span>
    </div>
  </div>
</template>
