<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ScheduleSubmissionSideView } from '@/types/v4ScheduleCard'

const { t } = useI18n()

defineProps<{
  mySubmission: ScheduleSubmissionSideView
  opponentSubmission: ScheduleSubmissionSideView
  hint?: string
}>()
</script>

<template>
  <section class="schedule-submission-status">
    <div class="schedule-submission-status__grid">
      <article
        class="schedule-submission-status__unit"
        :class="`schedule-submission-status__unit--${mySubmission.tone}`"
      >
        <p class="schedule-submission-status__side">{{ mySubmission.sideLabel }}</p>
        <p class="schedule-submission-status__label">{{ mySubmission.statusLabel }}</p>
        <p v-if="mySubmission.detail" class="schedule-submission-status__detail">
          {{ t('accountEvents.common.rejectReason', { reason: mySubmission.detail }) }}
        </p>
      </article>

      <article
        class="schedule-submission-status__unit"
        :class="`schedule-submission-status__unit--${opponentSubmission.tone}`"
      >
        <p class="schedule-submission-status__side">{{ opponentSubmission.sideLabel }}</p>
        <p class="schedule-submission-status__label">{{ opponentSubmission.statusLabel }}</p>
        <p v-if="opponentSubmission.detail" class="schedule-submission-status__detail">
          {{ opponentSubmission.detail }}
        </p>
      </article>
    </div>
    <p v-if="hint" class="schedule-submission-status__hint">{{ hint }}</p>
  </section>
</template>

<style scoped lang="scss">
@import './schedule-match-card.scss';
</style>
