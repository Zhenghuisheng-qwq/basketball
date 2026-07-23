<script setup lang="ts">
import NotificationIcon from './NotificationIcon.vue'
import type { AccountNotificationItem } from '@/types/accountNotifications'

defineProps<{
  item: AccountNotificationItem
}>()

const emit = defineEmits<{
  primary: []
  back: []
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

    <div class="notification-detail-card__actions">
      <button type="button" class="account-btn-primary" @click="emit('primary')">
        {{ item.primaryActionText }}
      </button>
      <button type="button" class="account-btn-secondary" @click="emit('back')">
        {{ item.secondaryActionText }}
      </button>
    </div>
  </article>
</template>

<style scoped lang="scss">
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

    :deep(.account-btn-primary) {
      min-height: 44px;
      padding: 0 36px;
      background: linear-gradient(180deg, #e00000, #b30000);
    }

    :deep(.account-btn-secondary) {
      min-height: 42px;
      padding: 0 32px;
    }
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

    :deep(button) {
      width: 100%;
    }
  }
}
</style>
