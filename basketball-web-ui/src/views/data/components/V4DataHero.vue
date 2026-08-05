<script setup lang="ts">
import PublicHero from '@/components/layout/PublicHero.vue'
import type { V4PublicDivisionBrief, V4PublicSeasonItem } from '@/api/types/v4Public'
import { displayText } from '@/utils/publicDisplay'
import { v4PlatformLabel, v4SeriesLabel } from '@/utils/v4Display'

defineProps<{
  loading: boolean
  error: string
  seasons: V4PublicSeasonItem[]
  selectedSeasonId: string
  selectedDivisionId: string
  divisions: V4PublicDivisionBrief[]
}>()

const emit = defineEmits<{
  'update:selectedSeasonId': [value: string]
  'update:selectedDivisionId': [value: string]
}>()
</script>

<template>
  <PublicHero variant="data" aria-label="CNPA 数据中心">
    <div class="v4-data-hero__controls">
      <div v-if="loading" class="v4-data-hero__state">加载中...</div>
      <div v-else-if="error" class="v4-data-hero__state v4-data-hero__state--error">{{ error }}</div>
      <template v-else>
        <label class="v4-data-hero__field">
          <span class="sr-only">赛季</span>
          <select
            class="v4-data-hero__select"
            :value="selectedSeasonId"
            @change="emit('update:selectedSeasonId', ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="season in seasons" :key="season.seasonId" :value="season.seasonId">
              {{ v4SeriesLabel(season.seriesCode) }} · {{ displayText(season.displayTitle) }}
            </option>
          </select>
        </label>
        <label class="v4-data-hero__field">
          <span class="sr-only">平台赛区</span>
          <select
            class="v4-data-hero__select"
            :value="selectedDivisionId"
            @change="emit('update:selectedDivisionId', ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="division in divisions" :key="division.divisionId" :value="division.divisionId">
              {{ v4PlatformLabel(division.platformCode) }} · {{ displayText(division.divisionName) }}
            </option>
          </select>
        </label>
      </template>
    </div>
  </PublicHero>
</template>

<style scoped lang="scss">
.v4-data-hero__controls {
  position: absolute;
  top: clamp(18px, 3.2vw, 30px);
  right: clamp(12px, 2.4vw, 28px);
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(100%, 254px);
}

.v4-data-hero__field {
  display: block;
  width: 100%;
}

.v4-data-hero__select {
  display: block;
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #4a4a4a;
  border-radius: 6px;
  background: rgba(12, 12, 12, 0.92);
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  appearance: none;
  backdrop-filter: blur(8px) saturate(120%);
  -webkit-backdrop-filter: blur(8px) saturate(120%);

  &:focus-visible {
    outline: 2px solid rgba(255, 255, 255, 0.36);
    outline-offset: 2px;
  }
}

.v4-data-hero__state {
  padding: 10px 14px;
  border: 1px solid #4a4a4a;
  border-radius: 6px;
  background: rgba(12, 12, 12, 0.92);
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 700;

  &--error {
    color: #ffb4b4;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 767px) {
  .v4-data-hero__controls {
    top: 12px;
    right: 10px;
    width: min(68%, 220px);
  }
}
</style>
