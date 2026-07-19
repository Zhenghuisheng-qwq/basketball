<script setup lang="ts">
import { computed, ref } from 'vue'
import TeamBadge from '@/components/home/TeamBadge.vue'
import { getTeamBadgeKey } from '@/utils/teamBadge'

const props = defineProps<{
  team: {
    id: string
    name: string
    shortName: string
    logoUrl?: string
    code?: string
  }
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'hero' | 'selector'
}>()

const imageError = ref(false)

const badgeKey = computed(() => getTeamBadgeKey(props.team.id))
const logoSrc = computed(() => props.team.logoUrl?.trim() ?? '')
const fallbackText = computed(() =>
  (props.team.shortName || props.team.code || props.team.name).slice(0, 2).toUpperCase(),
)
const useTinyBadge = computed(() => props.size === 'xs' || props.size === 'sm')

function handleError() {
  imageError.value = true
}
</script>

<template>
  <div class="team-logo" :class="[`team-logo--${size || 'md'}`]">
    <img
      v-if="logoSrc && !imageError"
      :src="logoSrc"
      :alt="team.name"
      class="team-logo__img"
      @error="handleError"
    />
    <TeamBadge
      v-else-if="badgeKey"
      :type="badgeKey"
      :tiny="useTinyBadge"
    />
    <div v-else class="team-logo__fallback">
      {{ fallbackText }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.team-logo {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  overflow: hidden;
}

.team-logo__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.team-logo__fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: #151515;
  color: #fff;
  font-weight: 900;
}

.team-logo--xs {
  width: 38px;
  height: 38px;

  .team-logo__fallback {
    font-size: 9px;
    letter-spacing: 0;
  }

  :deep(.badge:not(.tiny)) {
    transform: scale(0.59);
  }
}

.team-logo--sm {
  width: 36px;
  height: 36px;

  .team-logo__fallback {
    font-size: 10px;
  }
}

.team-logo--md {
  width: 48px;
  height: 48px;

  .team-logo__fallback {
    font-size: 12px;
  }

  :deep(.badge:not(.tiny)) {
    transform: scale(0.75);
  }
}

.team-logo--selector {
  width: 56px;
  height: 56px;

  .team-logo__fallback {
    font-size: 12px;
    letter-spacing: 0.04em;
  }

  :deep(.badge:not(.tiny)) {
    transform: scale(0.875);
  }
}

.team-logo--lg {
  width: 64px;
  height: 64px;

  .team-logo__fallback {
    font-size: 12px;
  }
}

.team-logo--xl {
  width: 120px;
  height: 120px;

  .team-logo__fallback {
    font-size: 24px;
    letter-spacing: 0.06em;
    border-radius: 4px;
  }

  :deep(.badge) {
    transform: scale(1.875);
  }
}

.team-logo--hero {
  width: 190px;
  height: 126px;

  .team-logo__fallback {
    font-size: 38px;
    letter-spacing: 3px;
    border: 1px solid rgba(230, 190, 105, 0.5);
    border-radius: 3px;
    background: linear-gradient(135deg, rgba(205, 160, 75, 0.95) 0%, rgba(83, 59, 22, 0.95) 100%);
  }

  :deep(.badge) {
    transform: scale(1.97);
  }
}
</style>
