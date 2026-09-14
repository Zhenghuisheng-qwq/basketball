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

function isPlaceholderName(name: string) {
  const text = String(name || '').trim()
  return !text || text === '待定' || text.includes('上一轮') || text === '轮空'
}

const isPending = computed(() => {
  if (statusCode.value === 'COMPLETED' || statusCode.value === 'RUNNING') return false
  if (statusCode.value === 'PENDING') return true
  const missingIds = !props.series.topTeamId || !props.series.bottomTeamId
  const placeholders = isPlaceholderName(props.series.topTeamName) || isPlaceholderName(props.series.bottomTeamName)
  return missingIds || placeholders
})

const isLive = computed(() => statusCode.value === 'RUNNING')
const isFinished = computed(() => statusCode.value === 'COMPLETED')
const isUpcoming = computed(() => !isLive.value && !isFinished.value && !isPending.value)

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
    :class="{
      'playoff-series-card--final': emphasize,
      'playoff-series-card--pending': isPending,
      'playoff-series-card--upcoming': isUpcoming,
      'playoff-series-card--live': isLive,
      'playoff-series-card--done': isFinished,
      'is-link': Boolean(seriesPath),
    }"
  >
    <div class="playoff-series-card__meta">
      <strong v-if="emphasize">FINAL</strong>
      <span>{{ metaLabel }}</span>
    </div>
    <div
      v-for="row in rows"
      :key="row.key"
      class="playoff-series-card__row"
      :class="{
        'is-winner': row.winner,
        'is-loser': isFinished && !row.winner,
        'has-seed': showSeed,
      }"
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
    <div v-else-if="footer" class="playoff-series-card__foot">
      <i v-if="isLive" class="playoff-series-card__live-dot" aria-hidden="true" />
      <span>{{ footer }}</span>
    </div>
  </component>
</template>

<style scoped lang="scss">
.playoff-series-card {
  display: block;
  width: 100%;
  min-width: 0;
  min-height: 114px;
  padding: 11px 12px 10px;
  color: inherit;
  text-decoration: none;
  background: #faf9f7;
  border: 1px solid rgba(17, 17, 17, 0.1);
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(17, 17, 17, 0.03);
  box-sizing: border-box;
  cursor: default;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;

  &.is-link {
    cursor: pointer;

    &:hover {
      transform: translateY(-1px);
      border-color: rgba(225, 6, 19, 0.42);
      box-shadow: 0 2px 6px rgba(17, 17, 17, 0.05);
    }
  }
}

.playoff-series-card--final {
  min-height: 132px;
  padding: 13px 14px 12px;
  border-color: rgba(225, 6, 19, 0.55);
  box-shadow: 0 0 0 1px rgba(225, 6, 19, 0.08);
}

.playoff-series-card--pending {
  background: #f4f3f0;
  border-color: rgba(17, 17, 17, 0.07);
  box-shadow: none;

  .playoff-series-card__name,
  .playoff-series-card__score {
    color: rgba(17, 17, 17, 0.42);
    font-weight: 600;
  }

  .playoff-series-card__logo {
    background: #b8b4ae;
    box-shadow: inset 0 0 0 1px rgba(17, 17, 17, 0.06);

    em {
      color: rgba(255, 255, 255, 0.58);
      font-weight: 600;
    }
  }
}

.playoff-series-card--upcoming {
  .playoff-series-card__name {
    color: #333;
    font-weight: 700;
  }

  .playoff-series-card__score {
    color: rgba(17, 17, 17, 0.38);
  }
}

.playoff-series-card--live {
  border-color: rgba(225, 6, 19, 0.38);
}

.playoff-series-card__meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
  color: rgba(17, 17, 17, 0.4);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;

  strong {
    color: var(--cnpa-red, #e10613);
    font-size: 11px;
    font-weight: 800;
  }
}

.playoff-series-card__row {
  position: relative;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 36px;
  padding: 4px 0;

  &.has-seed {
    grid-template-columns: 16px 36px minmax(0, 1fr) auto;
  }
}

.playoff-series-card__row.is-winner {
  .playoff-series-card__name {
    color: #111;
    font-weight: 900;
  }

  .playoff-series-card__score {
    color: var(--cnpa-red, #e10613);
    font-weight: 900;
  }

  &::before {
    content: '';
    position: absolute;
    left: -12px;
    top: 8px;
    bottom: 8px;
    width: 2px;
    background: var(--cnpa-red, #e10613);
  }
}

.playoff-series-card__row.is-loser {
  opacity: 0.62;

  .playoff-series-card__name,
  .playoff-series-card__score {
    color: rgba(17, 17, 17, 0.5);
    font-weight: 600;
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
  border-radius: 3px;
  background: #161616;

  img {
    width: 36px;
    height: 36px;
    object-fit: contain;
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

.playoff-series-card__score {
  min-width: 22px;
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
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: #888;
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.playoff-series-card__live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cnpa-red, #e10613);
}
</style>
