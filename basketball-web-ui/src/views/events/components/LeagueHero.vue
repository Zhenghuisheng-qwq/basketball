<script setup lang="ts">
import { computed, inject } from 'vue'
import LeagueHeroMeta from './LeagueHeroMeta.vue'
import { useLeagueHeroMeta } from '../composables/useLeagueHeroMeta'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { resolveCompetitionTheme } from '@/config/competitionTheme'

const { heroMetaItems } = useLeagueHeroMeta()
const ctx = inject<ReturnType<typeof usePublicSeasonContext> | null>('publicSeasonContext', null)
const seriesLabel = computed(
  () => ctx?.currentTournament.value?.tournamentName || ctx?.divisionDetail.value?.seriesName || '赛事',
)
const seriesCode = computed(
  () =>
    ctx?.divisionDetail.value?.seriesCode
    || ctx?.seasonDetail.value?.seriesCode
    || ctx?.currentTournament.value?.tournamentNo
    || 'CNPA',
)
const heroTheme = computed(() => resolveCompetitionTheme(seriesCode.value))
const heroImage = computed(
  () => ctx?.currentTournament.value?.coverUrl?.trim() || heroTheme.value.fallbackImage,
)
const heroOverlayStyle = computed(() => ({ background: heroTheme.value.overlay }))
</script>
<template>
  <section class="league-hero" :aria-label="seriesLabel">
    <img
      class="league-hero__image"
      :src="heroImage"
      :alt="seriesLabel"
      :style="{ objectPosition: heroTheme.backgroundPosition }"
    />
    <div class="league-hero__overlay" :style="heroOverlayStyle" aria-hidden="true" />

    <LeagueHeroMeta class="league-hero__meta" :items="heroMetaItems" />
  </section>
</template>
<style scoped lang="scss">
.league-hero {
  --hero-meta-right: 3.8%;
  --hero-meta-bottom: 5%;
  --hero-meta-width: 43%;

  position: relative;
  width: 100%;
  overflow: hidden;
  background: #050505;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 2px 2px 0 0;
  box-sizing: border-box;
}

.league-hero__image {
  position: relative;
  z-index: 0;
  display: block;
  width: 100%;
  height: auto;
  object-fit: cover;
}

.league-hero__overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.league-hero__meta {
  position: absolute;
  right: var(--hero-meta-right);
  left: auto;
  bottom: var(--hero-meta-bottom);
  z-index: 3;
  width: var(--hero-meta-width);
  height: auto;
  min-height: 0;
  box-sizing: border-box;
}

@media (min-width: 1600px) {
  .league-hero {
    --hero-meta-right: 3.8%;
    --hero-meta-bottom: 5%;
    --hero-meta-width: 42%;
  }
}

@media (min-width: 1366px) and (max-width: 1599px) {
  .league-hero {
    --hero-meta-right: 3.8%;
    --hero-meta-bottom: 4.8%;
    --hero-meta-width: 43%;
  }
}

@media (min-width: 1200px) and (max-width: 1365px) {
  .league-hero {
    --hero-meta-right: 3.6%;
    --hero-meta-bottom: 4.2%;
    --hero-meta-width: 48%;
  }
}

@media (min-width: 992px) and (max-width: 1199px) {
  .league-hero {
    --hero-meta-right: 3.4%;
    --hero-meta-bottom: 3.8%;
    --hero-meta-width: 52%;
  }
}

@media (max-width: 991px) {
  .league-hero {
    --hero-meta-right: 3.2%;
    --hero-meta-bottom: 3.5%;
    --hero-meta-width: min(92%, 520px);
    overflow: hidden;
  }

  .league-hero__meta {
    position: absolute;
    right: var(--hero-meta-right);
    left: auto;
    bottom: var(--hero-meta-bottom);
    width: var(--hero-meta-width);
  }

}
</style>
