<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { MegaMenuType, PublicMenuEntry } from '@/constants/publicCompetitionMenu'

const props = defineProps<{
  type: MegaMenuType
  ariaLabel: string
  groups: Array<{
    title: string
    entries: PublicMenuEntry[]
  }>
  getEntryHighlight: (entry: PublicMenuEntry) => 'primary' | 'secondary' | null
}>()

const emit = defineEmits<{
  navigate: [entry: PublicMenuEntry]
}>()

const menuRef = ref<HTMLElement | null>(null)
const entryRefs = ref<(HTMLButtonElement | null)[]>([])
const focusIndex = ref(-1)

function setEntryRef(index: number, element: HTMLButtonElement | null) {
  entryRefs.value[index] = element
}

const flatEntries = ref<PublicMenuEntry[]>([])

watch(
  () => props.groups,
  (groups) => {
    flatEntries.value = groups.flatMap((group) => group.entries)
    entryRefs.value = []
    focusIndex.value = -1
  },
  { immediate: true, deep: true },
)

function handleNavigate(entry: PublicMenuEntry) {
  emit('navigate', entry)
}

function focusItem(index: number) {
  const total = flatEntries.value.length
  if (total === 0) return
  const nextIndex = ((index % total) + total) % total
  focusIndex.value = nextIndex
  entryRefs.value[nextIndex]?.focus()
}

function focusFirstItem() {
  nextTick(() => {
    focusItem(0)
  })
}

function moveFocus(delta: number) {
  const base = focusIndex.value >= 0 ? focusIndex.value : 0
  focusItem(base + delta)
}

function getGlobalIndex(groupIndex: number, entryIndex: number): number {
  let offset = 0
  for (let i = 0; i < groupIndex; i += 1) {
    offset += props.groups[i]?.entries.length ?? 0
  }
  return offset + entryIndex
}

defineExpose({
  menuRef,
  focusFirstItem,
  moveFocus,
})
</script>

<template>
  <div
    ref="menuRef"
    class="competition-mega-menu"
    :data-menu-type="type"
    role="dialog"
    :aria-label="ariaLabel"
    @keydown.down.prevent="moveFocus(1)"
    @keydown.up.prevent="moveFocus(-1)"
  >
    <div class="competition-mega-menu__body">
      <section
        v-for="(group, groupIndex) in groups"
        :key="group.title"
        class="menu-section"
      >
        <div class="menu-section__title">{{ group.title }}</div>

        <button
          v-for="(entry, entryIndex) in group.entries"
          :key="entry.id"
          :ref="(el) => setEntryRef(getGlobalIndex(groupIndex, entryIndex), el as HTMLButtonElement | null)"
          type="button"
          class="menu-entry"
          :class="{
            'menu-entry--active': getEntryHighlight(entry) === 'primary',
            'menu-entry--secondary': getEntryHighlight(entry) === 'secondary',
          }"
          @click="handleNavigate(entry)"
        >
          <span class="menu-entry__icon" aria-hidden="true">{{ entry.abbrev }}</span>
          <span class="menu-entry__name">{{ entry.label }}</span>
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.competition-mega-menu {
  --mega-menu-width: 340px;

  width: var(--mega-menu-width);
  max-height: min(620px, calc(100vh - var(--site-header-height) - 24px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: rgba(52, 52, 52, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-top: 4px solid var(--cnpa-red, #e10613);
  border-radius: 0 0 3px 3px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.36);
  box-sizing: border-box;

  /* 赛事：贴近 master CompetitionDropdown 紧凑列表 */
  &[data-menu-type='event'] {
    --mega-menu-width: clamp(220px, 16vw, 260px);
  }
}

.competition-mega-menu__body {
  flex: 1;
  min-height: 0;
  padding: 8px 0 10px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.competition-mega-menu[data-menu-type='data'] .competition-mega-menu__body {
  padding: 14px 12px 16px;
}

.menu-section + .menu-section {
  margin-top: 14px;
}

.menu-section__title {
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.competition-mega-menu[data-menu-type='event'] .menu-section__title {
  display: none;
}

.menu-entry {
  position: relative;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  align-items: center;
  column-gap: 10px;
  width: 100%;
  min-height: 44px;
  margin: 0;
  padding: 6px 20px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.82);
  text-align: left;
  cursor: pointer;
  font-family: inherit;
  transition:
    background 0.16s ease,
    color 0.16s ease;

  &:hover,
  &:focus-visible {
    background: rgba(139, 0, 0, 0.55);
    color: #fff;
    outline: none;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--cnpa-red, #e10613);
    }
  }

  &--active {
    background: rgba(139, 0, 0, 0.72);
    color: #fff;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--cnpa-red, #e10613);
    }
  }

  &--secondary {
    background: rgba(255, 255, 255, 0.05);
    color: rgba(255, 255, 255, 0.92);
  }
}

.competition-mega-menu[data-menu-type='data'] .menu-entry {
  display: flex;
  gap: 12px;
  min-height: 46px;
  padding: 6px 10px 6px 8px;
  border-radius: 4px;

  &:hover,
  &:focus-visible,
  &--active {
    &::before {
      top: 6px;
      bottom: 6px;
      border-radius: 0 2px 2px 0;
    }
  }
}

.menu-entry__icon {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.9);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.menu-entry__name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-entry--active .menu-entry__icon {
  background: rgba(225, 6, 19, 0.28);
}
</style>
