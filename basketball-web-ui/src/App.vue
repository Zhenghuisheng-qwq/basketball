<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import WebHeader from '@/components/layout/WebHeader.vue'
import WebFooter from '@/components/layout/WebFooter.vue'

const route = useRoute()

const useStandaloneAccountChrome = computed(
  () => route.path.startsWith('/register') || route.path.startsWith('/account'),
)
</script>

<template>
  <div class="app">
    <WebHeader v-if="!useStandaloneAccountChrome" />
    <main class="app-main">
      <router-view />
    </main>
    <WebFooter v-if="!useStandaloneAccountChrome" />
  </div>
</template>

<style scoped lang="scss">
.app {
  width: 100%;
  min-height: 100vh;
  background: #050505;
  color: #fff;
  overflow-x: hidden;
}

.app-main {
  width: 100%;
  min-height: calc(100vh - var(--header-height));
}
</style>
