<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { setRegisterEmail } from '@/composables/useRegisterFlow'
import RegisterSteps from './components/RegisterSteps.vue'

const router = useRouter()

const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  agreed: false,
})

const errors = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  agreed: '',
})

const submitted = ref(false)

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
  errors.password = validatePassword(form.password)
  errors.confirmPassword = validateConfirmPassword(form.confirmPassword)
  errors.agreed = form.agreed ? '' : '请先阅读并同意相关协议'

  return !errors.email && !errors.password && !errors.confirmPassword && !errors.agreed
}

function handleSubmit() {
  submitted.value = true
  if (!validateForm()) return

  setRegisterEmail(form.email.trim())
  router.push('/register/verify-email')
}

function goLogin() {
  router.push('/account')
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
        <input
          v-model="form.email"
          type="email"
          class="form-field__input"
          placeholder="name@example.com"
          autocomplete="email"
          @blur="errors.email = validateEmail(form.email)"
        />
        <span v-if="submitted && errors.email" class="form-field__error">{{ errors.email }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">设置密码</span>
        <input
          v-model="form.password"
          type="password"
          class="form-field__input"
          placeholder="请输入密码"
          autocomplete="new-password"
          @blur="errors.password = validatePassword(form.password)"
        />
        <span v-if="submitted && errors.password" class="form-field__error">{{ errors.password }}</span>
      </label>

      <label class="form-field">
        <span class="form-field__label">确认密码</span>
        <input
          v-model="form.confirmPassword"
          type="password"
          class="form-field__input"
          placeholder="请再次输入密码"
          autocomplete="new-password"
          @blur="errors.confirmPassword = validateConfirmPassword(form.confirmPassword)"
        />
        <span v-if="submitted && errors.confirmPassword" class="form-field__error">
          {{ errors.confirmPassword }}
        </span>
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

      <button type="submit" class="btn-primary">创建 CNPA 账号</button>

      <p class="register-form__login">
        已有账号？
        <button type="button" class="text-link" @click="goLogin">立即登录</button>
      </p>

      <p class="register-form__hint">创建账号后，我们将向你的邮箱发送 6 位验证码</p>
    </form>
  </div>
</template>

<style scoped lang="scss">
@import './styles/account-form.scss';
</style>
