<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicMatchDetail, getV4PublicPlayerStat, getV4PublicTeamStat } from '@/api/v4Public'
import type {
  PlayerStatsMode,
  PlayoffStatsMode,
  TeamPlayerAverageStat,
  TeamPlayerTotalStat,
  TeamSeasonAverages,
  TeamSingleMatchPlayerStat,
  TeamStage,
} from '@/types/team'
import PlayerLink from '@/components/player/PlayerLink.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, initials } from '@/utils/publicDisplay'
import { mapTeamSeasonAverages } from '@/utils/publicTeamMap'
import {
  PUBLIC_TEAM_PAGE_KEY,
  type PublicTeamPageContext,
} from './composables/usePublicTeamPage'
import { mapV4TeamStatToSeasonStat } from './utils/mapV4PublicTeam'
import {
  mapV4MatchDetailToSingleMatchStats,
  mapV4PlayerStatToAverage,
  mapV4PlayerStatToTotal,
} from './utils/mapTeamPlayerStats'

type NumericColumn =
  | 'points'
  | 'rebounds'
  | 'assists'
  | 'steals'
  | 'blocks'
  | 'fouls'
  | 'turnovers'
  | 'fgPct'
  | 'tpPct'
  | 'ftPct'

const numericColumns: NumericColumn[] = [
  'points',
  'rebounds',
  'assists',
  'steals',
  'blocks',
  'fouls',
  'turnovers',
  'fgPct',
  'tpPct',
  'ftPct',
]

const singleNumericColumns: Array<
  'points' | 'rebounds' | 'assists' | 'steals' | 'blocks' | 'fouls' | 'turnovers'
> = ['points', 'rebounds', 'assists', 'steals', 'blocks', 'fouls', 'turnovers']

const ctx = inject(PUBLIC_TEAM_PAGE_KEY) as PublicTeamPageContext
const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const competitionFilter = ref('all')
const stageFilter = ref<TeamStage>('regular')
const playerStatsMode = ref<PlayerStatsMode>('average')
const playoffStatsMode = ref<PlayoffStatsMode>('summary')

const teamId = computed(() => ctx.teamId.value)
const matchId = computed(() => (route.query.matchId as string) || '')

const seasonStatState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const playerStatState = ref<'loading' | 'success' | 'empty' | 'error'>('empty')
const singleMatchState = ref<'loading' | 'success' | 'empty' | 'error'>('empty')
const seasonErrorMessage = ref('')
const playerErrorMessage = ref('')
const singleMatchErrorMessage = ref('')

const seasonStatsList = ref(ctx.stats.value)
const averagePlayerStats = ref<TeamPlayerAverageStat[]>([])
const totalPlayerStats = ref<TeamPlayerTotalStat[]>([])
const singleMatchStats = ref<ReturnType<typeof mapV4MatchDetailToSingleMatchStats>>(null)

let seasonReqId = 0
let playerReqId = 0
let singleReqId = 0

const activeSeasonStat = computed(() => {
  const stats = seasonStatsList.value
  if (!stats.length) return null
  return stats[0]
})

const seasonStats = computed<TeamSeasonAverages | null>(() =>
  mapTeamSeasonAverages(activeSeasonStat.value),
)

const divisionId = computed(() => ctx.divisionId.value)

const competitionOptions = computed(() => {
  const name = displayText(ctx.teamView.value?.seasonName, '')
  return name ? [name] : []
})

const seasonCardTitle = computed(() => ({
  cn: t('publicPages.team.eventAverages'),
  en: 'Event Averages',
}))

type MetricTier = 'primary' | 'secondary' | 'tertiary'

