<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { CompetitionOption } from '@/views/events/types/competition'
import CompetitionLogo from './CompetitionLogo.vue'

const props = defineProps<{
  competitions: CompetitionOption[]
  currentCompetitionId: string
  mobile?: boolean
}>()

const emit = defineEmits<{
  select: [competition: CompetitionOption]
}>()

const menuRef = ref<HTMLElement | null>(null)
const itemRefs = ref<(HTMLButtonElement | null)[]>([])
const focusIndex = ref(-1)

function setItemRef(index: number, element: HTMLButtonElement | null) {
  itemRefs.value[index] = element
}

function handleSelect(competition: CompetitionOption) {
  if (!competition.enabled) return
  emit('select', competition)
}

function focusItem(index: number) {
  const total = props.competitions.length
  if (total === 0) return

  const nextIndex = ((index % total) + total) % total
  focusIndex.value = nextIndex
  itemRefs.value[nextIndex]?.focus()
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

defineExpose({
  menuRef,
  focusFirstItem,
  moveFocus,
})

watch(
  () => props.competitions,
  () => {
    itemRefs.value = []
    focusIndex.value = -1
  },
)
</script>

<template>
  <div
    ref="menuRef"
    class="competition-dropdown"
    :class="{ 'competition-dropdown--mobile': mobile }"
    role="menu"
    aria-label="赛事选择"
    @keydown.down.prevent="moveFocus(1)"
    @keydown.up.prevent="moveFocus(-1)"
  >
    <button
      v-for="(competition, index) in competitions"
      :key="competition.id"
      :ref="(el) => setItemRef(index, el as HTMLButtonElement | null)"
      type="button"
      class="competition-dropdown__item"
      :class="{
        'competition-dropdown__item--active': competition.id === currentCompetitionId,
        'competition-dropdown__item--disabled': !competition.enabled,
      }"
      role="menuitem"
      :aria-current="competition.id === currentCompetitionId ? 'true' : undefined"
      :disabled="!competition.enabled"
      @click="handleSelect(competition)"
    >
      <CompetitionLogo
        :src="competition.logo"
        :name="competition.name"
        :code="competition.code"
      />
      <span class="competition-dropdown__label">{{ competition.shortName }}</span>
    </button>
  </div>
</template>

<style scoped lang="scss">
.competition-dropdown {
  width: clamp(220px, 16vw, 260px);
  padding: 8px 0 10px;
  background: rgba(52, 52, 52, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-top: none;
  border-radius: 0 0 3px 3px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.36);
  box-sizing: border-box;
}

.competition-dropdown--mobile {
  position: static;
  width: 100%;
  transform: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0;
  box-shadow: none;
}

.competition-dropdown__item {
  position: relative;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  align-items: center;
  column-gap: 10px;
  width: 100%;
  min-height: 44px;
  padding: 6px 20px;
  color: rgba(255, 255, 255, 0.82);
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;
  transition:
    background 0.16s ease,
    color 0.16s ease;

  &:hover:not(:disabled),
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
      background: var(--cnpa-red);
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
      background: var(--cnpa-red);
    }
  }

  &--disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.competition-dropdown__label {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
