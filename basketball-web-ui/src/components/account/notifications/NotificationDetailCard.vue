<script setup lang="ts">
import { RouterLink } from 'vue-router'
import NotificationIcon from './NotificationIcon.vue'
import type { AccountNotificationItem } from '@/types/accountNotifications'

defineProps<{
  item: AccountNotificationItem
  actionLoading?: boolean
}>()

defineEmits<{
  'accept-invite': []
  'reject-invite': []
}>()
</script>

<template>
  <article class="notification-detail-card">
    <div class="notification-detail-card__head">
      <div class="notification-detail-card__type">
        <NotificationIcon :type="item.type" />
        <span>{{ item.typeText }}</span>
      </div>
      <time>{{ item.createdAt }}</time>
    </div>

    <div v-if="item.teamInvite?.teamLogo || item.teamInvite?.teamName" class="notification-detail-card__team">
      <img
        v-if="item.teamInvite.teamLogo"
        :src="item.teamInvite.teamLogo"
        :alt="item.teamInvite.teamName || '球队 Logo'"
        class="notification-detail-card__team-logo"
      />
      <div v-else class="notification-detail-card__team-logo notification-detail-card__team-logo--placeholder">
        {{ (item.teamInvite.teamName || '队').slice(0, 1) }}
      </div>
      <div>
        <p class="notification-detail-card__team-name">{{ item.teamInvite.teamName || '未知球队' }}</p>
        <p v-if="item.teamInvite.inviterName" class="notification-detail-card__team-sub">
          邀请人：{{ item.teamInvite.inviterName }}
        </p>
      </div>
    </div>

    <h2>{{ item.detailTitle }}</h2>

    <div class="notification-detail-card__body">
      <p v-for="(paragraph, index) in item.body" :key="index">{{ paragraph }}</p>
    </div>

    <dl class="notification-detail-card__meta">
      <div v-for="meta in item.meta" :key="meta.label" class="notification-detail-card__meta-row">
        <dt>{{ meta.label }}</dt>
        <dd>{{ meta.value }}</dd>
      </div>
    </dl>

    <div
      class="notification-detail-card__result"
      :class="`notification-detail-card__result--${item.resultStatus.toLowerCase()}`"
    >
      {{ item.resultText }}
    </div>

    <div
      v-if="item.teamInvite?.canRespond || item.teamInvite?.cannotAcceptReason"
      class="notification-detail-card__actions"
    >
      <p v-if="item.teamInvite?.cannotAcceptReason" class="notification-detail-card__hint">
        {{ item.teamInvite.cannotAcceptReason }}
      </p>
      <button
        v-if="item.teamInvite?.canAccept"
        type="button"
        class="account-btn-primary"
        :disabled="actionLoading"
        @click="$emit('accept-invite')"
      >
        接受邀请
      </button>
      <button
        v-if="item.teamInvite?.canRespond"
        type="button"
        class="account-btn-secondary"
        :disabled="actionLoading"
        @click="$emit('reject-invite')"
      >
        拒绝邀请
      </button>
    </div>

    <div v-else-if="item.teamInvite?.inviteStatus === 1" class="notification-detail-card__actions">
      <RouterLink to="/account/team" class="account-btn-primary notification-detail-card__link-btn">
        查看我的球队
      </RouterLink>
    </div>
  </article>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.notification-detail-card {
  padding: 30px 32px;
  background: rgba(255, 255, 255, 0.34);
  border: 1px solid rgba(0, 0, 0, 0.18);
  border-radius: 4px;

  &__head {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 18px;
  }

  &__type {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 800;
  }

  time {
    color: #666;
    font-size: 14px;
  }

  &__team {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 18px;
    padding: 14px 16px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.5);
  }

  &__team-logo {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    object-fit: cover;

    &--placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.06);
      font-size: 22px;
      font-weight: 800;
    }
  }

  &__team-name {
    margin: 0;
    font-size: 18px;
    font-weight: 800;
  }

  &__team-sub {
    margin: 4px 0 0;
    color: #666;
    font-size: 14px;
  }

  h2 {
    margin: 0 0 16px;
    font-size: clamp(24px, 2.4vw, 30px);
    font-weight: 900;
  }

  &__body {
    margin-bottom: 20px;

    p {
      margin: 0 0 10px;
      font-size: 16px;
      line-height: 1.85;
      color: #333;
    }
  }

  &__meta {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px 46px;
    margin: 0 0 20px;
    padding: 18px 0;
    border-top: 1px solid rgba(0, 0, 0, 0.1);
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  }

  &__meta-row {
    dt {
      margin-bottom: 4px;
      font-size: 12px;
      color: #666;
    }

    dd {
      margin: 0;
      font-size: 15px;
      font-weight: 700;
    }
  }

  &__result {
    display: inline-block;
    margin-bottom: 20px;
    padding: 8px 14px;
    border-radius: 4px;
    font-size: 13px;
    font-weight: 800;

    &--success {
      border: 1px solid rgba(35, 148, 71, 0.35);
      background: rgba(35, 148, 71, 0.08);
      color: #239447;
    }

    &--pending {
      border: 1px solid rgba(214, 138, 22, 0.35);
      background: rgba(214, 138, 22, 0.08);
      color: #b56a00;
    }

    &--warning {
      border: 1px solid rgba(214, 138, 22, 0.35);
      background: rgba(214, 138, 22, 0.08);
      color: #b56a00;
    }

    &--failed {
      border: 1px solid rgba(200, 10, 16, 0.35);
      background: rgba(200, 10, 16, 0.08);
      color: var(--account-danger, #c80a10);
    }
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__hint {
    flex: 1 1 100%;
    margin: 0 0 4px;
    color: #b56a00;
    font-size: 14px;
    line-height: 1.6;
  }

  &__link-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    text-decoration: none;
  }
}

@media (max-width: 991px) {
  .notification-detail-card__meta {
    gap: 16px 24px;
  }
}

@media (max-width: 767px) {
  .notification-detail-card {
    padding: 20px 18px;
  }

  .notification-detail-card__meta {
    grid-template-columns: 1fr;
  }

  .notification-detail-card__actions {
    flex-direction: column;

    .account-btn-primary,
    .account-btn-secondary,
    .notification-detail-card__link-btn {
      width: 100%;
    }
  }
}
</style>
