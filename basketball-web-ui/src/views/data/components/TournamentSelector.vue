<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TournamentGroup, TournamentOption } from '../types'

const props = defineProps<{
  modelValue: string
  groups: TournamentGroup[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()
const open = ref(false)
const keyword = ref('')
const rootRef = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)

const flatOptions = computed(() => props.groups.flatMap((group) => group.tournaments))

const selectedTournament = computed(
  () =>
    flatOptions.value.find((item) => item.id === props.modelValue) ?? flatOptions.value[0],
)

const filteredGroups = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  return props.groups
    .map((group) => {
      const tournaments = group.tournaments.filter((item) => {
        if (!normalized) return true
        return (
          item.name.toLowerCase().includes(normalized) ||
          item.label.toLowerCase().includes(normalized) ||
          item.season.toLowerCase().includes(normalized)
        )
      })
      return { ...group, tournaments }
    })
    .filter((group) => group.tournaments.length > 0)
})

const hasMatches = computed(() => filteredGroups.value.length > 0)

function closeDropdown() {
  open.value = false
  keyword.value = ''
}

function toggleDropdown() {
  if (props.loading || props.error) return
  open.value = !open.value
}

function selectTournament(option: TournamentOption) {
  emit('update:modelValue', option.id)
  closeDropdown()
}

function handleDocumentClick(event: MouseEvent) {
  if (!open.value || !rootRef.value) return
  if (!rootRef.value.contains(event.target as Node)) {
    closeDropdown()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && open.value) {
    closeDropdown()
  }
}

watch(open, async (visible) => {
  if (visible) {
    await nextTick()
    searchInputRef.value?.focus()
  }
})

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div ref="rootRef" class="data-tournament-selector" :class="{ 'is-open': open }">
    <button
      type="button"
      class="data-tournament-selector__trigger"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click.stop="toggleDropdown"
    >
      <span class="data-tournament-selector__label">
        <template v-if="loading">{{ t('publicPages.data.loadingSeasons') }}</template>
        <template v-else-if="error">{{ error }}</template>
        <template v-else>{{ selectedTournament?.label || t('publicPages.data.selectSeason') }}</template>
      </span>
      <span class="data-tournament-selector__arrow" aria-hidden="true" />
    </button>

    <div
      v-show="open"
      class="data-tournament-selector__dropdown"
      role="listbox"
      :aria-label="t('publicPages.data.seasonList')"
    >
      <h3 class="data-tournament-selector__title">{{ t('publicPages.data.selectSeason') }}</h3>

      <label class="data-tournament-selector__search">
        <span class="data-tournament-selector__search-icon" aria-hidden="true">⌕</span>
        <input
          ref="searchInputRef"
          v-model="keyword"
          type="search"
          :placeholder="t('publicPages.data.searchSeason')"
          autocomplete="off"
        />
      </label>

      <div v-if="!hasMatches" class="data-tournament-selector__empty">{{ t('publicPages.data.noMatchingSeason') }}</div>

      <div
        v-for="group in filteredGroups"
        :key="group.season"
        class="data-tournament-selector__group"
      >
        <p class="data-tournament-selector__group-title">{{ group.season }}</p>
        <button
          v-for="item in group.tournaments"
          :key="item.id"
          type="button"
          class="data-tournament-selector__option"
          :class="{ 'is-selected': item.id === modelValue }"
          role="option"
          :aria-selected="item.id === modelValue"
          @click="selectTournament(item)"
        >
          <span>{{ item.name }}</span>
          <span class="data-tournament-selector__check" aria-hidden="true" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.data-tournament-selector {
  position: relative;
  z-index: 40;
  width: min(100%, 254px);
}

.data-tournament-selector__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #4a4a4a;
  border-radius: 6px;
  background: rgba(12, 12, 12, 0.92);
  color: #fff;
  cursor: pointer;
}

.data-tournament-selector__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
}

.data-tournament-selector__arrow {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-right: 1.5px solid #ddd;
  border-bottom: 1.5px solid #ddd;
  transform: rotate(45deg) translateY(-2px);
}

.data-tournament-selector.is-open .data-tournament-selector__arrow {
  transform: rotate(225deg) translateY(-1px);
}

.data-tournament-selector__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 50;
  width: 279px;
  min-height: 280px;
  max-height: min(70vh, 460px);
  overflow-y: auto;
  padding: 14px 12px 12px;
  border: 1px solid #4a4a4a;
  border-radius: 5px;
  background: linear-gradient(180deg, rgba(17, 17, 17, 0.99), rgba(9, 9, 9, 0.99));
  color: #fff;
}

.data-tournament-selector__title {
  margin: 0 0 11px;
  font-size: 15px;
  font-weight: 700;
}

.data-tournament-selector__search {
  position: relative;
  display: block;
  height: 36px;
  margin-bottom: 15px;
}

.data-tournament-selector__search input {
  width: 100%;
  height: 36px;
  padding: 0 14px 0 34px;
  border: 1px solid #3c3c3c;
  border-radius: 4px;
  outline: none;
  background: #0a0a0a;
  color: #eee;
  font-size: 13px;
  box-sizing: border-box;
}

.data-tournament-selector__search-icon {
  position: absolute;
  top: 50%;
  left: 10px;
  color: #aaa;
  transform: translateY(-50%);
  pointer-events: none;
}

.data-tournament-selector__group + .data-tournament-selector__group {
  margin-top: 13px;
}

.data-tournament-selector__group-title {
  margin: 0 0 7px;
  font-size: 12px;
  color: #8c8c8c;
}

.data-tournament-selector__option {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 31px;
  margin: 0 0 3px;
  padding: 0 34px 0 11px;
  border: 0;
  border-radius: 4px;
  background: linear-gradient(90deg, #2d2d2d, #232323);
  color: #eaeaea;
  cursor: pointer;
  text-align: left;
  font-size: 13px;

  &.is-selected {
    background: linear-gradient(90deg, #fb2727, #ed1111);
    color: #fff;
  }
}

.data-tournament-selector__check {
  position: absolute;
  right: 10px;
  top: 50%;
  display: none;
  width: 16px;
  height: 16px;
  border: 1.5px solid #fff;
  border-radius: 50%;
  transform: translateY(-50%);
}

.data-tournament-selector__option.is-selected .data-tournament-selector__check {
  display: block;
}

.data-tournament-selector__empty {
  padding: 28px 8px;
  color: #8c8c8c;
  font-size: 13px;
  text-align: center;
}

@media (max-width: 767px) {
  .data-tournament-selector {
    width: 100%;
  }

  .data-tournament-selector__dropdown {
    right: 0;
    left: 0;
    width: 100%;
  }
}
</style>
