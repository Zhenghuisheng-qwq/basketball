<script setup lang="ts">
import type { NotificationTabKey } from '@/types/accountNotifications'

defineProps<{
  activeTab: NotificationTabKey
  total: number
  unread: number
}>()

const emit = defineEmits<{
  change: [tab: NotificationTabKey]
}>()
</script>

<template>
  <div class="notification-center-tabs">
    <button
      type="button"
      class="notification-center-tabs__item"
      :class="{ 'notification-center-tabs__item--active': activeTab === 'ALL' }"
      @click="emit('change', 'ALL')"
    >
      全部 {{ total }}
    </button>
    <button
      type="button"
      class="notification-center-tabs__item"
      :class="{ 'notification-center-tabs__item--active': activeTab === 'UNREAD' }"
      @click="emit('change', 'UNREAD')"
    >
      未读 {{ unread }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.notification-center-tabs {
  display: flex;
  gap: clamp(24px, 4vw, 56px);
  margin-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);

  &__item {
    position: relative;
    min-height: 56px;
    padding: 0 4px 12px;
    border: 0;
    background: transparent;
    color: #111;
    font-size: 15px;
    font-weight: 800;
    cursor: pointer;

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
