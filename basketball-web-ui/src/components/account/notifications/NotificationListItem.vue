<script setup lang="ts">
import NotificationIcon from './NotificationIcon.vue'
import type { AccountNotificationItem } from '@/types/accountNotifications'

defineProps<{
  item: AccountNotificationItem
}>()

const emit = defineEmits<{
  view: [id: string]
}>()
</script>

<template>
  <div class="notification-row" :class="{ 'notification-row--unread': item.isUnread }">
    <div class="notification-row__type">
      <NotificationIcon :type="item.type" />
      <span>{{ item.typeText }}</span>
      <span v-if="item.isUnread" class="notification-row__dot" aria-label="未读" />
    </div>
    <div class="notification-row__content">
      <strong>{{ item.title }}</strong>
      <p>{{ item.summary }}</p>
    </div>
    <time class="notification-row__time">{{ item.createdAt }}</time>
    <div class="notification-row__action">
      <button type="button" class="account-btn-secondary notification-row__btn" @click="emit('view', item.id)">
        查看详情
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.notification-row {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr) 180px 120px;
  align-items: center;
  column-gap: 22px;
  min-height: 88px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  box-sizing: border-box;

  &:last-child {
    border-bottom: 0;
  }

  &__type {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 120px;
    font-size: 15px;
    font-weight: 800;
    color: #111;
  }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--account-red, #d90000);
    flex-shrink: 0;
  }

  &__content {
    min-width: 0;

    strong {
      display: block;
      margin-bottom: 6px;
      font-size: 18px;
      font-weight: 900;
      color: #111;
    }

    p {
      margin: 0;
      overflow: hidden;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.62);
      line-height: 1.5;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__time {
    width: 170px;
    font-size: 14px;
    color: #777;
    white-space: nowrap;
  }

  &__action {
    width: 120px;
    text-align: right;
  }

  &__btn {
    min-height: 40px;
    height: 40px;
    padding: 0 16px;
    font-size: 15px;
  }
}

@media (max-width: 1199px) and (min-width: 768px) {
  .notification-row {
    grid-template-columns: 130px minmax(0, 1fr) auto;
    grid-template-areas:
      'type content action'
      'type time action';
    column-gap: 12px;
    row-gap: 4px;

    &__type {
      grid-area: type;
    }

    &__content {
      grid-area: content;

      p {
        white-space: normal;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
      }
    }

    &__time {
      grid-area: time;
    }

    &__action {
      grid-area: action;
      align-self: center;
    }
  }
}

@media (max-width: 767px) {
  .notification-row {
    grid-template-columns: 1fr;
    gap: 10px;
    min-height: 0;
    padding: 16px 0;
  }

  .notification-row__content p {
    white-space: normal;
  }

  .notification-row__action {
    text-align: left;

    .notification-row__btn {
      width: 100%;
    }
  }
}
</style>
