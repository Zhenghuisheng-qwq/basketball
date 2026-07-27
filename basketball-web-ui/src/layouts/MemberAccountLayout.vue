<script setup lang="ts">
import WebHeader from '@/components/layout/WebHeader.vue'
import AccountSidebar from '@/components/account/AccountSidebar.vue'
import AccountFooter from '@/components/account/AccountFooter.vue'
</script>

<template>
  <div class="member-account-layout account-center-vars">
    <WebHeader />

    <div class="account-page">
      <div class="account-layout account-page__shell account-shell">
        <AccountSidebar class="account-sidebar" />

        <main class="account-main">
          <div class="account-main-inner account-page-inner">
            <RouterView />
          </div>
        </main>
      </div>
    </div>

    <AccountFooter />
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
