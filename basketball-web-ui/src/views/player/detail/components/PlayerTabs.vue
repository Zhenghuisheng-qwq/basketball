<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { PlayerDetailTab } from '@/utils/playerNavigate'

defineProps<{
  modelValue: PlayerDetailTab
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PlayerDetailTab]
}>()

const { t } = useI18n()

const tabs = computed<Array<{ value: PlayerDetailTab; label: string }>>(() => [
  { value: 'overview', label: t('publicPages.player.tabOverview') },
  { value: 'matches', label: t('publicPages.player.tabMatches') },
  { value: 'data', label: t('publicPages.player.tabData') },
  { value: 'career', label: t('publicPages.player.tabCareer') },
])
</script>
<template>
  <div class="player-detail-tabs" role="tablist" :aria-label="t('publicPages.player.tabsAria')">
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
  margin: 0 0 22px;
  padding-top: 4px;
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

.player-detail-tabs {
  gap: clamp(20px, 3vw, 40px);
  padding-inline: 2px;
  scrollbar-width: none;
}

.player-detail-tabs::-webkit-scrollbar {
  display: none;
}

.player-detail-tabs__item {
  min-height: 48px;
  padding-bottom: 13px;
  font-weight: 800;
}

.player-detail-tabs__item:focus-visible {
  color: var(--cnpa-red);
  outline: 2px solid rgba(225, 6, 19, 0.35);
  outline-offset: -2px;
}
</style>