const seasonStatItems = computed(() => {
  if (!seasonStats.value) return []
  return [
    { key: 'points', label: t('publicPages.team.points'), value: seasonStats.value.points, tier: 'primary' as MetricTier },
    { key: 'rebounds', label: t('publicPages.team.rebounds'), value: seasonStats.value.rebounds, tier: 'secondary' as MetricTier },
    { key: 'assists', label: t('publicPages.team.assists'), value: seasonStats.value.assists, tier: 'secondary' as MetricTier },
    { key: 'steals', label: t('publicPages.team.steals'), value: seasonStats.value.steals, tier: 'tertiary' as MetricTier },
    { key: 'blocks', label: t('publicPages.team.blocks'), value: seasonStats.value.blocks, tier: 'tertiary' as MetricTier },
    { key: 'fouls', label: t('publicPages.team.fouls'), value: seasonStats.value.fouls, tier: 'tertiary' as MetricTier },
    { key: 'turnovers', label: t('publicPages.team.turnovers'), value: seasonStats.value.turnovers, tier: 'tertiary' as MetricTier },
  ]
})

const rosterJerseyMap = computed(() => {
  const map = new Map<string, string>()
  for (const item of ctx.roster.value) {
    if (item.playerId && item.jerseyNo) {
      map.set(String(item.playerId), String(item.jerseyNo))
    }
  }
  return map
})

const summaryPlayerStats = computed(() => {
  if (stageFilter.value === 'playoffs' && playoffStatsMode.value === 'single') {
    return []
  }
  return playerStatsMode.value === 'average'
    ? averagePlayerStats.value
    : totalPlayerStats.value
})

const showFgPctColumn = computed(() => summaryPlayerStats.value.some((row) => row.fgPct != null))
const showTpPctColumn = computed(() => summaryPlayerStats.value.some((row) => row.tpPct != null))
const showFtPctColumn = computed(() => summaryPlayerStats.value.some((row) => row.ftPct != null))

const summaryStatsMode = computed<PlayerStatsMode>(() => playerStatsMode.value)

const summaryMaxMap = computed(() => buildMaxMap(summaryPlayerStats.value, summaryStatsMode.value))

const singleMaxMap = computed(() => {
  if (!singleMatchStats.value) return {}
  return buildSingleMaxMap(singleMatchStats.value.players)
})

async function loadSeasonStats() {
  const id = teamId.value
  const divId = divisionId.value
  const current = ++seasonReqId
  if (!id || !divId) {
    seasonStatsList.value = []
    seasonStatState.value = 'empty'
    return
  }

  if (ctx.stats.value.length) {
    seasonStatsList.value = ctx.stats.value
    seasonStatState.value = seasonStatsList.value.length ? 'success' : 'empty'
    return
  }

  seasonStatState.value = 'loading'
  seasonErrorMessage.value = ''
  try {
    const res = await getV4PublicTeamStat(divId)
    if (current !== seasonReqId) return
    const row = (res.data ?? []).find((item) => String(item.eventTeamId) === String(id))
    seasonStatsList.value = row
      ? [mapV4TeamStatToSeasonStat(row, {
          seasonId: ctx.seasonId.value,
          divisionId: divId,
        })]
      : []
    seasonStatState.value = seasonStatsList.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== seasonReqId) return
    seasonStatsList.value = []
    seasonStatState.value = 'error'
    seasonErrorMessage.value = err instanceof Error ? err.message : t('publicPages.team.loadAveragesFailed')
  }
}

async function loadPlayerStats() {
  const divId = divisionId.value
  const tid = teamId.value
  const current = ++playerReqId

  if (!divId || !tid) {
    averagePlayerStats.value = []
    totalPlayerStats.value = []
    playerStatState.value = 'empty'
    return
  }

  playerStatState.value = 'loading'
  playerErrorMessage.value = ''
  try {
    const res = await getV4PublicPlayerStat(divId)
    if (current !== playerReqId) return
    const rosterEventPlayerIds = new Set(
      ctx.roster.value
        .map((item) => String(item.eventPlayerId || '').trim())
        .filter(Boolean),
    )
    const teamName = displayText(ctx.teamView.value?.name, '')
    const records = (res.data ?? []).filter((item) => {
      if (rosterEventPlayerIds.has(String(item.eventPlayerId))) return true
      return displayText(item.teamName, '') === teamName
    })
    averagePlayerStats.value = records.map(mapV4PlayerStatToAverage)
    totalPlayerStats.value = records.map(mapV4PlayerStatToTotal)
    playerStatState.value = records.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== playerReqId) return
    averagePlayerStats.value = []
    totalPlayerStats.value = []
    playerStatState.value = 'error'
    playerErrorMessage.value = err instanceof Error ? err.message : t('publicPages.team.loadPlayersFailed')
  }
}

