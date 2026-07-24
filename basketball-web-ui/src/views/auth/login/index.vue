<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAuth } from '@/composables/useAuth'
import { sendEmailCode } from '@/api/memberAuth'
import { sanitizeRedirect } from '@/utils/auth'

type LoginMode = 'password' | 'emailCode'

const route = useRoute()
const router = useRouter()
const { login, loginWithEmailCode } = useAuth()

const mode = ref<LoginMode>('password')
const form = reactive({
  account: '',
  email: '',
  password: '',
  code: '',
  remember: true,
})
const errors = reactive({
  account: '',
  email: '',
  password: '',
  code: '',
  form: '',
})
const submitted = ref(false)
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const loginBizNo = ref('')
let countdownTimer: ReturnType<typeof setInterval> | null = null

function validateAccount(value: string): string {
  if (!value.trim()) return '请输入邮箱 / 手机号 / 账号编号'
  return ''
}

function validateEmail(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return '请输入邮箱地址'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return '邮箱格式不正确'
  return ''
}

function validatePassword(value: string): string {
  if (!value) return '请输入登录密码'
  if (value.length < 8 || value.length > 20) return '密码长度应为 8 至 20 位'
  return ''
}

function validateCode(value: string): string {
  if (!value.trim()) return '请输入验证码'
  if (!/^\d{6}$/.test(value.trim())) return '验证码为 6 位数字'
  return ''
}

function validateForm(): boolean {
  errors.form = ''
  if (mode.value === 'password') {
    errors.account = validateAccount(form.account)
    errors.password = validatePassword(form.password)
    errors.email = ''
    errors.code = ''
    return !errors.account && !errors.password
  }
  errors.email = validateEmail(form.email)
  errors.code = validateCode(form.code)
  errors.account = ''
  errors.password = ''
  if (!errors.email && !errors.code && !loginBizNo.value) {
    errors.form = '请先获取邮箱验证码'
    return false
  }
  return !errors.email && !errors.code
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
    const res = await sendEmailCode(form.email.trim(), 'LOGIN')
    loginBizNo.value = res.data.bizNo
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '验证码发送失败'
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
    const result = mode.value === 'password'
      ? await login(form.account.trim(), form.password, form.remember)
      : await loginWithEmailCode(form.email.trim(), loginBizNo.value, form.code.trim(), form.remember)

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

function switchMode(next: LoginMode) {
  mode.value = next
  errors.form = ''
  submitted.value = false
}

function goRegister() {
  router.push('/register')
}

function handleForgotPassword() {
  ElMessage.info('密码找回功能将在后续开放')
}

onMounted(() => {
  const emailQuery = route.query.email
  if (typeof emailQuery === 'string' && emailQuery.trim()) {
    form.account = emailQuery.trim()
    form.email = emailQuery.trim()
  }
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<template>
  <div class="auth-page auth-login">
    <div class="auth-login__eyebrow">
      <span class="auth-login__eyebrow-bar" aria-hidden="true" />
      CNPA MEMBER SYSTEM
    </div>
    <p class="auth-login__welcome">欢迎回来</p>
    <header class="auth-login__header">
      <h1>账号登录</h1>
      <p>ACCOUNT LOGIN</p>
    </header>

    <div class="auth-login__tabs" role="tablist">
      <button type="button" class="auth-login__tab" :class="{ 'auth-login__tab--active': mode === 'password' }" @click="switchMode('password')">密码登录</button>
      <button type="button" class="auth-login__tab" :class="{ 'auth-login__tab--active': mode === 'emailCode' }" @click="switchMode('emailCode')">邮箱验证码登录</button>
    </div>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div v-if="mode === 'password'" class="auth-field">
        <label class="auth-field__label" for="login-account">账号</label>
        <input
          id="login-account"
          v-model="form.account"
          type="text"
          class="auth-input"
          :class="{ 'auth-input--invalid': submitted && errors.account }"
          placeholder="邮箱 / 手机号 / 账号编号"
          autocomplete="username"
          @blur="errors.account = validateAccount(form.account)"
        />
        <span v-if="submitted && errors.account" class="auth-field__error">{{ errors.account }}</span>
      </div>

      <div v-else class="auth-field">
        <label class="auth-field__label" for="login-email">邮箱地址</label>
        <input
          id="login-email"
          v-model="form.email"
          type="email"
          class="auth-input"
          :class="{ 'auth-input--invalid': submitted && errors.email }"
          placeholder="请输入注册邮箱"
          autocomplete="email"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="auth-field__error">{{ errors.email }}</span>
      </div>

      <div v-if="mode === 'password'" class="auth-field">
        <label class="auth-field__label" for="login-password">登录密码</label>
        <PasswordInput
          id="login-password"
          v-model="form.password"
          placeholder="请输入登录密码"
          autocomplete="current-password"
          :invalid="Boolean(submitted && errors.password)"
          @blur="errors.password = validatePassword(form.password)"
        />
        <span v-if="submitted && errors.password" class="auth-field__error">{{ errors.password }}</span>
      </div>

      <div v-else class="auth-field">
        <label class="auth-field__label" for="login-code">邮箱验证码</label>
        <div class="auth-login__code-row">
          <input
            id="login-code"
            v-model="form.code"
            type="text"
            inputmode="numeric"
            maxlength="6"
            class="auth-input"
            :class="{ 'auth-input--invalid': submitted && errors.code }"
            placeholder="请输入 6 位验证码"
            autocomplete="one-time-code"
          />
          <button type="button" class="auth-login__code-btn" :disabled="sendingCode || countdown > 0" @click="handleSendCode">
            {{ countdown > 0 ? `${countdown}s` : sendingCode ? '发送中' : '获取验证码' }}
          </button>
        </div>
        <span v-if="submitted && errors.code" class="auth-field__error">{{ errors.code }}</span>
      </div>

      <div class="auth-login__meta">
        <label class="auth-login__remember">
          <input v-model="form.remember" type="checkbox" />
          <span>记住登录状态</span>
        </label>
        <button v-if="mode === 'password'" type="button" class="auth-text-link" @click="handleForgotPassword">忘记密码？</button>
      </div>

      <p v-if="errors.form" class="auth-form__error-banner">{{ errors.form }}</p>
      <button type="submit" class="auth-primary-button" :disabled="loading">
        {{ loading ? '登录中...' : '登录 CNPA' }}
      </button>
      <p class="auth-form__footer-link">
        还没有账号？
        <button type="button" class="auth-text-link" @click="goRegister">立即注册</button>
      </p>
      <p class="auth-form__hint">短信验证码登录暂未开放，请使用密码或邮箱验证码登录</p>
    </form>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/auth-shared.scss';

.auth-login__tabs {
  display: flex;
  gap: 8px;
  margin: 0 0 20px;
}

.auth-login__tab {
  flex: 1;
  height: 40px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(0, 0, 0, 0.18);
  color: rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.auth-login__tab--active {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.12);
}

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
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.auth-login__code-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
