<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string | null
    alt?: string
    initials?: string
    size?: number
    rounded?: boolean
  }>(),
  {
    alt: '',
    initials: '--',
    size: 48,
    rounded: true,
  },
)

const failed = ref(false)

const showImage = computed(() => Boolean(props.src?.trim()) && !failed.value)

function onError() {
  failed.value = true
}
</script>

<template>
  <div
    class="safe-image"
    :class="{ 'safe-image--round': rounded }"
    :style="{ width: `${size}px`, height: `${size}px` }"
  >
    <img
      v-if="showImage"
      :src="src!"
      :alt="alt"
      class="safe-image__img"
      @error="onError"
    />
    <span v-else class="safe-image__placeholder">{{ initials }}</span>
  </div>
</template>

<style scoped lang="scss">
.safe-image {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  flex-shrink: 0;
}

.safe-image--round {
  border-radius: 50%;
}

.safe-image__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.safe-image__placeholder {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}
</style>
