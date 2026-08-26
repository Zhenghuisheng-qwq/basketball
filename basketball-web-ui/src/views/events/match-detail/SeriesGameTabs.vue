<script setup lang="ts">
import { SERIES_TOTAL_TAB } from './matchDetailDisplay'

export type SeriesTab = {
  key: string
  label: string
}

defineProps<{
  tabs: SeriesTab[]
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function select(key: string) {
  emit('update:modelValue', key)
}

defineExpose({ SERIES_TOTAL_TAB })
</script>

<template>
  <div class="series-game-tabs" role="tablist" aria-label="系列赛场次">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      role="tab"
      class="series-game-tabs__btn"
      :class="{ 'is-active': modelValue === tab.key }"
      :aria-selected="modelValue === tab.key"
      @click="select(tab.key)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.series-game-tabs {
  display: flex;
  justify-content: center;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  max-width: 100%;
  padding-bottom: 2px;
  scrollbar-width: thin;
}

.series-game-tabs__btn {
  flex: 0 0 auto;
  min-height: 36px;
  padding: 0 16px;
  border: 0;
  background: rgba(0, 0, 0, 0.42);
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;

  &.is-active {
    background: var(--cnpa-red, #e51422);
    color: #fff;
  }
}
</style>
