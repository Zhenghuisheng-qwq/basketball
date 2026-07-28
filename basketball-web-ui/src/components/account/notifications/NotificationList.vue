<script setup lang="ts">
import { computed } from 'vue'
import NotificationListItem from './NotificationListItem.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import type { AccountNotificationItem } from '@/types/accountNotifications'

const props = defineProps<{
  items: AccountNotificationItem[]
  page: number
  total: number
  pageSize: number
  totalPages: number
  emptyMessage: string
}>()

const emit = defineEmits<{
  view: [id: string]
  'update:page': [page: number]
}>()

const showPagination = computed(() => props.total > props.pageSize)
</script>

<template>
  <div class="notification-list-card">
    <AccountEmptyState v-if="!items.length" :message="emptyMessage" />

    <template v-else>
      <NotificationListItem
        v-for="item in items"
        :key="item.id"
        :item="item"
        @view="emit('view', $event)"
      />
    </template>

    <nav
      v-if="showPagination"
      class="notification-pagination"
      aria-label="通知分页"
    >
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
    </nav>
  </div>
</template>

<style scoped lang="scss">
.notification-list-card {
  width: 100%;
  min-width: 0;
  padding: 8px 8px 18px;
  background: transparent;
  border: 0;
  border-radius: 0;
  overflow: visible;
  max-height: none;
}

.notification-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
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

    &:hover:not(&--active) {
      border-color: rgba(217, 0, 0, 0.45);
      color: var(--account-red, #d90000);
    }
  }
}

@media (max-width: 767px) {
  .notification-pagination {
    justify-content: center;
  }
}
</style>
