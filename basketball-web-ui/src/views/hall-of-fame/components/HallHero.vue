<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import mrtTopBackground from '@/assets/icon/mrt/mrt_top_background.png'

const { t } = useI18n()
</script>

<template>
  <section class="hall-hero-section" :aria-label="t('publicPages.hall.heroAria')">
    <div class="site-shell">
      <div class="hall-hero__frame cnpa-public-hero-frame">
        <img
          class="hall-hero__image"
          :src="mrtTopBackground"
          :alt="t('publicPages.hall.heroAria')"
          draggable="false"
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
</style>
