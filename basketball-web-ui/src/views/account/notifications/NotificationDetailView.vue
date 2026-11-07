<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
const notificationStore = useNotificationCenterStore()
const teamStore = useTeamStore()
const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')

const notificationId = computed(() => String(route.params.notificationId || ''))
const detailItem = ref<AccountNotificationItem | null>(null)

const notification = computed(() =>
  detailItem.value || notificationStore.getById(notificationId.value) || null,
)

function getHeroTitle(item: AccountNotificationItem) {
  if (item.type === 'TEAM_INVITE') return t('accountShell.notifications.teamInvite')
  if (item.type === 'ACCOUNT_SECURITY') return t('accountShell.notifications.securityAlert')
  return t('accountShell.notifications.typedTitle', { type: item.typeText })
}

const heroTitle = computed(() => (notification.value ? getHeroTitle(notification.value) : ''))
const heroDescription = computed(() => notification.value?.detailSubtitle || '')

async function loadNotification() {
  if (!notificationId.value) return
  loading.value = true
  loadError.value = ''
  try {
    const detail = await notificationStore.loadDetail(notificationId.value)
    detailItem.value = detail
    if (detail.isUnread) {
      await notificationStore.markAsRead(notificationId.value)
      detailItem.value = {
        ...detail,
        isUnread: false,
        status: 'READ',
      }
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountShell.notifications.loadFailedShort')
  } finally {
    loading.value = false
  }
}

async function refreshAfterInviteAction(accept: boolean) {
  const inviteId = notification.value?.teamInvite?.inviteId
  if (!inviteId) return

  actionLoading.value = true
  try {
    const result = await teamStore.respondInvitationAction(inviteId, accept)
    const refreshed = await notificationStore.loadDetail(notificationId.value)
    detailItem.value = refreshed
    const awaitingAudit = accept && result?.invitationStatus === 5
    ElMessage.success(accept
      ? t(awaitingAudit ? 'accountShell.notifications.awaitingAudit' : 'accountShell.notifications.joined')
      : t('accountShell.notifications.declined'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.notifications.actionFailed'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

async function handleAcceptInvite() {
  const teamName = notification.value?.teamInvite?.teamName || t('accountShell.notifications.thisTeam')
  try {
    await ElMessageBox.confirm(
      t('accountShell.notifications.acceptMessage', { name: teamName }),
      t('accountShell.notifications.acceptTitle'),
      {
        confirmButtonText: t('accountShell.notifications.confirmJoin'),
        cancelButtonText: t('accountShell.common.cancel'),
        type: 'warning',
      },
    )
    await refreshAfterInviteAction(true)
  } catch {
    // cancelled or failed
  }
}

async function handleRejectInvite() {
  const teamName = notification.value?.teamInvite?.teamName || t('accountShell.notifications.thisTeam')
  try {
    await ElMessageBox.confirm(
      t('accountShell.notifications.rejectMessage', { name: teamName }),
      t('accountShell.notifications.rejectTitle'),
      {
        confirmButtonText: t('accountShell.notifications.confirmReject'),
        cancelButtonText: t('accountShell.common.cancel'),
        type: 'warning',
      },
    )
    await refreshAfterInviteAction(false)
  } catch {
    // cancelled or failed
  }
}

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter'), to: '/account/overview' },
  { label: t('accountShell.notifications.title'), to: '/account/notifications' },
  { label: t('accountShell.notifications.detailTitle') },
])

const detailStatusText = computed(() => {
  const item = notification.value
  if (!item) return ''
  return item.teamInvite?.inviteStatusText || (item.isUnread ? t('accountShell.notifications.unread') : t('accountShell.notifications.read'))
})

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
      :zh-title="t('accountShell.notifications.detailTitle')"
      en-title="NOTIFICATION DETAIL"
      :breadcrumb-items="breadcrumbs"
    />

    <div v-if="loading" class="notification-detail-empty">{{ t('accountShell.common.loading') }}</div>

    <AccountEmptyState
      v-else-if="loadError"
      :message="loadError"
    />

    <template v-else-if="notification">
      <AccountStatusBanner
        :title="heroTitle"
        :description="heroDescription"
        :status-text="detailStatusText"
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
      <AccountEmptyState :message="t('accountShell.notifications.missing')" />
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
