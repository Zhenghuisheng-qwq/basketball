<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendEmailCode } from '@/api/memberAuth'
import { setRegisterDraft } from '@/composables/useRegisterFlow'
import RegisterSteps from './components/RegisterSteps.vue'

const router = useRouter()

const form = reactive({
  email: '',
  nickname: '',
  mobile: '',
  password: '',
  confirmPassword: '',
  agreed: false,
})

const errors = reactive({
  email: '',
  nickname: '',
  mobile: '',
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

function validateEmail(value: string): string {
  if (!value.trim()) return '请输入邮箱'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return '邮箱格式不正确'
  return ''
}

function validateNickname(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return '请输入昵称'
  if (trimmed.length < 2 || trimmed.length > 20) return '昵称长度应为 2 至 20 位'
  return ''
}

function validateMobile(value: string): string {
  if (!value.trim()) return '请输入手机号'
  if (!/^1\d{10}$/.test(value.trim())) return '手机号格式不正确'
  return ''
}

function validatePassword(value: string): string {
  if (!value) return '请输入密码'
  if (value.length < 8) return '密码至少 8 位'
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
  errors.nickname = validateNickname(form.nickname)
  errors.mobile = validateMobile(form.mobile)
  errors.password = validatePassword(form.password)
  errors.confirmPassword = validateConfirmPassword(form.confirmPassword)
  errors.agreed = form.agreed ? '' : '请先阅读并同意相关协议'
  return !errors.email && !errors.nickname && !errors.mobile && !errors.password && !errors.confirmPassword && !errors.agreed
}

async function handleSubmit() {
  submitted.value = true
  if (!validateForm() || loading.value) return

  loading.value = true
  errors.form = ''
  try {
    const email = form.email.trim()
    const res = await sendEmailCode(email, 'REGISTER')
    setRegisterDraft({
      email,
      password: form.password,
      nickname: form.nickname.trim(),
      mobile: form.mobile.trim(),
      verifyBizNo: res.data.bizNo,
      agreeUserAgreement: true,
      agreePrivacyPolicy: true,
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    await router.push('/register/verify-email')
  } catch (error) {
    errors.form = error instanceof Error ? error.message : '验证码发送失败'
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
        <span class="form-field__label">邮箱</span>
        <input v-model="form.email" type="email" class="form-field__input" placeholder="name@example.com" autocomplete="email" @blur="errors.email = validateEmail(form.email)" />
        <span v-if="submitted && errors.email" class="form-field__error">{{ errors.email }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">昵称</span>
        <input v-model="form.nickname" type="text" class="form-field__input" placeholder="2-20 位昵称" maxlength="20" @blur="errors.nickname = validateNickname(form.nickname)" />
        <span v-if="submitted && errors.nickname" class="form-field__error">{{ errors.nickname }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">手机号</span>
        <input v-model="form.mobile" type="tel" class="form-field__input" placeholder="11 位手机号" maxlength="11" @blur="errors.mobile = validateMobile(form.mobile)" />
        <span v-if="submitted && errors.mobile" class="form-field__error">{{ errors.mobile }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">设置密码</span>
        <input v-model="form.password" type="password" class="form-field__input" placeholder="请输入密码" autocomplete="new-password" @blur="errors.password = validatePassword(form.password)" />
        <span v-if="submitted && errors.password" class="form-field__error">{{ errors.password }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">确认密码</span>
        <input v-model="form.confirmPassword" type="password" class="form-field__input" placeholder="请再次输入密码" autocomplete="new-password" @blur="errors.confirmPassword = validateConfirmPassword(form.confirmPassword)" />
        <span v-if="submitted && errors.confirmPassword" class="form-field__error">{{ errors.confirmPassword }}</span>
      </label>

      <ul class="password-rules">
        <li :class="{ 'is-valid': passwordRules.length }">至少 8 位</li>
        <li :class="{ 'is-valid': passwordRules.letter }">包含字母</li>
        <li :class="{ 'is-valid': passwordRules.number }">包含数字</li>
      </ul>

      <label class="form-agreement">
        <input v-model="form.agreed" type="checkbox" />
        <span>我已阅读并同意 CNPA 用户协议与隐私政策</span>
      </label>
      <span v-if="submitted && errors.agreed" class="form-field__error">{{ errors.agreed }}</span>
      <p v-if="errors.form" class="form-field__error">{{ errors.form }}</p>

      <button type="submit" class="btn-primary" :disabled="loading">
        {{ loading ? '发送验证码...' : '创建 CNPA 账号' }}
      </button>

      <p class="register-form__login">
        已有账号？
        <button type="button" class="text-link" @click="goLogin">立即登录</button>
      </p>
      <p class="register-form__hint">创建账号后进入邮箱验证；本地开发未配 SMTP 时可任意输入 6 位数字验证码</p>
    </form>
  </div>
</template>

<style scoped lang="scss">
@import './styles/account-form.scss';
</style>
