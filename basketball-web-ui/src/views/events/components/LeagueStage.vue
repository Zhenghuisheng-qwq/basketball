<script setup lang="ts">
import { ref } from 'vue'
import type { StageType } from '../types/events'
import RegularStandings from './RegularStandings.vue'
import PlayoffBracket from './PlayoffBracket.vue'
import CompetitionFormat from './CompetitionFormat.vue'
import LeagueAbout from './LeagueAbout.vue'

const selectedStage = ref<StageType>('regular')

const stageOptions: Array<{ key: StageType; label: string }> = [
  { key: 'regular', label: '常规赛' },
  { key: 'playoffs', label: '季后赛' },
]
</script>

<template>
  <div class="league-stage">
    <section class="events-section events-section-dark stage-main">
      <div class="events-inner">
        <div class="stage-head">
          <div>
            <h2 class="section-head-title">赛事进度</h2>
            <span class="section-head-sub">Stage Progress</span>
          </div>

          <div class="stage-switch">
            <button
              v-for="option in stageOptions"
              :key="option.key"
              type="button"
              class="stage-btn"
              :class="{ active: selectedStage === option.key }"
              @click="selectedStage = option.key"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <RegularStandings v-if="selectedStage === 'regular'" />
        <PlayoffBracket v-else />
      </div>
    </section>

    <CompetitionFormat :current-stage="selectedStage === 'regular' ? 'regular' : 'playoffs'" />
    <LeagueAbout />
  </div>
</template>

<style scoped lang="scss">
.stage-main {
  padding: 48px 0 24px;
}

.stage-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.stage-switch {
  display: inline-flex;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.stage-btn {
  min-width: 96px;
  height: 40px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &.active {
    background: var(--cnpa-red);
    color: #fff;
  }

  &:hover:not(.active) {
    color: #fff;
    background: rgba(225, 6, 19, 0.12);
  }
}

@media (max-width: 768px) {
  .stage-head {
    flex-direction: column;
  }
}
</style>
