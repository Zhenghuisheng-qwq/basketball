<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import NotificationDetailCard from '@/components/account/notifications/NotificationDetailCard.vue'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { useTeamStore } from '@/stores/team'
import type { AccountNotificationItem } from '@/types/accountNotifications'

const route = useRoute()
const notificationStore = useNotificationCenterStore()
const teamStore = useTeamStore()
const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')

const notificationId = computed(() => String(route.params.notificationId || ''))

const notification = computed(() => notificationStore.getById(notificationId.value))

function getHeroTitle(item: AccountNotificationItem) {
  if (item.type === 'TEAM_INVITE') return '球队邀请'
  if (item.type === 'ACCOUNT_SECURITY') return '账号安全提醒'
  return `${item.typeText}通知`
}

const heroTitle = computed(() => (notification.value ? getHeroTitle(notification.value) : ''))
const heroDescription = computed(() => notification.value?.detailSubtitle || '')

async function loadNotification() {
  if (!notificationId.value) return
  loading.value = true
  loadError.value = ''
  try {
    await notificationStore.loadDetail(notificationId.value)
    await notificationStore.markAsRead(notificationId.value)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function refreshAfterInviteAction(accept: boolean) {
  const inviteId = notification.value?.teamInvite?.inviteId
  if (!inviteId) return

  actionLoading.value = true
  try {
    await teamStore.respondInvitationAction(inviteId, accept)
    await notificationStore.loadDetail(notificationId.value)
    ElMessage.success(accept ? '已加入球队' : '已拒绝邀请')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
    throw error
  } finally {
    actionLoading.value = false
  }
}

async function handleAcceptInvite() {
  const teamName = notification.value?.teamInvite?.teamName || '该球队'
  try {
    await ElMessageBox.confirm(
      `确认接受球队「${teamName}」的邀请并加入该球队吗？`,
      '确认加入球队？',
      {
        confirmButtonText: '确认加入',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await refreshAfterInviteAction(true)
  } catch {
    // cancelled or failed
  }
}

async function handleRejectInvite() {
  const teamName = notification.value?.teamInvite?.teamName || '该球队'
  try {
    await ElMessageBox.confirm(
      `确认拒绝球队「${teamName}」的邀请吗？`,
      '确认拒绝邀请？',
      {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await refreshAfterInviteAction(false)
  } catch {
    // cancelled or failed
  }
}

watch(notificationId, () => {
  loadNotification()
}, { immediate: true })

onMounted(() => {
  if (!notificationStore.items.length) {
    notificationStore.refresh().catch(() => undefined)
  }
})
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

    <div v-if="loading" class="notification-detail-empty">加载中...</div>

    <AccountEmptyState
      v-else-if="loadError"
      :message="loadError"
    />

    <template v-else-if="notification">
      <AccountStatusBanner
        :title="heroTitle"
        :description="heroDescription"
        :status-text="notification.teamInvite?.inviteStatusText || (notification.isUnread ? '未读' : '已读')"
        :status-type="notification.teamInvite?.canRespond ? 'warning' : (notification.isUnread ? 'danger' : 'success')"
      />

      <NotificationDetailCard
        :item="notification"
        :action-loading="actionLoading"
        @accept-invite="handleAcceptInvite"
        @reject-invite="handleRejectInvite"
      />
    </template>

    <div v-else class="notification-detail-empty">
      <AccountEmptyState message="通知不存在" />
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
