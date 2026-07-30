<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { maskEmail } from '@/utils/maskEmail'

const route = useRoute()
const router = useRouter()

const registeredEmail = computed(() => {
  const email = route.query.email
  return typeof email === 'string' && email.trim() ? email.trim() : ''
})

const maskedEmail = computed(() => (registeredEmail.value ? maskEmail(registeredEmail.value) : '****@example.com'))

function goLogin() {
  router.push({
    path: '/login',
    query: registeredEmail.value ? { email: registeredEmail.value } : {},
  })
}

function goHome() {
  router.push('/home')
}
</script>

<template>
  <div class="auth-page auth-complete">
    <header class="auth-register__title">
      <h1 class="auth-register__title-cn">注册完成</h1>
      <p class="auth-register__title-en">ACCOUNT READY</p>
    </header>
    <div class="auth-complete__icon" aria-hidden="true">
      <span class="auth-complete__check" />
    </div>
    <h2 class="auth-complete__heading">CNPA 账号创建成功</h2>
    <p class="auth-complete__email">注册邮箱：{{ maskedEmail }}</p>
    <p class="auth-complete__email">请使用注册邮箱和密码登录</p>
    <button type="button" class="auth-primary-button" @click="goLogin">立即登录</button>
    <button type="button" class="auth-secondary-button" @click="goHome">返回首页</button>
    <p class="auth-form__hint">登录后可进入个人中心并申请球员认证</p>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>
