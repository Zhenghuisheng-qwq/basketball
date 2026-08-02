<script setup lang="ts">
defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  search: []
}>()

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}

function onSearch() {
  emit('search')
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    onSearch()
  }
}
</script>

<template>
  <div class="data-search-bar">
    <label class="data-search-bar__field">
      <span class="data-search-bar__icon" aria-hidden="true">⌕</span>
      <input
        :value="modelValue"
        type="search"
        :placeholder="placeholder"
        @input="onInput"
        @keydown="onKeydown"
      />
    </label>
    <button type="button" class="data-search-bar__btn" @click="onSearch">查询</button>
  </div>
</template>

<style scoped lang="scss">
.data-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.data-search-bar__field {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 240px;
  max-width: 420px;
  flex: 1 1 auto;
  height: 42px;
  padding: 0 12px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 4px;
  background: #fff;
}

.data-search-bar__icon {
  color: #999;
  font-size: 16px;
  line-height: 1;
}

.data-search-bar__field input {
  width: 100%;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--cnpa-paper-text);
  font-size: 14px;

  &::placeholder {
    color: #999;
  }
}

.data-search-bar__btn {
  min-width: 72px;
  height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 4px;
  background: #ff1717;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;

  &:hover {
    background: #e01414;
  }
}
</style>