async function loadSingleMatchStats() {
  const mid = matchId.value
  const tid = teamId.value
  const current = ++singleReqId

  if (!showSingleTable() || !mid) {
    singleMatchStats.value = null
    singleMatchState.value = 'empty'
    return
  }

  singleMatchState.value = 'loading'
  singleMatchErrorMessage.value = ''
  try {
    const res = await getV4PublicMatchDetail(mid)
    if (current !== singleReqId) return
    singleMatchStats.value = res.data
      ? mapV4MatchDetailToSingleMatchStats(res.data, tid)
      : null
    singleMatchState.value = singleMatchStats.value ? 'success' : 'empty'
  } catch (err) {
    if (current !== singleReqId) return
    singleMatchStats.value = null
    singleMatchState.value = 'error'
    singleMatchErrorMessage.value = err instanceof Error ? err.message : t('publicPages.team.loadSingleFailed')
  }
}

watch([teamId, divisionId], () => {
  void loadSeasonStats()
}, { immediate: true })

watch(
  () => ctx.stats.value,
  (stats) => {
    if (stats.length) {
      seasonStatsList.value = stats
      seasonStatState.value = 'success'
    }
  },
)

watch([divisionId, teamId, stageFilter, playerStatsMode, playoffStatsMode], () => {
  void loadPlayerStats()
}, { immediate: true })

watch([matchId, teamId, stageFilter, playoffStatsMode], () => {
  void loadSingleMatchStats()
}, { immediate: true })

function normalizeSummaryValue(value: number | null | undefined, col: NumericColumn, mode: PlayerStatsMode): number | null {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return null
  const num = Number(value)
  if (col === 'fgPct' || col === 'tpPct' || col === 'ftPct') {
    return Number(num.toFixed(1))
  }
  if (mode === 'average') {
    return Number(num.toFixed(1))
  }
  return Math.round(num)
}

function buildMaxMap(
  rows: Array<TeamPlayerAverageStat | TeamPlayerTotalStat>,
  mode: PlayerStatsMode,
) {
  const maxes: Partial<Record<NumericColumn, number>> = {}
  if (!rows.length) return maxes

  for (const col of numericColumns) {
    const values = rows
      .map((row) => normalizeSummaryValue(row[col], col, mode))
      .filter((value): value is number => value !== null)
    if (values.length) {
      maxes[col] = Math.max(...values)
    }
  }
  return maxes
}

function buildSingleMaxMap(rows: TeamSingleMatchPlayerStat[]) {
  const maxes: Partial<Record<(typeof singleNumericColumns)[number], number>> = {}
  if (!rows.length) return maxes

  for (const col of singleNumericColumns) {
    const values = rows
      .map((row) => row[col])
      .filter((value): value is number => value !== null && value !== undefined && !Number.isNaN(Number(value)))
    if (values.length) {
      maxes[col] = Math.max(...values)
    }
  }
  return maxes
}

function isSummaryMax(
  row: TeamPlayerAverageStat | TeamPlayerTotalStat,
  col: NumericColumn,
): boolean {
  const maxValue = summaryMaxMap.value[col]
  if (maxValue === undefined || maxValue === 0) return false
  return normalizeSummaryValue(row[col], col, summaryStatsMode.value) === maxValue
}

function isSingleMax(row: TeamSingleMatchPlayerStat, col: (typeof singleNumericColumns)[number]) {
  const maxValue = singleMaxMap.value[col]
  if (maxValue === undefined || maxValue === 0) return false
  return row[col] === maxValue
}

