<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AnnouncementCategory } from '../types'

const props = defineProps<{
  modelValue: string
  categories: AnnouncementCategory[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()

const tabs = computed(() => [
  { code: 'ALL', name: t('publicPages.announcement.all') },
  ...props.categories,
])

function selectCategory(code: string) {
  emit('update:modelValue', code)
}
</script>

<template>
  <div class="announcement-category-tabs" role="tablist" :aria-label="t('publicPages.announcement.categoryAria')">
    <button
      v-for="tab in tabs"
      :key="tab.code"
      type="button"
      role="tab"
      class="announcement-category-tabs__btn"
      :class="{ 'is-active': modelValue === tab.code }"
      :aria-selected="modelValue === tab.code"
      @click="selectCategory(tab.code)"
    >
      {{ tab.name }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.announcement-category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1 1 auto;
}

.announcement-category-tabs__btn {
  min-width: 84px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(0, 0, 0, 0.16);
  background: var(--cnpa-paper-surface, #f1efef);
  color: #171717;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.18s ease,
    color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;

  &.is-active {
    background: #111;
    border-color: #111;
    color: #fff;
    box-shadow: inset 0 -3px 0 var(--cnpa-red, #e10613);
  }
}

@media (max-width: 767px) {
  .announcement-category-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .announcement-category-tabs::-webkit-scrollbar {
    display: none;
  }

  .announcement-category-tabs__btn {
    flex: 0 0 auto;
  }
}
</style>
