<script setup lang="ts">
export type StepState = 'done' | 'current' | 'upcoming' | 'failed'

export interface StepItem {
  label: string
  state: StepState
  subLabel?: string
}

defineProps<{
  steps: StepItem[]
}>()
</script>

<template>
  <ol class="account-stepper">
    <li
      v-for="(step, index) in steps"
      :key="`${step.label}-${index}`"
      class="account-stepper__item"
      :class="`account-stepper__item--${step.state}`"
    >
      <span class="account-stepper__index">{{ String(index + 1).padStart(2, '0') }}</span>
      <div class="account-stepper__body">
        <strong>{{ step.label }}</strong>
        <span v-if="step.subLabel">{{ step.subLabel }}</span>
      </div>
    </li>
  </ol>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
