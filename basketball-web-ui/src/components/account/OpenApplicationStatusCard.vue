<script setup lang="ts">
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'

defineProps<{
  variant: 'pending' | 'rejected' | 'approved'
  title: string
  description?: string
  badgeText: string
  badgeType?: 'success' | 'warning' | 'danger'
  reviewRemark?: string
  reviewTime?: string
  groupingStatusText?: string
  actionDisabled?: boolean
}>()

defineEmits<{
  action: []
  secondary: []
}>()

const actionLabels: Record<string, { primary: string; secondary?: string }> = {
  pending: { primary: '返回我的赛事', secondary: undefined },
  rejected: { primary: '修改并重新提交', secondary: undefined },
  approved: { primary: '查看赛事', secondary: undefined },
}
</script>

<template>
  <div class="open-application-status-card" :class="`open-application-status-card--${variant}`">
    <div class="open-application-status-card__icon" aria-hidden="true">
      <svg v-if="variant === 'pending'" viewBox="0 0 24 24">
        <path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z" />
      </svg>
      <svg v-else-if="variant === 'rejected'" viewBox="0 0 24 24">
        <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
      </svg>
      <svg v-else viewBox="0 0 24 24">
        <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
      </svg>
    </div>
    <strong>{{ title }}</strong>
    <AccountStatusBadge :type="badgeType || (variant === 'approved' ? 'success' : variant === 'pending' ? 'warning' : 'danger')" :text="badgeText" />
    <p v-if="description">{{ description }}</p>
    <p v-if="reviewRemark" class="open-application-status-card__remark">{{ reviewRemark }}</p>
    <div v-if="reviewTime || groupingStatusText" class="account-info-list">
      <div v-if="reviewTime" class="account-info-row">
        <span class="account-info-row__label">审核时间</span>
        <span>{{ reviewTime }}</span>
      </div>
      <div v-if="groupingStatusText" class="account-info-row">
        <span class="account-info-row__label">赛事状态</span>
        <span>{{ groupingStatusText }}</span>
      </div>
    </div>
    <div class="open-application-status-card__actions">
      <button
        type="button"
        class="account-btn-primary"
        :disabled="actionDisabled"
        @click="$emit('action')"
      >
        {{ actionLabels[variant].primary }}
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
