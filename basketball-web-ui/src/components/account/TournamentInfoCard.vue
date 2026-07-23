<script setup lang="ts">
import type { OpenTournament } from '@/types/account'

defineProps<{
  tournament: OpenTournament
}>()
</script>

<template>
  <div class="tournament-info-card">
    <div class="account-info-list">
      <div class="account-info-row">
        <span class="account-info-row__label">赛事名称</span>
        <span class="account-info-row__value">{{ tournament.name }}</span>
      </div>
      <div class="account-info-row">
        <span class="account-info-row__label">比赛平台</span>
        <span class="account-info-row__value">{{ tournament.platform }}</span>
      </div>
      <div class="account-info-row">
        <span class="account-info-row__label">报名方式</span>
        <span class="account-info-row__value">{{ tournament.registrationTypeText }}</span>
      </div>
      <div class="account-info-row">
        <span class="account-info-row__label">报名截止</span>
        <span class="account-info-row__value">{{ tournament.registrationDeadline }}</span>
      </div>
      <div class="account-info-row">
        <span class="account-info-row__label">名额状态</span>
        <span class="account-info-row__value">{{ tournament.quotaUsed }} / {{ tournament.quotaTotal }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
