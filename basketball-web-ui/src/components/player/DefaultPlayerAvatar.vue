<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  avatarInitial,
  DEFAULT_AVATAR_THEMES,
  resolveDefaultAvatarTheme,
} from '@/utils/defaultAvatarTheme'

const props = withDefaults(
  defineProps<{
    avatarUrl?: string | null
    playerName?: string | null
    playerNo?: string | null
    playerId?: string | number | null
    size?: number
  }>(),
  {
    avatarUrl: '',
    playerName: '',
    playerNo: '',
    playerId: null,
    size: 40,
  },
)

const imageError = ref(false)

const showImage = computed(() => Boolean(props.avatarUrl?.trim()) && !imageError.value)

const themeIndex = computed(() => resolveDefaultAvatarTheme(props.playerNo, props.playerId))

const theme = computed(() => DEFAULT_AVATAR_THEMES[themeIndex.value])

const initial = computed(() => avatarInitial(props.playerName, '球'))

const styleVars = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.max(12, Math.floor(props.size * 0.38))}px`,
  background: theme.value.bg,
  color: theme.value.fg,
  borderColor: theme.value.border,
}))
</script>

<template>
  <img
    v-if="showImage"
    :src="avatarUrl || ''"
    :alt="playerName || '球员头像'"
    class="default-player-avatar__img"
    :style="{ width: `${size}px`, height: `${size}px` }"
    @error="imageError = true"
  />
  <span
    v-else
    class="default-player-avatar__fallback"
    :style="styleVars"
    :aria-label="playerName || '球员头像'"
  >
    {{ initial }}
  </span>
</template>

<style scoped>
.default-player-avatar__img,
.default-player-avatar__fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  object-fit: cover;
  flex-shrink: 0;
}

.default-player-avatar__fallback {
  border: 1px solid;
  font-weight: 600;
  line-height: 1;
  user-select: none;
}
</style>
