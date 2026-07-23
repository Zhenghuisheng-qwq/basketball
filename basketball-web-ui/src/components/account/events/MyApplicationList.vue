<script setup lang="ts">
import EventStatusBadge from './EventStatusBadge.vue'
import type { MyApplication } from '@/types/accountEvents'

defineProps<{
  applications: MyApplication[]
}>()

const emit = defineEmits<{
  action: [application: MyApplication]
}>()
</script>

<template>
  <div class="my-events-list">
    <article
      v-for="item in applications"
      :key="item.applicationId"
      class="my-events-list__item"
      :class="{ 'my-events-list__item--pending': item.pending }"
    >
      <div class="my-events-list__main">
        <div class="my-events-list__head">
          <strong>{{ item.tournamentName }}</strong>
          <span>{{ item.platform }}</span>
        </div>
        <div class="my-events-list__meta">
          <span>报名类型：{{ item.applicationTypeText }}</span>
          <EventStatusBadge :text="item.statusText" :pending="item.pending" />
        </div>
        <p v-if="item.invitedTeamCode" class="my-events-list__extra">邀请球队：{{ item.invitedTeamCode }}</p>
        <p v-else-if="item.confirmedCount != null" class="my-events-list__extra">
          {{ item.confirmedCount }} / {{ item.rosterMax }} 人已确认
        </p>
        <p v-else-if="item.submitTime" class="my-events-list__extra">提交时间：{{ item.submitTime }}</p>
      </div>
      <div class="my-events-list__action">
        <button
          type="button"
          :class="item.pending ? 'account-btn-primary' : 'account-btn-secondary'"
          @click="emit('action', item)"
        >
          {{ item.actionText }}
        </button>
      </div>
    </article>
  </div>
</template>

<style scoped lang="scss">
.my-events-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__item {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;
    gap: 20px;
    width: 100%;
    min-height: 96px;
    padding: 22px 28px;
    background: rgba(255, 255, 255, 0.28);
    border: 1px solid rgba(0, 0, 0, 0.18);
    border-radius: 4px;
    box-sizing: border-box;

    &--pending {
      border-color: rgba(215, 25, 32, 0.35);
      background: rgba(215, 25, 32, 0.03);
    }
  }

  &__main {
    min-width: 0;
    flex: 1;
  }

  &__head {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: baseline;
    margin-bottom: 10px;

    strong {
      font-size: 20px;
      font-weight: 900;
    }

    span {
      color: #666;
      font-size: 15px;
    }
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    font-size: 16px;
  }

  &__extra {
    margin: 10px 0 0;
    font-size: 14px;
    color: #666;
  }

  &__action {
    display: flex;
    align-items: center;
    margin-left: auto;

    :deep(.account-btn-primary),
    :deep(.account-btn-secondary) {
      min-width: 160px;
      width: 180px;
      min-height: 44px;
      height: 44px;
      font-size: 16px;
    }
  }
}

@media (max-width: 767px) {
  .my-events-list__action {
    width: 100%;

    button {
      width: 100%;
    }
  }
}
</style>
