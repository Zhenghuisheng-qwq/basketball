<script setup lang="ts">
import type { HallTab } from '../types'

defineProps<{
  modelValue: HallTab
}>()

const emit = defineEmits<{
  'update:modelValue': [value: HallTab]
}>()

const tabs: Array<{ value: HallTab; label: string }> = [
  { value: 'weekly', label: '周最佳' },
  { value: 'event', label: '赛事荣誉' },
  { value: 'champions', label: '历届冠军' },
]
</script>

<template>
  <div class="hall-tabs" role="tablist" aria-label="名人堂栏目">
    <button
      v-for="(tab, index) in tabs"
      :key="tab.value"
      type="button"
      class="hall-tabs__item"
      :class="{ 'is-active': modelValue === tab.value }"
      role="tab"
      :aria-selected="modelValue === tab.value"
      @click="emit('update:modelValue', tab.value)"
    >
      <span v-if="index > 0" class="hall-tabs__divider" aria-hidden="true" />
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.hall-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  height: 56px;
  background: #0a0a0a;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  scroll-margin-top: var(--site-header-height, 72px);
}

.hall-tabs__item {
  position: relative;
  margin: 0;
  height: 100%;
  padding: 0 12px;
  border: 0;
  background: #111;
  color: #b9b9b9;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;

  &.is-active {
    background: linear-gradient(180deg, #2a0a0d 0%, #120608 100%);
    color: #fff;
    box-shadow: inset 0 -3px 0 var(--hall-red, #e71922);
  }

  &:hover:not(.is-active) {
    color: rgba(255, 255, 255, 0.88);
  }
}

.hall-tabs__divider {
  position: absolute;
  left: 0;
  top: 50%;
  width: 1px;
  height: 18px;
  background: rgba(255, 255, 255, 0.12);
  transform: translateY(-50%);
}

@media (max-width: 767px) {
  .hall-tabs {
    display: flex;
    overflow-x: auto;
    height: 54px;
  }

  .hall-tabs__item {
    min-width: 120px;
    flex: 0 0 auto;
  }
}
</style>
