<template>
  <router-link
    v-if="hasTeamId"
    :to="teamTo"
    class="team-entry-link"
  >
    <slot />
  </router-link>
  <span v-else class="team-entry-link team-entry-link--plain">
    <slot />
  </span>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { teamDetailLocation } from '@/utils/eventContext'
import type { Team } from '@/views/events/types/events'
import type { usePublicSeasonContext } from '@/views/events/composables/usePublicSeasonContext'

const props = defineProps<{
  team: Team
}>()

const ctx = inject<ReturnType<typeof usePublicSeasonContext> | null>('publicSeasonContext', null)

const hasTeamId = computed(() => {
  const id = String(props.team?.teamId || '').trim()
  if (!id || id === 'null' || id === 'undefined' || id === '0') return false
  return true
})

const teamTo = computed(() =>
  teamDetailLocation(props.team.teamId, {
    divisionId: ctx?.divisionId.value,
    seasonId: ctx?.seasonId.value,
  }),
)
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
