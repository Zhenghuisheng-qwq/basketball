<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    name: string
    code: string
    size?: number
  }>(),
  {
    size: 24,
  },
)

const hasError = ref(false)

const fallbackText = computed(() => {
  const map: Record<string, string> = {
    CNPA_1V1: '1V',
    CNPA_2V2: '2V',
    CNPA_3V3: '3V',
  }

  return map[props.code] ?? props.name.slice(0, 2).toUpperCase()
})

function handleError() {
  hasError.value = true
}
</script>

<template>
  <span
    class="competition-logo"
    :style="{ width: `${size}px`, height: `${size}px` }"
    :aria-label="name"
  >
    <img
      v-if="src && !hasError"
      class="competition-logo__image"
      :src="src"
      :alt="name"
      @error="handleError"
    />
    <span v-else class="competition-logo__fallback">{{ fallbackText }}</span>
  </span>
</template>

<style scoped lang="scss">
.competition-logo {
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  overflow: hidden;
}

.competition-logo__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.competition-logo__fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.88);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.04em;
}
</style>
