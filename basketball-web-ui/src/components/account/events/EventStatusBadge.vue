<script setup lang="ts">
defineProps<{
  text: string
  pending?: boolean
  finished?: boolean
}>()
</script>

<template>
  <span
    class="event-status-badge"
    :class="{
      'event-status-badge--pending': pending,
      'event-status-badge--finished': finished,
      'event-status-badge--default': !pending && !finished,
    }"
  >
    {{ text }}
  </span>
</template>

<style scoped lang="scss">
.event-status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;

  &--pending {
    background: rgba(215, 25, 32, 0.12);
    color: var(--account-danger, #c80a10);
  }

  &--finished {
    background: rgba(0, 0, 0, 0.08);
    color: #555;
  }

  &--default {
    background: rgba(214, 138, 22, 0.12);
    color: #b56a00;
  }
}
</style>
