<script setup lang="ts">
import { ref } from 'vue'
import TeamBadge from '@/components/home/TeamBadge.vue'
import type { Team } from '../types/events'

defineProps<{
  team: Team
  size?: 'sm' | 'md' | 'lg'
}>()

const imageError = ref(false)

function handleError() {
  imageError.value = true
}
</script>

<template>
  <div class="team-logo" :class="[`team-logo-${size || 'md'}`]">
    <TeamBadge
      v-if="team.badgeKey && imageError"
      :type="team.badgeKey"
      :tiny="size === 'sm'"
    />
    <img
      v-else-if="!imageError"
      :src="team.logo"
      :alt="team.name"
      class="team-logo-image"
      @error="handleError"
    />
    <div v-else class="team-logo-fallback">
      {{ team.shortName.slice(0, 2) }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.team-logo {
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.team-logo-sm {
  width: 36px;
  height: 36px;
}

.team-logo-md {
  width: 48px;
  height: 48px;
}

.team-logo-lg {
  width: 64px;
  height: 64px;
}

.team-logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.team-logo-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: #151515;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.team-logo-sm .team-logo-fallback {
  font-size: 10px;
}
</style>
