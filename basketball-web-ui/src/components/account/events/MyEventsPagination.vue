<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  'update:page': [page: number]
  'update:pageSize': [pageSize: number]
}>()

const pageSizeOptions = [5, 10, 20]

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

function handlePageSizeChange(event: Event) {
  const next = Number((event.target as HTMLSelectElement).value)
  if (!Number.isNaN(next) && next > 0) {
    emit('update:pageSize', next)
  }
}
</script>

<template>
  <nav v-if="total > 0" class="my-events-pagination" aria-label="赛事列表分页">
    <span class="my-events-pagination__meta">共 {{ total }} 条</span>
    <label class="my-events-pagination__size">
      每页
      <select
        class="my-events-pagination__size-select"
        :value="pageSize"
        @change="handlePageSizeChange"
      >
        <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
      </select>
      条
    </label>
    <button
      type="button"
      class="account-btn-secondary my-events-pagination__btn"
      :disabled="page <= 1"
      @click="emit('update:page', page - 1)"
    >
      上一页
    </button>
    <span class="my-events-pagination__current">{{ page }} / {{ totalPages }}</span>
    <button
      type="button"
      class="account-btn-secondary my-events-pagination__btn"
      :disabled="page >= totalPages"
      @click="emit('update:page', page + 1)"
    >
      下一页
    </button>
  </nav>
</template>

<style scoped lang="scss">
.my-events-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.my-events-pagination__meta {
  margin-right: auto;
  font-size: 13px;
  color: #666;
}

.my-events-pagination__size {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.my-events-pagination__size-select {
  min-height: 32px;
  padding: 0 8px;
  border: 1px solid rgba(0, 0, 0, 0.18);
  border-radius: 3px;
  background: #fff;
  font-size: 13px;
}

.my-events-pagination__current {
  min-width: 56px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #333;
}

.my-events-pagination__btn {
  min-height: 32px;
  padding: 0 12px;
  font-size: 13px;
}
</style>
