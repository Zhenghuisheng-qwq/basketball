<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { buildPaginationItems } from '@/utils/announcementNavigate'

const props = defineProps<{
  page: number
  totalPages: number
}>()

const emit = defineEmits<{
  'update:page': [value: number]
}>()

const { t } = useI18n()

const pageItems = computed(() => buildPaginationItems(props.page, props.totalPages))

function goPage(next: number) {
  if (next < 1 || next > props.totalPages || next === props.page) return
  emit('update:page', next)
}
</script>

<template>
  <nav
    v-if="totalPages > 1"
    class="announcement-pagination"
    :aria-label="t('publicPages.announcement.pagination')"
  >
    <button
      type="button"
      class="announcement-pagination__nav"
      :disabled="page <= 1"
      :aria-label="t('publicPages.announcement.prevPage')"
      @click="goPage(page - 1)"
    >
      ‹
    </button>

    <template v-for="(item, index) in pageItems" :key="`${item}-${index}`">
      <span v-if="item === 'ellipsis'" class="announcement-pagination__ellipsis">...</span>
      <button
        v-else
        type="button"
        class="announcement-pagination__page"
        :class="{ 'is-active': item === page }"
        @click="goPage(item)"
      >
        {{ item }}
      </button>
    </template>

    <button
      type="button"
      class="announcement-pagination__nav"
      :disabled="page >= totalPages"
      :aria-label="t('publicPages.announcement.nextPage')"
      @click="goPage(page + 1)"
    >
      ›
    </button>
  </nav>
</template>

<style scoped lang="scss">
.announcement-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 28px;
  padding-bottom: 8px;
}

.announcement-pagination__nav,
.announcement-pagination__page {
  min-width: 40px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: var(--cnpa-paper-bg, #e9e7e7);
  color: #111;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    opacity 0.18s ease;

  &:disabled {
    opacity: 0.42;
    cursor: not-allowed;
  }
}

.announcement-pagination__page.is-active {
  background: var(--cnpa-red, #e10613);
  border-color: var(--cnpa-red, #e10613);
  color: #fff;
}

.announcement-pagination__ellipsis {
  min-width: 24px;
  text-align: center;
  color: var(--cnpa-paper-text-secondary, #666);
}
</style>
