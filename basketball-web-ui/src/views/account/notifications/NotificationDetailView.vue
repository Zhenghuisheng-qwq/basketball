<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import NotificationDetailCard from '@/components/account/notifications/NotificationDetailCard.vue'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { AccountNotificationItem } from '@/types/accountNotifications'

const route = useRoute()
const router = useRouter()
const notificationStore = useNotificationCenterStore()

const notificationId = computed(() => String(route.params.notificationId || ''))

const notification = computed(() => notificationStore.getById(notificationId.value))

function getHeroTitle(item: AccountNotificationItem) {
  if (item.type === 'TEAM_INVITE') return '球队邀请'
  if (item.type === 'ACCOUNT_SECURITY') return '账号安全提醒'
  return `${item.typeText}通知`
}

const heroTitle = computed(() => (notification.value ? getHeroTitle(notification.value) : ''))
const heroDescription = computed(() => notification.value?.detailSubtitle || '')

function handlePrimary() {
  if (!notification.value) return
  router.push(notification.value.primaryActionRoute)
}

function handleBack() {
  router.push('/account/notifications')
}

watch(
  notificationId,
  (id) => {
    if (id) notificationStore.markAsRead(id)
  },
  { immediate: true },
)
</script>

<template>
  <div class="account-view account-view--notification-detail account-center-vars">
    <AccountPageHeader
      zh-title="通知详情"
      en-title="NOTIFICATION DETAIL"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '消息通知', to: '/account/notifications' },
        { label: '通知详情' },
      ]"
    />

    <template v-if="notification">
      <AccountStatusBanner
        :title="heroTitle"
        :description="heroDescription"
        status-text="已读"
        status-type="danger"
      />

      <NotificationDetailCard
        :item="notification"
        @primary="handlePrimary"
        @back="handleBack"
      />
    </template>

    <div v-else class="notification-detail-empty">
      <AccountEmptyState message="通知不存在" />
      <button type="button" class="account-btn-secondary" @click="handleBack">
        返回通知列表
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view--notification-detail {
  width: 100%;
  min-width: 0;
}

.notification-detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px 20px;
}
</style>
