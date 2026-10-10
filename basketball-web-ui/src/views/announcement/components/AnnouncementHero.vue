<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ssggTopBackground from '@/assets/icon/ssgg/ssgg_top_background.png'

const { t } = useI18n()

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
          class="announcement-hero__image"
          :src="ssggTopBackground"
          :alt="t('publicPages.announcement.centerAria')"
          draggable="false"
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
</style>
