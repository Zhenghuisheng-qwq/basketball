<script setup lang="ts">
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
  <section class="public-hero v4-data-hero" aria-label="CNPA 赛事数据">
    <div class="public-hero__bg" aria-hidden="true" />
    <div class="public-hero__overlay" aria-hidden="true" />

    <div class="site-shell public-hero__inner">
      <nav class="public-hero__breadcrumb" aria-label="面包屑">
        <router-link to="/" class="public-hero__crumb">首页</router-link>
        <span class="public-hero__crumb-sep">/</span>
        <span class="public-hero__crumb public-hero__crumb--current">数据</span>
      </nav>

      <p class="public-hero__eyebrow">DATA CENTER</p>
      <h1 class="public-hero__title">赛事数据</h1>
      <p class="public-hero__desc">
        查看各赛事赛区的积分排名、球队数据、球员数据与排行榜。
      </p>

      <div v-if="loading" class="public-hero__state">加载中...</div>
      <div v-else-if="error" class="public-hero__state public-hero__state--error">{{ error }}</div>
      <div v-else class="public-hero__filters">
        <label class="public-hero__filter-field">
          <span class="public-hero__filter-label">赛季</span>
          <select
            class="public-hero__select"
            :value="selectedSeasonId"
            @change="emit('update:selectedSeasonId', ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="season in seasons" :key="season.seasonId" :value="season.seasonId">
              {{ v4SeriesLabel(season.seriesCode) }} · {{ displayText(season.displayTitle) }}
            </option>
          </select>
        </label>
        <label class="public-hero__filter-field">
          <span class="public-hero__filter-label">平台赛区</span>
          <select
            class="public-hero__select"
            :value="selectedDivisionId"
            @change="emit('update:selectedDivisionId', ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="division in divisions" :key="division.divisionId" :value="division.divisionId">
              {{ v4PlatformLabel(division.platformCode) }} · {{ displayText(division.divisionName) }}
            </option>
          </select>
        </label>
      </div>
    </div>
  </section>
</template>
