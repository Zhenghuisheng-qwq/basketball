<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerMember, sendEmailCode } from '@/api/memberAuth'
import PasswordInput from './components/PasswordInput.vue'

const router = useRouter()

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
  if (!trimmed) return '请输入邮箱地址'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return '邮箱格式不正确'
  return ''
}

function validateVerifyCode(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return '请输入验证码'
  if (!/^\d{6}$/.test(trimmed)) return '验证码为 6 位数字'
  return ''
}

function validatePassword(value: string): string {
  if (!value) return '请输入密码'
  if (value.length < 8 || value.length > 20) return '密码长度为 8 至 20 位'
  if (!/[A-Za-z]/.test(value)) return '密码需包含字母'
  if (!/\d/.test(value)) return '密码需包含数字'
  return ''
}

function validateConfirmPassword(value: string): string {
  if (!value) return '请再次输入密码'
  if (value !== form.password) return '两次输入的密码不一致'
  return ''
}

function validateForm(): boolean {
  errors.email = validateEmail(form.email)
  errors.verifyCode = validateVerifyCode(form.verifyCode)
  errors.password = validatePassword(form.password)
  errors.confirmPassword = validateConfirmPassword(form.confirmPassword)
  errors.agreed = form.agreed ? '' : '请先阅读并同意相关协议'
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
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '验证码发送失败，请稍后重试'
  } finally {
    sending.value = false
  }
}

async function handleSubmit() {
  submitted.value = true
  if (!validateForm() || loading.value) return
  if (!verifyBizNo.value) {
    errors.form = '请先发送验证码'
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
    ElMessage.success('注册成功，请登录')
    await router.push({
      path: '/register/success',
      query: { email },
    })
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '注册失败'
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
      <h2 class="register-page__title">创建账号</h2>
      <span class="register-page__sub">Create Account</span>
    </div>

    <form class="register-form" @submit.prevent="handleSubmit">
      <label class="form-field">
        <span class="form-field__label">邮箱</span>
        <input
          v-model="form.email"
          type="email"
          class="form-field__input"
          placeholder="请输入常用邮箱"
          autocomplete="email"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="form-field__error">{{ errors.email }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">邮箱验证码</span>
        <div class="register-form__code-row">
          <input
            v-model="form.verifyCode"
            type="text"
            inputmode="numeric"
            maxlength="6"
            class="form-field__input"
            placeholder="请输入 6 位验证码"
            autocomplete="one-time-code"
            @blur="errors.verifyCode = validateVerifyCode(form.verifyCode)"
          />
          <button
            type="button"
            class="register-form__code-btn"
            :disabled="sending || countdown > 0"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : sending ? '发送中...' : '发送验证码' }}
          </button>
        </div>
        <span v-if="submitted && errors.verifyCode" class="form-field__error">{{ errors.verifyCode }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">设置密码</span>
        <PasswordInput
          v-model="form.password"
          placeholder="8-20 位字符，包含字母和数字"
          autocomplete="new-password"
          @blur="errors.password = validatePassword(form.password)"
        />
        <span v-if="submitted && errors.password" class="form-field__error">{{ errors.password }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">确认密码</span>
        <PasswordInput
          v-model="form.confirmPassword"
          placeholder="请再次输入密码"
          autocomplete="new-password"
          @blur="errors.confirmPassword = validateConfirmPassword(form.confirmPassword)"
        />
        <span v-if="submitted && errors.confirmPassword" class="form-field__error">{{ errors.confirmPassword }}</span>
      </label>

      <ul class="password-rules">
        <li :class="{ 'is-valid': passwordRules.length }">至少 8 位</li>
        <li :class="{ 'is-valid': passwordRules.letter }">包含字母</li>
        <li :class="{ 'is-valid': passwordRules.number }">包含数字</li>
      </ul>

      <label class="form-agreement">
        <input v-model="form.agreed" type="checkbox" />
        <span>我已阅读并同意《用户协议》和《隐私政策》</span>
      </label>
      <span v-if="submitted && errors.agreed" class="form-field__error">{{ errors.agreed }}</span>
      <p v-if="errors.form" class="form-field__error">{{ errors.form }}</p>

      <button type="submit" class="btn-primary" :disabled="loading">
        {{ loading ? '注册中...' : '注册 CNPA 账号' }}
      </button>

      <p class="register-form__login">
        已有账号？
        <button type="button" class="text-link" @click="goLogin">立即登录</button>
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
