<script setup lang="ts">
import type { AccountNotificationType } from '@/types/accountNotifications'

defineProps<{
  type: AccountNotificationType
}>()
</script>

<template>
  <span class="notification-icon" aria-hidden="true">
    <svg v-if="type === 'MATCH_DATA'" viewBox="0 0 24 24">
      <path fill="currentColor" d="M3 13h4v8H3v-8zm7-6h4v14h-4V7zm7 3h4v11h-4V10z" />
    </svg>
    <svg v-else-if="type === 'REGISTRATION'" viewBox="0 0 24 24">
      <path fill="currentColor" d="M7 2h10v2H7V2zm-2 4h14v16H5V6zm2 2v12h10V8H7zm2 2h6v2H9v-2zm0 4h6v2H9v-2z" />
    </svg>
    <svg v-else-if="type === 'TEAM_INVITE'" viewBox="0 0 24 24">
      <path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
    </svg>
    <svg v-else-if="type === 'PLAYER_VERIFY'" viewBox="0 0 24 24">
      <path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z" />
    </svg>
    <svg v-else viewBox="0 0 24 24">
      <path fill="currentColor" d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z" />
    </svg>
  </span>
</template>

<style scoped lang="scss">
.notification-icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #111;

  svg {
    width: 22px;
    height: 22px;
  }
}
</style>
