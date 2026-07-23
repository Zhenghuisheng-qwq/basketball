<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMaskedRegisterEmail } from '@/composables/useRegisterFlow'
import AuthStepper from './components/AuthStepper.vue'

const router = useRouter()

const digits = ref(['', '', '', '', '', ''])
const inputRefs = ref<Array<HTMLInputElement | null>>([])
const countdown = ref(60)
const verifying = ref(false)
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
  if (event.key === 'Enter') {
    event.preventDefault()
    handleVerify()
    return
  }
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

async function handleVerify() {
  if (verifying.value) return
  verifying.value = true
  await new Promise((resolve) => window.setTimeout(resolve, 320))
  verifying.value = false
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
  router.push('/login')
}
</script>

<template>
  <div class="auth-page auth-verify">
    <AuthStepper :current="2" />

    <header class="auth-register__title">
      <h1 class="auth-register__title-cn">邮箱验证</h1>
      <p class="auth-register__title-en">VERIFY EMAIL</p>
    </header>

    <div class="auth-verify__icon" aria-hidden="true">@</div>

    <p class="auth-verify__desc">验证码已发送至</p>
    <p class="auth-verify__email">{{ maskedEmail }}</p>
    <p class="auth-verify__timer">验证码 10 分钟内有效</p>

    <div class="verify-code" @paste="handlePaste">
      <input
        v-for="(_, index) in digits"
        :key="index"
        :ref="(el) => setInputRef(el as HTMLInputElement | null, index)"
        class="verify-code__input"
        type="text"
        inputmode="numeric"
        maxlength="1"
        :value="digits[index]"
        :aria-label="`验证码第 ${index + 1} 位`"
        @input="handleDigitInput(index, $event)"
        @keydown="handleDigitKeydown(index, $event)"
      />
    </div>

    <button
      type="button"
      class="auth-primary-button"
      :disabled="verifying"
      @click="handleVerify"
    >
      {{ verifying ? '验证中...' : '验证邮箱' }}
    </button>

    <div class="auth-verify__actions">
      <button type="button" class="auth-text-link" :disabled="countdown > 0" @click="handleResend">
        {{ countdown > 0 ? `${countdown}s 后重发` : '重新发送验证码' }}
      </button>
      <button type="button" class="auth-text-link" @click="handleChangeEmail">更换邮箱</button>
    </div>

    <p class="auth-form__hint">请检查垃圾邮件或广告邮件文件夹</p>

    <p class="auth-form__footer-link">
      已有账号？
      <button type="button" class="auth-text-link" @click="goLogin">立即登录</button>
    </p>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>