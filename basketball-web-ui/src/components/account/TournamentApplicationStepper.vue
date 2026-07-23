<script setup lang="ts">
import AccountStepper, { type StepItem } from '@/components/account/AccountStepper.vue'

defineProps<{
  steps: StepItem[]
}>()
</script>

<template>
  <AccountStepper class="tournament-application-stepper" :steps="steps" />
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
