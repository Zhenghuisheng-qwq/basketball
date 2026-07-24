<script setup lang="ts">
import type { AnnouncementListItem } from '../types'
import AnnouncementListItemCard from './AnnouncementListItem.vue'

defineProps<{
  items: AnnouncementListItem[]
  emptyText: string
}>()

const emit = defineEmits<{
  select: [id: string]
}>()
</script>

<template>
  <div class="announcement-list">
    <AnnouncementListItemCard
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
