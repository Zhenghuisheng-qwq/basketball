<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <footer class="account-footer">
    <div class="site-shell account-footer__inner">
      <span>{{ t('accountShell.leagueName') }}</span>
      <span>© 2026 CNPA. All rights reserved.</span>
    </div>
  </footer>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
