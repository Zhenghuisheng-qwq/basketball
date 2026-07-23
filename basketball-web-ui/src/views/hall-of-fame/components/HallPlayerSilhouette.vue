<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    name: string
    avatarUrl?: string
    size?: 'card' | 'feature' | 'roster' | 'mvp' | 'lineup'
  }>(),
  {
    avatarUrl: '',
    size: 'feature',
  },
)

const imageError = ref(false)
const showImage = computed(() => Boolean(props.avatarUrl?.trim()) && !imageError.value)
const initials = computed(() => props.name.trim().slice(0, 3).toUpperCase())

watch(
  () => props.avatarUrl,
  () => {
    imageError.value = false
  },
)

function handleError() {
  imageError.value = true
}
</script>

<template>
  <div
    class="hall-player-visual"
    :class="`hall-player-visual--${size}`"
    aria-hidden="true"
  >
    <img
      v-if="showImage"
      :src="avatarUrl"
      :alt="name"
      class="hall-player-visual__photo"
      @error="handleError"
    />
    <template v-else>
      <div class="hall-player-visual__glow" />
      <div class="hall-player-visual__particles" />
      <div class="hall-player-visual__silhouette">
        <span class="hall-player-visual__head" />
        <span class="hall-player-visual__shoulder" />
        <span class="hall-player-visual__torso" />
      </div>
      <span class="hall-player-visual__initials">{{ initials }}</span>
    </template>
  </div>
</template>

<style scoped lang="scss">
.hall-player-visual {
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
  width: 100%;
  background:
    radial-gradient(circle at 50% 22%, rgba(231, 25, 34, 0.22), transparent 38%),
    radial-gradient(circle at 72% 78%, rgba(210, 166, 72, 0.08), transparent 28%),
    linear-gradient(180deg, #1a1a1a 0%, #0a0a0a 48%, #050505 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.hall-player-visual__photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: top center;
}

.hall-player-visual__glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 30%, rgba(231, 25, 34, 0.28), transparent 42%),
    linear-gradient(180deg, transparent 40%, rgba(0, 0, 0, 0.55) 100%);
  pointer-events: none;
}

.hall-player-visual__particles {
  position: absolute;
  inset: 0;
  opacity: 0.45;
  background-image:
    radial-gradient(circle at 22% 28%, rgba(255, 255, 255, 0.35) 0 1px, transparent 1.5px),
    radial-gradient(circle at 68% 18%, rgba(255, 255, 255, 0.28) 0 1px, transparent 1.5px),
    radial-gradient(circle at 78% 52%, rgba(231, 25, 34, 0.55) 0 1px, transparent 1.5px),
    radial-gradient(circle at 36% 72%, rgba(255, 255, 255, 0.2) 0 1px, transparent 1.5px);
  pointer-events: none;
}

.hall-player-visual__silhouette {
  position: absolute;
  left: 50%;
  bottom: 8%;
  width: 58%;
  height: 78%;
  transform: translateX(-50%);
}

.hall-player-visual__head {
  position: absolute;
  top: 0;
  left: 50%;
  width: 34%;
  aspect-ratio: 1;
  border-radius: 50%;
  background:
    linear-gradient(180deg, #4a4a4a 0%, #1c1c1c 100%);
  box-shadow: 0 0 18px rgba(231, 25, 34, 0.35);
  transform: translateX(-50%);
}

.hall-player-visual__shoulder {
  position: absolute;
  top: 28%;
  left: 50%;
  width: 92%;
  height: 18%;
  border-radius: 40% 40% 20% 20%;
  background: linear-gradient(180deg, #3d3d3d 0%, #171717 100%);
  transform: translateX(-50%);
  box-shadow: 0 0 22px rgba(231, 25, 34, 0.22);
}

.hall-player-visual__torso {
  position: absolute;
  top: 42%;
  left: 50%;
  width: 70%;
  height: 58%;
  border-radius: 18% 18% 8% 8%;
  background:
    linear-gradient(180deg, #3a3a3a 0%, #141414 55%, #0a0a0a 100%);
  transform: translateX(-50%);
  box-shadow: inset 0 0 0 1px rgba(231, 25, 34, 0.18);
}

.hall-player-visual__initials {
  position: absolute;
  left: 50%;
  bottom: 10%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.42);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  pointer-events: none;
}

.hall-player-visual--feature {
  min-height: 210px;
  height: 100%;
}

.hall-player-visual--mvp {
  width: 100%;
  height: 100%;
  min-height: 100%;
  border: 0;
  background:
    radial-gradient(circle at 58% 36%, rgba(255, 255, 255, 0.16), transparent 28%),
    radial-gradient(circle at 42% 58%, rgba(231, 25, 34, 0.22), transparent 34%),
    linear-gradient(90deg, #090909 0%, #101010 70%, #080808 100%);
}

.hall-player-visual--mvp .hall-player-visual__photo {
  position: absolute;
  left: 8%;
  bottom: 0;
  width: 88%;
  height: 94%;
  object-fit: contain;
  object-position: center bottom;
}

.hall-player-visual--mvp .hall-player-visual__silhouette {
  bottom: 0;
  width: 70%;
  height: 92%;
}

.hall-player-visual--mvp .hall-player-visual__initials {
  bottom: 8%;
  font-size: 14px;
  letter-spacing: 0.22em;
}

.hall-player-visual--card {
  height: 100%;
  min-height: 168px;
}

.hall-player-visual--lineup {
  width: 100%;
  height: 100%;
  min-height: 100%;
  border: 0;
  background:
    radial-gradient(circle at 50% 30%, rgba(255, 255, 255, 0.13), transparent 27%),
    radial-gradient(circle at 50% 45%, rgba(160, 0, 0, 0.22), transparent 55%),
    #090909;
}

.hall-player-visual--lineup .hall-player-visual__photo {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center bottom;
}

.hall-player-visual--lineup .hall-player-visual__silhouette {
  bottom: 0;
  width: 78%;
  height: 88%;
}

.hall-player-visual--lineup .hall-player-visual__head {
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.12),
    0 0 22px rgba(231, 25, 34, 0.4);
}

.hall-player-visual--lineup .hall-player-visual__shoulder,
.hall-player-visual--lineup .hall-player-visual__torso {
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.08),
    0 0 24px rgba(231, 25, 34, 0.22);
}

.hall-player-visual--lineup .hall-player-visual__initials {
  bottom: 14%;
  font-size: 12px;
  letter-spacing: 0.2em;
}

.hall-player-visual--roster {
  width: 52px;
  height: 64px;
  min-height: 64px;
  flex: 0 0 52px;
}

.hall-player-visual--roster .hall-player-visual__initials {
  font-size: 9px;
  letter-spacing: 0.08em;
}
</style>
