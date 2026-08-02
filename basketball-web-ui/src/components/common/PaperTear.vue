<script setup lang="ts">
import TornEdge from '@/components/home/TornEdge.vue'

withDefaults(
  defineProps<{
    position?: 'top' | 'bottom'
    tone?: 'light' | 'gray' | 'dark'
  }>(),
  {
    position: 'top',
    tone: 'light',
  },
)
</script>

<template>
  <div class="paper-tear" :class="`paper-tear--${position}`" aria-hidden="true">
    <TornEdge :position="position" :tone="tone" />
  </div>
</template>

<style scoped lang="scss">
.paper-tear {
  --paper-tear-color: var(--cnpa-paper-bg, #e9e7e7);
  position: relative;
  width: 100%;
  pointer-events: none;
  z-index: 5;
}

.paper-tear :deep(.torn-edge-main) {
  fill: var(--paper-tear-color) !important;
}

.paper-tear--top {
  height: 0;
}

.paper-tear--bottom {
  height: 0;
}

.paper-tear :deep(.torn-edge-shadow) {
  display: block;
  opacity: 0.22;
}
</style>
