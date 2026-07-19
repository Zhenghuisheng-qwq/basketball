<script setup lang="ts">
import type { EventTab } from '../types/events'

defineProps<{
  activeTab: EventTab
}>()

const emit = defineEmits<{
  change: [tab: EventTab]
}>()

const tabs: Array<{ key: EventTab; label: string }> = [
  { key: 'overview', label: '总览' },
  { key: 'schedule', label: '赛程' },
  { key: 'stage', label: '赛段' },
]
</script>

<template>
  <nav class="league-tabs" aria-label="赛事二级导航">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      class="league-tab"
      :class="{ active: activeTab === tab.key }"
      :aria-current="activeTab === tab.key ? 'page' : undefined"
      @click="emit('change', tab.key)"
    >
      {{ tab.label }}
    </button>
  </nav>
</template>

<style scoped lang="scss">
.league-tabs {
  display: inline-flex;
  align-items: stretch;
  width: fit-content;
  max-width: 100%;
  min-height: 64px;
  margin-top: 0;
  background: #080808;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-top: none;
  border-radius: 0 0 2px 2px;
  box-sizing: border-box;
}

.league-tab {
  position: relative;
  flex: 0 0 auto;
  min-width: clamp(88px, 8vw, 120px);
  padding: 0 clamp(20px, 2vw, 36px);
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s ease, background 0.2s ease;

  &:hover {
    color: #fff;
    background: rgba(225, 6, 19, 0.08);
  }

  &.active {
    color: #fff;
    background: linear-gradient(180deg, rgba(225, 6, 19, 0.28) 0%, rgba(8, 8, 8, 0.2) 100%);

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 3px;
      background: var(--cnpa-red);
    }
  }
}

@media (max-width: 480px) {
  .league-tabs {
    min-height: 56px;
  }

  .league-tab {
    min-width: 72px;
    padding: 0 16px;
    font-size: 14px;
  }
}
</style>
