<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { iconAssets } from '@/constants/iconAssets'
import { usePublicPageHero, visibleHeroImage } from '@/composables/usePublicPageHero'

const { t } = useI18n()
const mrtTopBackground = iconAssets['mrt/mrt_top_background.png']
const pageHero = usePublicPageHero('HALL')
const heroSrc = computed(() => visibleHeroImage(
  pageHero.settled.value,
  pageHero.configured.value,
  pageHero.imageUrl.value,
  mrtTopBackground,
))
const heroStyle = computed(() => (
  pageHero.configured.value ? { objectPosition: pageHero.objectPosition.value } : undefined
))
</script>

<template>
  <section class="hall-hero-section" :aria-label="t('publicPages.hall.heroAria')">
    <div class="site-shell">
      <div class="hall-hero__frame cnpa-public-hero-frame">
        <img
          v-if="heroSrc"
          class="hall-hero__image"
          :src="heroSrc"
          :alt="pageHero.altText.value || t('publicPages.hall.heroAria')"
          :style="heroStyle"
          draggable="false"
        />
        <div
          v-if="pageHero.configured.value && !pageHero.fullImage.value"
          class="hall-hero__mask"
          :style="{ background: `rgba(0, 0, 0, ${pageHero.maskOpacity.value / 100})` }"
          aria-hidden="true"
        />
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.hall-hero-section {
  width: 100%;
  background: #000;
  overflow: hidden;
}

.hall-hero__frame {
  background: #000;
}

.hall-hero__mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}
</style>
