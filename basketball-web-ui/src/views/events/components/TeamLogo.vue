<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Team } from '../types/events'
import { initials } from '@/utils/publicDisplay'

const props = defineProps<{
  team: Team
  size?: 'sm' | 'md' | 'lg' | 'schedule'
}>()

const imageError = ref(false)

const logoSrc = computed(() => props.team.logo?.trim() || '')

watch(
  () => props.team.logo,
  () => {
    imageError.value = false
  },
)

function handleError() {
  imageError.value = true
}
</script>

<template>
  <div class="team-logo" :class="[`team-logo-${size || 'md'}`]">
    <img
      v-if="logoSrc && !imageError"
      :src="logoSrc"
      :alt="team.name"
      class="team-logo-image"
      @error="handleError"
    />
    <div v-else class="team-logo-fallback">
      {{ initials(team.shortName || team.name) }}
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

.team-logo-schedule {
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
  border: 1px solid rgba(225, 6, 19, 0.35);
  background: #151515;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.team-logo-sm .team-logo-fallback {
  font-size: 10px;
}

.team-logo-schedule .team-logo-fallback {
  font-size: 14px;
}
</style>
