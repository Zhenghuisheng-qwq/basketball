<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import WebHeader from '@/components/layout/WebHeader.vue'
import authBackground from '@/assets/icon/login/login_background.png'

type AuthContentVariant = 'login' | 'register' | 'verify' | 'complete'

const route = useRoute()
const backgroundLoadFailed = ref(false)

const contentVariant = computed<AuthContentVariant>(() => {
  const value = route.meta.authContentVariant
  if (value === 'login' || value === 'register' || value === 'verify' || value === 'complete') {
    return value
  }
  return 'register'
})

function handleBackgroundError() {
  backgroundLoadFailed.value = true
}
</script>

<template>
  <div class="member-auth-layout">
    <WebHeader />

    <main class="member-auth-page">
      <div class="site-shell member-auth-page__shell">
        <div
          class="member-auth-canvas"
          :class="{ 'member-auth-canvas--fallback': backgroundLoadFailed }"
        >
          <img
            v-if="!backgroundLoadFailed"
            class="member-auth-canvas__background"
            :src="authBackground"
            alt=""
            @error="handleBackgroundError"
          />

          <div
            class="member-auth-mobile-visual"
            :style="{ backgroundImage: backgroundLoadFailed ? 'none' : `url(${authBackground})` }"
            aria-hidden="true"
          />

          <section class="member-auth-content">
            <div
              class="member-auth-content__inner"
              :class="`member-auth-content__inner--${contentVariant}`"
            >
              <RouterView />
            </div>
          </section>
        </div>
      </div>
    </main>

    <footer class="member-auth-footer">
      <div class="site-shell member-auth-footer__inner">
        <span>CNPA 中国职业电竞篮球联赛</span>
        <span>© 2026 CNPA. All rights reserved.</span>
      </div>
    </footer>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/member-auth.scss';
</style>
