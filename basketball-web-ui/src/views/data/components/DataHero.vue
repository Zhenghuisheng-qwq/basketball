<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import dataHeroBg from '@/assets/icon/data_hero_bg.png'
import { usePublicPageHero } from '@/composables/usePublicPageHero'
import type { TournamentGroup } from '../types'
import TournamentSelector from './TournamentSelector.vue'

defineProps<{
  tournamentId: string
  groups: TournamentGroup[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:tournamentId': [value: string]
}>()

const { t } = useI18n()
const pageHero = usePublicPageHero('DATA')
const heroSrc = computed(() => (pageHero.configured.value ? pageHero.imageUrl.value : dataHeroBg))
const heroStyle = computed(() => (
  pageHero.configured.value ? { objectPosition: pageHero.objectPosition.value } : undefined
))

function handleTournamentChange(value: string) {
  emit('update:tournamentId', value)
}
</script>
<template>
  <section class="data-hero" :aria-label="t('publicPages.data.heroAria')">
    <div class="data-hero__visual cnpa-public-hero-frame">
      <img
        class="data-hero__image"
        :src="heroSrc"
        :alt="pageHero.altText.value || t('publicPages.data.heroAria')"
        :style="heroStyle"
        draggable="false"
      />
      <div
        v-if="pageHero.configured.value && !pageHero.fullImage.value"
        class="data-hero__mask"
        :style="{ background: `rgba(0, 0, 0, ${pageHero.maskOpacity.value / 100})` }"
        aria-hidden="true"
      />
      <div class="data-hero__selector">
        <TournamentSelector
          :model-value="tournamentId"
          :groups="groups"
          :loading="loading"
          :error="error"
          @update:model-value="handleTournamentChange"
        />
      </div>
    </div>
  </section>
</template>
<style scoped lang="scss">
.data-hero {
  width: 100%;
  background: #000;
  overflow: visible;
}

.data-hero__visual {
  overflow: visible;
}

.data-hero__image {
  pointer-events: none;
}

.data-hero__mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.data-hero__selector {
  position: absolute;
  top: clamp(18px, 3.2vw, 30px);
  right: clamp(12px, 2.4vw, 28px);
  z-index: 40;
  width: min(100%, 254px);
}

@media (max-width: 767px) {
  .data-hero__selector {
    position: absolute;
    top: 12px;
    right: 10px;
    left: auto;
    width: min(68%, 220px);
  }
}
</style>