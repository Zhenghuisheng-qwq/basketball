<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import EventStatusBadge from './EventStatusBadge.vue'
import type { ParticipatingEvent } from '@/types/accountEvents'

const { t } = useI18n()

defineProps<{
  events: ParticipatingEvent[]
}>()

const emit = defineEmits<{
  action: [event: ParticipatingEvent]
  matchDataAction: [event: ParticipatingEvent]
  rosterChange: [event: ParticipatingEvent]
}>()
</script>

<template>
  <div class="my-events-list">
    <article
      v-for="item in events"
      :key="item.relationId"
      class="my-events-list__item my-events-list__item--joined"
    >
      <div class="my-events-list__main">
        <div class="my-events-list__head">
          <strong>{{ item.tournamentName }}</strong>
          <span>{{ item.platform }}</span>
        </div>
        <div class="my-events-list__meta">
          <span>{{ t('accountEvents.joined.participation', { type: item.participationTypeText }) }}</span>
          <span>{{ t('accountEvents.joined.identity', { identity: item.identityText }) }}</span>
          <span v-if="item.stageSummary">{{ t('accountEvents.joined.stage', { stage: item.stageSummary }) }}</span>
          <span v-if="item.rankSummary">{{ item.rankSummary }}</span>
          <span v-if="item.qualificationSummary">{{ t('accountEvents.joined.qualification', { status: item.qualificationSummary }) }}</span>
          <EventStatusBadge
            :text="item.statusText"
            :finished="item.status === 'FINISHED'"
            :pending="Boolean(item.pendingTaskCount)"
          />
        </div>
        <p v-if="item.pendingTaskText" class="my-events-list__tag">{{ item.pendingTaskText }}</p>
        <p v-if="item.nextMatchSummary || item.latestMatchSummary" class="my-events-list__sub">
          <span v-if="item.nextMatchSummary">{{ item.nextMatchSummary }}</span>
          <span v-if="item.latestMatchSummary">{{ item.latestMatchSummary }}</span>
        </p>
      </div>
      <div class="my-events-list__action">
        <button
          v-if="item.showRosterChange"
          type="button"
          class="account-btn-primary"
          @click="emit('rosterChange', item)"
        >
          {{ item.rosterChangeText }}
        </button>
        <button
          v-if="item.showMatchDataEntry"
          type="button"
          :class="item.matchDataPrimary ? 'account-btn-primary' : 'account-btn-secondary'"
          @click="emit('matchDataAction', item)"
        >
          {{ item.matchDataActionText }}
        </button>
        <button
          type="button"
          :class="item.primaryAction && !item.matchDataPrimary ? 'account-btn-primary' : 'account-btn-secondary'"
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
    min-height: 100px;
    padding: 22px 28px;
    background: rgba(255, 255, 255, 0.28);
    border: 1px solid rgba(0, 0, 0, 0.18);
    border-radius: 4px;
    box-sizing: border-box;
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

  &__tag {
    display: inline-block;
    margin: 10px 0 0;
    padding: 6px 12px;
    border-radius: 2px;
    background: rgba(215, 25, 32, 0.12);
    color: var(--account-danger, #c80a10);
    font-size: 13px;
    font-weight: 800;
  }

  &__sub {
    margin: 8px 0 0;
    font-size: 13px;
    color: rgba(0, 0, 0, 0.55);
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__action {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    margin-left: auto;

    :deep(.account-btn-primary),
    :deep(.account-btn-secondary) {
      min-width: 140px;
      width: auto;
      min-height: 44px;
      height: 44px;
      padding: 0 16px;
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
