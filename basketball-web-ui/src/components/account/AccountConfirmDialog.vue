<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const { t } = useI18n()
const confirmBtnRef = ref<HTMLButtonElement | null>(null)
const triggerRef = ref<HTMLElement | null>(null)

watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      await nextTick()
      confirmBtnRef.value?.focus()
    }
  },
)

function close() {
  emit('update:modelValue', false)
  triggerRef.value?.focus()
}

function handleConfirm() {
  emit('confirm')
  close()
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') close()
}

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      document.addEventListener('keydown', handleKeydown)
    } else {
      document.removeEventListener('keydown', handleKeydown)
    }
  },
)
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="account-confirm">
      <button type="button" class="account-confirm__backdrop" :aria-label="t('accountShell.common.close')" @click="close" />
      <div class="account-confirm__dialog" role="dialog" aria-modal="true" :aria-label="title">
        <h3 class="account-confirm__title">{{ title }}</h3>
        <p class="account-confirm__message">{{ message }}</p>
        <div class="account-confirm__actions">
          <button type="button" class="account-btn-secondary" @click="close">
            {{ cancelText || t('accountShell.common.cancel') }}
          </button>
          <button ref="confirmBtnRef" type="button" class="account-btn-primary" @click="handleConfirm">
            {{ confirmText || t('accountShell.common.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.account-confirm {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 20px;
}

.account-confirm__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.account-confirm__dialog {
  position: relative;
  z-index: 1;
  width: min(440px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg, #e9e7e7);
  color: var(--account-text);
}

.account-confirm__title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 900;
}

.account-confirm__message {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.6;
  color: #696969;
}

.account-confirm__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
