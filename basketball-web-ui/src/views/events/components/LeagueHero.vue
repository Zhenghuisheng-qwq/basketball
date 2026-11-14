<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import LeagueHeroMeta from './LeagueHeroMeta.vue'
import { useLeagueHeroMeta } from '../composables/useLeagueHeroMeta'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { resolveCompetitionTheme } from '@/config/competitionTheme'
import { usePublicPageHero } from '@/composables/usePublicPageHero'

function routeText(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  return String(raw ?? '').trim()
}

function firstKnownSeriesCode(...candidates: Array<string | null | undefined>) {
  for (const candidate of candidates) {
    const normalized = String(candidate || '').trim().toUpperCase()
    if (normalized === 'LEAGUE' || normalized === 'ARENA' || normalized === 'CUP' || normalized === 'OPEN') {
      return normalized
    }
  }
  return ''
}

const route = useRoute()
const { t } = useI18n()
const { heroMetaItems } = useLeagueHeroMeta()
const seasonHeroId = computed(() => routeText(route.params.seasonId) || routeText(route.query.seasonId))
const ctx = inject<ReturnType<typeof usePublicSeasonContext> | null>('publicSeasonContext', null)
const seriesLabel = computed(
  () => ctx?.currentTournament.value?.tournamentName || ctx?.divisionDetail.value?.seriesName || t('events.title'),
)
const explicitSeriesCode = computed(() => firstKnownSeriesCode(
  routeText(route.query.series ?? route.query.seriesCode),
  ctx?.divisionDetail.value?.seriesCode,
  ctx?.seasonDetail.value?.seriesCode,
  ctx?.currentTournament.value?.tournamentNo,
))
const seriesCode = computed(() => explicitSeriesCode.value || 'LEAGUE')
const pageHero = usePublicPageHero('EVENT_SEASON', seasonHeroId)
const seriesHeroKey = computed(() => (explicitSeriesCode.value ? `EVENT_${explicitSeriesCode.value}` : ''))
const seriesHero = usePublicPageHero(seriesHeroKey)
const heroTheme = computed(() => resolveCompetitionTheme(seriesCode.value))
const seasonHeroActive = computed(() => Boolean(
  seasonHeroId.value && pageHero.settled.value && pageHero.configured.value && pageHero.imageUrl.value,
))
const seriesHeroActive = computed(() => Boolean(
  seriesHero.settled.value && seriesHero.configured.value && seriesHero.imageUrl.value,
))
const heroReady = computed(() => pageHero.settled.value && seriesHero.settled.value)
const seriesLookupFinished = computed(() => {
  if (!seasonHeroId.value) return true
  const state = ctx?.overviewState.value
  return state === 'success' || state === 'empty' || state === 'error'
})
const heroImage = computed(() => {
  if (!heroReady.value) return ''
  if (seasonHeroActive.value) return pageHero.imageUrl.value
  if (seriesHeroActive.value) return seriesHero.imageUrl.value
  if (explicitSeriesCode.value || seriesLookupFinished.value) return heroTheme.value.topImage
  return ''
})
const heroPosition = computed(() => {
  if (seasonHeroActive.value) return pageHero.objectPosition.value
  if (seriesHeroActive.value) return seriesHero.objectPosition.value
  return heroTheme.value.backgroundPosition
})
const heroAlt = computed(() => pageHero.altText.value || seriesHero.altText.value || seriesLabel.value)
const showHeroMeta = computed(() => {
  if (seasonHeroActive.value) return !pageHero.fullImage.value
  if (seriesHeroActive.value) return !seriesHero.fullImage.value
  return true
})
const heroOverlayStyle = computed(() => {
  if (seasonHeroActive.value) {
    return pageHero.fullImage.value
      ? { background: 'transparent' }
      : { background: `rgba(0, 0, 0, ${pageHero.maskOpacity.value / 100})` }
  }
  if (seriesHeroActive.value) {
    return seriesHero.fullImage.value
      ? { background: 'transparent' }
      : { background: `rgba(0, 0, 0, ${seriesHero.maskOpacity.value / 100})` }
  }
  return { background: heroTheme.value.overlay }
})
</script>
<template>
  <section class="league-hero" :aria-label="seriesLabel">
    <img
      v-if="heroImage"
      class="league-hero__image"
      :src="heroImage"
      :alt="heroAlt"
      :style="{ objectPosition: heroPosition }"
    />
    <div class="league-hero__overlay" :style="heroOverlayStyle" aria-hidden="true" />

    <LeagueHeroMeta v-if="showHeroMeta" class="league-hero__meta" :items="heroMetaItems" />
  </section>
</template>
<style scoped lang="scss">
.league-hero {
  --hero-meta-right: 3.8%;
  --hero-meta-bottom: 5%;
  --hero-meta-width: 43%;

  position: relative;
  width: 100%;
  aspect-ratio: 21 / 9;
  min-height: 280px;
  max-height: 760px;
  overflow: hidden;
  background: #050505;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 2px 2px 0 0;
  box-sizing: border-box;
}

.league-hero__image {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
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
    aspect-ratio: 16 / 9;
    min-height: 340px;
    max-height: none;
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

@media (max-width: 767px) {
  .league-hero {
    aspect-ratio: auto;
    height: clamp(380px, 88vw, 520px);
    min-height: 380px;
    max-height: none;
  }
}
</style>
