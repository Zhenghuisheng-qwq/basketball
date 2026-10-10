<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAuth } from '@/composables/useAuth'
import { useUserStore } from '@/stores/user'
import { resolveAuthenticatedEntryRedirect, sanitizeRedirect } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const { login } = useAuth()

function label(value: string): string {
  return value.startsWith('auth.') ? t(value) : value
}

const authChecking = ref(true)
const form = reactive({
  email: '',
  password: '',
  remember: true,
})
const errors = reactive({
  email: '',
  password: '',
  form: '',
})
const submitted = ref(false)
const loading = ref(false)

function validateEmail(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return 'auth.registeredEmailPrompt'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return 'auth.emailInvalid'
  return ''
}

function validatePassword(value: string): string {
  if (!value) return 'auth.passwordPrompt'
  if (value.length < 8 || value.length > 20) return 'auth.passwordLength'
  return ''
}

function validateForm(): boolean {
  errors.form = ''
  errors.email = validateEmail(form.email)
  errors.password = validatePassword(form.password)
  return !errors.email && !errors.password
}

async function redirectIfAuthenticated() {
  await userStore.ensureInitialized()
  if (!userStore.isAuthenticated) return false
  await router.replace(resolveAuthenticatedEntryRedirect(route.query.redirect))
  return true
}

async function handleSubmit() {
  submitted.value = true
  if (!validateForm() || loading.value) return
  loading.value = true
  errors.form = ''
  try {
    const result = await login(form.email.trim().toLowerCase(), form.password, form.remember)
    if (!result.success) {
      errors.form = result.message || 'auth.loginFailed'
      if (!result.success) form.password = ''
      return
    }
    ElMessage.success({ message: t('auth.loginSuccess'), duration: 1800 })
    await router.replace(sanitizeRedirect(route.query.redirect))
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}

function handleForgotPassword() {
  router.push({
    path: '/forgot-password',
    query: form.email.trim() ? { email: form.email.trim() } : {},
  })
}

watch(
  () => userStore.isAuthenticated,
  async (authed) => {
    if (!authed) return
    await router.replace(resolveAuthenticatedEntryRedirect(route.query.redirect))
  },
)

onMounted(async () => {
  const redirected = await redirectIfAuthenticated()
  if (redirected) return

  authChecking.value = false
  if (route.query.reason === 'password_changed') {
    ElMessage.success(t('auth.passwordChanged'))
  } else if (route.query.reason === 'email_changed') {
    ElMessage.success(t('auth.emailChanged'))
  }
  const emailQuery = route.query.email
  if (typeof emailQuery === 'string' && emailQuery.trim()) {
    form.email = emailQuery.trim()
  }
})
</script>

<template>
  <div v-if="authChecking" class="auth-page auth-login auth-login--checking" aria-busy="true" :aria-label="t('auth.checking')" />
  <div v-else class="auth-page auth-login">
    <div class="auth-login__eyebrow">
      <span class="auth-login__eyebrow-bar" aria-hidden="true" />
      CNPA PLAYER CENTER
    </div>
    <p class="auth-login__welcome">{{ t('auth.welcomeBack') }}</p>
    <header class="auth-login__header">
      <h1>{{ t('auth.accountLogin') }}</h1>
      <p>ACCOUNT LOGIN</p>
    </header>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div class="auth-field">
        <label class="auth-field__label" for="login-email">{{ t('auth.email') }}</label>
        <input
          id="login-email"
          v-model="form.email"
          type="email"
          class="auth-input"
          :class="{ 'auth-input--invalid': submitted && errors.email }"
          :placeholder="t('auth.registeredEmailPrompt')"
          autocomplete="username"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="auth-field__error">{{ label(errors.email) }}</span>
      </div>

      <div class="auth-field">
        <label class="auth-field__label" for="login-password">{{ t('auth.password') }}</label>
        <PasswordInput
          id="login-password"
          v-model="form.password"
          :placeholder="t('auth.passwordPrompt')"
          autocomplete="current-password"
          :invalid="Boolean(submitted && errors.password)"
          @blur="errors.password = validatePassword(form.password)"
        />
        <span v-if="submitted && errors.password" class="auth-field__error">{{ label(errors.password) }}</span>
      </div>

      <div class="auth-login__meta">
        <label class="auth-login__remember">
          <input v-model="form.remember" type="checkbox" />
          <span>{{ t('auth.remember') }}</span>
        </label>
        <button type="button" class="auth-text-link" @click="handleForgotPassword">{{ t('auth.forgotLink') }}</button>
      </div>

      <p v-if="errors.form" class="auth-form__error-banner">{{ label(errors.form) }}</p>
      <button type="submit" class="auth-primary-button" :disabled="loading">
        {{ loading ? t('auth.loggingIn') : t('auth.loginCta') }}
      </button>
      <p class="auth-form__footer-link">
        {{ t('auth.noAccount') }}
        <button type="button" class="auth-text-link" @click="goRegister">{{ t('auth.registerAccount') }}</button>
      </p>
    </form>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/auth-shared.scss';

.auth-login--checking {
  min-height: 320px;
}
</style>
