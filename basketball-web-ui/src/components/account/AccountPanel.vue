<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<template>
  <article class="account-panel account-card">
    <div class="account-panel__head">
      <h3 class="account-panel__title account-card-title">{{ title }}</h3>
      <div v-if="$slots.extra" class="account-panel__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="account-panel__body">
      <slot />
    </div>
  </article>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
