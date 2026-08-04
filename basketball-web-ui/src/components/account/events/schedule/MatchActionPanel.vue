<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { ScheduleMatchActionsView } from '@/types/v4ScheduleCard'

defineProps<{
  actions: ScheduleMatchActionsView
}>()

const router = useRouter()

function navigate(route: ScheduleMatchActionsView['primary']) {
  if (!route) return
  router.push(route.route)
}
</script>

<template>
  <div class="schedule-action-panel">
    <p v-if="actions.readOnlyLabel && !actions.primary" class="schedule-action-panel__readonly">
      {{ actions.readOnlyLabel }}
    </p>
    <button
      v-if="actions.primary"
      type="button"
      :class="actions.primary.kind === 'primary' ? 'account-btn-primary' : 'account-btn-secondary'"
      @click="navigate(actions.primary)"
    >
      {{ actions.primary.label }}
    </button>
    <button
      v-if="actions.secondary"
      type="button"
      class="account-btn-secondary"
      @click="navigate(actions.secondary)"
    >
      {{ actions.secondary.label }}
    </button>
  </div>
</template>

<style scoped lang="scss">
@import './schedule-match-card.scss';
</style>
