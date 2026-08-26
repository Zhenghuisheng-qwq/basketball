<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Team } from '../types/events'
import { initials } from '@/utils/publicDisplay'

const props = defineProps<{
  team: Team
  size?: 'sm' | 'md' | 'lg' | 'schedule' | 'hero'
}>()

const imageError = ref(false)

const logoSrc = computed(() => props.team.logo?.trim() || '')

const fallbackText = computed(() => {
  const raw = String(props.team.shortName || props.team.code || props.team.name || '').replace(/\s+/g, '')
  if (props.size === 'schedule' || props.size === 'hero') {
    const tail = raw.replace(/^E2E-?/i, '').replace(/^CNPA-T-/i, '')
    return (tail || raw).slice(0, 3).toUpperCase() || '--'
  }
  return initials(raw)
})

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
      {{ fallbackText }}
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
  overflow: hidden;
  border-radius: 50%;
  background: #161616;
  border: 1px solid #2a2a2a;
  box-shadow: inset 0 0 0 1px rgba(229, 20, 34, 0.18);
}

.team-logo-lg {
  width: 64px;
  height: 64px;
}

.team-logo-hero {
  width: 104px;
  height: 104px;
  overflow: hidden;
  border-radius: 50%;
  background: #161616;
  border: 1px solid #2a2a2a;
  box-shadow: inset 0 0 0 1px rgba(229, 20, 34, 0.18);
}

.team-logo-hero .team-logo-image,
.team-logo-hero .team-logo-fallback {
  border-radius: 50%;
}

.team-logo-hero .team-logo-fallback {
  border: 0;
  background: linear-gradient(160deg, #1a1a1a 0%, #111 70%);
  color: #fff;
  font-size: 22px;
  font-weight: 800;
}

@media (max-width: 1024px) {
  .team-logo-hero {
    width: 80px;
    height: 80px;
  }

  .team-logo-hero .team-logo-fallback {
    font-size: 18px;
  }
}

@media (max-width: 640px) {
  .team-logo-hero {
    width: 68px;
    height: 68px;
  }

  .team-logo-hero .team-logo-fallback {
    font-size: 15px;
  }
}

.team-logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.team-logo-schedule .team-logo-image,
.team-logo-schedule .team-logo-fallback {
  border-radius: 50%;
}

.team-logo-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  border: 1px solid #292929;
  background: linear-gradient(145deg, #141414, #222);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.team-logo-sm .team-logo-fallback {
  font-size: 10px;
}

.team-logo-schedule .team-logo-fallback {
  border: 0;
  background: linear-gradient(160deg, #1a1a1a 0%, #111 70%);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}
</style>
