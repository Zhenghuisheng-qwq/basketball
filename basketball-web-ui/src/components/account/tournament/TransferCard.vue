<script setup lang="ts">
import TransferProgress from './TransferProgress.vue'
import type { TransferRecord } from '@/types/eventTeam'

defineProps<{
  transfer: TransferRecord
  directionLabel: string
}>()

const emit = defineEmits<{
  view: [transferId: string]
}>()
</script>

<template>
  <div class="transfer-card">
    <div class="transfer-card__head">
      <strong>{{ directionLabel }} {{ transfer.typeText.replace('申请', '') }}</strong>
      <span class="transfer-card__status">{{ transfer.statusText }}</span>
    </div>
    <div class="transfer-card__player">
      <strong>{{ transfer.playerName }}</strong>
      <span>位置 {{ transfer.position }}</span>
    </div>
    <p class="transfer-card__route">
      原球队 {{ transfer.fromTeamCode }} → 目标球队 {{ transfer.toTeamCode }}
    </p>
    <TransferProgress :steps="transfer.steps.slice(0, 3)" compact />
    <button type="button" class="account-btn-secondary" @click="emit('view', transfer.transferId)">
      查看进度
    </button>
  </div>
</template>

<style scoped lang="scss">
.transfer-card {
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__head {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 8px;
  }

  &__status {
    color: var(--account-warning, #d68a16);
    font-weight: 700;
    font-size: 13px;
  }

  &__player {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: baseline;

    strong {
      font-size: 18px;
    }

    span {
      color: #666;
      font-size: 13px;
    }
  }

  &__route {
    margin: 0;
    font-size: 13px;
    color: #555;
  }
}
</style>
