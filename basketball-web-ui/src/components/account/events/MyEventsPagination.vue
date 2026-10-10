<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  page: number
  pageSize: number
  total: number
  metaText?: string
  pageSizeOptions?: number[]
  disabled?: boolean
  ariaLabel?: string
}>(), {
  pageSizeOptions: () => [5, 10, 20],
  disabled: false,
  ariaLabel: '',
})

const { t } = useI18n()

const emit = defineEmits<{
  'update:page': [page: number]
  'update:pageSize': [pageSize: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const resolvedAriaLabel = computed(() => props.ariaLabel || t('accountEvents.pagination.ariaList'))
const resolvedMeta = computed(() => props.metaText || t('accountEvents.pagination.total', { total: props.total }))

function handlePageSizeChange(event: Event) {
  if (props.disabled) return
  const next = Number((event.target as HTMLSelectElement).value)
  if (!Number.isNaN(next) && next > 0) {
    emit('update:pageSize', next)
  }
}
</script>

<template>
  <nav v-if="total > 0" class="my-events-pagination" :aria-label="resolvedAriaLabel">
    <span class="my-events-pagination__meta">{{ resolvedMeta }}</span>
    <label class="my-events-pagination__size">
      {{ t('accountEvents.pagination.perPage') }}
      <select
        class="my-events-pagination__size-select"
        :value="pageSize"
        :disabled="disabled"
        @change="handlePageSizeChange"
      >
        <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
      </select>
      {{ t('accountEvents.pagination.items') }}
    </label>
    <button
      type="button"
      class="account-btn-secondary my-events-pagination__btn"
      :disabled="disabled || page <= 1"
      @click="emit('update:page', page - 1)"
    >
      {{ t('accountEvents.pagination.prev') }}
    </button>
    <span class="my-events-pagination__current">{{ page }} / {{ totalPages }}</span>
    <button
      type="button"
      class="account-btn-secondary my-events-pagination__btn"
      :disabled="disabled || page >= totalPages"
      @click="emit('update:page', page + 1)"
    >
      {{ t('accountEvents.pagination.next') }}
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
