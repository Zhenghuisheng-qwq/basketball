<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import WebHeader from '@/components/layout/WebHeader.vue'
import WebFooter from '@/components/layout/WebFooter.vue'

const route = useRoute()

const isAuthRoute = computed(
  () =>
    route.path.startsWith('/login')
    || route.path.startsWith('/register')
    || route.path.startsWith('/forgot-password'),
)

const isAccountRoute = computed(() => route.path.startsWith('/account'))

const useStandaloneChrome = computed(() => isAuthRoute.value || isAccountRoute.value)
</script>

<template>
  <div class="app">
    <WebHeader v-if="!useStandaloneChrome" />

    <main
      :class="[
        useStandaloneChrome ? 'app-main' : 'site-main',
        isAuthRoute ? 'app-main--auth' : '',
        isAccountRoute ? 'app-main--account' : '',
      ]"
    >
      <router-view />
    </main>

    <WebFooter v-if="!useStandaloneChrome" />
  </div>
</template>

<style scoped lang="scss">
.app {
  width: 100%;
  min-height: 100vh;
  background: #050505;
  color: #fff;
}

.app-main {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
  min-height: auto;
  overflow-x: hidden;
}

.app-main--auth {
  background: #050505;
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  overflow: hidden;
}

.app:has(.app-main--auth) {
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  overflow: hidden;
}

.app-main--account {
  background: #080808;
}
</style>
