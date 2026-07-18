<script setup lang="ts">
import WebHeader from '@/components/layout/WebHeader.vue'
import WebFooter from '@/components/layout/WebFooter.vue'
</script>

<template>
  <div class="app">
    <div class="site-shell">
      <WebHeader />
      <main class="app-main">
        <router-view />
      </main>
      <WebFooter />
    </div>
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

.site-shell {
  width: min(var(--site-width), var(--site-max-width));
  margin: 0 auto;
  min-height: 100vh;
  background: #050505;
  position: relative;
}

.app-main {
  width: 100%;
  min-height: calc(100vh - 72px);
}
</style>
