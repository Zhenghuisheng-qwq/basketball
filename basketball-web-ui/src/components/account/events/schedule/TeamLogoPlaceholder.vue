<script setup lang="ts">
import EventTeamLogo from '@/components/account/tournament/EventTeamLogo.vue'

defineProps<{
  logoUrl?: string
  teamName: string
  shortName: string
  size?: 'md' | 'lg'
}>()
</script>

<template>
  <EventTeamLogo
    :logo-url="logoUrl"
    :team-code="shortName || teamName"
    :size="size || 'lg'"
    variant="versus"
  />
</template>
