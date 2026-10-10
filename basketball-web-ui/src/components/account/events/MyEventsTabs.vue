<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { AccountEventTab, MyEventsTabKey } from '@/types/accountEvents'

const { t } = useI18n()

defineProps<{
  activeTab: MyEventsTabKey
  tabs: AccountEventTab[]
}>()

const emit = defineEmits<{
  change: [tab: MyEventsTabKey]
}>()
</script>

<template>
  <nav class="my-events-tabs" :aria-label="t('accountEvents.available.tabsAria')">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      class="my-events-tabs__item"
      :class="{ 'my-events-tabs__item--active': activeTab === tab.key }"
      @click="emit('change', tab.key)"
    >
      {{ tab.label }}
    </button>
  </nav>
</template>

<style scoped lang="scss">
.my-events-tabs {
  display: flex;
  gap: 42px;
  margin-bottom: 24px;
  padding-bottom: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  overflow-x: auto;

  &__item {
    position: relative;
    flex: 0 0 auto;
    min-height: 64px;
    padding: 0 4px 14px;
    border: 0;
    background: transparent;
    color: #111;
    font-size: 18px;
    font-weight: 800;
    cursor: pointer;
    transition: color 0.2s ease;

    &:hover {
      color: var(--account-red, #d30910);
    }

    &--active {
      color: var(--account-red, #d30910);

      &::after {
        content: '';
        position: absolute;
        left: 0;
        right: 0;
        bottom: -1px;
        height: 3px;
        background: var(--account-red, #d30910);
      }
    }
  }
}
</style>
