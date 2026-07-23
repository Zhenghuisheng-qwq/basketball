<script setup lang="ts">
import NotificationListItem from './NotificationListItem.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import type { AccountNotificationItem } from '@/types/accountNotifications'

defineProps<{
  items: AccountNotificationItem[]
  page: number
  totalPages: number
}>()

const emit = defineEmits<{
  view: [id: string]
  'update:page': [page: number]
}>()
</script>

<template>
  <div class="notification-list-card">
    <AccountEmptyState v-if="!items.length" message="暂无通知" />
    <template v-else>
      <NotificationListItem
        v-for="item in items"
        :key="item.id"
        :item="item"
        @view="emit('view', $event)"
      />
    </template>

    <div v-if="totalPages > 1" class="notification-pagination">
      <button
        type="button"
        class="account-btn-secondary"
        :disabled="page <= 1"
        @click="emit('update:page', page - 1)"
      >
        上一页
      </button>
      <button
        v-for="p in totalPages"
        :key="p"
        type="button"
        class="notification-pagination__page"
        :class="{ 'notification-pagination__page--active': p === page }"
        @click="emit('update:page', p)"
      >
        {{ p }}
      </button>
      <button
        type="button"
        class="account-btn-secondary"
        :disabled="page >= totalPages"
        @click="emit('update:page', page + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.notification-list-card {
  width: 100%;
  padding: 8px 8px 18px;
  background: transparent;
  border: 0;
  border-radius: 0;
}

.notification-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);

  :deep(.account-btn-secondary) {
    min-height: 40px;
    padding: 0 18px;
    font-size: 15px;
  }

  &__page {
    min-width: 40px;
    height: 40px;
    border: 1px solid rgba(0, 0, 0, 0.18);
    background: transparent;
    color: #111;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;

    &--active {
      border-color: var(--account-red, #d90000);
      color: var(--account-red, #d90000);
    }
  }
}
</style>
