<script setup lang="ts">
import EventTeamLogo from './EventTeamLogo.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import type { EventTeam } from '@/types/eventTeam'

defineProps<{
  eventTeam: EventTeam
  rosterCount: number
}>()
</script>

<template>
  <div class="event-team-summary">
    <EventTeamLogo :team-code="eventTeam.teamCode" :logo-url="eventTeam.teamLogoUrl" size="lg" />
    <div class="event-team-summary__body">
      <div class="event-team-summary__head">
        <strong>赛事球队：{{ eventTeam.teamName }}</strong>
        <AccountStatusBadge type="success" :text="eventTeam.rosterStatusText" />
      </div>
      <div class="account-info-list">
        <div class="account-info-row">
          <span class="account-info-row__label">永久球队 ID</span>
          <span>{{ eventTeam.permanentTeamNo }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">比赛平台</span>
          <span>{{ eventTeam.platform }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">当前队长</span>
          <span>{{ eventTeam.captainName }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">阵容人数</span>
          <span>{{ rosterCount }} / {{ eventTeam.rosterMax }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">当前阶段</span>
          <span>{{ eventTeam.stageName }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">阵容状态</span>
          <span class="account-status-text--success">{{ eventTeam.rosterStatusText }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.event-team-summary {
  display: flex;
  gap: 20px;
  align-items: flex-start;

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    strong {
      font-size: 20px;
    }
  }
}

@media (max-width: 767px) {
  .event-team-summary {
    flex-direction: column;
  }
}
</style>
