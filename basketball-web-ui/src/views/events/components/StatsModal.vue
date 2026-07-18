<script setup lang="ts">
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
    <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="stats-title">
      <button type="button" class="modal-close" aria-label="关闭" @click="emit('close')">×</button>
      <h3 id="stats-title">技术统计</h3>
      <p class="modal-meta">比赛 ID：{{ match.id }}</p>
      <div class="stats-placeholder">
        <div v-for="item in 4" :key="item" class="stats-row">
          <span>统计项 {{ item }}</span>
          <span>--</span>
        </div>
      </div>
      <p class="modal-tip">Mock 技术统计占位，后续接入真实数据。</p>
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

.stats-placeholder {
  margin: 20px 0;
  display: grid;
  gap: 10px;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.modal-tip {
  margin: 0;
  font-size: 13px;
  color: var(--cnpa-text-secondary);
}
</style>
