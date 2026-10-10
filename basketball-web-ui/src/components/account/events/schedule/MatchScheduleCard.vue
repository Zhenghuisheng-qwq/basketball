<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import MatchActionPanel from '@/components/account/events/schedule/MatchActionPanel.vue'
import MatchEventHeader from '@/components/account/events/schedule/MatchEventHeader.vue'
import MatchSubmissionStatusPanel from '@/components/account/events/schedule/MatchSubmissionStatusPanel.vue'
import MatchVersusPanel from '@/components/account/events/schedule/MatchVersusPanel.vue'
import type { ScheduleMatchCardViewModel } from '@/types/v4ScheduleCard'

defineProps<{
  card: ScheduleMatchCardViewModel
}>()

const { t } = useI18n()

const accentClassMap = {
  default: '',
  upcoming: 'schedule-match-card--upcoming',
  finished: 'schedule-match-card--finished',
  'my-pending-submit': 'schedule-match-card--my-pending-submit',
  'my-pending-review': 'schedule-match-card--my-pending-review',
  'my-rejected': 'schedule-match-card--my-rejected',
  'waiting-opponent': 'schedule-match-card--waiting-opponent',
  confirmed: 'schedule-match-card--confirmed',
  abnormal: 'schedule-match-card--abnormal',
} as const
</script>

<template>
  <article
    class="schedule-match-card"
    :class="accentClassMap[card.accent]"
  >
    <span v-if="card.isNextMatch" class="schedule-match-card__next-flag">{{ t('accountEvents.schedule.nextFlag') }}</span>

    <p v-if="card.accent === 'my-pending-submit'" class="schedule-match-card__alert">
      {{ t('accountEvents.schedule.pendingAlert') }}
    </p>

    <div class="schedule-match-card__body">
      <MatchEventHeader :header="card.header" />
      <MatchVersusPanel :home="card.home" :away="card.away" :center="card.center" />
      <MatchSubmissionStatusPanel
        :my-submission="card.mySubmission"
        :opponent-submission="card.opponentSubmission"
        :hint="card.actions.hint"
      />
    </div>

    <footer class="schedule-match-card__footer">
      <p v-if="card.matchNo" class="schedule-match-card__match-no">
        {{ t('accountEvents.schedule.matchCode') }}<strong>{{ card.matchNo }}</strong>
      </p>
      <MatchActionPanel :actions="card.actions" />
    </footer>
  </article>
</template>

<style scoped lang="scss">
@import './schedule-match-card.scss';
</style>
