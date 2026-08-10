<script setup lang="ts">
import LeagueHero from '@/views/events/components/LeagueHero.vue'
import { computed, provide } from 'vue'
import { V4_SERIES_FILTERS } from '@/utils/v4Display'
import type { LeagueHeroMetaItem } from '@/views/events/types/events'

const props = defineProps<{
  activeSeries: string
  seasonCount: number
  registrationOpenDivisionCount: number
}>()

const emit = defineEmits<{
  'select-series': [code: string]
}>()

const heroMetaItems = computed<LeagueHeroMetaItem[]>(() => [
  { key: 'season', icon: 'calendar', title: '开放赛季', value: String(props.seasonCount) },
  { key: 'teams', icon: 'trophy', title: '报名中赛区', value: String(props.registrationOpenDivisionCount) },
  { key: 'stage', icon: 'stage', title: '支持平台', value: 'PC / PS-Xbox' },
  { key: 'platform', icon: 'gamepad', title: '比赛模式', value: '5V5 / 3V3' },
])

// 复用 master LeagueHeroMeta inject 契约
provide('publicSeasonContext', {
  heroMeta: heroMetaItems,
  overviewState: computed(() => 'success'),
  seasonState: computed(() => 'success'),
  tournamentState: computed(() => 'success'),
})
</script>

<template>
  <div class="v4-events-hero">
    <div class="site-shell">
      <LeagueHero />
      <nav class="v4-events-hero__tabs" role="tablist" aria-label="赛事系列筛选">
        <button
          v-for="item in V4_SERIES_FILTERS"
          :key="item.code || 'all'"
          type="button"
          role="tab"
          class="v4-events-hero__tab"
          :class="{ 'is-active': activeSeries === item.code }"
          :aria-selected="activeSeries === item.code"
          @click="emit('select-series', item.code)"
        >
          {{ item.label }}
        </button>
      </nav>
    </div>
  </div>
</template>

<style scoped lang="scss">
.v4-events-hero {
  width: 100%;
  background: #050505;
}

.v4-events-hero__tabs {
  display: inline-flex;
  align-items: stretch;
  width: fit-content;
  max-width: 100%;
  min-height: 64px;
  background: #080808;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-top: none;
  border-radius: 0 0 2px 2px;
  box-sizing: border-box;
}

.v4-events-hero__tab {
  position: relative;
  flex: 0 0 auto;
  min-width: clamp(88px, 8vw, 120px);
  padding: 0 clamp(20px, 2vw, 36px);
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;

  &:hover {
    color: #fff;
    background: rgba(225, 6, 19, 0.08);
  }

  &.is-active {
    color: #fff;
    background: linear-gradient(180deg, rgba(225, 6, 19, 0.28) 0%, rgba(8, 8, 8, 0.2) 100%);

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 3px;
      background: var(--cnpa-red, #e10613);
    }
  }
}

@media (max-width: 767px) {
  .v4-events-hero__tabs {
    display: flex;
    width: 100%;
    min-height: 56px;
    overflow-x: auto;
  }

  .v4-events-hero__tab {
    min-width: 72px;
    padding: 0 16px;
    font-size: 14px;
  }
}
</style>
