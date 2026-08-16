<script setup lang="ts">
import type { PublicActiveDisciplineSummary } from '@/api/types/v4Public'

defineProps<{
  items?: PublicActiveDisciplineSummary[] | null
  title?: string
}>()

function formatTime(value?: string) {
  if (!value) return '--'
  return value.slice(0, 10)
}
</script>

<template>
  <section v-if="items?.length" class="discipline-status-panel">
    <div class="discipline-status-panel__head">
      <h3>{{ title || '赛事状态' }}</h3>
      <span>当前有效处罚</span>
    </div>
    <article v-for="(item, index) in items" :key="`${item.penaltyCode}-${index}`" class="discipline-status-panel__item">
      <div class="discipline-status-panel__row">
        <em>处罚类型</em>
        <strong>{{ item.penaltyLabel || item.tagLabel || '--' }}</strong>
      </div>
      <div class="discipline-status-panel__row">
        <em>生效时间</em>
        <strong>{{ formatTime(item.effectiveStartTime) }}</strong>
      </div>
      <div class="discipline-status-panel__row">
        <em>状态</em>
        <strong>{{ item.statusLabel || '生效中' }}</strong>
      </div>
      <p v-if="item.description" class="discipline-status-panel__desc">{{ item.description }}</p>
      <p v-if="item.publicReason" class="discipline-status-panel__desc">原因：{{ item.publicReason }}</p>
    </article>
  </section>
</template>

<style scoped lang="scss">
.discipline-status-panel {
  margin: 0 0 24px;
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(12, 8, 8, 0.55);
}

.discipline-status-panel__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;

  h3 {
    margin: 0;
    color: #fff;
    font-size: 16px;
    font-weight: 800;
  }

  span {
    color: rgba(255, 255, 255, 0.45);
    font-size: 12px;
  }
}

.discipline-status-panel__item + .discipline-status-panel__item {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.discipline-status-panel__row {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
  font-size: 13px;

  em {
    flex: none;
    width: 72px;
    color: rgba(255, 255, 255, 0.48);
    font-style: normal;
  }

  strong {
    color: #fff;
    font-weight: 700;
  }
}

.discipline-status-panel__desc {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  line-height: 1.5;
}
</style>
