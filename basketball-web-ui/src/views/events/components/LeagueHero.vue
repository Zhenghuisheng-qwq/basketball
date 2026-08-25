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
const seasonLabel = computed(
  () => ctx?.seasonDetail.value?.displayTitle || ctx?.currentSeason.value?.seasonName || 'CNPA 赛事',
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

    <div class="league-hero__title-block">
      <span class="league-hero__season">{{ seasonLabel }}</span>
      <h1 class="league-hero__title">{{ seriesLabel }}</h1>
      <p class="league-hero__subtitle" :style="{ color: heroTheme.accent }">
        {{ seriesCode }} · CHINA PRO-AM ASSOCIATION
      </p>
    </div>

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

.league-hero__title-block {
  position: absolute;
  left: clamp(24px, 4.6vw, 76px);
  top: 50%;
  z-index: 2;
  width: min(46%, 620px);
  color: #fff;
  transform: translateY(-52%);
  text-shadow: 0 3px 18px rgba(0, 0, 0, 0.5);
}

.league-hero__season {
  display: inline-block;
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.72);
  font-size: clamp(12px, 1vw, 15px);
  font-weight: 800;
}

.league-hero__title {
  margin: 0;
  font-size: clamp(32px, 4.2vw, 68px);
  line-height: 1.04;
  font-weight: 950;
  letter-spacing: 0;
}

.league-hero__subtitle {
  margin: 12px 0 0;
  color: #ff4b35;
  font-size: clamp(11px, 0.95vw, 14px);
  font-weight: 900;
  letter-spacing: 0.08em;
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

  .league-hero__title-block {
    top: 42%;
    width: 52%;
  }
}

@media (max-width: 640px) {
  .league-hero__title-block {
    left: 18px;
    top: 40%;
    width: 62%;
  }

  .league-hero__season {
    display: none;
  }

  .league-hero__subtitle {
    margin-top: 6px;
    font-size: 9px;
  }
}
</style>
