<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import NotificationTabs from '@/components/account/notifications/NotificationTabs.vue'
import NotificationList from '@/components/account/notifications/NotificationList.vue'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { NotificationTabKey } from '@/types/accountNotifications'

const PAGE_SIZE = 5

const router = useRouter()
const notificationStore = useNotificationCenterStore()

const activeTab = ref<NotificationTabKey>('ALL')
const page = ref(1)

const filteredItems = computed(() => {
  if (activeTab.value === 'UNREAD') {
    return notificationStore.items.filter((item) => item.isUnread)
  }
  return notificationStore.items
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / PAGE_SIZE)))

const pagedItems = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredItems.value.slice(start, start + PAGE_SIZE)
})

const unreadText = computed(() => `${notificationStore.unreadCount} 条未读`)

function setTab(tab: NotificationTabKey) {
  activeTab.value = tab
  page.value = 1
}

function handleMarkAllRead() {
  notificationStore.markAllAsRead()
}

function handleViewDetail(id: string) {
  router.push(`/account/notifications/${id}`)
}

function handlePageChange(nextPage: number) {
  page.value = nextPage
}
</script>

<template>
  <div class="account-view account-view--notifications account-center-vars">
    <AccountPageHeader
      zh-title="消息通知"
      en-title="NOTIFICATIONS"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '消息通知' },
      ]"
    />

    <AccountStatusBanner
      title="账号与赛事通知"
      description="集中查看审核、邀请、比赛与系统消息"
      :status-text="unreadText"
      status-type="danger"
    />

    <AccountPanel title="全部通知">
      <template #extra>
        <button type="button" class="account-btn-secondary account-btn-secondary--compact" @click="handleMarkAllRead">
          全部标记为已读
        </button>
      </template>

      <NotificationTabs
        :active-tab="activeTab"
        :total="notificationStore.total"
        :unread="notificationStore.unreadCount"
        @change="setTab"
      />

      <NotificationList
        :items="pagedItems"
        :page="page"
        :total-pages="totalPages"
        @view="handleViewDetail"
        @update:page="handlePageChange"
      />
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view--notifications {
  width: 100%;
  min-width: 0;
}

.account-btn-secondary--compact {
  min-height: 42px;
  padding: 0 20px;
  font-size: 13px;
}
</style>
