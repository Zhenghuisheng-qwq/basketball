<script setup lang="ts">
import { computed, ref } from 'vue'
import { DEFAULT_AVATAR_THEMES, resolveDefaultAvatarTheme } from '@/utils/defaultAvatarTheme'

const props = withDefaults(
  defineProps<{
    name: string
    avatarUrl?: string
    playerNo?: string
    playerId?: string | number
    size?: 'sm' | 'md' | 'lg' | 'hero' | 'card'
    tone?: 'dark' | 'gold'
  }>(),
  {
    avatarUrl: '',
    playerNo: '',
    playerId: undefined,
    size: 'md',
    tone: 'dark',
  },
)

const imageError = ref(false)

const initials = computed(() => props.name.replace(/[^a-zA-Z0-9\u4e00-\u9fa5]/g, '').slice(0, 1) || '球')
const showImage = computed(() => Boolean(props.avatarUrl?.trim()) && !imageError.value)
const theme = computed(() => DEFAULT_AVATAR_THEMES[resolveDefaultAvatarTheme(props.playerNo, props.playerId)])

function handleError() {
  imageError.value = true
}
</script>

<template>
  <div class="player-avatar" :class="[`player-avatar--${size}`, `player-avatar--${tone}`]">
    <img
      v-if="showImage"
      :src="avatarUrl"
      :alt="name"
      class="player-avatar__img"
      @error="handleError"
    />
    <span
      v-else
      class="player-avatar__placeholder"
      :style="{ background: theme.bg, color: theme.fg, borderColor: theme.border }"
    >{{ initials }}</span>
  </div>
</template>

<style scoped lang="scss">
.player-avatar {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 50%;
  box-sizing: border-box;
}

.player-avatar--dark {
  border: 2px solid rgba(201, 163, 90, 0.72);
  background: radial-gradient(circle at 30% 20%, #2a2418 0%, #111 58%, #050505 100%);
  color: #f1d59a;
}

.player-avatar--gold {
  border: 2px solid rgba(201, 163, 90, 0.95);
  background: linear-gradient(145deg, #2a2418 0%, #111 55%, #050505 100%);
  color: #f1d59a;
}

.player-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.player-avatar__placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  border: 1px solid;
  border-radius: 50%;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.player-avatar--sm {
  width: 32px;
  height: 32px;

  .player-avatar__placeholder {
    font-size: 10px;
  }
}

.player-avatar--md {
  width: 48px;
  height: 48px;

  .player-avatar__placeholder {
    font-size: 12px;
  }
}

.player-avatar--lg {
  width: 72px;
  height: 72px;

  .player-avatar__placeholder {
    font-size: 16px;
  }
}

.player-avatar--hero {
  width: 120px;
  height: 120px;

  .player-avatar__placeholder {
    font-size: 24px;
  }
}

.player-avatar--card {
  width: 96px;
  height: 96px;

  .player-avatar__placeholder {
    font-size: 20px;
  }
}
</style>
