<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerMember, sendEmailCode } from '@/api/memberAuth'
import PasswordInput from './components/PasswordInput.vue'

const router = useRouter()
const { t } = useI18n()

function label(value: string): string {
  return value.startsWith('auth.') ? t(value) : value
}

const form = reactive({
  email: '',
  verifyCode: '',
  password: '',
  confirmPassword: '',
  agreed: false,
})

const errors = reactive({
  email: '',
  verifyCode: '',
  password: '',
  confirmPassword: '',
  agreed: '',
  form: '',
})

const submitted = ref(false)
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const verifyBizNo = ref('')
let timer: ReturnType<typeof setInterval> | null = null

const passwordRules = computed(() => {
  const value = form.password
  return {
    length: value.length >= 8,
    letter: /[A-Za-z]/.test(value),
    number: /\d/.test(value),
  }
})

function validateEmail(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return 'auth.emailAddressRequired'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return 'auth.emailInvalid'
  return ''
}

function validateVerifyCode(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return 'auth.codeRequired'
  if (!/^\d{6}$/.test(trimmed)) return 'auth.codeDigits'
  return ''
}

function validatePassword(value: string): string {
  if (!value) return 'auth.passwordPrompt'
  if (value.length < 8 || value.length > 20) return 'auth.passwordLengthRegister'
  if (!/[A-Za-z]/.test(value)) return 'auth.passwordNeedLetter'
  if (!/\d/.test(value)) return 'auth.passwordNeedNumber'
  return ''
}

function validateConfirmPassword(value: string): string {
  if (!value) return 'auth.confirmPasswordPrompt'
  if (value !== form.password) return 'auth.passwordMismatch'
  return ''
}

function validateForm(): boolean {
  errors.email = validateEmail(form.email)
  errors.verifyCode = validateVerifyCode(form.verifyCode)
  errors.password = validatePassword(form.password)
  errors.confirmPassword = validateConfirmPassword(form.confirmPassword)
  errors.agreed = form.agreed ? '' : 'auth.agreeRequired'
  return !errors.email && !errors.verifyCode && !errors.password && !errors.confirmPassword && !errors.agreed
}

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

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function handleSendCode() {
  errors.email = validateEmail(form.email)
  errors.form = ''
  if (errors.email || sending.value || countdown.value > 0) return

  sending.value = true
  try {
    const email = form.email.trim().toLowerCase()
    const res = await sendEmailCode(email, 'REGISTER')
    verifyBizNo.value = res.data.bizNo
    ElMessage.success(t('auth.codeSent'))
    startCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    errors.form = error instanceof Error && error.message ? error.message : 'auth.codeSendFailed'
  } finally {
    sending.value = false
  }
}

async function handleSubmit() {
  submitted.value = true
  if (!validateForm() || loading.value) return
  if (!verifyBizNo.value) {
    errors.form = 'auth.sendCodeFirst'
    return
  }

  loading.value = true
  errors.form = ''
  try {
    const email = form.email.trim().toLowerCase()
    await registerMember({
      email,
      password: form.password,
      confirmPassword: form.confirmPassword,
      terminalType: 'WEB',
      verifyBizNo: verifyBizNo.value,
      verifyCode: form.verifyCode.trim(),
      agreeUserAgreement: true,
      agreePrivacyPolicy: true,
    })
    ElMessage.success(t('auth.registerSuccessLogin'))
    await router.push({
      path: '/register/success',
      query: { email },
    })
  } catch (error) {
    errors.form = error instanceof Error && error.message ? error.message : 'auth.registerFailed'
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <div class="register-page__head">
      <h2 class="register-page__title">{{ t('auth.createAccount') }}</h2>
      <span class="register-page__sub">Create Account</span>
    </div>

    <form class="register-form" @submit.prevent="handleSubmit">
      <label class="form-field">
        <span class="form-field__label">{{ t('auth.email') }}</span>
        <input
          v-model="form.email"
          type="email"
          class="form-field__input"
          :placeholder="t('auth.emailCommonPlaceholder')"
          autocomplete="email"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="form-field__error">{{ label(errors.email) }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">{{ t('auth.emailCode') }}</span>
        <div class="register-form__code-row">
          <input
            v-model="form.verifyCode"
            type="text"
            inputmode="numeric"
            maxlength="6"
            class="form-field__input"
            :placeholder="t('auth.codePlaceholder')"
            autocomplete="one-time-code"
            @blur="errors.verifyCode = validateVerifyCode(form.verifyCode)"
          />
          <button
            type="button"
            class="register-form__code-btn"
            :disabled="sending || countdown > 0"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : sending ? t('auth.sending') : t('auth.sendCode') }}
          </button>
        </div>
        <span v-if="submitted && errors.verifyCode" class="form-field__error">{{ label(errors.verifyCode) }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">{{ t('auth.setPassword') }}</span>
        <PasswordInput
          v-model="form.password"
          :placeholder="t('auth.passwordHint')"
          autocomplete="new-password"
          @blur="errors.password = validatePassword(form.password)"
        />
        <span v-if="submitted && errors.password" class="form-field__error">{{ label(errors.password) }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">{{ t('auth.confirmPassword') }}</span>
        <PasswordInput
          v-model="form.confirmPassword"
          :placeholder="t('auth.confirmPasswordPrompt')"
          autocomplete="new-password"
          @blur="errors.confirmPassword = validateConfirmPassword(form.confirmPassword)"
        />
        <span v-if="submitted && errors.confirmPassword" class="form-field__error">{{ label(errors.confirmPassword) }}</span>
      </label>

      <ul class="password-rules">
        <li :class="{ 'is-valid': passwordRules.length }">{{ t('auth.ruleLength') }}</li>
        <li :class="{ 'is-valid': passwordRules.letter }">{{ t('auth.ruleLetter') }}</li>
        <li :class="{ 'is-valid': passwordRules.number }">{{ t('auth.ruleNumber') }}</li>
      </ul>

      <label class="form-agreement">
        <input v-model="form.agreed" type="checkbox" />
        <span>
          {{ t('auth.agreeLead') }}
          <router-link to="/legal/user-agreement">{{ t('auth.userAgreement') }}</router-link>
          {{ t('auth.agreeAnd') }}
          <router-link to="/legal/privacy">{{ t('auth.privacyPolicy') }}</router-link>
        </span>
      </label>
      <span v-if="submitted && errors.agreed" class="form-field__error">{{ label(errors.agreed) }}</span>
      <p v-if="errors.form" class="form-field__error">{{ label(errors.form) }}</p>

      <button type="submit" class="btn-primary" :disabled="loading">
        {{ loading ? t('auth.registering') : t('auth.registerCta') }}
      </button>

      <p class="register-form__login">
        {{ t('auth.hasAccount') }}
        <button type="button" class="text-link" @click="goLogin">{{ t('auth.loginNow') }}</button>
      </p>
    </form>
  </div>
</template>

<style scoped lang="scss">
@import './styles/account-form.scss';

.form-field :deep(.auth-password-input) {
  width: 100%;
}

.form-field :deep(.auth-input) {
  width: 100%;
}

.register-form__code-row {
  display: flex;
  gap: 10px;
}

.register-form__code-row .form-field__input {
  flex: 1;
}

.register-form__code-btn {
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

.register-form__code-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
