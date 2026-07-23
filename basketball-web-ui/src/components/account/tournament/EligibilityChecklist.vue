<script setup lang="ts">
import type { TransferEligibilityCheck } from '@/types/eventTeam'

defineProps<{
  checks: TransferEligibilityCheck[]
}>()
</script>

<template>
  <ul class="eligibility-checklist">
    <li
      v-for="item in checks"
      :key="item.key"
      class="eligibility-checklist__item"
      :class="{ 'eligibility-checklist__item--passed': item.passed }"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path
          fill="currentColor"
          :d="item.passed
            ? 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z'
            : 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z'"
        />
      </svg>
      <span>{{ item.label }}</span>
    </li>
  </ul>
</template>

<style scoped lang="scss">
.eligibility-checklist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    font-size: 14px;
    line-height: 1.5;
    color: var(--account-danger, #c80a10);

    svg {
      width: 20px;
      height: 20px;
      flex-shrink: 0;
      margin-top: 2px;
    }

    &--passed {
      color: var(--account-success, #239447);
    }
  }
}
</style>
