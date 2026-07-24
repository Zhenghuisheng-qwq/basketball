<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerMember, sendEmailCode } from '@/api/memberAuth'
import {
  clearRegisterDraft,
  getMaskedRegisterEmail,
  getRegisterDraft,
  updateRegisterBizNo,
} from '@/composables/useRegisterFlow'
import { useUserStore } from '@/stores/user'
import { useAccountStore } from '@/stores/account'
import AuthStepper from './components/AuthStepper.vue'

const router = useRouter()
const userStore = useUserStore()
const accountStore = useAccountStore()

const digits = ref(['', '', '', '', '', ''])
const inputRefs = ref<Array<HTMLInputElement | null>>([])
const countdown = ref(60)
const verifying = ref(false)
const sending = ref(false)
const formError = ref('')
const maskedEmail = computed(() => getMaskedRegisterEmail())
let timer: ReturnType<typeof setInterval> | null = null

function startCountdown(seconds = 60) {
  countdown.value = seconds
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

onMounted(() => {
  const draft = getRegisterDraft()
  if (!draft?.email || !draft.password || !draft.verifyBizNo) {
    router.replace('/register')
    return
  }
  startCountdown()
})

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
  const draft = getRegisterDraft()
  const code = digits.value.join('')
  if (!draft) {
    await router.replace('/register')
    return
  }
  if (!/^\d{6}$/.test(code)) {
    formError.value = '请输入 6 位验证码'
    return
  }

  verifying.value = true
  formError.value = ''
  try {
    const res = await registerMember({
      email: draft.email,
      password: draft.password,
      confirmPassword: draft.password,
      nickname: draft.nickname,
      mobile: draft.mobile,
      terminalType: 'WEB',
      verifyBizNo: draft.verifyBizNo,
      verifyCode: code,
      agreeUserAgreement: draft.agreeUserAgreement,
      agreePrivacyPolicy: draft.agreePrivacyPolicy,
    })
    userStore.applyLoginResult(res.data, true)
    await accountStore.loadOverview()
    clearRegisterDraft()
    ElMessage.success('注册成功')
    await router.push('/register/success')
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '验证失败'
  } finally {
    verifying.value = false
  }
}

async function handleResend() {
  if (countdown.value > 0 || sending.value) return
  const draft = getRegisterDraft()
  if (!draft?.email) {
    await router.replace('/register')
    return
  }
  sending.value = true
  formError.value = ''
  try {
    const res = await sendEmailCode(draft.email, 'REGISTER')
    updateRegisterBizNo(res.data.bizNo)
    ElMessage.success('验证码已重新发送')
    startCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '发送失败'
  } finally {
    sending.value = false
  }
}

function handleChangeEmail() {
  clearRegisterDraft()
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
    <p class="auth-verify__timer">验证码 5 分钟内有效</p>

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

    <p v-if="formError" class="auth-form__error-banner">{{ formError }}</p>
    <button type="button" class="auth-primary-button" :disabled="verifying" @click="handleVerify">
      {{ verifying ? '验证中...' : '验证邮箱' }}
    </button>
    <div class="auth-verify__actions">
      <button type="button" class="auth-text-link" :disabled="countdown > 0 || sending" @click="handleResend">
        {{ countdown > 0 ? `${countdown}s 后重发` : sending ? '发送中...' : '重新发送验证码' }}
      </button>
      <button type="button" class="auth-text-link" @click="handleChangeEmail">更换邮箱</button>
    </div>
    <p class="auth-form__hint">请检查垃圾邮件或广告邮件文件夹。验证码不会在页面展示。</p>
    <p class="auth-form__footer-link">
      已有账号？
      <button type="button" class="auth-text-link" @click="goLogin">立即登录</button>
    </p>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>
