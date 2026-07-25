<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { resetMemberPassword, sendEmailCode } from '@/api/memberAuth'
import { validatePasswordRules } from '@/utils/passwordRules'

const route = useRoute()
const router = useRouter()

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
  if (!trimmed) return '请输入邮箱地址'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return '邮箱格式不正确'
  return ''
}

function validateCode(value: string): string {
  if (!value.trim()) return '请输入验证码'
  if (!/^\d{6}$/.test(value.trim())) return '验证码为 6 位数字'
  return ''
}

function validateForm(): boolean {
  errors.email = validateEmail(form.email)
  errors.code = validateCode(form.code)
  const ruleError = validatePasswordRules(form.password)
  errors.password = ruleError || (!form.password ? '请输入新密码' : '')
  if (!form.confirmPassword) errors.confirmPassword = '请确认新密码'
  else if (form.confirmPassword !== form.password) errors.confirmPassword = '两次密码输入不一致'
  else errors.confirmPassword = ''
  if (!errors.email && !errors.code && !resetBizNo.value) {
    errors.form = '请先获取邮箱验证码'
    return false
  }
  return !errors.email && !errors.code && !errors.password && !errors.confirmPassword
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
    const res = await sendEmailCode(form.email.trim(), 'RESET_PASSWORD')
    resetBizNo.value = res.data.bizNo
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
    await resetMemberPassword({
      email: form.email.trim(),
      verifyBizNo: resetBizNo.value,
      verifyCode: form.code.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    await router.replace({
      path: '/login',
      query: route.query.redirect ? { redirect: route.query.redirect as string } : {},
    })
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '重置失败'
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
      CNPA MEMBER SYSTEM
    </div>
    <p class="auth-login__welcome">找回密码</p>
    <header class="auth-login__header">
      <h1>重置登录密码</h1>
      <p>RESET PASSWORD</p>
    </header>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div class="auth-field">
        <label class="auth-field__label" for="reset-email">注册邮箱</label>
        <input
          id="reset-email"
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

      <div class="auth-field">
        <label class="auth-field__label" for="reset-code">邮箱验证码</label>
        <div class="auth-login__code-row">
          <input
            id="reset-code"
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

      <div class="auth-field">
        <label class="auth-field__label" for="reset-password">新密码</label>
        <PasswordInput
          id="reset-password"
          v-model="form.password"
          placeholder="请输入新密码"
          autocomplete="new-password"
          :invalid="Boolean(submitted && errors.password)"
          @blur="errors.password = validatePasswordRules(form.password) || ''"
        />
        <span v-if="submitted && errors.password" class="auth-field__error">{{ errors.password }}</span>
      </div>

      <div class="auth-field">
        <label class="auth-field__label" for="reset-confirm">确认新密码</label>
        <PasswordInput
          id="reset-confirm"
          v-model="form.confirmPassword"
          placeholder="请再次输入新密码"
          autocomplete="new-password"
          :invalid="Boolean(submitted && errors.confirmPassword)"
        />
        <span v-if="submitted && errors.confirmPassword" class="auth-field__error">{{ errors.confirmPassword }}</span>
      </div>

      <p v-if="errors.form" class="auth-form__error-banner">{{ errors.form }}</p>
      <button type="submit" class="auth-primary-button" :disabled="loading">
        {{ loading ? '提交中...' : '重置密码' }}
      </button>
      <p class="auth-form__footer-link">
        想起密码了？
        <button type="button" class="auth-text-link" @click="goLogin">返回登录</button>
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