function getPlayerNumber(row: { playerId: string; nickname: string }): string {
  const jersey = rosterJerseyMap.value.get(row.playerId)
  if (jersey) return jersey.padStart(2, '0')
  const match = row.nickname.match(/(\d+)$/)
  return match ? match[1].padStart(2, '0') : initials(row.nickname)
}

function formatMetric(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—'
  return Number(value).toFixed(1)
}

function formatCount(value: number | null | undefined, mode: PlayerStatsMode): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—'
  return mode === 'average' ? Number(value).toFixed(1) : String(Math.round(Number(value)))
}

function formatPct(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—'
  const num = Number(value)
  const pct = num <= 1 ? num * 100 : num
  return `${pct.toFixed(1)}%`
}

function formatSingleValue(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '' || value === '--') return '—'
  return String(value)
}

function handlePlayerDetailClick(playerId: string) {
  goToPlayerDetail(router, playerId, 'overview', {
    ...(divisionId.value ? { divisionId: divisionId.value } : {}),
  })
}

function showSummaryTable() {
  return stageFilter.value === 'regular' || playoffStatsMode.value === 'summary'
}

function showSingleTable() {
  return stageFilter.value === 'playoffs' && playoffStatsMode.value === 'single'
}
</script>

<template>
  <div class="team-stats">
    <div class="team-stats__toolbar">
      <div class="section-head-group">
        <h2 class="section-head-title">{{ t('publicPages.team.teamData') }}</h2>
        <span class="section-head-sub">Team Stats</span>
      </div>

      <div class="stats-filters">
        <label v-if="competitionOptions.length" class="filter-select">
          <select v-model="competitionFilter">
            <option value="all">{{ t('publicPages.team.allEvents') }}</option>
            <option v-for="option in competitionOptions" :key="option" :value="option">
              {{ option }}
            </option>
          </select>
        </label>

        <label class="filter-select">
          <select v-model="stageFilter">
            <option value="regular">{{ t('publicPages.team.regular') }}</option>
            <option value="playoffs">{{ t('publicPages.team.playoffs') }}</option>
          </select>
        </label>
      </div>
    </div>

    <section class="stats-section">
      <p class="season-caption">
        {{ seasonCardTitle.cn }}
        <em>{{ seasonCardTitle.en }}</em>
      </p>

      <div v-if="seasonStatState === 'loading'" class="stats-empty">{{ t('publicPages.team.loading') }}</div>
      <div v-else-if="seasonStatState === 'error'" class="stats-empty is-error">
        {{ seasonErrorMessage || t('publicPages.team.loadAveragesFailed') }}
      </div>
      <div v-else-if="!seasonStats" class="stats-empty">{{ t('publicPages.team.noAverages') }}</div>

      <div v-else class="season-cards">
        <article
          v-for="item in seasonStatItems"
          :key="item.key"
          class="season-card"
          :class="`season-card--${item.tier}`"
        >
          <span class="season-card__label">{{ item.label }}</span>
          <strong class="season-card__value">{{ formatMetric(item.value) }}</strong>
        </article>
      </div>
      <p class="stats-note stats-note--metrics">
        {{ t('publicPages.team.averagesNote') }}
      </p>
    </section>

    <section class="stats-section">
      <div class="section-head section-head--compact">
        <div class="section-head-group">
          <h3 class="section-head-title section-head-title--sm">{{ t('publicPages.team.playerData') }}</h3>
          <span class="section-head-sub">Player Stats</span>
        </div>

        <div class="mode-tabs">
          <button
            type="button"
            class="mode-tab"
            :class="{ active: showSummaryTable() && playerStatsMode === 'average' }"
            @click="playerStatsMode = 'average'; playoffStatsMode = 'summary'"
          >
            {{ t('publicPages.team.perGame') }}
          </button>
          <button
            type="button"
            class="mode-tab"
            :class="{ active: showSummaryTable() && playerStatsMode === 'total' }"
            @click="playerStatsMode = 'total'; playoffStatsMode = 'summary'"
          >
            {{ t('publicPages.team.cumulative') }}
          </button>
          <button
            v-if="stageFilter === 'playoffs'"
            type="button"
            class="mode-tab"
            :class="{ active: playoffStatsMode === 'single' }"
            @click="playoffStatsMode = 'single'"
          >
            {{ t('publicPages.team.singleGame') }}
          </button>
        </div>
      </div>

      <div v-if="showSummaryTable()" class="stats-table-wrap">
        <div v-if="playerStatState === 'loading'" class="stats-empty">{{ t('publicPages.team.loading') }}</div>
        <div v-else-if="playerStatState === 'error'" class="stats-empty is-error">
          {{ playerErrorMessage || t('publicPages.team.loadPlayersFailed') }}
        </div>
        <div v-else-if="summaryPlayerStats.length === 0" class="stats-empty">{{ t('publicPages.team.noPlayerData') }}</div>

        <table v-else class="stats-table">
          <thead>
            <tr>
              <th class="col-player">{{ t('publicPages.team.player') }}</th>
              <th>{{ t('publicPages.team.games') }}</th>
              <th>{{ t('publicPages.team.points') }}</th>
              <th>{{ t('publicPages.team.rebounds') }}</th>
              <th>{{ t('publicPages.team.assists') }}</th>
              <th>{{ t('publicPages.team.steals') }}</th>
              <th>{{ t('publicPages.team.blocks') }}</th>
              <th>{{ t('publicPages.team.fouls') }}</th>
              <th>{{ t('publicPages.team.turnovers') }}</th>
              <th v-if="showFgPctColumn">{{ t('publicPages.team.fgRate') }}</th>
              <th v-if="showTpPctColumn">{{ t('publicPages.team.tpRate') }}</th>
              <th v-if="showFtPctColumn">{{ t('publicPages.team.ftRate') }}</th>
              <th class="col-action">{{ t('publicPages.team.view') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in summaryPlayerStats" :key="row.playerId">
              <td class="col-player">
                <div class="player-cell">
                  <div class="player-cell__avatar">
                    <img
                      v-if="row.avatar"
                      :src="row.avatar"
                      :alt="row.nickname"
                      class="player-cell__avatar-img"
                    />
                    <span v-else class="player-cell__avatar-placeholder">
                      {{ getPlayerNumber(row) }}
                    </span>
                  </div>
                  <PlayerLink
                    class="player-cell__name"
                    :player-id="row.playerId"
                    :player-name="row.nickname"
                    tab="overview"
                  />
                </div>
              </td>
              <td class="is-num">{{ row.games }}</td>
              <td
                class="is-num"
                :class="{ 'is-max': isSummaryMax(row, 'points'), 'is-max--lead': isSummaryMax(row, 'points') }"
              >
                {{ formatCount(row.points, summaryStatsMode) }}
              </td>
              <td class="is-num" :class="{ 'is-max': isSummaryMax(row, 'rebounds') }">
                {{ formatCount(row.rebounds, summaryStatsMode) }}
              </td>
              <td class="is-num" :class="{ 'is-max': isSummaryMax(row, 'assists') }">
                {{ formatCount(row.assists, summaryStatsMode) }}
              </td>
              <td class="is-num" :class="{ 'is-max': isSummaryMax(row, 'steals') }">
                {{ formatCount(row.steals, summaryStatsMode) }}
              </td>
              <td class="is-num" :class="{ 'is-max': isSummaryMax(row, 'blocks') }">
                {{ formatCount(row.blocks, summaryStatsMode) }}
              </td>
              <td class="is-num" :class="{ 'is-max': isSummaryMax(row, 'fouls') }">
                {{ formatCount(row.fouls, summaryStatsMode) }}
              </td>
              <td class="is-num" :class="{ 'is-max': isSummaryMax(row, 'turnovers') }">
                {{ formatCount(row.turnovers, summaryStatsMode) }}
              </td>
              <td v-if="showFgPctColumn" class="is-num" :class="{ 'is-max': isSummaryMax(row, 'fgPct') }">
                {{ formatPct(row.fgPct) }}
              </td>
              <td v-if="showTpPctColumn" class="is-num" :class="{ 'is-max': isSummaryMax(row, 'tpPct') }">
                {{ formatPct(row.tpPct) }}
              </td>
              <td v-if="showFtPctColumn" class="is-num" :class="{ 'is-max': isSummaryMax(row, 'ftPct') }">
                {{ formatPct(row.ftPct) }}
              </td>
              <td class="col-action">
                <button type="button" class="player-action" @click="handlePlayerDetailClick(row.playerId)">
                  {{ t('publicPages.team.viewPlayer') }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="showSingleTable()" class="stats-table-wrap">
        <div v-if="!matchId" class="stats-empty">{{ t('publicPages.team.pickMatch') }}</div>
        <div v-else-if="singleMatchState === 'loading'" class="stats-empty">{{ t('publicPages.team.loading') }}</div>
        <div v-else-if="singleMatchState === 'error'" class="stats-empty is-error">
          {{ singleMatchErrorMessage || t('publicPages.team.loadSingleFailed') }}
        </div>
        <div v-else-if="!singleMatchStats" class="stats-empty">{{ t('publicPages.team.noSingleData') }}</div>

        <template v-else>
          <p class="single-match-label">{{ singleMatchStats.label }}</p>

          <table class="stats-table">
            <thead>
              <tr>
                <th class="col-player">{{ t('publicPages.team.player') }}</th>
                <th>{{ t('publicPages.team.grade') }}</th>
                <th>{{ t('publicPages.team.points') }}</th>
                <th>{{ t('publicPages.team.rebounds') }}</th>
                <th>{{ t('publicPages.team.assists') }}</th>
                <th>{{ t('publicPages.team.steals') }}</th>
                <th>{{ t('publicPages.team.blocks') }}</th>
                <th>{{ t('publicPages.team.fouls') }}</th>
                <th>{{ t('publicPages.team.turnovers') }}</th>
                <th>{{ t('publicPages.team.fieldGoals') }}</th>
                <th>{{ t('publicPages.team.threePoints') }}</th>
                <th>{{ t('publicPages.team.freeThrows') }}</th>
                <th class="col-action">{{ t('publicPages.team.view') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in singleMatchStats.players" :key="row.playerId">
                <td class="col-player">
                  <div class="player-cell">
                    <div class="player-cell__avatar">
                      <img
                        v-if="row.avatar"
                        :src="row.avatar"
                        :alt="row.nickname"
                        class="player-cell__avatar-img"
                      />
                      <span v-else class="player-cell__avatar-placeholder">
                        {{ getPlayerNumber(row) }}
                      </span>
                    </div>
                    <PlayerLink
                      class="player-cell__name"
                      :player-id="row.playerId"
                      :player-name="row.nickname"
                      tab="overview"
                    />
                  </div>
                </td>
                <td class="is-num">{{ formatSingleValue(row.grade) }}</td>
                <td
                  class="is-num"
                  :class="{ 'is-max': isSingleMax(row, 'points'), 'is-max--lead': isSingleMax(row, 'points') }"
                >
                  {{ row.points }}
                </td>
                <td class="is-num" :class="{ 'is-max': isSingleMax(row, 'rebounds') }">{{ row.rebounds }}</td>
                <td class="is-num" :class="{ 'is-max': isSingleMax(row, 'assists') }">{{ row.assists }}</td>
                <td class="is-num" :class="{ 'is-max': isSingleMax(row, 'steals') }">{{ row.steals }}</td>
                <td class="is-num" :class="{ 'is-max': isSingleMax(row, 'blocks') }">{{ row.blocks }}</td>
                <td class="is-num" :class="{ 'is-max': isSingleMax(row, 'fouls') }">
                  {{ formatSingleValue(row.fouls) }}
                </td>
                <td class="is-num" :class="{ 'is-max': isSingleMax(row, 'turnovers') }">{{ row.turnovers }}</td>
                <td class="is-num">{{ row.fg }}</td>
                <td class="is-num">{{ row.tp }}</td>
                <td class="is-num">{{ row.ft }}</td>
                <td class="col-action">
                  <button type="button" class="player-action" @click="handlePlayerDetailClick(row.playerId)">
                    {{ t('publicPages.team.viewPlayer') }}
                  </button>
                </td>
              </tr>
              <tr class="team-total-row">
                <td class="col-player">{{ t('publicPages.team.teamTotal') }}</td>
                <td class="is-num">{{ formatSingleValue(singleMatchStats.teamTotal.grade) }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.points }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.rebounds }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.assists }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.steals }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.blocks }}</td>
                <td class="is-num">{{ formatSingleValue(singleMatchStats.teamTotal.fouls) }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.turnovers }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.fg }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.tp }}</td>
                <td class="is-num">{{ singleMatchStats.teamTotal.ft }}</td>
                <td class="col-action" />
              </tr>
            </tbody>
          </table>
        </template>
      </div>

      <p class="stats-note stats-note--source">
        <span class="stats-note__icon" aria-hidden="true">
          <svg viewBox="0 0 16 16" width="13" height="13" fill="none">
            <circle cx="8" cy="8" r="6.25" stroke="currentColor" stroke-width="1.2" />
            <path d="M8 7.2v4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
            <circle cx="8" cy="5.15" r="0.7" fill="currentColor" />
          </svg>
        </span>
        {{ t('publicPages.team.dataSource') }}
      </p>
    </section>
  </div>
