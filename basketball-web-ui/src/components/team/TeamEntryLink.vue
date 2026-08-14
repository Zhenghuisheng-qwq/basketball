<template>
  <router-link
    v-if="hasTeamId"
    :to="`/tournament/team/${team.teamId}/overview`"
    class="team-entry-link"
  >
    <slot />
  </router-link>
  <span v-else class="team-entry-link team-entry-link--plain">
    <slot />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Team } from '@/views/events/types/events'

const props = defineProps<{
  team: Team
}>()

const hasTeamId = computed(() => {
  const id = String(props.team?.teamId || '').trim()
  if (!id || id === 'null' || id === 'undefined' || id === '0') return false
  return true
})
</script>

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
