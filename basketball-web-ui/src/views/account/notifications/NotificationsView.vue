<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import NotificationTabs from '@/components/account/notifications/NotificationTabs.vue'
import NotificationList from '@/components/account/notifications/NotificationList.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { NotificationTabKey } from '@/types/accountNotifications'

const PAGE_SIZE = 10

const router = useRouter()
const notificationStore = useNotificationCenterStore()

const activeTab = ref<NotificationTabKey>('ALL')
const page = ref(1)

onMounted(async () => {
  await loadPage()
})

async function loadPage() {
  const readStatus = activeTab.value === 'UNREAD' ? 0 : undefined
  await notificationStore.refresh(page.value, PAGE_SIZE, readStatus)
}

const filteredItems = computed(() => notificationStore.items)

const totalPages = computed(() => Math.max(1, Math.ceil(notificationStore.total / PAGE_SIZE)))

const pagedItems = computed(() => filteredItems.value)

const unreadText = computed(() => `${notificationStore.unreadCount} 条未读`)

async function setTab(tab: NotificationTabKey) {
  activeTab.value = tab
  page.value = 1
  await loadPage()
}

async function handleMarkAllRead() {
  await notificationStore.markAllAsRead()
  await loadPage()
}

function handleViewDetail(id: string) {
  router.push(`/account/notifications/${id}`)
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage
  await loadPage()
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

      <div v-if="notificationStore.loading" class="notifications-loading">加载中...</div>

      <AccountEmptyState
        v-else-if="notificationStore.error"
        message="加载通知失败"
        :subtitle="notificationStore.error"
      />

      <template v-else>
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
      </template>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view--notifications {
  width: 100%;
  min-width: 0;
}

.notifications-loading {
  padding: 32px 0;
  color: #666;
}

.account-btn-secondary--compact {
  min-height: 42px;
  padding: 0 20px;
  font-size: 13px;
}
</style>
