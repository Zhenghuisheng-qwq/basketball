<script setup lang="ts">
import type { Announcement } from '../types'
import AnnouncementListItem from './AnnouncementListItem.vue'

defineProps<{
  items: Announcement[]
  emptyText: string
}>()

const emit = defineEmits<{
  select: [id: number]
}>()
</script>

<template>
  <div class="announcement-list">
    <AnnouncementListItem
      v-for="item in items"
      :key="item.id"
      :item="item"
      @click="emit('select', $event)"
    />

    <div v-if="items.length === 0" class="announcement-list__empty">
      {{ emptyText }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.announcement-list {
  width: 100%;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.announcement-list__empty {
  padding: 48px 20px;
  text-align: center;
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 15px;
  background: transparent;
}
</style>
