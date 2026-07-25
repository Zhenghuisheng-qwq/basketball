<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerMember, sendEmailCode } from '@/api/memberAuth'
import {
  clearRegisterDraft,
  getRegisterDraft,
  getRegisterUsername,
  updateRegisterBizNo,
  updateRegisterEmail,
} from '@/composables/useRegisterFlow'
import { useUserStore } from '@/stores/user'
import { useAccountStore } from '@/stores/account'
import AuthStepper from './components/AuthStepper.vue'

const router = useRouter()
const userStore = useUserStore()
const accountStore = useAccountStore()

const form = reactive({
  email: '',
  verifyCode: '',
})

const errors = reactive({
  email: '',
  verifyCode: '',
  form: '',
})

const submitted = ref(false)
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
const verifyBizNo = ref('')
let timer: ReturnType<typeof setInterval> | null = null

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
  if (!draft?.username || !draft.password) {
    router.replace('/register')
    return
  }
  if (draft.email) {
    form.email = draft.email
  }
  if (draft.verifyBizNo) {
    verifyBizNo.value = draft.verifyBizNo
  }
})

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
    updateRegisterEmail(email, res.data.bizNo)
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function handleSubmit() {
  submitted.value = true
  errors.email = validateEmail(form.email)
  errors.verifyCode = validateVerifyCode(form.verifyCode)
  errors.form = ''
  if (errors.email || errors.verifyCode || submitting.value) return
  if (!verifyBizNo.value) {
    errors.form = '请先发送验证码'
    return
  }

  const draft = getRegisterDraft()
  if (!draft?.username || !draft.password) {
    await router.replace('/register')
    return
  }

  submitting.value = true
  try {
    const email = form.email.trim().toLowerCase()
    const res = await registerMember({
      username: draft.username,
      email,
      password: draft.password,
      confirmPassword: draft.password,
      terminalType: 'WEB',
      verifyBizNo: verifyBizNo.value,
      verifyCode: form.verifyCode.trim(),
      agreeUserAgreement: draft.agreeUserAgreement,
      agreePrivacyPolicy: draft.agreePrivacyPolicy,
    })
    userStore.applyLoginResult(res.data, true)
    await accountStore.loadOverview()
    clearRegisterDraft()
    ElMessage.success('注册成功')
    await router.push('/register/success')
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '绑定失败'
  } finally {
    submitting.value = false
  }
}

async function handleResend() {
  if (countdown.value > 0 || sending.value) return
  await handleSendCode()
}

function handleBack() {
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
      <h1 class="auth-register__title-cn">绑定邮箱</h1>
      <p class="auth-register__title-en">BIND EMAIL</p>
    </header>

    <p class="auth-verify__desc">登录账号：<strong>{{ getRegisterUsername() }}</strong></p>
    <p class="auth-verify__hint">验证码将发送至你的邮箱，请完成验证后继续。</p>

    <form class="auth-form auth-verify__form" @submit.prevent="handleSubmit">
      <div class="auth-field">
        <label class="auth-field__label" for="register-email">邮箱地址</label>
        <input
          id="register-email"
          v-model="form.email"
          type="email"
          class="auth-input"
          :class="{ 'auth-input--invalid': submitted && errors.email }"
          placeholder="请输入常用邮箱"
          autocomplete="email"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="auth-field__error">{{ errors.email }}</span>
      </div>

      <div class="auth-field">
        <label class="auth-field__label" for="register-code">邮箱验证码</label>
        <div class="auth-verify__code-row">
          <input
            id="register-code"
            v-model="form.verifyCode"
            type="text"
            inputmode="numeric"
            maxlength="6"
            class="auth-input"
            :class="{ 'auth-input--invalid': submitted && errors.verifyCode }"
            placeholder="请输入 6 位验证码"
            autocomplete="one-time-code"
            @blur="errors.verifyCode = validateVerifyCode(form.verifyCode)"
          />
          <button
            type="button"
            class="auth-verify__code-btn"
            :disabled="sending || countdown > 0"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : sending ? '发送中' : '发送验证码' }}
          </button>
        </div>
        <span v-if="submitted && errors.verifyCode" class="auth-field__error">{{ errors.verifyCode }}</span>
      </div>

      <p v-if="errors.form" class="auth-form__error-banner">{{ errors.form }}</p>

      <button type="submit" class="auth-primary-button" :disabled="submitting">
        {{ submitting ? '绑定中...' : '完成绑定' }}
      </button>

      <div class="auth-verify__actions">
        <button type="button" class="auth-text-link" :disabled="countdown > 0 || sending" @click="handleResend">
          {{ countdown > 0 ? `${countdown}s 后可重发` : '重新发送验证码' }}
        </button>
        <button type="button" class="auth-text-link" @click="handleBack">返回上一步</button>
      </div>

      <p class="auth-form__hint">请检查垃圾邮件或广告邮件文件夹。验证码不会在页面展示。</p>
      <p class="auth-form__footer-link">
        已有账号？
        <button type="button" class="auth-text-link" @click="goLogin">立即登录</button>
      </p>
    </form>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';

.auth-verify__hint {
  margin: 0 0 20px;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.auth-verify__form {
  width: 100%;
}

.auth-verify__code-row {
  display: flex;
  gap: 10px;
}

.auth-verify__code-row .auth-input {
  flex: 1;
}

.auth-verify__code-btn {
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

.auth-verify__code-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
