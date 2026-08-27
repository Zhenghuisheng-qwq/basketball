<script setup lang="ts">
import { computed } from 'vue'
import { publicContestDetailPath, v4BestOfLabel, v4MatchSeriesStatusLabel } from '@/utils/v4Display'
import type { PlayoffSeries } from '../types/events'

const props = defineProps<{
  series: PlayoffSeries
  emphasize?: boolean
}>()

const seriesPath = computed(() => publicContestDetailPath({
  unitType: props.series.matchSeriesId ? 'MATCH_SERIES' : 'MATCH',
  matchId: props.series.matchId || (!props.series.matchSeriesId ? props.series.id : ''),
  matchSeriesId: props.series.matchSeriesId,
}))

const metaLabel = computed(() => v4BestOfLabel(props.series.bestOf))

const statusCode = computed(() => String(props.series.seriesStatus || '').toUpperCase())

const footer = computed(() => {
  if (statusCode.value === 'RUNNING') return v4MatchSeriesStatusLabel(props.series.seriesStatus)
  if (statusCode.value === 'COMPLETED' || statusCode.value === 'CANCELLED') return ''
  return props.series.scheduledLabel || ''
})

const rows = computed(() => [
  {
    key: 'home',
    name: props.series.topTeamName || '待定',
    logo: props.series.topTeamLogo,
    seed: props.series.topSeed,
    score: props.series.topScore,
    winner: Boolean(props.series.winnerTeamId) && props.series.winnerTeamId === props.series.topTeamId,
  },
  {
    key: 'away',
    name: props.series.bottomTeamName || '待定',
    logo: props.series.bottomTeamLogo,
    seed: props.series.bottomSeed,
    score: props.series.bottomScore,
    winner: Boolean(props.series.winnerTeamId) && props.series.winnerTeamId === props.series.bottomTeamId,
  },
])

const showSeed = computed(() => rows.value.some((row) => row.seed != null && String(row.seed).trim() !== ''))

const championName = computed(() => {
  if (!props.emphasize || statusCode.value !== 'COMPLETED') return ''
  return rows.value.find((row) => row.winner)?.name || ''
})

function scoreText(value: number | null | undefined) {
  return value == null ? '-' : String(value)
}

function badgeText(name: string) {
  return String(name || '').replace(/\s+/g, '').slice(0, 2).toUpperCase() || '--'
}
</script>

<template>
  <component
    :is="seriesPath ? 'router-link' : 'article'"
    :to="seriesPath || undefined"
    class="playoff-series-card"
    :class="{ 'playoff-series-card--final': emphasize }"
  >
    <div class="playoff-series-card__meta">
      <strong v-if="emphasize">总决赛</strong>
      <span>{{ metaLabel }}</span>
    </div>
    <div
      v-for="row in rows"
      :key="row.key"
      class="playoff-series-card__row"
      :class="{ 'is-winner': row.winner, 'has-seed': showSeed }"
    >
      <span v-if="showSeed" class="playoff-series-card__seed">{{ row.seed || '' }}</span>
      <span class="playoff-series-card__logo" aria-hidden="true">
        <img v-if="row.logo" :src="row.logo" :alt="''" />
        <em v-else>{{ badgeText(row.name) }}</em>
      </span>
      <span class="playoff-series-card__name">{{ row.name }}</span>
      <strong class="playoff-series-card__score">{{ scoreText(row.score) }}</strong>
    </div>
    <div v-if="championName" class="playoff-series-card__champion">总冠军 {{ championName }}</div>
    <div v-else-if="footer" class="playoff-series-card__foot">{{ footer }}</div>
  </component>
</template>

<style scoped lang="scss">
.playoff-series-card {
  display: block;
  width: 100%;
  min-width: 0;
  min-height: 108px;
  padding: 10px 12px 9px;
  color: inherit;
  text-decoration: none;
  background: #f7f4f0;
  border: 1px solid #dedede;
  border-radius: 3px;
  box-shadow: 0 2px 8px rgba(17, 17, 17, 0.05);
  box-sizing: border-box;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;

  &:hover {
    border-color: rgba(225, 6, 19, 0.55);
    background: #fff8f6;
  }
}

.playoff-series-card--final {
  min-height: 128px;
  padding: 12px 14px 11px;
  border-color: var(--cnpa-red, #e10613);
  box-shadow: 0 3px 12px rgba(225, 6, 19, 0.08);
}

.playoff-series-card__meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
  color: rgba(17, 17, 17, 0.42);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;

  strong {
    color: var(--cnpa-red, #e10613);
    font-size: 12px;
    font-weight: 800;
  }
}

.playoff-series-card__row {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 28px;
  gap: 10px;
  align-items: center;
  min-height: 36px;
  padding: 3px 0;

  &.has-seed {
    grid-template-columns: 16px 36px minmax(0, 1fr) 28px;
  }
}

.playoff-series-card__row.is-winner {
  .playoff-series-card__name,
  .playoff-series-card__score {
    color: var(--cnpa-red, #e10613);
    font-weight: 800;
  }
}

.playoff-series-card__seed {
  color: rgba(17, 17, 17, 0.45);
  font-size: 11px;
  font-weight: 800;
  text-align: center;
}

.playoff-series-card__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  overflow: hidden;
  border-radius: 2px;
  background: #161616;

  img {
    width: 36px;
    height: 36px;
    object-fit: cover;
  }

  em {
    font-style: normal;
    font-size: 11px;
    font-weight: 800;
    color: #fff;
    letter-spacing: 0.02em;
  }
}

.playoff-series-card__name {
  min-width: 0;
  overflow: hidden;
  color: #222;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playoff-series-card__row:not(.is-winner) .playoff-series-card__name {
  color: rgba(17, 17, 17, 0.62);
  font-weight: 700;
}

.playoff-series-card__score {
  width: 28px;
  color: #111;
  font-size: 16px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.playoff-series-card__champion {
  margin-top: 8px;
  color: var(--cnpa-red, #e10613);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.playoff-series-card__foot {
  margin-top: 6px;
  color: rgba(17, 17, 17, 0.5);
  font-size: 11px;
  font-weight: 700;
}
</style>
