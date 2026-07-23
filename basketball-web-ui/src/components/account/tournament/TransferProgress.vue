<script setup lang="ts">
import type { TransferStep } from '@/types/eventTeam'

defineProps<{
  steps: TransferStep[]
  compact?: boolean
}>()
</script>

<template>
  <ol class="transfer-progress" :class="{ 'transfer-progress--compact': compact }">
    <li
      v-for="(step, index) in steps"
      :key="step.key"
      class="transfer-progress__item"
      :class="`transfer-progress__item--${step.status}`"
    >
      <span class="transfer-progress__index">{{ String(index + 1).padStart(2, '0') }}</span>
      <div>
        <strong>{{ step.text }}</strong>
        <span v-if="step.time">{{ step.time }}</span>
      </div>
    </li>
  </ol>
</template>

<style scoped lang="scss">
.transfer-progress {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__item {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    font-size: 14px;

    strong {
      display: block;
    }

    span {
      font-size: 12px;
      color: #666;
    }

    &--done .transfer-progress__index {
      background: #239447;
    }

    &--current .transfer-progress__index {
      background: var(--account-red, #d30910);
    }

    &--waiting .transfer-progress__index,
    &--failed .transfer-progress__index {
      background: #bbb;
    }
  }

  &__index {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    color: #fff;
    font-size: 11px;
    font-weight: 800;
    flex-shrink: 0;
  }

  &--compact {
    gap: 6px;

    .transfer-progress__index {
      width: 22px;
      height: 22px;
      font-size: 10px;
    }
  }
}
</style>
