<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMaskedRegisterEmail } from '@/composables/useRegisterFlow'
import RegisterSteps from './components/RegisterSteps.vue'

const router = useRouter()

const digits = ref(['', '', '', '', '', ''])
const inputRefs = ref<Array<HTMLInputElement | null>>([])
const countdown = ref(60)
const maskedEmail = computed(() => getMaskedRegisterEmail())

let timer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0
      if (timer) clearInterval(timer)
      timer = null
      return
    }
    countdown.value -= 1
  }, 1000)
}

startCountdown()

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function setInputRef(el: HTMLInputElement | null, index: number) {
  inputRefs.value[index] = el
}

function handleDigitInput(index: number, event: Event) {
  const target = event.target as HTMLInputElement
  const value = target.value.replace(/\D/g, '').slice(-1)
  digits.value[index] = value
  target.value = value

  if (value && index < digits.value.length - 1) {
    inputRefs.value[index + 1]?.focus()
  }
}

function handleDigitKeydown(index: number, event: KeyboardEvent) {
  if (event.key !== 'Backspace') return
  if (digits.value[index]) {
    digits.value[index] = ''
    return
  }
  if (index > 0) {
    inputRefs.value[index - 1]?.focus()
    digits.value[index - 1] = ''
  }
}

function handlePaste(event: ClipboardEvent) {
  event.preventDefault()
  const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) ?? ''
  pasted.split('').forEach((char, index) => {
    digits.value[index] = char
  })
  inputRefs.value[Math.min(pasted.length, 5)]?.focus()
}

function handleVerify() {
  router.push('/register/success')
}

function handleResend() {
  if (countdown.value > 0) return
  startCountdown()
}

function handleChangeEmail() {
  router.push('/register')
}

function goLogin() {
  router.push('/account')
}
</script>

<template>
  <div class="register-page">
    <RegisterSteps :current="2" />

    <div class="register-page__head">
      <h2 class="register-page__title">邮箱验证</h2>
      <span class="register-page__sub">Verify Email</span>
    </div>

    <div class="verify-panel">
      <div class="verify-panel__icon" aria-hidden="true">
        <span class="verify-panel__icon-envelope">@</span>
      </div>

      <p class="verify-panel__email">验证码已发送至 {{ maskedEmail }}</p>

      <div class="otp-inputs" @paste="handlePaste">
        <input
          v-for="(_, index) in digits"
          :key="index"
          :ref="(el) => setInputRef(el as HTMLInputElement | null, index)"
          class="otp-input"
          type="text"
          inputmode="numeric"
          maxlength="1"
          :value="digits[index]"
          :aria-label="`验证码第 ${index + 1} 位`"
          @input="handleDigitInput(index, $event)"
          @keydown="handleDigitKeydown(index, $event)"
        />
      </div>

      <button type="button" class="btn-primary" @click="handleVerify">验证邮箱</button>

      <div class="verify-panel__actions">
        <button type="button" class="text-link" :disabled="countdown > 0" @click="handleResend">
          {{ countdown > 0 ? `${countdown}s 后重发` : '重新发送验证码' }}
        </button>
        <button type="button" class="text-link" @click="handleChangeEmail">更换邮箱</button>
      </div>

      <p class="register-form__hint">请检查垃圾邮件或广告邮件文件夹</p>

      <p class="register-form__login">
        已有账号？
        <button type="button" class="text-link" @click="goLogin">立即登录</button>
      </p>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import './styles/account-form.scss';

.verify-panel {
  max-width: 420px;
}

.verify-panel__icon {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  margin-bottom: 20px;
  border: 2px solid rgba(17, 17, 17, 0.12);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
}

.verify-panel__icon-envelope {
  font-size: 28px;
  font-weight: 900;
  color: var(--cnpa-red);
}

.verify-panel__email {
  margin: 0 0 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--cnpa-paper-text-muted);
}

.otp-inputs {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 24px;
}

.otp-input {
  width: 100%;
  height: 52px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  border-radius: 2px;
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 22px;
  font-weight: 900;
  text-align: center;
  font-family: inherit;

  &:focus {
    outline: none;
    border-color: var(--cnpa-red);
  }
}

.verify-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;

  .text-link:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}
</style>
