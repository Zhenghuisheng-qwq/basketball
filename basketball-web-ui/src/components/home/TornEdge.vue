<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    position?: 'top' | 'bottom'
    tone?: 'light' | 'dark' | 'gray'
  }>(),
  {
    position: 'top',
    tone: 'light',
  },
)

const edgeColor = computed(() => {
  if (props.tone === 'gray') return '#ecebe7'
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
          :seed="position === 'top' ? 31 : 57"
          result="noise"
        />
        <feDisplacementMap
          in="SourceGraphic"
          in2="noise"
          scale="6"
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
        L0 50
        C 18 54, 35 46, 52 52
        C 68 58, 85 42, 103 49
        C 121 56, 138 44, 156 51
        C 174 58, 191 40, 209 47
        C 227 54, 244 46, 262 53
        C 280 60, 297 42, 315 49
        C 333 56, 350 44, 368 52
        C 386 60, 403 41, 421 48
        C 439 55, 456 45, 474 52
        C 492 59, 509 43, 527 50
        C 545 57, 562 44, 580 51
        C 598 58, 615 42, 633 49
        C 651 56, 668 45, 686 52
        C 704 59, 721 41, 739 48
        C 757 55, 774 46, 792 53
        C 810 60, 827 42, 845 49
        C 863 56, 880 44, 898 51
        C 916 58, 933 43, 951 50
        C 969 57, 986 45, 1004 52
        C 1022 59, 1039 41, 1057 48
        C 1075 55, 1092 46, 1110 53
        C 1128 60, 1145 42, 1163 49
        C 1181 56, 1198 44, 1216 51
        C 1234 58, 1251 43, 1269 50
        C 1287 57, 1304 45, 1322 52
        C 1340 59, 1357 41, 1375 48
        C 1393 55, 1410 46, 1428 53
        C 1446 60, 1463 44, 1481 51
        C 1499 58, 1516 42, 1534 49
        C 1552 56, 1569 45, 1587 52
        C 1605 59, 1622 43, 1640 50
        C 1658 57, 1675 44, 1693 51
        C 1711 58, 1728 46, 1746 53
        C 1764 60, 1781 48, 1800 52
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
        L0 45
        C 18 49, 35 41, 52 47
        C 68 53, 85 37, 103 44
        C 121 51, 138 39, 156 46
        C 174 53, 191 35, 209 42
        C 227 49, 244 41, 262 48
        C 280 55, 297 37, 315 44
        C 333 51, 350 39, 368 47
        C 386 55, 403 36, 421 43
        C 439 50, 456 40, 474 47
        C 492 54, 509 38, 527 45
        C 545 52, 562 39, 580 46
        C 598 53, 615 37, 633 44
        C 651 51, 668 40, 686 47
        C 704 54, 721 36, 739 43
        C 757 50, 774 41, 792 48
        C 810 55, 827 37, 845 44
        C 863 51, 880 39, 898 46
        C 916 53, 933 38, 951 45
        C 969 52, 986 40, 1004 47
        C 1022 54, 1039 36, 1057 43
        C 1075 50, 1092 41, 1110 48
        C 1128 55, 1145 37, 1163 44
        C 1181 51, 1198 39, 1216 46
        C 1234 53, 1251 38, 1269 45
        C 1287 52, 1304 40, 1322 47
        C 1340 54, 1357 36, 1375 43
        C 1393 50, 1410 41, 1428 48
        C 1446 55, 1463 39, 1481 46
        C 1499 53, 1516 37, 1534 44
        C 1552 51, 1569 40, 1587 47
        C 1605 54, 1622 38, 1640 45
        C 1658 52, 1675 39, 1693 46
        C 1711 53, 1728 41, 1746 48
        C 1764 55, 1781 43, 1800 47
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
</style>
