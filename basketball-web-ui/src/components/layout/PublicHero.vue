<script setup lang="ts">
import { computed } from 'vue'
import tournamentTop from '@/assets/icon/tournament_top.png'
import dataHeroBg from '@/assets/icon/data_hero_bg.png'

export type PublicHeroVariant = 'competition' | 'data'

const props = withDefaults(
  defineProps<{
    variant: PublicHeroVariant
    ariaLabel: string
    backgroundImage?: string
  }>(),
  {},
)

const resolvedBackground = computed(() => {
  if (props.backgroundImage) return props.backgroundImage
  return props.variant === 'data' ? dataHeroBg : tournamentTop
})
</script>

<template>
  <section
    class="public-hero"
    :class="`public-hero--${variant}`"
    :aria-label="ariaLabel"
  >
    <div class="site-shell public-hero__shell">
      <div class="public-hero__frame">
        <img
          class="public-hero__image"
          :src="resolvedBackground"
          :alt="ariaLabel"
          draggable="false"
        />
        <slot />
      </div>

      <slot name="after" />
    </div>
  </section>
</template>
