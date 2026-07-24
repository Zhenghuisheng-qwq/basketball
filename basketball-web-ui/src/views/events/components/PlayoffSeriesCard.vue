<script setup lang="ts">
// 季后赛对阵卡片暂无公开接口；PlayoffBracket 已改为空状态，本组件保留占位避免断链。
import type { PlayoffSeries } from '../types/events'

defineProps<{
  series: PlayoffSeries
  variant?: 'default' | 'semifinal' | 'final'
}>()
</script>

<template>
  <div class="playoff-series-card playoff-series-card--empty">对阵待定</div>
</template>

<style scoped lang="scss">
.playoff-series-card--empty {
  min-height: 72px;
  display: grid;
  place-items: center;
  border: 1px dashed rgba(17, 17, 17, 0.18);
  color: rgba(17, 17, 17, 0.45);
  font-size: 13px;
  font-weight: 700;
}
</style>
