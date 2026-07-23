<script setup lang="ts">
import type { PlayerDetailTab } from '@/utils/playerNavigate'

defineProps<{
  modelValue: PlayerDetailTab
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PlayerDetailTab]
}>()

const tabs: Array<{ value: PlayerDetailTab; label: string }> = [
  { value: 'overview', label: '总览' },
  { value: 'matches', label: '比赛' },
  { value: 'data', label: '数据' },
  { value: 'career', label: '生涯' },
]
</script>

<template>
  <div class="player-detail-tabs" role="tablist" aria-label="球员详情页签">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      type="button"
      class="player-detail-tabs__item"
      :class="{ 'is-active': modelValue === tab.value }"
      role="tab"
      :aria-selected="modelValue === tab.value"
      @click="emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.player-detail-tabs {
  display: flex;
  align-items: flex-end;
  gap: 36px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--cnpa-paper-border);
  overflow-x: auto;
}

.player-detail-tabs__item {
  position: relative;
  margin: 0;
  padding: 0 2px 14px;
  border: 0;
  background: transparent;
  color: var(--cnpa-paper-text-secondary);
  font-size: 20px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;

  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: -1px;
    height: 3px;
    background: transparent;
  }

  &.is-active {
    color: var(--cnpa-red);

    &::after {
      background: var(--cnpa-red);
    }
  }

  &:hover:not(.is-active) {
    color: var(--cnpa-paper-text);
  }
}
</style>
