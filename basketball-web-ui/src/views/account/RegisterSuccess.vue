<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { maskEmail } from '@/utils/maskEmail'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

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
      <h1 class="auth-register__title-cn">{{ t('auth.registerDone') }}</h1>
      <p class="auth-register__title-en">ACCOUNT READY</p>
    </header>
    <div class="auth-complete__icon" aria-hidden="true">
      <span class="auth-complete__check" />
    </div>
    <h2 class="auth-complete__heading">{{ t('auth.accountCreated') }}</h2>
    <p class="auth-complete__email">{{ t('auth.registeredEmailLine', { email: maskedEmail }) }}</p>
    <p class="auth-complete__email">{{ t('auth.loginWithEmail') }}</p>
    <button type="button" class="auth-primary-button" @click="goLogin">{{ t('auth.loginNow') }}</button>
    <button type="button" class="auth-secondary-button" @click="goHome">{{ t('auth.backHome') }}</button>
    <p class="auth-form__hint">{{ t('auth.afterLoginHint') }}</p>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>
