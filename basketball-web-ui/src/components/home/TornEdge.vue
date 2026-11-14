<script setup lang="ts">
import { computed, useId } from 'vue'

type TornEdgePosition = 'top' | 'bottom'
type TornEdgeTone = 'light' | 'dark' | 'gray'
type TornEdgeVariant = 'default' | 'soft' | 'sharp' | 'wave'

const props = withDefaults(
  defineProps<{
    position?: TornEdgePosition
    tone?: TornEdgeTone
    variant?: TornEdgeVariant
  }>(),
  {
    position: 'top',
    tone: 'light',
    variant: 'default',
  },
)

const edgeColor = computed(() => {
  if (props.tone === 'gray') return 'var(--theme-content, #e7e5e5)'
  return props.tone === 'light' ? 'var(--theme-content, #e9e7e7)' : 'var(--theme-bg, #050505)'
})

const shadowColor = computed(() => {
  return props.tone === 'dark' ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.24)'
})

const fiberColor = computed(() => {
  return props.tone === 'dark' ? 'rgba(255, 255, 255, 0.12)' : 'rgba(255, 255, 255, 0.72)'
})

const turbulenceSeed = computed(() => {
  const seeds: Record<TornEdgeVariant, number> = {
    default: props.position === 'top' ? 31 : 57,
    soft: props.position === 'top' ? 14 : 68,
    sharp: props.position === 'top' ? 42 : 91,
    wave: props.position === 'top' ? 73 : 26,
  }

  return seeds[props.variant]
})

const displacementScale = computed(() => {
  const scales: Record<TornEdgeVariant, number> = {
    default: 4.8,
    soft: 3.2,
    sharp: 6.8,
    wave: 4.2,
  }

  return scales[props.variant]
})

const instanceId = useId().replace(/[^a-zA-Z0-9_-]/g, '')
const filterId = computed(
  () => `paper-edge-${instanceId}-${props.position}-${props.tone}-${props.variant}`,
)

const edgeLinePath = computed(() => {
  if (props.variant === 'soft') {
    return `
      M0 40
      C 42 32, 76 44, 122 36 C 168 28, 208 46, 258 37
      C 304 30, 342 43, 390 35 C 438 26, 478 47, 528 39
      C 574 31, 614 44, 662 34 C 710 27, 752 46, 802 38
      C 850 30, 890 45, 938 36 C 986 27, 1028 46, 1078 38
      C 1126 30, 1166 44, 1214 35 C 1262 27, 1304 47, 1354 39
      C 1402 31, 1442 43, 1490 34 C 1538 26, 1580 46, 1630 38
      C 1678 30, 1718 43, 1764 36 C 1780 34, 1792 38, 1800 35
    `
  }

  if (props.variant === 'sharp') {
    return `
      M0 41 L28 35 L61 42 L94 29 L124 38 L158 32 L191 45 L225 21
      L258 39 L292 30 L326 43 L359 35 L394 18 L428 41 L463 31 L497 45
      L532 27 L566 39 L600 32 L635 44 L670 23 L704 40 L739 34 L774 46
      L808 29 L843 39 L878 20 L912 42 L947 31 L982 45 L1017 27 L1052 40
      L1087 32 L1122 44 L1157 22 L1192 41 L1227 33 L1262 46 L1297 28 L1332 39
      L1367 19 L1402 42 L1437 31 L1472 45 L1507 26 L1542 40 L1577 33 L1612 44
      L1647 23 L1682 41 L1717 31 L1752 45 L1778 29 L1800 37
    `
  }

  if (props.variant === 'wave') {
    return `
      M0 39
      C 52 24, 108 48, 170 36 C 232 23, 286 47, 348 34
      C 410 21, 466 49, 528 38 C 590 25, 644 45, 706 33
      C 768 22, 824 48, 886 37 C 948 24, 1002 46, 1064 34
      C 1126 22, 1182 49, 1244 37 C 1306 24, 1360 46, 1422 34
      C 1484 21, 1540 48, 1602 38 C 1664 25, 1718 45, 1768 34
      C 1782 31, 1792 38, 1800 35
    `
  }

  return `
    M0 40 L32 35 L68 42 L103 31 L138 39 L174 28 L210 38 L247 34
    L283 45 L319 30 L355 40 L392 25 L428 42 L465 32 L501 39 L538 29
    L574 44 L611 34 L647 27 L684 41 L720 31 L757 39 L793 28 L830 44
    L866 34 L903 24 L939 41 L976 31 L1012 45 L1049 29 L1085 39
    L1122 27 L1158 43 L1195 34 L1231 40 L1268 29 L1304 42 L1341 25
    L1377 41 L1414 31 L1450 45 L1487 28 L1523 39 L1560 32 L1596 43
    L1633 26 L1669 40 L1706 31 L1742 44 L1773 29 L1800 37
  `
})

const mainPath = computed(() => `${edgeLinePath.value} L1800 80 L0 80 Z`)
</script>

<template>
  <svg
    class="torn-edge"
    :class="[`torn-edge-${position}`, `torn-edge-${tone}`, `torn-edge-${variant}`]"
    viewBox="0 0 1800 80"
    preserveAspectRatio="none"
    aria-hidden="true"
  >
    <defs>
      <filter
        :id="filterId"
        x="-2%"
        y="-25%"
        width="104%"
        height="150%"
        color-interpolation-filters="sRGB"
      >
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.011 0.15"
          numOctaves="4"
          :seed="turbulenceSeed"
          result="noise"
        />
        <feDisplacementMap
          in="SourceGraphic"
          in2="noise"
          :scale="displacementScale"
          xChannelSelector="R"
          yChannelSelector="G"
        />
      </filter>
    </defs>

    <path class="torn-edge-shadow" :fill="shadowColor" :d="mainPath" />
    <path
      class="torn-edge-main"
      :fill="edgeColor"
      :filter="`url(#${filterId})`"
      :d="mainPath"
    />
    <path
      class="torn-edge-fiber"
      :stroke="fiberColor"
      :d="edgeLinePath"
      vector-effect="non-scaling-stroke"
    />
  </svg>
</template>

<style scoped lang="scss">
.torn-edge {
  position: absolute;
  left: 0;
  display: block;
  width: 100%;
  height: var(--cnpa-paper-edge-height, clamp(34px, 2.5vw, 48px));
  pointer-events: none;
  overflow: visible;
  z-index: 5;
}

.torn-edge-top {
  top: 0;
  transform: translateY(-50%);
}

.torn-edge-bottom {
  bottom: 0;
  transform: translateY(50%) rotate(180deg);
}

.torn-edge-shadow {
  opacity: 0.38;
  filter: blur(1.75px);
  transform: translateY(3px);
}

.torn-edge-main {
  opacity: 1;
}

.torn-edge-fiber {
  fill: none;
  stroke-width: 1.05;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.72;
}
</style>
