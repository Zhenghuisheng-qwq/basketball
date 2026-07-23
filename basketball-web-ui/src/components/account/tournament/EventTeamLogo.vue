<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  logoUrl?: string
  teamCode: string
  size?: 'sm' | 'md' | 'lg'
}>()

const imageError = ref(false)

const showImage = computed(
  () => Boolean(props.logoUrl?.trim()) && !imageError.value,
)

function onError() {
  imageError.value = true
}
</script>

<template>
  <div
    class="event-team-logo"
    :class="`event-team-logo--${size || 'md'}`"
    :aria-label="`${teamCode} 球队 Logo`"
  >
    <img
      v-if="showImage"
      :src="logoUrl"
      :alt="`${teamCode} Logo`"
      @error="onError"
    />
    <span v-else>{{ teamCode.slice(0, 2) }}</span>
  </div>
</template>

<style scoped lang="scss">
.event-team-logo {
  display: grid;
  place-items: center;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: linear-gradient(145deg, #1a1a1a, #3a1010);
  color: #fff;
  font-weight: 900;
  letter-spacing: 0.04em;
  overflow: hidden;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &--sm {
    width: 48px;
    height: 48px;
    font-size: 14px;
  }

  &--md {
    width: 72px;
    height: 72px;
    font-size: 18px;
  }

  &--lg {
    width: 96px;
    height: 96px;
    font-size: 24px;
  }
}
</style>
