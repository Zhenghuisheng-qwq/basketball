<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  search: [keyword: string]
}>()

const draft = ref(props.modelValue)

watch(
  () => props.modelValue,
  (value) => {
    draft.value = value
  },
)

function submitSearch() {
  emit('search', draft.value.trim())
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    submitSearch()
  }
}
</script>

<template>
  <div class="announcement-search">
    <label class="announcement-search__field">
      <span class="announcement-search__icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="18" height="18">
          <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2" />
          <path d="M20 20l-4-4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        </svg>
      </span>
      <input
        v-model="draft"
        type="search"
        placeholder="搜索公告标题"
        autocomplete="off"
        @keydown="handleKeydown"
      />
    </label>
    <button type="button" class="announcement-search__btn" @click="submitSearch">搜索</button>
  </div>
</template>

<style scoped lang="scss">
.announcement-search {
  display: flex;
  align-items: stretch;
  width: min(360px, 100%);
  flex: 0 0 auto;
}

.announcement-search__field {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(0, 0, 0, 0.16);
  border-right: 0;
  background: var(--cnpa-paper-surface, #f1efef);

  input {
    width: 100%;
    border: 0;
    background: transparent;
    color: #171717;
    font: inherit;
    outline: none;

    &::placeholder {
      color: rgba(0, 0, 0, 0.42);
    }
  }
}

.announcement-search__icon {
  color: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
}

.announcement-search__btn {
  width: 70px;
  min-width: 70px;
  height: 40px;
  padding: 0;
  border: 1px solid #111;
  background: #111;
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease;

  &:hover {
    background: var(--cnpa-red, #e10613);
    border-color: var(--cnpa-red, #e10613);
  }
}

@media (max-width: 767px) {
  .announcement-search {
    width: 100%;
  }
}
</style>
