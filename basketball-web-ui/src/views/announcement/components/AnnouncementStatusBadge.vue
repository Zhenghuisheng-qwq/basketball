<script setup lang="ts">
import type { AnnouncementStatus } from '../types'

defineProps<{
  status: AnnouncementStatus
}>()

const labelMap: Record<Exclude<AnnouncementStatus, 'NORMAL'>, string> = {
  ACTIVE: '现行有效',
  REPLACED: '已被替代',
  EXPIRED: '已失效',
}
</script>

<template>
  <span
    v-if="status !== 'NORMAL'"
    class="announcement-status-badge"
    :class="`announcement-status-badge--${status.toLowerCase()}`"
  >
    {{ labelMap[status] }}
  </span>
</template>

<style scoped lang="scss">
.announcement-status-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.announcement-status-badge--active {
  border: 1px solid var(--cnpa-red, #e10613);
  color: var(--cnpa-red, #e10613);
  background: transparent;
}

.announcement-status-badge--replaced {
  background: #3a3a3a;
  color: rgba(255, 255, 255, 0.88);
}

.announcement-status-badge--expired {
  background: #d8d8d8;
  color: #666;
}
</style>
