<script setup lang="ts">
import { ref } from 'vue'
import PaperSection from '@/components/common/PaperSection.vue'
import type { StageType } from '../types/events'
import RegularStandings from './RegularStandings.vue'
import PlayoffBracket from './PlayoffBracket.vue'
import CompetitionFormat from './CompetitionFormat.vue'
import LeagueAbout from './LeagueAbout.vue'
import StageProgressHeader from './StageProgressHeader.vue'

const selectedStage = ref<StageType>('regular')
</script>

<template>
  <div class="league-stage">
    <template v-if="selectedStage === 'regular'">
      <div class="site-shell">
        <section class="events-section-dark stage-main">
          <div class="site-shell-inner">
            <StageProgressHeader
              :selected-stage="selectedStage"
              variant="dark"
              @update:selected-stage="selectedStage = $event"
            />
            <RegularStandings />
          </div>
        </section>
      </div>

      <CompetitionFormat current-stage="regular" />
    </template>

    <template v-else>
      <div class="site-shell stage-playoffs-shell">
        <PaperSection edge="top" tone="gray" class="stage-progress-section">
          <div class="stage-progress-section__inner">
            <StageProgressHeader
              :selected-stage="selectedStage"
              variant="paper"
              @update:selected-stage="selectedStage = $event"
            />
            <PlayoffBracket />
          </div>
        </PaperSection>

        <PaperSection edge="bottom" tone="gray" class="competition-format-section">
          <div class="stage-section-divider" aria-hidden="true" />
          <CompetitionFormat embedded current-stage="playoffs" />
        </PaperSection>
      </div>
    </template>

    <LeagueAbout />
  </div>
</template>

<style scoped lang="scss">
.stage-main {
  padding: 48px 0 24px;
}

.stage-playoffs-shell {
  display: flex;
  flex-direction: column;
}

.stage-progress-section,
.competition-format-section {
  margin-top: 0;
}

.stage-progress-section__inner {
  padding:
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(18px, 1.6vw, 28px);
}

.stage-section-divider {
  height: 1px;
  margin:
    0
    clamp(28px, 3vw, 52px)
    clamp(4px, 0.5vw, 8px);
  background:
    linear-gradient(
      90deg,
      transparent 0%,
      rgba(17, 17, 17, 0.28) 8%,
      rgba(17, 17, 17, 0.28) 92%,
      transparent 100%
    );
}

@media (max-width: 768px) {
  .stage-progress-section__inner {
    padding-left: 16px;
    padding-right: 16px;
  }

  .stage-section-divider {
    margin-inline: 16px;
  }
}
</style>
