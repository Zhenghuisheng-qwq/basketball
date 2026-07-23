<script setup lang="ts">
withDefaults(
  defineProps<{
    text: string
    type?: 'success' | 'warning' | 'danger' | 'default'
  }>(),
  {
    type: 'default',
  },
)
</script>

<template>
  <span class="account-status-badge" :class="`account-status-badge--${type}`">
    {{ text }}
  </span>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
