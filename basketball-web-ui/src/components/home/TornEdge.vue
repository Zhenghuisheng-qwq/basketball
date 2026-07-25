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
    default: 14,
    soft: 10,
    sharp: 20,
    wave: 12,
  }
  return map[props.variant] ?? 14
})

const filterId = computed(
  () => `rough-edge-${props.position}-${props.tone}-${props.variant}`,
)

const mainPath = computed(() => {
  if (props.variant === 'wave') {
    return `
      M0 96 L0 44
      C 28 72, 52 20, 82 58 C 108 82, 128 18, 162 52 C 192 76, 206 14, 240 48
      C 274 72, 288 24, 318 38 C 348 54, 368 12, 402 50 C 434 74, 448 26, 478 42
      C 512 66, 524 16, 558 48 C 594 72, 608 22, 638 36 C 672 56, 688 10, 722 52
      C 754 76, 768 24, 798 40 C 832 64, 848 14, 882 50 C 914 74, 928 22, 958 38
      C 992 62, 1008 12, 1042 48 C 1074 72, 1088 24, 1118 36 C 1152 58, 1168 16, 1202 52
      C 1234 76, 1248 22, 1278 40 C 1312 64, 1328 12, 1362 48 C 1394 72, 1408 24, 1438 38
      C 1472 62, 1488 14, 1522 50 C 1554 74, 1568 22, 1598 36 C 1632 58, 1648 10, 1682 48
      C 1714 72, 1731 26, 1764 42 C 1782 54, 1792 36, 1800 46
      L1800 96 Z
    `
  }

  if (props.variant === 'sharp') {
    return `
      M0 96 L0 42
      C 14 60, 22 22, 40 44 L 56 8 L 72 52 L 90 18 C 104 40, 114 74, 134 50
      L 150 14 L 168 62 L 188 26 C 202 48, 214 10, 232 46 L 248 72 L 268 30
      L 286 58 C 300 78, 312 34, 330 16 L 346 54 L 366 20 L 384 68 C 398 84, 410 40, 428 22
      L 444 60 L 464 12 L 482 50 C 496 70, 508 28, 526 46 L 542 78 L 562 34 L 580 14
      C 594 56, 606 82, 624 58 L 640 20 L 660 64 L 678 28 C 692 48, 704 8, 722 44
      L 738 74 L 758 30 L 776 56 C 790 76, 802 32, 820 14 L 836 52 L 856 18 L 874 66
      C 888 84, 900 40, 918 22 L 934 60 L 954 12 L 972 50 C 986 72, 998 28, 1016 46
      L 1032 78 L 1052 34 L 1070 16 C 1084 54, 1096 80, 1114 56 L 1130 20 L 1150 64
      L 1168 28 C 1182 48, 1194 10, 1212 46 L 1228 72 L 1248 30 L 1266 58 C 1280 78, 1292 34, 1310 16
      L 1326 54 L 1346 20 L 1364 68 C 1378 84, 1390 40, 1408 22 L 1424 60 L 1444 12
      L 1462 50 C 1476 70, 1488 28, 1506 46 L 1522 78 L 1542 34 L 1560 14 C 1574 56, 1586 82, 1604 58
      L 1620 20 L 1640 64 L 1658 28 C 1672 48, 1684 10, 1702 46 L 1718 74 L 1738 30
      L 1758 56 C 1772 74, 1786 36, 1800 44 L1800 96 Z
    `
  }

  if (props.variant === 'soft') {
    return `
      M0 96 L0 48
      C 28 68, 54 32, 86 58 C 116 76, 142 28, 176 54 C 208 72, 226 22, 260 50
      C 294 68, 312 30, 346 52 C 380 70, 400 26, 434 56 C 466 74, 484 32, 518 50
      C 552 68, 572 24, 606 54 C 638 72, 654 28, 688 52 C 722 70, 742 30, 776 56
      C 808 74, 824 26, 858 50 C 892 68, 912 32, 946 54 C 978 72, 994 24, 1028 52
      C 1062 70, 1082 28, 1116 56 C 1148 74, 1164 30, 1198 50 C 1232 68, 1252 32, 1286 54
      C 1318 72, 1334 24, 1368 52 C 1402 70, 1422 28, 1456 56 C 1488 74, 1504 30, 1538 50
      C 1572 68, 1592 32, 1626 54 C 1658 72, 1674 24, 1708 52 C 1742 70, 1766 36, 1800 54
      L1800 96 Z
    `
  }

  return `
    M0 96 L0 40
    C 18 62, 30 20, 52 54 C 70 78, 82 34, 104 60 C 120 74, 130 12, 156 48
    C 178 70, 188 30, 210 66 C 226 82, 240 40, 262 22 C 280 8, 292 56, 312 72
    C 330 84, 342 44, 364 26 C 384 14, 396 62, 418 78 C 434 88, 448 38, 470 18
    C 490 6, 502 54, 524 70 C 542 82, 554 28, 576 50 C 596 66, 608 16, 632 42
    C 654 62, 664 88, 688 68 C 708 52, 720 14, 744 40 C 766 62, 778 22, 800 58
    C 818 74, 830 34, 852 20 C 872 8, 884 56, 906 74 C 924 86, 936 40, 958 24
    C 978 12, 990 60, 1012 76 C 1030 88, 1042 46, 1064 28 C 1084 14, 1096 62, 1118 44
    C 1138 30, 1150 82, 1174 62 C 1194 46, 1206 12, 1230 38 C 1252 58, 1262 88, 1286 66
    C 1306 50, 1318 16, 1342 42 C 1364 62, 1376 22, 1398 56 C 1416 70, 1428 30, 1450 18
    C 1470 8, 1482 58, 1504 74 C 1522 86, 1534 42, 1556 24 C 1576 12, 1588 60, 1610 44
    C 1630 30, 1642 80, 1666 58 C 1686 42, 1698 14, 1722 40 C 1744 60, 1758 24, 1780 52
    C 1790 64, 1795 44, 1800 50 L1800 96 Z
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
          baseFrequency="0.018 0.32"
          numOctaves="6"
          :seed="turbulenceSeed"
          result="noise"
        />
        <feDisplacementMap
          in="SourceGraphic"
          in2="noise"
          :scale="displacementScale"
          xChannelSelector="R"
          yChannelSelector="G"
          result="displaced"
        />
        <feTurbulence
          type="turbulence"
          baseFrequency="0.08 0.6"
          numOctaves="3"
          :seed="turbulenceSeed + 11"
          result="noise2"
        />
        <feDisplacementMap
          in="displaced"
          in2="noise2"
          :scale="displacementScale * 0.5"
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
  height: clamp(48px, 4vw, 72px);
}

.torn-edge-wave {
  height: clamp(44px, 3.6vw, 68px);
}
</style>
