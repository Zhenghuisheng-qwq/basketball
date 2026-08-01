<script setup lang="ts">
import NotificationListItem from './NotificationListItem.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import TeamListPagination from '@/components/account/team/TeamListPagination.vue'
import type { AccountNotificationItem } from '@/types/accountNotifications'

defineProps<{
  items: AccountNotificationItem[]
  page: number
  total: number
  pageSize: number
  loading?: boolean
  emptyMessage: string
}>()

const emit = defineEmits<{
  view: [id: string]
  'update:page': [page: number]
}>()
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

    <TeamListPagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      :loading="loading"
      @update:page="emit('update:page', $event)"
    />
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
</style>
