<script setup lang="ts">
import { getTeamById } from '../mock/teams.mock'
import type { Match } from '../types/events'

defineProps<{
  visible: boolean
  match: Match | null
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <div v-if="visible && match" class="modal-mask" @click.self="emit('close')">
    <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="match-detail-title">
      <button type="button" class="modal-close" aria-label="关闭" @click="emit('close')">×</button>
      <h3 id="match-detail-title">比赛详情</h3>
      <p class="modal-meta">{{ match.date }} · {{ match.startTime }} · {{ match.venue }}</p>
      <div class="modal-versus sport-num">
        <span>{{ getTeamById(match.awayTeamId)?.name }}</span>
        <strong>
          <template v-if="match.homeScore !== undefined">{{ match.awayScore }} : {{ match.homeScore }}</template>
          <template v-else>VS</template>
        </strong>
        <span>{{ getTeamById(match.homeTeamId)?.name }}</span>
      </div>
      <p class="modal-tip">Mock 详情占位，后续接入真实比赛详情页。</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.72);
}

.modal-card {
  position: relative;
  width: min(520px, 100%);
  padding: 28px;
  background: #111;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
}

.modal-meta {
  color: var(--cnpa-text-secondary);
  font-size: 13px;
}

.modal-versus {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 24px 0;
  font-size: 28px;
}

.modal-tip {
  margin: 0;
  font-size: 13px;
  color: var(--cnpa-text-secondary);
}
</style>
