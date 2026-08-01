<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  pageSize: number
  total: number
  loading?: boolean
  compact?: boolean
}>()

const emit = defineEmits<{
  'update:page': [page: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const showPagination = computed(() => props.total > props.pageSize)

function changePage(next: number) {
  if (next < 1 || next > totalPages.value || next === props.page) return
  emit('update:page', next)
}
</script>

<template>
  <nav
    v-if="showPagination"
    class="team-list-pagination"
    :class="{ 'team-list-pagination--compact': compact }"
    aria-label="列表分页"
  >
    <span class="team-list-pagination__meta">
      共 {{ total }} 条<span v-if="compact"> · {{ page }}/{{ totalPages }}</span><template v-else>，第 {{ page }} / {{ totalPages }} 页</template>
    </span>
    <div class="team-list-pagination__controls">
      <button
        type="button"
        class="account-btn-secondary"
        :disabled="page <= 1 || loading"
        @click="changePage(page - 1)"
      >
        上一页
      </button>
      <template v-if="!compact">
        <button
          v-for="p in totalPages"
          :key="p"
          type="button"
          class="team-list-pagination__page"
          :class="{ 'team-list-pagination__page--active': p === page }"
          :disabled="loading"
          @click="changePage(p)"
        >
          {{ p }}
        </button>
      </template>
      <button
        type="button"
        class="account-btn-secondary"
        :disabled="page >= totalPages || loading"
        @click="changePage(page + 1)"
      >
        下一页
      </button>
    </div>
  </nav>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.team-list-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;

  &--compact {
    margin-top: 12px;
    flex-direction: column;
    align-items: stretch;
    gap: 10px;

    .team-list-pagination__meta {
      font-size: 12px;
      text-align: center;
    }

    .team-list-pagination__controls {
      justify-content: center;

      .account-btn-secondary {
        min-height: 32px;
        padding: 0 10px;
        font-size: 12px;
      }
    }
  }
}

.team-list-pagination__meta {
  font-size: 13px;
  color: #696969;
}

.team-list-pagination__controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.team-list-pagination__page {
  min-width: 36px;
  height: 36px;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: transparent;
  color: #111;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;

  &--active {
    border-color: var(--account-red, #d90000);
    color: var(--account-red, #d90000);
  }

  &:hover:not(&--active):not(:disabled) {
    border-color: rgba(217, 0, 0, 0.45);
    color: var(--account-red, #d90000);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

@media (max-width: 767px) {
  .team-list-pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .team-list-pagination__controls {
    justify-content: center;
  }
}
</style>
