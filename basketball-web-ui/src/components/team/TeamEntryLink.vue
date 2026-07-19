<script setup lang="ts">
import type { Team } from '@/views/events/types/events'

defineProps<{
  team: Team
}>()
</script>

<template>
  <router-link
    :to="`/tournament/team/${team.teamId}/overview`"
    class="team-entry-link"
  >
    <slot />
  </router-link>
</template>

<style scoped lang="scss">
.team-entry-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: inherit;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.82;
  }
}
</style>
