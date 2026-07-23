<script setup lang="ts">
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'

defineProps<{
  tournamentName: string
  teamCode: string
  description: string
  statusText: string
  statusType?: 'default' | 'success' | 'warning' | 'danger' | 'info'
}>()
</script>

<template>
  <AccountStatusBanner
    :title="`${tournamentName} · ${teamCode}`"
    :description="description"
    :status-text="statusText"
    :status-type="statusType || 'info'"
  />
</template>
