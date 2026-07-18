<script setup lang="ts">
import WebHeader from '@/components/layout/WebHeader.vue'
import WebFooter from '@/components/layout/WebFooter.vue'
</script>

<template>
  <div class="app">
    <WebHeader />
    <main class="app-main">
      <router-view />
    </main>
    <WebFooter />
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
