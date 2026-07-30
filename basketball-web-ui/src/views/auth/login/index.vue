<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAuth } from '@/composables/useAuth'
import { useUserStore } from '@/stores/user'
import { resolveAuthenticatedEntryRedirect, sanitizeRedirect } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { login } = useAuth()

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
  if (!trimmed) return '请输入注册邮箱'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return '邮箱格式不正确'
  return ''
}

function validatePassword(value: string): string {
  if (!value) return '请输入密码'
  if (value.length < 8 || value.length > 20) return '密码长度应为 8 至 20 位'
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
      errors.form = result.message || '登录失败'
      if (result.errorCode === 'INVALID_CREDENTIALS') form.password = ''
      return
    }
    ElMessage.success({ message: '登录成功', duration: 1800 })
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
    ElMessage.success('密码修改成功，请重新登录')
  } else if (route.query.reason === 'email_changed') {
    ElMessage.success('邮箱修改成功，请重新登录')
  }
  const emailQuery = route.query.email
  if (typeof emailQuery === 'string' && emailQuery.trim()) {
    form.email = emailQuery.trim()
  }
})
</script>

<template>
  <div v-if="authChecking" class="auth-page auth-login auth-login--checking" aria-busy="true" aria-label="正在检查登录状态" />
  <div v-else class="auth-page auth-login">
    <div class="auth-login__eyebrow">
      <span class="auth-login__eyebrow-bar" aria-hidden="true" />
      CNPA PLAYER CENTER
    </div>
    <p class="auth-login__welcome">欢迎回来</p>
    <header class="auth-login__header">
      <h1>账号登录</h1>
      <p>ACCOUNT LOGIN</p>
    </header>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div class="auth-field">
        <label class="auth-field__label" for="login-email">邮箱</label>
        <input
          id="login-email"
          v-model="form.email"
          type="email"
          class="auth-input"
          :class="{ 'auth-input--invalid': submitted && errors.email }"
          placeholder="请输入注册邮箱"
          autocomplete="username"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="auth-field__error">{{ errors.email }}</span>
      </div>

      <div class="auth-field">
        <label class="auth-field__label" for="login-password">密码</label>
        <PasswordInput
          id="login-password"
          v-model="form.password"
          placeholder="请输入密码"
          autocomplete="current-password"
          :invalid="Boolean(submitted && errors.password)"
          @blur="errors.password = validatePassword(form.password)"
        />
        <span v-if="submitted && errors.password" class="auth-field__error">{{ errors.password }}</span>
      </div>

      <div class="auth-login__meta">
        <label class="auth-login__remember">
          <input v-model="form.remember" type="checkbox" />
          <span>记住登录状态</span>
        </label>
        <button type="button" class="auth-text-link" @click="handleForgotPassword">忘记密码？</button>
      </div>

      <p v-if="errors.form" class="auth-form__error-banner">{{ errors.form }}</p>
      <button type="submit" class="auth-primary-button" :disabled="loading">
        {{ loading ? '登录中...' : '登录 CNPA' }}
      </button>
      <p class="auth-form__footer-link">
        还没有账号？
        <button type="button" class="auth-text-link" @click="goRegister">注册账号</button>
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