</template>

<style scoped lang="scss">
.team-stats {
  .section-head-group {
    display: flex;
    align-items: baseline;
    flex-wrap: nowrap;
    gap: clamp(14px, 1.2vw, 18px);
  }

  .section-head-title {
    font-size: clamp(34px, 3.2vw, 38px);
    font-weight: 900;
    color: var(--cnpa-paper-text);
  }

  .section-head-sub {
    color: #e51b23;
    font-size: clamp(16px, 1.5vw, 22px);
    font-style: italic;
    text-transform: uppercase;
    font-weight: 800;
    letter-spacing: 0.02em;
    white-space: nowrap;
  }

  .section-head--compact .section-head-sub {
    font-size: clamp(14px, 1.3vw, 18px);
  }
}

.team-stats__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.section-head--compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.section-head-title--sm {
  font-size: clamp(18px, 1.8vw, 22px);
}

.season-caption {
  margin: 0 0 16px;
  color: #e51b23;
  font-size: 15px;
  font-weight: 800;
  font-style: italic;
  letter-spacing: 0.02em;

  em {
    margin-left: 10px;
    font-style: italic;
    text-transform: uppercase;
  }
}

.stats-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-left: auto;
}

.filter-select {
  position: relative;
  display: inline-flex;
}

.filter-select select {
  min-width: 148px;
  height: 36px;
  padding: 0 30px 0 12px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: 2px;
  background-color: #faf8f6;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6' viewBox='0 0 10 6'%3E%3Cpath fill='%23111111' d='M1.1.3 5 4.2 8.9.3'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 11px center;
  background-size: 10px 6px;
  color: #111;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  appearance: none;
  -webkit-appearance: none;
}

