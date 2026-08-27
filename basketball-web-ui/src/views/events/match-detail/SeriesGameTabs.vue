<script setup lang="ts">
import { SERIES_TOTAL_TAB } from './matchDetailDisplay'

export type SeriesTab = {
  key: string
  label: string
}

defineProps<{
  tabs: SeriesTab[]
  modelValue: string
  primaryKey?: string
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
      :class="{
        'is-active': modelValue === tab.key,
        'is-primary': tab.key === (primaryKey || SERIES_TOTAL_TAB),
      }"
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
  gap: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  max-width: 100%;
  padding: 0;
  background: transparent;
  border: 0;
  box-shadow: none;
  scrollbar-width: thin;
}

.series-game-tabs__btn {
  flex: 0 0 auto;
  height: 34px;
  min-height: 34px;
  min-width: 44px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 3px;
  background: #111;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: none;

  &.is-primary {
    min-width: 100px;
    padding: 0 12px;
  }

  &:hover:not(.is-active) {
    border-color: rgba(225, 6, 19, 0.55);
    color: #fff;
  }

  &.is-active {
    background: var(--cnpa-red, #e10613);
    border-color: var(--cnpa-red, #e10613);
    color: #fff;
  }
}
</style>
