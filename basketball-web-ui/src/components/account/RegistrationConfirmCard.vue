<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  disabled?: boolean
  submitLabel?: string
}>()

const emit = defineEmits<{
  submit: []
}>()

const confirmData = ref(false)
const confirmRules = ref(false)

const canSubmit = computed(() => confirmData.value && confirmRules.value && !props.disabled)

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit')
}
</script>

<template>
  <div class="registration-confirm-card">
    <label class="registration-confirm-card__item">
      <input v-model="confirmData" type="checkbox" :disabled="disabled" />
      <span>确认使用以上认证资料</span>
    </label>
    <label class="registration-confirm-card__item">
      <input v-model="confirmRules" type="checkbox" :disabled="disabled" />
      <span>同意赛事规则与管理员审核</span>
    </label>
    <button
      type="button"
      class="account-btn-primary"
      :disabled="!canSubmit"
      @click="handleSubmit"
    >
      {{ submitLabel || '提交个人报名' }}
    </button>
    <p class="registration-confirm-card__hint">提交后进入管理员审核</p>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
