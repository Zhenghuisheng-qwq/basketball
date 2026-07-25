<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { checkUsernameAvailable } from '@/api/memberAuth'
import { setRegisterDraft } from '@/composables/useRegisterFlow'
import RegisterSteps from './components/RegisterSteps.vue'
import PasswordInput from './components/PasswordInput.vue'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  agreed: false,
})

const errors = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  agreed: '',
  form: '',
})

const submitted = ref(false)
const loading = ref(false)

const passwordRules = computed(() => {
  const value = form.password
  return {
    length: value.length >= 8,
    letter: /[A-Za-z]/.test(value),
    number: /\d/.test(value),
  }
})

function validateUsername(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return '请输入登录账号'
  if (!/^[A-Za-z0-9_]{4,20}$/.test(trimmed)) {
    return '登录账号为 4 至 20 位英文字母、数字或下划线'
  }
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
  errors.username = validateUsername(form.username)
  errors.password = validatePassword(form.password)
  errors.confirmPassword = validateConfirmPassword(form.confirmPassword)
  errors.agreed = form.agreed ? '' : '请先阅读并同意相关协议'
  return !errors.username && !errors.password && !errors.confirmPassword && !errors.agreed
}

async function handleSubmit() {
  submitted.value = true
  if (!validateForm() || loading.value) return

  loading.value = true
  errors.form = ''
  try {
    const username = form.username.trim()
    await checkUsernameAvailable(username)
    setRegisterDraft({
      username,
      password: form.password,
      agreeUserAgreement: true,
      agreePrivacyPolicy: true,
    })
    await router.push('/register/verify-email')
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '创建账号失败'
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
    <RegisterSteps :current="1" />

    <div class="register-page__head">
      <h2 class="register-page__title">创建账号</h2>
      <span class="register-page__sub">Create Account</span>
    </div>

    <form class="register-form" @submit.prevent="handleSubmit">
      <label class="form-field">
        <span class="form-field__label">登录账号</span>
        <input
          v-model="form.username"
          type="text"
          class="form-field__input"
          placeholder="4-20 位英文字母、数字或下划线"
          autocomplete="username"
          @blur="errors.username = validateUsername(form.username)"
        />
        <span v-if="submitted && errors.username" class="form-field__error">{{ errors.username }}</span>
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
        {{ loading ? '校验中...' : '创建 CNPA 账号' }}
      </button>

      <p class="register-form__login">
        已有账号？
        <button type="button" class="text-link" @click="goLogin">立即登录</button>
      </p>
      <p class="register-form__hint">下一步将绑定邮箱，用于接收验证码、找回密码和账号安全验证。</p>
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
</style>
