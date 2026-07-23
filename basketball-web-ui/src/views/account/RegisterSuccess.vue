<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getMaskedRegisterEmail, getRegisterEmail } from '@/composables/useRegisterFlow'
import AuthStepper from './components/AuthStepper.vue'

const router = useRouter()
const maskedEmail = computed(() => getMaskedRegisterEmail())

function goLogin() {
  router.push({
    path: '/login',
    query: { email: getRegisterEmail() },
  })
}

function goHome() {
  router.push('/home')
}
</script>

<template>
  <div class="auth-page auth-complete">
    <AuthStepper :current="3" />

    <header class="auth-register__title">
      <h1 class="auth-register__title-cn">注册完成</h1>
      <p class="auth-register__title-en">ACCOUNT READY</p>
    </header>

    <div class="auth-complete__icon" aria-hidden="true">
      <span class="auth-complete__check" />
    </div>

    <h2 class="auth-complete__heading">CNPA 账号创建成功</h2>
    <p class="auth-complete__email">绑定邮箱：{{ maskedEmail }}</p>

    <button type="button" class="auth-primary-button" @click="goLogin">立即登录</button>
    <button type="button" class="auth-secondary-button" @click="goHome">返回首页</button>

    <p class="auth-form__hint">登录后可进入个人中心并申请球员认证</p>

    <div class="auth-notice-strip">
      <span aria-hidden="true">✓</span>
      <span>你的 CNPA 账号已就绪，登录后即可管理个人资料与球员认证</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>