<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  currentPage: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  'update:currentPage': [value: number]
  'update:pageSize': [value: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const rangeText = computed(() => {
  if (props.total === 0) {
    return '显示第 0 - 0 条，共 0 条'
  }
  const start = (props.currentPage - 1) * props.pageSize + 1
  const end = Math.min(props.currentPage * props.pageSize, props.total)
  return `显示第 ${start} - ${end} 条，共 ${props.total} 条`
})

const pageNumbers = computed(() => {
  const pages: number[] = []
  const total = totalPages.value
  const current = props.currentPage
  let start = Math.max(1, current - 2)
  let end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let page = start; page <= end; page += 1) {
    pages.push(page)
  }
  return pages
})

function goTo(page: number) {
  if (page < 1 || page > totalPages.value || page === props.currentPage) return
  emit('update:currentPage', page)
}
</script>
<template>
  <div class="data-pagination">
    <p class="data-pagination__text">{{ rangeText }}</p>
    <div class="data-pagination__controls">
      <button
        type="button"
        class="data-pagination__btn"
        :disabled="currentPage <= 1"
        aria-label="第一页"
        @click="goTo(1)"
      >
        «
      </button>
      <button
        type="button"
        class="data-pagination__btn"
        :disabled="currentPage <= 1"
        aria-label="上一页"
        @click="goTo(currentPage - 1)"
      >
        ‹
      </button>
      <button
        v-for="page in pageNumbers"
        :key="page"
        type="button"
        class="data-pagination__btn"
        :class="{ 'is-active': page === currentPage }"
        :aria-current="page === currentPage ? 'page' : undefined"
        @click="goTo(page)"
      >
        {{ page }}
      </button>
      <button
        type="button"
        class="data-pagination__btn"
        :disabled="currentPage >= totalPages"
        aria-label="下一页"
        @click="goTo(currentPage + 1)"
      >
        ›
      </button>
      <button
        type="button"
        class="data-pagination__btn"
        :disabled="currentPage >= totalPages"
        aria-label="最后一页"
        @click="goTo(totalPages)"
      >
        »
      </button>
    </div>
  </div>
</template>
<style scoped lang="scss">
.data-pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 22px 0 32px;
}

.data-pagination__text {
  margin: 0;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  line-height: 1;
}

.data-pagination__controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.data-pagination__btn {
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 3px;
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text-secondary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition:
    background-color 0.14s ease,
    color 0.14s ease,
    border-color 0.14s ease,
    opacity 0.14s ease;

  &:hover:not(:disabled):not(.is-active) {
    border-color: rgba(25, 25, 25, 0.22);
    background: #ebe9e9;
  }

  &.is-active {
    border-color: #ff1717;
    background: #ff1717;
    color: #fff;
  }

  &:disabled {
    opacity: 0.42;
    cursor: not-allowed;
  }
}
</style>