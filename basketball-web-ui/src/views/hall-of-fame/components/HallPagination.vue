<script setup lang="ts">
const props = defineProps<{
  page: number
  totalPages: number
}>()

const emit = defineEmits<{
  'update:page': [value: number]
}>()

function goPage(next: number) {
  if (next < 1 || next > props.totalPages) return
  emit('update:page', next)
}
</script>

<template>
  <div class="hall-pagination">
    <button
      type="button"
      class="hall-pagination__btn"
      :disabled="page <= 1"
      @click="goPage(page - 1)"
    >
      上一页
    </button>

    <button
      v-for="pageNo in totalPages"
      :key="pageNo"
      type="button"
      class="hall-pagination__page"
      :class="{ 'is-active': pageNo === page }"
      @click="goPage(pageNo)"
    >
      {{ pageNo }}
    </button>

    <button
      type="button"
      class="hall-pagination__btn"
      :disabled="page >= totalPages"
      @click="goPage(page + 1)"
    >
      下一页
    </button>
  </div>
</template>

<style scoped lang="scss">
.hall-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 28px;
  padding-bottom: 4px;
}

.hall-pagination__btn,
.hall-pagination__page {
  min-width: 44px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(0, 0, 0, 0.16);
  background: var(--hall-paper, #e9e7e7);
  color: #111;
  font: inherit;
  cursor: pointer;
  transition: border-color 0.18s ease, opacity 0.18s ease, background 0.18s ease;

  &:disabled {
    opacity: 0.42;
    cursor: not-allowed;
  }
}

.hall-pagination__page.is-active {
  background: #111;
  border-color: #111;
  color: #fff;
}
</style>
