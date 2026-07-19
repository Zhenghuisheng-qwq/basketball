<script setup lang="ts">
import { computed } from 'vue'
import {
  formatMetricValue,
  getWinnerSide,
  type MetricFormatType,
} from '@/utils/compareMetrics'

const props = defineProps<{
  label: string
  icon?: string
  leftValue: number | null | undefined
  rightValue: number | null | undefined
  leftTeamCode: string
  rightTeamCode: string
  higherIsBetter: boolean
  formatType: MetricFormatType
}>()

const winner = computed(() =>
  getWinnerSide(props.leftValue, props.rightValue, props.higherIsBetter),
)

const leftDisplay = computed(() => formatMetricValue(props.leftValue, props.formatType))
const rightDisplay = computed(() => formatMetricValue(props.rightValue, props.formatType))
</script>

<template>
  <article class="summary-card">
    <div class="summary-card__title">
      <div v-if="icon" class="summary-card__icon">
        <img :src="icon" :alt="label" />
      </div>
      <span>{{ label }}</span>
    </div>

    <div class="summary-card__accent" aria-hidden="true" />

    <div class="summary-card__values">
      <div
        class="summary-card__side summary-card__side--left"
        :class="{ 'is-leading': winner === 'left' }"
      >
        <strong>{{ leftDisplay }}</strong>
      </div>

      <div class="summary-card__divider" aria-hidden="true" />

      <div
        class="summary-card__side summary-card__side--right"
        :class="{ 'is-leading': winner === 'right' }"
      >
        <strong>{{ rightDisplay }}</strong>
      </div>
    </div>

    <div class="summary-card__teams">
      <span>{{ leftTeamCode }}</span>
      <i aria-hidden="true" />
      <span>{{ rightTeamCode }}</span>
    </div>
  </article>
</template>
