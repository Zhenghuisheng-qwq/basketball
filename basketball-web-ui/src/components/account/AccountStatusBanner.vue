<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description: string
    statusText: string
    statusType?: 'default' | 'success' | 'warning' | 'danger' | 'info'
  }>(),
  {
    statusType: 'default',
  },
)
</script>

<template>
  <section class="account-welcome-banner account-hero-banner account-status-banner">
    <div class="account-welcome-banner__content">
      <h2 class="account-welcome-banner__title account-hero-title">{{ title }}</h2>
      <p class="account-welcome-banner__desc account-hero-desc">{{ description }}</p>
    </div>

    <div
      class="account-welcome-banner__status account-hero-status"
      :class="`account-welcome-banner__status--${statusType}`"
    >
      <span class="account-welcome-banner__status-icon" aria-hidden="true">
        <svg v-if="statusType === 'success'" viewBox="0 0 24 24" width="22" height="22">
          <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
        </svg>
        <svg v-else-if="statusType === 'warning'" viewBox="0 0 24 24" width="22" height="22">
          <path fill="currentColor" d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z" />
        </svg>
        <svg v-else viewBox="0 0 24 24" width="22" height="22">
          <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
        </svg>
      </span>
      <span>{{ statusText }}</span>
    </div>

    <div class="account-welcome-banner__tear" aria-hidden="true" />
  </section>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
