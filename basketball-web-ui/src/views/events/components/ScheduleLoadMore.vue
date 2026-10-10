<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  canLoadMore: boolean
  remainingGroups: number
  visibleCount: number
  total: number
  allLoaded: boolean
}>()

const emit = defineEmits<{
  loadMore: []
}>()
</script>

<template>
  <div class="schedule-load-more">
    <button
      v-if="canLoadMore"
      type="button"
      class="schedule-load-more__btn"
      @click="emit('loadMore')"
    >
      <span class="schedule-load-more__title">{{ t('eventsPage.schedule.loadMore') }}</span>
      <span class="schedule-load-more__meta">{{ t('eventsPage.count.remainingDays', { groups: remainingGroups, visible: visibleCount, total }) }}</span>
    </button>
    <p v-else-if="allLoaded" class="schedule-load-more__done">{{ t('eventsPage.count.allGames', { total }) }}</p>
  </div>
</template>

<style scoped lang="scss">
.schedule-load-more {
  margin-top: 22px;
}

.schedule-load-more__btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  min-height: 52px;
  padding: 12px 16px;
  border: 1px solid #d8d4cf;
  background: #f7f5f2;
  color: #151515;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    border-color: var(--cnpa-red, #e51422);
    color: var(--cnpa-red, #e51422);
    background: #fff;
  }
}

.schedule-load-more__title {
  font-size: 14px;
  font-weight: 800;
}

.schedule-load-more__meta {
  color: inherit;
  font-size: 12px;
  font-weight: 600;
  opacity: 0.78;
}

.schedule-load-more__done {
  margin: 0;
  color: #888;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}
</style>
