<script setup lang="ts">
import type { AvailableEvent } from '@/types/accountEvents'

defineProps<{
  events: AvailableEvent[]
}>()

const emit = defineEmits<{
  action: [event: AvailableEvent]
}>()

function quotaText(event: AvailableEvent) {
  return `${event.quotaUsed} / ${event.quotaTotal}`
}

function deadlineText(event: AvailableEvent) {
  return event.deadline || '-'
}
</script>

<template>
  <div class="event-list-card">
    <div class="event-table-head">
      <span>赛事名称</span>
      <span>平台</span>
      <span>报名类型</span>
      <span>报名截止</span>
      <span>名额</span>
      <span>操作</span>
    </div>

    <div
      v-for="event in events"
      :key="event.tournamentId"
      class="event-table-row"
    >
      <strong class="event-table-row__name">{{ event.tournamentName }}</strong>
      <span>{{ event.platform }}</span>
      <span>{{ event.registrationTypeText }}</span>
      <span>{{ deadlineText(event) }}</span>
      <span>{{ quotaText(event) }}</span>
      <div class="event-table-row__action">
        <button
          type="button"
          :class="event.actionType === 'DISABLED' ? 'account-btn-secondary' : 'account-btn-primary'"
          :disabled="event.actionType === 'DISABLED'"
          @click="emit('action', event)"
        >
          {{ event.actionText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.event-list-card {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.18);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.28);
  overflow: hidden;
}

.event-table-head,
.event-table-row {
  display: grid;
  grid-template-columns: minmax(260px, 1.4fr) 140px 160px 190px 130px 180px;
  align-items: center;
  column-gap: 16px;
  padding: 0 28px;
  box-sizing: border-box;
}

.event-table-head {
  min-height: 56px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.14);
  background: rgba(0, 0, 0, 0.04);
  color: rgba(0, 0, 0, 0.62);
  font-size: 14px;
  font-weight: 800;

  span:last-child {
    text-align: right;
  }
}

.event-table-row {
  min-height: 92px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  font-size: 16px;
  color: #222;

  &:last-child {
    border-bottom: 0;
  }

  &__name {
    font-size: 20px;
    font-weight: 900;
    color: #111;
    line-height: 1.25;
  }

  &__action {
    display: flex;
    justify-content: flex-end;

    :deep(.account-btn-primary),
    :deep(.account-btn-secondary) {
      min-width: 150px;
      width: min(100%, 180px);
      min-height: 44px;
      height: 44px;
      padding: 0 16px;
      font-size: 15px;
    }
  }
}

@media (max-width: 1199px) {
  .event-table-head {
    display: none;
  }

  .event-table-row {
    grid-template-columns: 1fr 1fr;
    gap: 12px 18px;
    min-height: 0;
    padding: 22px 24px;

    &__name {
      grid-column: 1 / -1;
    }

    &__action {
      grid-column: 1 / -1;
      justify-content: flex-start;

      :deep(.account-btn-primary),
      :deep(.account-btn-secondary) {
        width: min(100%, 220px);
      }
    }
  }
}

@media (max-width: 767px) {
  .event-table-row {
    grid-template-columns: 1fr;

    &__action {
      :deep(.account-btn-primary),
      :deep(.account-btn-secondary) {
        width: 100%;
      }
    }
  }
}
</style>
