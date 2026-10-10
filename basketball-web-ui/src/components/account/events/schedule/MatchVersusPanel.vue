<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import TeamLogoPlaceholder from '@/components/account/events/schedule/TeamLogoPlaceholder.vue'
import type { ScheduleVersusCenterView, ScheduleVersusTeamView } from '@/types/v4ScheduleCard'

defineProps<{
  home: ScheduleVersusTeamView
  away: ScheduleVersusTeamView
  center: ScheduleVersusCenterView
}>()

const { t } = useI18n()
</script>

<template>
  <div class="schedule-versus-panel">
    <div class="schedule-versus-panel__team">
      <TeamLogoPlaceholder
        :logo-url="home.logoUrl"
        :team-name="home.name"
        :short-name="home.shortName"
      />
      <div class="schedule-versus-panel__team-name" :title="home.name">{{ home.name }}</div>
      <div class="schedule-versus-panel__team-short">{{ home.shortName }}</div>
      <span v-if="home.isMine" class="schedule-versus-panel__team-tag schedule-versus-panel__team-tag--mine">
        {{ t('accountEvents.schedule.myTeam') }}
      </span>
      <span
        v-else-if="home.isWinner"
        class="schedule-versus-panel__team-tag schedule-versus-panel__team-tag--winner"
      >
        {{ t('accountEvents.schedule.win') }}
      </span>
    </div>

    <div class="schedule-versus-panel__center">
      <template v-if="center.mode === 'score' || center.mode === 'playing'">
        <div class="schedule-versus-panel__score">
          <span>{{ center.homeScore ?? '-' }}</span>
          <span class="schedule-versus-panel__score-sep">:</span>
          <span>{{ center.awayScore ?? '-' }}</span>
        </div>
        <p class="schedule-versus-panel__status">{{ center.statusText }}</p>
      </template>

      <template v-else-if="center.mode === 'status'">
        <div class="schedule-versus-panel__vs">{{ center.statusText || 'VS' }}</div>
      </template>

      <template v-else>
        <p class="schedule-versus-panel__date">{{ center.dateText }}</p>
        <p v-if="center.timeText" class="schedule-versus-panel__time">{{ center.timeText }}</p>
        <div class="schedule-versus-panel__vs">VS</div>
        <p class="schedule-versus-panel__status">{{ center.statusText }}</p>
        <p v-if="center.countdownHint" class="schedule-versus-panel__countdown">{{ center.countdownHint }}</p>
      </template>
    </div>

    <div class="schedule-versus-panel__team schedule-versus-panel__team--away">
      <TeamLogoPlaceholder
        :logo-url="away.logoUrl"
        :team-name="away.name"
        :short-name="away.shortName"
      />
      <div class="schedule-versus-panel__team-name" :title="away.name">{{ away.name }}</div>
      <div class="schedule-versus-panel__team-short">{{ away.shortName }}</div>
      <span v-if="away.isMine" class="schedule-versus-panel__team-tag schedule-versus-panel__team-tag--mine">
        {{ t('accountEvents.schedule.myTeam') }}
      </span>
      <span
        v-else-if="away.isWinner"
        class="schedule-versus-panel__team-tag schedule-versus-panel__team-tag--winner"
      >
        {{ t('accountEvents.schedule.win') }}
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import './schedule-match-card.scss';
</style>
