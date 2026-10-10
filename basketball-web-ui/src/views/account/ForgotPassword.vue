<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { resetMemberPassword, sendEmailCode } from '@/api/memberAuth'
import { validatePasswordRules } from '@/utils/passwordRules'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

function label(value: string): string {
  return value.startsWith('auth.') ? t(value) : value
}

const form = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
})

const errors = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  form: '',
})

const submitted = ref(false)
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const resetBizNo = ref('')
let countdownTimer: ReturnType<typeof setInterval> | null = null

function validateEmail(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return 'auth.emailAddressRequired'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return 'auth.emailInvalid'
  return ''
}

function validateCode(value: string): string {
  if (!value.trim()) return 'auth.codeRequired'
  if (!/^\d{6}$/.test(value.trim())) return 'auth.codeDigits'
  return ''
}

function validateForm(): boolean {
  errors.email = validateEmail(form.email)
  errors.code = validateCode(form.code)
  errors.password = passwordError(form.password)
  if (!form.confirmPassword) errors.confirmPassword = 'auth.confirmNewPasswordRequired'
  else if (form.confirmPassword !== form.password) errors.confirmPassword = 'auth.passwordMismatchShort'
  else errors.confirmPassword = ''
  if (!errors.email && !errors.code && !resetBizNo.value) {
    errors.form = 'auth.getCodeFirst'
    return false
  }
  return !errors.email && !errors.code && !errors.password && !errors.confirmPassword
}

function passwordError(password: string): string {
  if (!validatePasswordRules(password)) return ''
  if (!password) return 'auth.newPasswordPrompt'
  if (password.length < 8 || password.length > 20) return 'auth.passwordLength'
  return 'auth.passwordNeedBoth'
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
      return
    }
    countdown.value -= 1
  }, 1000)
}

async function handleSendCode() {
  errors.email = validateEmail(form.email)
  errors.form = ''
  if (errors.email || sendingCode.value || countdown.value > 0) return
  sendingCode.value = true
  try {
    const res = await sendEmailCode(form.email.trim().toLowerCase(), 'RESET_PASSWORD')
    resetBizNo.value = res.data.bizNo
    ElMessage.success(t('auth.codeSentIfRegistered'))
    startCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    errors.form = error instanceof Error && error.message ? error.message : 'auth.codeSendFailed'
  } finally {
    sendingCode.value = false
  }
}

async function handleSubmit() {
  submitted.value = true
  if (!validateForm() || loading.value) return
  loading.value = true
  errors.form = ''
  try {
    await resetMemberPassword({
      email: form.email.trim().toLowerCase(),
      verifyBizNo: resetBizNo.value,
      verifyCode: form.code.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success(t('auth.resetSuccess'))
    await router.replace({
      path: '/login',
      query: route.query.redirect ? { redirect: route.query.redirect as string } : {},
    })
  } catch (error) {
    errors.form = error instanceof Error && error.message ? error.message : 'auth.resetFailed'
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

const emailQuery = route.query.email
if (typeof emailQuery === 'string' && emailQuery.trim()) {
  form.email = emailQuery.trim()
}
</script>

<template>
  <div class="auth-page auth-login">
    <div class="auth-login__eyebrow">
      <span class="auth-login__eyebrow-bar" aria-hidden="true" />
      CNPA PLAYER CENTER
    </div>
    <p class="auth-login__welcome">{{ t('auth.forgotPassword') }}</p>
    <header class="auth-login__header">
      <h1>{{ t('auth.resetTitle') }}</h1>
      <p>RESET PASSWORD</p>
    </header>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div class="auth-field">
        <label class="auth-field__label" for="reset-email">{{ t('auth.registeredEmail') }}</label>
        <input
          id="reset-email"
          v-model="form.email"
          type="email"
          class="auth-input"
          :class="{ 'auth-input--invalid': submitted && errors.email }"
          :placeholder="t('auth.registeredEmailPrompt')"
          autocomplete="email"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="auth-field__error">{{ label(errors.email) }}</span>
      </div>

      <div class="auth-field">
        <label class="auth-field__label" for="reset-code">{{ t('auth.emailCode') }}</label>
        <div class="auth-login__code-row">
          <input
            id="reset-code"
            v-model="form.code"
            type="text"
            inputmode="numeric"
            maxlength="6"
            class="auth-input"
            :class="{ 'auth-input--invalid': submitted && errors.code }"
            :placeholder="t('auth.codePlaceholder')"
            autocomplete="one-time-code"
          />
          <button type="button" class="auth-login__code-btn" :disabled="sendingCode || countdown > 0" @click="handleSendCode">
            {{ countdown > 0 ? `${countdown}s` : sendingCode ? t('auth.sendingShort') : t('auth.getCode') }}
          </button>
        </div>
        <span v-if="submitted && errors.code" class="auth-field__error">{{ label(errors.code) }}</span>
      </div>

      <div class="auth-field">
        <label class="auth-field__label" for="reset-password">{{ t('auth.newPassword') }}</label>
        <PasswordInput
          id="reset-password"
          v-model="form.password"
          :placeholder="t('auth.newPasswordPrompt')"
          autocomplete="new-password"
          :invalid="Boolean(submitted && errors.password)"
          @blur="errors.password = passwordError(form.password)"
        />
        <span v-if="submitted && errors.password" class="auth-field__error">{{ label(errors.password) }}</span>
      </div>

      <div class="auth-field">
        <label class="auth-field__label" for="reset-confirm">{{ t('auth.confirmNewPassword') }}</label>
        <PasswordInput
          id="reset-confirm"
          v-model="form.confirmPassword"
          :placeholder="t('auth.confirmNewPasswordPlaceholder')"
          autocomplete="new-password"
          :invalid="Boolean(submitted && errors.confirmPassword)"
        />
        <span v-if="submitted && errors.confirmPassword" class="auth-field__error">{{ label(errors.confirmPassword) }}</span>
      </div>

      <p v-if="errors.form" class="auth-form__error-banner">{{ label(errors.form) }}</p>
      <button type="submit" class="auth-primary-button" :disabled="loading">
        {{ loading ? t('auth.submitting') : t('auth.resetAction') }}
      </button>
      <p class="auth-form__footer-link">
        {{ t('auth.remembered') }}
        <button type="button" class="auth-text-link" @click="goLogin">{{ t('auth.backToLogin') }}</button>
      </p>
    </form>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/auth-shared.scss';

.auth-login__code-row {
  display: flex;
  gap: 10px;
}

.auth-login__code-row .auth-input {
  flex: 1;
}

.auth-login__code-btn {
  flex: 0 0 112px;
  height: 44px;
  border: 1px solid rgba(15, 15, 15, 0.35);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.35);
  color: #151515;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
}

.auth-login__code-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
