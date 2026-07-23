<script setup lang="ts">
withDefaults(
  defineProps<{
    message: string
    subtitle?: string
    large?: boolean
  }>(),
  {
    subtitle: '',
    large: false,
  },
)
</script>

<template>
  <div
    class="account-empty-state"
    :class="{ 'account-empty-state--large': large }"
  >
    <p>{{ message }}</p>
    <p v-if="subtitle" class="account-empty-state__subtitle">{{ subtitle }}</p>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
