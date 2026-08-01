<script setup lang="ts">
export type MatchDetailTab = 'overview' | 'roster' | 'data'

defineProps<{
  activeTab: MatchDetailTab
}>()

const emit = defineEmits<{
  change: [tab: MatchDetailTab]
}>()

const tabs: Array<{ key: MatchDetailTab; label: string }> = [
  { key: 'overview', label: '总览' },
  { key: 'roster', label: '阵容' },
  { key: 'data', label: '比赛数据' },
]
</script>

<template>
  <nav class="match-detail-tabs" aria-label="比赛详情导航">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      class="match-detail-tab"
      :class="{ active: activeTab === tab.key }"
      :aria-current="activeTab === tab.key ? 'page' : undefined"
      @click="emit('change', tab.key)"
    >
      {{ tab.label }}
    </button>
  </nav>
</template>

<style scoped lang="scss">
.match-detail-tabs {
  display: inline-flex;
  align-items: stretch;
  width: fit-content;
  max-width: 100%;
  min-height: 56px;
  background: #080808;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 2px;
  box-sizing: border-box;
}

.match-detail-tab {
  position: relative;
  flex: 0 0 auto;
  min-width: clamp(88px, 10vw, 128px);
  padding: 0 clamp(18px, 2vw, 32px);
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 15px;
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
      background: var(--cnpa-red, #e10613);
    }
  }
}

@media (max-width: 480px) {
  .match-detail-tabs {
    display: flex;
    width: 100%;
  }

  .match-detail-tab {
    flex: 1 1 0;
    min-width: 0;
    padding: 0 10px;
    font-size: 13px;
  }
}
</style>
