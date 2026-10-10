<script setup lang="ts">
import { computed, inject } from 'vue'
import { localizedText } from '@/utils/publicDisplay'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { pickCompetitionSeriesCode, resolveCompetitionTheme } from '@/config/competitionTheme'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'

const route = useRoute()
const { t } = useI18n()
const ctx = inject<ReturnType<typeof usePublicSeasonContext> | null>('publicSeasonContext', null)
const seriesLabel = computed(
  () => ctx?.currentTournament.value?.tournamentName || ctx?.divisionDetail.value?.seriesName || t('events.title'),
)
const aboutLabel = computed(() => t('eventsPage.about.label', { name: seriesLabel.value }))
const seriesCode = computed(() => {
  const querySeries = route.query.series ?? route.query.seriesCode
  return pickCompetitionSeriesCode(
    typeof querySeries === 'string' ? querySeries : '',
    ctx?.divisionDetail.value?.seriesCode,
    ctx?.seasonDetail.value?.seriesCode,
    ctx?.currentTournament.value?.tournamentNo,
  )
})
const aboutTheme = computed(() => resolveCompetitionTheme(seriesCode.value))
const description = computed(() => localizedText(
  ctx?.seasonDetail.value?.description,
  ctx?.seasonDetail.value?.descriptionEn,
  '',
))
</script>
<template>
  <section class="site-section site-section--dark league-about" :aria-label="aboutLabel">
    <div class="site-shell">
      <img
        class="league-about__image"
        :src="aboutTheme.footImage"
        :alt="aboutLabel"
      />
      <div v-if="description" class="league-about__copy">
        <span class="league-about__eyebrow">ABOUT THE EVENT</span>
        <h2>{{ t('eventsPage.about.title') }}</h2>
        <p>{{ description }}</p>
      </div>
    </div>
  </section>
</template>
<style scoped lang="scss">
.league-about {
  overflow: hidden;
}

.league-about__image {
  display: block;
  width: 100%;
  height: auto;
}

.league-about .site-shell {
  position: relative;
}

.league-about__copy {
  position: absolute;
  right: clamp(20px, 5vw, 84px);
  bottom: clamp(22px, 5vw, 72px);
  width: min(42%, 540px);
  padding: clamp(20px, 2.5vw, 34px);
  border-left: 3px solid var(--cnpa-red, #e10613);
  background: rgba(8, 8, 8, 0.76);
  color: #fff;
  box-sizing: border-box;
}

.league-about__eyebrow {
  color: #ff4b35;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.league-about__copy h2 {
  margin: 9px 0 0;
  font-size: clamp(24px, 2.2vw, 36px);
  font-weight: 950;
}

.league-about__copy p {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 14px;
  line-height: 1.8;
}

@media (max-width: 700px) {
  .league-about__copy {
    position: relative;
    right: auto;
    bottom: auto;
    width: 100%;
    padding: 22px 16px 26px;
    border-left: 0;
    border-top: 3px solid var(--cnpa-red, #e10613);
    background: #111;
  }
}
</style>
