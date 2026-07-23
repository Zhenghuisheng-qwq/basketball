<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  modelValue: string
  placeholder?: string
  autocomplete?: string
  invalid?: boolean
  id?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  blur: []
}>()

const visible = ref(false)

function handleInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="auth-password-input" :class="{ 'auth-password-input--invalid': invalid }">
    <input
      :id="id"
      class="auth-input"
      :class="{ 'auth-input--invalid': invalid }"
      :type="visible ? 'text' : 'password'"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      @input="handleInput"
      @blur="emit('blur')"
    />
    <button
      type="button"
      class="auth-password-input__toggle"
      :aria-label="visible ? '隐藏密码' : '显示密码'"
      @click="visible = !visible"
    >
      <svg v-if="visible" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
        <path
          d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
        />
        <circle cx="12" cy="12" r="2.8" fill="none" stroke="currentColor" stroke-width="1.8" />
      </svg>
      <svg v-else viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
        <path
          d="M3 3l18 18M10.6 10.6A3 3 0 0 0 12 15a3 3 0 0 0 2.4-1.2M6.7 6.7C4.6 8.1 3 10 3 10s3.5 6 9 6c1.6 0 3-.4 4.2-1M14.1 14.1C15 13.4 15.6 12.3 15.9 11"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
        />
        <path
          d="M9.9 5.1A10.7 10.7 0 0 1 12 5c5.5 0 9 6 9 6s-1 1.7-3 3.3"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
        />
      </svg>
    </button>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/auth-shared.scss';
</style>