.filter-select:first-child select {
  min-width: 196px;
  max-width: 240px;
}

.filter-select:last-child select {
  min-width: 132px;
  max-width: 160px;
}

.stats-section + .stats-section {
  margin-top: 40px;
}

.stats-empty {
  padding: 40px 16px;
  text-align: center;
  border: 1px dashed rgba(17, 17, 17, 0.14);
  background: transparent;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;

  &.is-error {
    color: var(--cnpa-red);
  }
}

.season-cards {
  display: grid;
  grid-template-columns: minmax(0, 1.16fr) repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.season-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 108px;
  padding: 18px 12px 16px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  border-radius: 2px;
  background: #fff;
  box-shadow: none;
  text-align: center;
  box-sizing: border-box;
}

.season-card--primary {
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 2px;
    background: #e51b23;
  }
}

.season-card__label {
  display: block;
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.58);
  text-align: center;
}

.season-card__value {
  margin: 0;
  font-size: clamp(32px, 2.5vw, 44px);
  line-height: 1;
  font-weight: 800;
  color: #111;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.season-card--primary .season-card__value {
  font-size: clamp(36px, 2.85vw, 50px);
  font-weight: 900;
}

.season-card--secondary .season-card__value {
  font-weight: 800;
}

.mode-tabs {
  display: inline-flex;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: #faf8f6;
}

