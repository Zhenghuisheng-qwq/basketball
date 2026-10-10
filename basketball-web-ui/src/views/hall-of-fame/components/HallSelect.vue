<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

export interface HallSelectOption {
  value: string | number
  label: string
  subLabel?: string
}

const props = withDefaults(
  defineProps<{
    modelValue: string | number
    options: HallSelectOption[]
    placeholder?: string
    triggerVariant?: 'dark' | 'week'
    disabled?: boolean
    width?: 'default' | 'filter'
  }>(),
  {
    placeholder: '',
    triggerVariant: 'dark',
    disabled: false,
    width: 'default',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()

const { t } = useI18n()
const placeholderText = computed(() => props.placeholder || t('publicPages.hall.pleaseSelect'))
const open = ref(false)
const rootRef = ref<HTMLElement | null>(null)

const selectedOption = computed(
  () => props.options.find((item) => item.value === props.modelValue) ?? props.options[0],
)

function toggleOpen() {
  if (props.disabled) return
  open.value = !open.value
}

function close() {
  open.value = false
}

function selectOption(value: string | number) {
  emit('update:modelValue', value)
  close()
}

function handleDocumentPointerDown(event: PointerEvent) {
  if (!open.value) return
  const target = event.target as Node | null
  if (target && rootRef.value?.contains(target)) return
  close()
}

function handleDocumentKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape' && open.value) {
    event.preventDefault()
    close()
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown)
  document.addEventListener('keydown', handleDocumentKeyDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
  document.removeEventListener('keydown', handleDocumentKeyDown)
})

async function handleTriggerClick() {
  toggleOpen()
  if (open.value) {
    await nextTick()
    rootRef.value?.querySelector<HTMLElement>('.hall-select__option.is-active')?.focus()
  }
}
</script>

<template>
  <div
    ref="rootRef"
    class="hall-select"
    :class="[`hall-select--${triggerVariant}`, `hall-select--${width}`]"
  >
    <button
      type="button"
      class="hall-select__trigger"
      :disabled="disabled"
      :aria-expanded="open"
      @click="handleTriggerClick"
    >
      <span class="hall-select__label">{{ selectedOption?.label ?? placeholderText }}</span>
      <span class="hall-select__arrow" aria-hidden="true">▾</span>
    </button>

    <div v-if="open" class="hall-select__panel" role="listbox">
      <button
        v-for="option in options"
        :key="String(option.value)"
        type="button"
        class="hall-select__option"
        :class="{ 'is-active': option.value === modelValue }"
        role="option"
        :aria-selected="option.value === modelValue"
        @click="selectOption(option.value)"
      >
        <span>{{ option.label }}</span>
        <span v-if="option.subLabel" class="hall-select__sub">{{ option.subLabel }}</span>
        <span v-if="option.value === modelValue" class="hall-select__check">✓</span>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.hall-select {
  position: relative;
  min-width: 180px;
}

.hall-select--filter {
  min-width: 150px;
  width: 160px;
}

.hall-select__trigger {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #101010;
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.hall-select--week .hall-select__trigger {
  border-color: rgba(231, 25, 34, 0.55);
  background: linear-gradient(180deg, #2a0a0d 0%, #120608 100%);
}

.hall-select__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
}

.hall-select__arrow {
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  flex-shrink: 0;
}

.hall-select__panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 1200;
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: #101010;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.45);
}

.hall-select__option {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 8px;
  padding: 10px 14px;
  border: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background: transparent;
  color: #fff;
  text-align: left;
  font: inherit;
  cursor: pointer;

  &:hover,
  &.is-active {
    background: rgba(231, 25, 34, 0.12);
  }
}

.hall-select__sub {
  grid-column: 1 / -1;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
}

.hall-select__check {
  color: var(--hall-red, #e71922);
  font-weight: 800;
}

@media (max-width: 767px) {
  .hall-select,
  .hall-select--filter {
    width: 100%;
    min-width: 0;
  }
}
</style>
