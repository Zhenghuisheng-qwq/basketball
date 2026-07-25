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

const canvasStyle = computed(() => {
  if (backgroundLoadFailed.value) return undefined
  return { backgroundImage: `url(${authBackground})` }
})

function handleBackgroundError() {
  backgroundLoadFailed.value = true
}
</script>

<template>
  <div class="member-auth-layout">
    <WebHeader />

    <main class="member-auth-page">
      <div class="member-auth-page__shell">
        <div
          class="member-auth-canvas"
          :class="{ 'member-auth-canvas--fallback': backgroundLoadFailed }"
          :style="canvasStyle"
        >
          <div
            class="member-auth-form-area"
            :class="`member-auth-form-area--${contentVariant}`"
          >
            <div class="member-auth-form-area__inner">
              <RouterView />
            </div>
          </div>
        </div>
      </div>
    </main>

    <footer class="member-auth-footer">
      <div class="site-shell member-auth-footer__inner">
        <span>CNPA 中国职业电竞篮球联赛</span>
        <span>© 2026 CNPA. All rights reserved.</span>
      </div>
    </footer>

    <!-- 仅用于检测背景图是否加载失败 -->
    <img
      v-show="false"
      :src="authBackground"
      alt=""
      @error="handleBackgroundError"
    />
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/member-auth.scss';
</style>