.mode-tab {
  min-width: 80px;
  height: 34px;
  padding: 0 14px;
  border: none;
  border-right: 1px solid rgba(17, 17, 17, 0.1);
  background: transparent;
  color: #111;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;

  &:last-child {
    border-right: none;
  }

  &.active {
    background: var(--cnpa-red);
    color: #fff;
  }
}

.stats-table-wrap {
  overflow-x: auto;
}

.single-match-label {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
}

.stats-table {
  width: 100%;
  min-width: 820px;
  border-collapse: collapse;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: #fff;

  th,
  td {
    padding: 13px 8px;
    border-top: 1px solid rgba(17, 17, 17, 0.08);
    text-align: center;
    font-size: 13px;
    color: var(--cnpa-paper-text);
    white-space: nowrap;
  }

  th {
    height: 40px;
    padding: 10px 8px;
    background: #f3f2f0;
    color: #6b6b6b;
    font-weight: 600;
    border-top: 0;
  }

  tbody tr {
    background: #fff;
  }

  tbody tr:nth-child(even) {
    background: #faf9f7;
  }

  tbody tr:hover {
    background: #f4efe9;
  }

  .is-num {
    font-variant-numeric: tabular-nums;
  }

  .col-player {
    text-align: left;
    padding-left: 14px;
  }

  .col-action {
    width: 88px;
    padding-left: 6px;
    padding-right: 10px;
  }
}

