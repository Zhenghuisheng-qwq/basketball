<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    position?: 'top' | 'bottom'
    tone?: 'light' | 'dark'
  }>(),
  {
    position: 'top',
    tone: 'light',
  },
)

const edgeColor = computed(() => {
  return props.tone === 'light' ? '#f2efe8' : '#050505'
})

const shadowColor = computed(() => {
  return props.tone === 'light'
    ? 'rgba(0, 0, 0, 0.22)'
    : 'rgba(255, 255, 255, 0.05)'
})

const filterId = computed(() => `rough-edge-${props.position}-${props.tone}`)
</script>

<template>
  <svg
    class="torn-edge"
    :class="[`torn-edge-${position}`, `torn-edge-${tone}`]"
    viewBox="0 0 1800 96"
    preserveAspectRatio="none"
    aria-hidden="true"
  >
    <defs>
      <filter
        :id="filterId"
        x="-3%"
        y="-40%"
        width="106%"
        height="190%"
        color-interpolation-filters="sRGB"
      >
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.009 0.22"
          numOctaves="3"
          :seed="position === 'top' ? 31 : 57"
          result="noise"
        />
        <feDisplacementMap
          in="SourceGraphic"
          in2="noise"
          scale="10"
          xChannelSelector="R"
          yChannelSelector="G"
        />
      </filter>
    </defs>

    <path
      class="torn-edge-shadow"
      :fill="shadowColor"
      :filter="`url(#${filterId})`"
      d="
        M0 96
        L0 56
        L31 61
        L57 47
        L88 64
        L126 49
        L151 69
        L193 52
        L224 59
        L259 42
        L287 66
        L326 51
        L358 72
        L397 48
        L431 60
        L468 44
        L503 70
        L546 50
        L579 63
        L617 41
        L652 68
        L696 53
        L731 74
        L773 46
        L815 62
        L852 43
        L891 71
        L932 49
        L971 65
        L1012 42
        L1051 69
        L1094 52
        L1131 74
        L1176 47
        L1218 63
        L1257 41
        L1298 70
        L1344 50
        L1381 66
        L1425 44
        L1463 72
        L1507 48
        L1548 62
        L1589 43
        L1632 68
        L1674 51
        L1714 70
        L1756 48
        L1800 61
        L1800 96
        Z
      "
    />

    <path
      class="torn-edge-main"
      :fill="edgeColor"
      :filter="`url(#${filterId})`"
      d="
        M0 96
        L0 48
        L24 54
        L49 39
        L76 57
        L109 44
        L135 63
        L169 38
        L202 51
        L238 34
        L269 61
        L305 43
        L337 67
        L374 40
        L408 54
        L445 32
        L479 64
        L521 42
        L556 58
        L593 31
        L630 62
        L673 45
        L708 69
        L751 37
        L792 56
        L830 33
        L868 66
        L911 40
        L949 59
        L991 30
        L1030 64
        L1073 43
        L1110 70
        L1153 36
        L1197 57
        L1234 32
        L1275 65
        L1319 41
        L1358 60
        L1401 34
        L1441 68
        L1485 39
        L1525 55
        L1567 31
        L1609 63
        L1650 44
        L1691 67
        L1733 37
        L1771 56
        L1800 45
        L1800 96
        Z
      "
    />
  </svg>
</template>

<style scoped lang="scss">
.torn-edge {
  position: absolute;
  left: 0;
  display: block;
  width: 100%;
  height: clamp(44px, 3.8vw, 72px);
  pointer-events: none;
  overflow: visible;
  z-index: 5;
}

.torn-edge-top {
  top: -1px;
  transform: translateY(-50%);
}

.torn-edge-bottom {
  bottom: -1px;
  transform: translateY(50%) rotate(180deg);
}

.torn-edge-shadow {
  filter: blur(2px);
  opacity: 0.7;
}

.torn-edge-main {
  opacity: 1;
}
</style>
