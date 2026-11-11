<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { iconAssets } from '@/constants/iconAssets'
import { usePublicPageHero, visibleHeroImage } from '@/composables/usePublicPageHero'

const { t } = useI18n()
const ssggTopBackground = iconAssets['ssgg/ssgg_top_background.png']
const pageHero = usePublicPageHero('ANNOUNCEMENT')
const heroSrc = computed(() => visibleHeroImage(
  pageHero.settled.value,
  pageHero.configured.value,
  pageHero.imageUrl.value,
  ssggTopBackground,
))
const heroStyle = computed(() => (
  pageHero.configured.value ? { objectPosition: pageHero.objectPosition.value } : undefined
))

withDefaults(
  defineProps<{
    variant?: 'list' | 'detail'
  }>(),
  {
    variant: 'list',
  },
)
</script>

<template>
  <section
    class="announcement-hero-section"
    :aria-label="variant === 'detail' ? t('publicPages.announcement.detailAria') : t('publicPages.announcement.centerAria')"
  >
    <div class="site-shell">
      <div class="announcement-hero__frame cnpa-public-hero-frame">
        <img
          v-if="heroSrc"
          class="announcement-hero__image"
          :src="heroSrc"
          :alt="pageHero.altText.value || t('publicPages.announcement.centerAria')"
          :style="heroStyle"
          draggable="false"
        />
        <div
          v-if="pageHero.configured.value && !pageHero.fullImage.value"
          class="announcement-hero__mask"
          :style="{ background: `rgba(0, 0, 0, ${pageHero.maskOpacity.value / 100})` }"
          aria-hidden="true"
        />
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.announcement-hero-section {
  width: 100%;
  background: #000;
  overflow: hidden;
}

.announcement-hero__frame {
  background: #000;
}

.announcement-hero__mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}
</style>
