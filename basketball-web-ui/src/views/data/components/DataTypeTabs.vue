<script setup lang="ts">
import type { DataType } from '../mock/dataCenter'

defineProps<{
  modelValue: DataType
}>()

const emit = defineEmits<{
  'update:modelValue': [value: DataType]
}>()

const tabs: Array<{ value: DataType; label: string }> = [
  { value: 'team', label: '球队数据' },
  { value: 'player', label: '个人数据' },
]
</script>

<template>
  <div class="data-type-tabs" role="tablist" aria-label="数据类型">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      type="button"
      class="data-type-tabs__item"
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
.data-type-tabs {
  display: flex;
  align-items: flex-end;
  gap: 40px;
  width: 100%;
  margin-bottom: 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.data-type-tabs__item {
  position: relative;
  margin: 0;
  padding: 0 2px 14px;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.46);
  font-size: 23px;
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: color 0.16s ease;

  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: -1px;
    height: 3px;
    background: transparent;
  }

  &:hover {
    color: rgba(255, 255, 255, 0.72);
  }

  &.is-active {
    color: #ff1717;

    &::after {
      background: #ff1717;
    }
  }
}

@media (max-width: 767px) {
  .data-type-tabs {
    gap: 24px;
    margin-bottom: 18px;
  }

  .data-type-tabs__item {
    font-size: 20px;
    padding-bottom: 12px;
  }
}
</style>
