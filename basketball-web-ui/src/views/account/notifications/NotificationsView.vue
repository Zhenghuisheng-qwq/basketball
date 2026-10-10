<script setup lang="ts">
import { computed, onActivated, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import NotificationTabs from '@/components/account/notifications/NotificationTabs.vue'
import NotificationList from '@/components/account/notifications/NotificationList.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { NotificationTabKey } from '@/types/accountNotifications'

const router = useRouter()
const { t } = useI18n()
const notificationStore = useNotificationCenterStore()

onMounted(async () => {
  await notificationStore.loadPage('ALL')
})

onActivated(async () => {
  if (!notificationStore.loading) {
    await notificationStore.refresh()
  }
})

const unreadText = computed(() =>
  t('accountShell.notifications.unreadCount', { count: notificationStore.unreadCount }),
)

const emptyMessage = computed(() =>
  notificationStore.activeTab === 'UNREAD'
    ? t('accountShell.notifications.emptyUnread')
    : t('accountShell.notifications.emptyAll'),
)

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter'), to: '/account/overview' },
  { label: t('accountShell.notifications.title') },
])

async function setTab(tab: NotificationTabKey) {
  await notificationStore.loadPage(tab)
}

async function handleMarkAllRead() {
  await notificationStore.markAllAsRead()
}

function handleViewDetail(id: string) {
  router.push(`/account/notifications/${id}`)
}

async function handlePageChange(nextPage: number) {
  await notificationStore.changePage(nextPage)
}
</script>

<template>
  <div class="account-view account-view--notifications account-center-vars">
    <AccountPageHeader
      :zh-title="t('accountShell.notifications.title')"
      en-title="NOTIFICATIONS"
      :breadcrumb-items="breadcrumbs"
    />

    <AccountStatusBanner
      :title="t('accountShell.notifications.bannerTitle')"
      :description="t('accountShell.notifications.bannerDesc')"
      :status-text="unreadText"
      status-type="danger"
    />

    <AccountPanel :title="t('accountShell.notifications.panelTitle')">
      <template #extra>
        <button type="button" class="account-btn-secondary account-btn-secondary--compact" @click="handleMarkAllRead">
          {{ t('accountShell.notifications.markAllRead') }}
        </button>
      </template>

      <div v-if="notificationStore.loading" class="notifications-loading">{{ t('accountShell.common.loading') }}</div>

      <AccountEmptyState
        v-else-if="notificationStore.error"
        :message="t('accountShell.notifications.loadFailed')"
        :subtitle="notificationStore.error"
      />

      <template v-else>
        <NotificationTabs
          :active-tab="notificationStore.activeTab"
          :total="notificationStore.allCount"
          :unread="notificationStore.unreadCount"
          @change="setTab"
        />

        <NotificationList
          :items="notificationStore.items"
          :page="notificationStore.pageNum"
          :total="notificationStore.total"
          :page-size="notificationStore.pageSize"
          :loading="notificationStore.loading"
          :empty-message="emptyMessage"
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

  :deep(.account-panel__body) {
    overflow: visible;
    max-height: none;
  }
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