.is-max {
  font-weight: 700;
}

.is-max--lead {
  color: #e51b23;
}

.player-cell {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
}

.player-cell__avatar {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}

.player-cell__avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.player-cell__avatar-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: #1a1a1a;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.player-cell__name {
  font-size: 14px;
  font-weight: 600;
}

.player-action {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #5c5c5c;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;

  &:hover {
    color: #e51b23;
  }
}

.team-total-row {
  background: #f3ead8 !important;
  font-weight: 800;
}

.stats-note {
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #6f6d69;
}

.stats-note--metrics {
  margin-top: 12px;
}

.stats-note--source {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.stats-note__icon {
  display: inline-flex;
  color: #8a8680;
  transform: translateY(2px);
  flex-shrink: 0;
}

.team-stats {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
  box-sizing: border-box;
}

.team-stats .filter-select select:focus-visible,
.team-stats .mode-tab:focus-visible,
.team-stats .player-action:focus-visible {
  outline: 2px solid rgba(225, 6, 19, 0.4);
  outline-offset: 2px;
}

@media (max-width: 1440px) {
  .season-cards {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .season-card--primary {
    grid-column: span 1;
  }
}

@media (max-width: 1280px) {
  .season-cards {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
  }
}

@media (max-width: 768px) {
  .team-stats__toolbar,
  .stats-filters {
    align-items: stretch;
    width: 100%;
    flex-direction: column;
    margin-left: 0;
  }

  .stats-filters {
    justify-content: flex-start;
  }

  .filter-select,
  .filter-select select {
    width: 100%;
    max-width: none;
  }

  .section-head--compact {
    flex-direction: column;
    align-items: flex-start;
  }

  .season-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
