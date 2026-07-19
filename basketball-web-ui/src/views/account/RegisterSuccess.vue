<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getMaskedRegisterEmail } from '@/composables/useRegisterFlow'
import RegisterSteps from './components/RegisterSteps.vue'

const router = useRouter()
const maskedEmail = computed(() => getMaskedRegisterEmail())

function goLogin() {
  router.push('/account')
}

function goHome() {
  router.push('/home')
}
</script>

<template>
  <div class="register-page">
    <RegisterSteps :current="3" />

    <div class="register-page__head">
      <h2 class="register-page__title">注册完成</h2>
      <span class="register-page__sub">Account Ready</span>
    </div>

    <div class="success-panel">
      <div class="success-panel__icon" aria-hidden="true">
        <span class="success-panel__check" />
      </div>

      <h3 class="success-panel__title">CNPA 账号创建成功</h3>
      <p class="success-panel__email">绑定邮箱：{{ maskedEmail }}</p>

      <button type="button" class="btn-primary" @click="goLogin">立即登录</button>
      <button type="button" class="btn-secondary" @click="goHome">返回首页</button>

      <p class="register-form__hint">登录后可进入个人中心并申请球员认证</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import './styles/account-form.scss';

.success-panel {
  max-width: 420px;
}

.success-panel__icon {
  width: 80px;
  height: 80px;
  display: grid;
  place-items: center;
  margin-bottom: 24px;
  border: 3px solid #18b46b;
  border-radius: 50%;
  background: rgba(24, 180, 107, 0.08);
}

.success-panel__check {
  width: 18px;
  height: 32px;
  border-right: 4px solid #18b46b;
  border-bottom: 4px solid #18b46b;
  transform: rotate(45deg) translate(-2px, -4px);
}

.success-panel__title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 900;
  color: var(--cnpa-paper-text);
}

.success-panel__email {
  margin: 0 0 28px;
  font-size: 14px;
  font-weight: 600;
  color: var(--cnpa-paper-text-muted);
}

.btn-secondary {
  margin-top: 12px;
}
</style>
