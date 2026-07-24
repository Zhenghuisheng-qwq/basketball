<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    position?: 'top' | 'bottom'
    tone?: 'light' | 'dark' | 'gray'
    variant?: 'default' | 'soft' | 'sharp' | 'wave'
  }>(),
  {
    position: 'top',
    tone: 'light',
    variant: 'default',
  },
)

const edgeColor = computed(() => {
  if (props.tone === 'gray') return '#e7e5e5'
  return props.tone === 'light' ? '#e9e7e7' : '#050505'
})

const shadowColor = computed(() => {
  return props.tone === 'light' || props.tone === 'gray'
    ? 'rgba(0, 0, 0, 0.18)'
    : 'rgba(255, 255, 255, 0.05)'
})

const turbulenceSeed = computed(() => {
  const map: Record<string, number> = {
    default: props.position === 'top' ? 31 : 57,
    soft: props.position === 'top' ? 14 : 68,
    sharp: props.position === 'top' ? 42 : 91,
    wave: props.position === 'top' ? 73 : 26,
  }
  return map[props.variant] ?? 31
})

const displacementScale = computed(() => {
  const map: Record<string, number> = {
    default: 6,
    soft: 4,
    sharp: 9,
    wave: 5,
  }
  return map[props.variant] ?? 6
})

const filterId = computed(
  () => `rough-edge-${props.position}-${props.tone}-${props.variant}`,
)

const mainPath = computed(() => {
  if (props.variant === 'wave') {
    return `
      M0 96 L0 48
      C 40 42, 80 58, 120 46 C 160 34, 200 56, 240 44 C 280 32, 320 54, 360 42
      C 400 30, 440 52, 480 40 C 520 28, 560 50, 600 38 C 640 26, 680 48, 720 36
      C 760 24, 800 46, 840 34 C 880 22, 920 44, 960 32 C 1000 20, 1040 42, 1080 30
      C 1120 18, 1160 40, 1200 28 C 1240 16, 1280 38, 1320 26 C 1360 14, 1400 36, 1440 24
      C 1480 12, 1520 34, 1560 22 C 1600 10, 1640 32, 1680 20 C 1720 8, 1760 30, 1800 18
      L1800 96 Z
    `
  }

  if (props.variant === 'sharp') {
    return `
      M0 96 L0 40
      C 22 46, 44 34, 66 42 C 88 50, 110 32, 132 40 C 154 48, 176 30, 198 38
      C 220 46, 242 28, 264 36 C 286 44, 308 26, 330 34 C 352 42, 374 24, 396 32
      C 418 40, 440 22, 462 30 C 484 38, 506 20, 528 28 C 550 36, 572 18, 594 26
      C 616 34, 638 16, 660 24 C 682 32, 704 14, 726 22 C 748 30, 770 12, 792 20
      C 814 28, 836 10, 858 18 C 880 26, 902 8, 924 16 C 946 24, 968 6, 990 14
      C 1012 22, 1034 4, 1056 12 C 1078 20, 1100 2, 1122 10 C 1144 18, 1166 0, 1188 8
      C 1210 16, 1232 0, 1254 8 C 1276 16, 1298 0, 1320 8 C 1342 16, 1364 0, 1386 8
      C 1408 16, 1430 0, 1452 8 C 1474 16, 1496 0, 1518 8 C 1540 16, 1562 0, 1584 8
      C 1606 16, 1628 0, 1650 8 C 1672 16, 1694 0, 1716 8 C 1738 16, 1760 0, 1782 8
      L1800 12 L1800 96 Z
    `
  }

  if (props.variant === 'soft') {
    return `
      M0 96 L0 52
      C 30 56, 60 48, 90 54 C 120 60, 150 46, 180 52 C 210 58, 240 44, 270 50
      C 300 56, 330 42, 360 48 C 390 54, 420 40, 450 46 C 480 52, 510 38, 540 44
      C 570 50, 600 36, 630 42 C 660 48, 690 34, 720 40 C 750 46, 780 32, 810 38
      C 840 44, 870 30, 900 36 C 930 42, 960 28, 990 34 C 1020 40, 1050 26, 1080 32
      C 1110 38, 1140 24, 1170 30 C 1200 36, 1230 22, 1260 28 C 1290 34, 1320 20, 1350 26
      C 1380 32, 1410 18, 1440 24 C 1470 30, 1500 16, 1530 22 C 1560 28, 1590 14, 1620 20
      C 1650 26, 1680 12, 1710 18 C 1740 24, 1770 10, 1800 16
      L1800 96 Z
    `
  }

  return `
    M0 96 L0 45
    C 18 49, 35 41, 52 47 C 68 53, 85 37, 103 44 C 121 51, 138 39, 156 46
    C 174 53, 191 35, 209 42 C 227 49, 244 41, 262 48 C 280 55, 297 37, 315 44
    C 333 51, 350 39, 368 47 C 386 55, 403 36, 421 43 C 439 50, 456 40, 474 47
    C 492 54, 509 38, 527 45 C 545 52, 562 39, 580 46 C 598 53, 615 37, 633 44
    C 651 51, 668 40, 686 47 C 704 54, 721 36, 739 43 C 757 50, 774 41, 792 48
    C 810 55, 827 37, 845 44 C 863 51, 880 39, 898 46 C 916 53, 933 38, 951 45
    C 969 52, 986 40, 1004 47 C 1022 54, 1039 36, 1057 43 C 1075 50, 1092 41, 1110 48
    C 1128 55, 1145 37, 1163 44 C 1181 51, 1198 39, 1216 46 C 1234 53, 1251 38, 1269 45
    C 1287 52, 1304 40, 1322 47 C 1340 54, 1357 36, 1375 43 C 1393 50, 1410 41, 1428 48
    C 1446 55, 1463 39, 1481 46 C 1499 53, 1516 37, 1534 44 C 1552 51, 1569 40, 1587 47
    C 1605 54, 1622 38, 1640 45 C 1658 52, 1675 39, 1693 46 C 1711 53, 1728 41, 1746 48
    C 1764 55, 1781 43, 1800 47 L1800 96 Z
  `
})
</script>

<template>
  <svg
    class="torn-edge"
    :class="[`torn-edge-${position}`, `torn-edge-${tone}`, `torn-edge-${variant}`]"
    viewBox="0 0 1800 96"
    preserveAspectRatio="none"
    aria-hidden="true"
  >
    <defs>
      <filter
        :id="filterId"
        x="-5%"
        y="-50%"
        width="110%"
        height="200%"
        color-interpolation-filters="sRGB"
      >
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.02 0.15"
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

    <path
      class="torn-edge-shadow"
      :fill="shadowColor"
      :filter="`url(#${filterId})`"
      :d="mainPath"
    />

    <path
      class="torn-edge-main"
      :fill="edgeColor"
      :filter="`url(#${filterId})`"
      :d="mainPath"
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
  filter: blur(3px);
  opacity: 0.5;
}

.torn-edge-top .torn-edge-shadow {
  transform: translateY(2px);
}

.torn-edge-bottom .torn-edge-shadow {
  transform: translateY(-2px);
}

.torn-edge-main {
  opacity: 1;
}

.torn-edge-sharp {
  height: clamp(36px, 3vw, 56px);
}

.torn-edge-wave {
  height: clamp(40px, 3.4vw, 64px);
}
</style>
