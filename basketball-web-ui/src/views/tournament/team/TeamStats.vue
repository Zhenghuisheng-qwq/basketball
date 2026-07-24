<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicMatchDetail } from '@/api/publicMatch'
import { getPublicSeasonPlayerStatPage } from '@/api/publicSeasonStat'
import { getPublicTeamStat } from '@/api/publicTeam'
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
import {
  mapPublicMatchDetailToSingleMatchStats,
  mapPublicPlayerSeasonStatToAverage,
  mapPublicPlayerSeasonStatToTotal,
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
const singleMatchStats = ref<ReturnType<typeof mapPublicMatchDetailToSingleMatchStats>>(null)

let seasonReqId = 0
let playerReqId = 0
let singleReqId = 0

const activeSeasonStat = computed(() => {
  const stats = seasonStatsList.value
  if (!stats.length) return null
  return stats[0]
})

const seasonId = computed(() => activeSeasonStat.value?.seasonId || '')

const seasonStats = computed<TeamSeasonAverages | null>(() =>
  mapTeamSeasonAverages(activeSeasonStat.value),
)

const competitionOptions = computed(() => {
  const name = displayText(activeSeasonStat.value?.seasonId ? `赛季 ${activeSeasonStat.value.seasonId}` : '', '')
  return name ? [name] : []
})

const seasonCardTitle = computed(() =>
  stageFilter.value === 'regular'
    ? { cn: '常规赛场均', en: 'Regular Season Averages' }
    : { cn: '季后赛场均', en: 'Playoff Averages' },
)

const seasonStatItems = computed(() => {
  if (!seasonStats.value) return []
  return [
    { label: '得分', value: seasonStats.value.points },
    { label: '篮板', value: seasonStats.value.rebounds },
    { label: '助攻', value: seasonStats.value.assists },
    { label: '抢断', value: seasonStats.value.steals },
    { label: '盖帽', value: seasonStats.value.blocks },
    { label: '犯规', value: seasonStats.value.fouls },
    { label: '失误', value: seasonStats.value.turnovers },
  ]
})

const summaryPlayerStats = computed(() => {
  if (stageFilter.value === 'regular') {
    return playerStatsMode.value === 'average'
      ? averagePlayerStats.value
      : totalPlayerStats.value
  }
  return playoffStatsMode.value === 'summary'
    ? totalPlayerStats.value
    : averagePlayerStats.value
})

const summaryStatsMode = computed<PlayerStatsMode>(() => {
  if (stageFilter.value === 'regular') {
    return playerStatsMode.value
  }
  return playoffStatsMode.value === 'summary' ? 'total' : 'average'
})

const summaryMaxMap = computed(() => buildMaxMap(summaryPlayerStats.value, summaryStatsMode.value))

const singleMaxMap = computed(() => {
  if (!singleMatchStats.value) return {}
  return buildSingleMaxMap(singleMatchStats.value.players)
})

async function loadSeasonStats() {
  const id = teamId.value
  const current = ++seasonReqId
  if (!id) {
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
    const res = await getPublicTeamStat(id)
    if (current !== seasonReqId) return
    seasonStatsList.value = res.data ?? []
    seasonStatState.value = seasonStatsList.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== seasonReqId) return
    seasonStatsList.value = []
    seasonStatState.value = 'error'
    seasonErrorMessage.value = err instanceof Error ? err.message : '加载场均数据失败'
  }
}

async function loadPlayerStats() {
  const sid = seasonId.value
  const tid = teamId.value
  const current = ++playerReqId

  if (!sid || !tid) {
    averagePlayerStats.value = []
    totalPlayerStats.value = []
    playerStatState.value = 'empty'
    return
  }

  playerStatState.value = 'loading'
  playerErrorMessage.value = ''
  try {
    const res = await getPublicSeasonPlayerStatPage(sid, {
      teamId: tid,
      currentPage: 1,
      pageSize: 100,
    })
    if (current !== playerReqId) return
    const records = res.data?.records ?? []
    averagePlayerStats.value = records.map(mapPublicPlayerSeasonStatToAverage)
    totalPlayerStats.value = records.map(mapPublicPlayerSeasonStatToTotal)
    playerStatState.value = records.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== playerReqId) return
    averagePlayerStats.value = []
    totalPlayerStats.value = []
    playerStatState.value = 'error'
    playerErrorMessage.value = err instanceof Error ? err.message : '加载球员数据失败'
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
    const res = await getPublicMatchDetail(mid)
    if (current !== singleReqId) return
    singleMatchStats.value = res.data
      ? mapPublicMatchDetailToSingleMatchStats(res.data, tid)
      : null
    singleMatchState.value = singleMatchStats.value ? 'success' : 'empty'
  } catch (err) {
    if (current !== singleReqId) return
    singleMatchStats.value = null
    singleMatchState.value = 'error'
    singleMatchErrorMessage.value = err instanceof Error ? err.message : '加载单场数据失败'
  }
}

watch(teamId, () => {
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

watch([seasonId, teamId, stageFilter, playerStatsMode, playoffStatsMode], () => {
  void loadPlayerStats()
}, { immediate: true })

watch([matchId, teamId, stageFilter, playoffStatsMode], () => {
  void loadSingleMatchStats()
}, { immediate: true })

function normalizeSummaryValue(value: number, col: NumericColumn, mode: PlayerStatsMode): number {
  if (col === 'fgPct' || col === 'tpPct' || col === 'ftPct') {
    return Number(value.toFixed(1))
  }
  if (mode === 'average') {
    return Number(value.toFixed(1))
  }
  return Math.round(value)
}

function buildMaxMap(
  rows: Array<TeamPlayerAverageStat | TeamPlayerTotalStat>,
  mode: PlayerStatsMode,
) {
  const maxes: Partial<Record<NumericColumn, number>> = {}
  if (!rows.length) return maxes

  for (const col of numericColumns) {
    maxes[col] = Math.max(...rows.map((row) => normalizeSummaryValue(row[col], col, mode)))
  }
  return maxes
}

function buildSingleMaxMap(rows: TeamSingleMatchPlayerStat[]) {
  const maxes: Partial<Record<(typeof singleNumericColumns)[number], number>> = {}
  if (!rows.length) return maxes

  for (const col of singleNumericColumns) {
    maxes[col] = Math.max(...rows.map((row) => row[col]))
  }
  return maxes
}

function isSummaryMax(
  row: TeamPlayerAverageStat | TeamPlayerTotalStat,
  col: NumericColumn,
): boolean {
  const maxValue = summaryMaxMap.value[col]
  if (maxValue === undefined) return false
  return normalizeSummaryValue(row[col], col, summaryStatsMode.value) === maxValue
}

function isSingleMax(row: TeamSingleMatchPlayerStat, col: (typeof singleNumericColumns)[number]) {
  const maxValue = singleMaxMap.value[col]
  if (maxValue === undefined) return false
  return row[col] === maxValue
}

function getPlayerNumber(nickname: string): string {
  const match = nickname.match(/(\d+)$/)
  return match ? match[1].padStart(2, '0') : initials(nickname)
}

function formatCount(value: number, mode: PlayerStatsMode): string {
  return mode === 'average' ? value.toFixed(1) : String(Math.round(value))
}

function formatPct(value: number): string {
  return `${value.toFixed(1)}%`
}

function handlePlayerDetailClick(playerId: string) {
  goToPlayerDetail(router, playerId, 'overview')
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
        <h2 class="section-head-title">球队数据</h2>
        <span class="section-head-sub">Team Stats</span>
      </div>

      <div class="stats-filters">
        <label v-if="competitionOptions.length" class="filter-select">
          <select v-model="competitionFilter">
            <option value="all">全部赛事</option>
            <option v-for="option in competitionOptions" :key="option" :value="option">
              {{ option }}
            </option>
          </select>
        </label>

        <label class="filter-select">
          <select v-model="stageFilter">
            <option value="regular">常规赛</option>
            <option value="playoffs">季后赛</option>
          </select>
        </label>
      </div>
    </div>

    <section class="stats-section">
      <div class="section-head section-head--compact">
        <div class="section-head-group">
          <h3 class="section-head-title section-head-title--sm">{{ seasonCardTitle.cn }}</h3>
          <span class="section-head-sub">{{ seasonCardTitle.en }}</span>
        </div>
      </div>

      <div v-if="seasonStatState === 'loading'" class="stats-empty">加载中...</div>
      <div v-else-if="seasonStatState === 'error'" class="stats-empty is-error">
        {{ seasonErrorMessage || '加载场均数据失败' }}
      </div>
      <div v-else-if="!seasonStats" class="stats-empty">暂无场均数据</div>

      <div v-else class="season-cards">
        <article v-for="item in seasonStatItems" :key="item.label" class="season-card">
          <span class="season-card__label">{{ item.label }}</span>
          <strong class="season-card__value">{{ item.value.toFixed(1) }}</strong>
        </article>
      </div>
    </section>

    <section class="stats-section">
      <div class="section-head section-head--compact">
        <div class="section-head-group">
          <h3 class="section-head-title section-head-title--sm">球员数据</h3>
          <span class="section-head-sub">Player Stats</span>
        </div>

        <div v-if="stageFilter === 'regular'" class="mode-tabs">
          <button
            type="button"
            class="mode-tab"
            :class="{ active: playerStatsMode === 'average' }"
            @click="playerStatsMode = 'average'"
          >
            场均
          </button>
          <button
            type="button"
            class="mode-tab"
            :class="{ active: playerStatsMode === 'total' }"
            @click="playerStatsMode = 'total'"
          >
            累计
          </button>
        </div>

        <div v-else class="mode-tabs">
          <button
            type="button"
            class="mode-tab"
            :class="{ active: playoffStatsMode === 'summary' }"
            @click="playoffStatsMode = 'summary'"
          >
            季后赛汇总
          </button>
          <button
            type="button"
            class="mode-tab"
            :class="{ active: playoffStatsMode === 'single' }"
            @click="playoffStatsMode = 'single'"
          >
            单场数据
          </button>
        </div>
      </div>

      <div v-if="showSummaryTable()" class="stats-table-wrap">
        <div v-if="playerStatState === 'loading'" class="stats-empty">加载中...</div>
        <div v-else-if="playerStatState === 'error'" class="stats-empty is-error">
          {{ playerErrorMessage || '加载球员数据失败' }}
        </div>
        <div v-else-if="summaryPlayerStats.length === 0" class="stats-empty">暂无球员数据</div>

        <table v-else class="stats-table">
          <thead>
            <tr>
              <th>球员</th>
              <th>出场</th>
              <th>得分</th>
              <th>篮板</th>
              <th>助攻</th>
              <th>抢断</th>
              <th>盖帽</th>
              <th>犯规</th>
              <th>失误</th>
              <th>投篮命中率</th>
              <th>三分命中率</th>
              <th>罚球命中率</th>
              <th>查看</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in summaryPlayerStats" :key="row.playerId">
              <td>
                <div class="player-cell">
                  <div class="player-cell__avatar">
                    <img
                      v-if="row.avatar"
                      :src="row.avatar"
                      :alt="row.nickname"
                      class="player-cell__avatar-img"
                    />
                    <span v-else class="player-cell__avatar-placeholder">
                      {{ getPlayerNumber(row.nickname) }}
                    </span>
                  </div>
                  <PlayerLink
                    :player-id="row.playerId"
                    :player-name="row.nickname"
                    tab="overview"
                  />
                </div>
              </td>
              <td>{{ row.games }}</td>
              <td :class="{ 'is-max': isSummaryMax(row, 'points') }">
                {{ formatCount(row.points, summaryStatsMode) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'rebounds') }">
                {{ formatCount(row.rebounds, summaryStatsMode) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'assists') }">
                {{ formatCount(row.assists, summaryStatsMode) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'steals') }">
                {{ formatCount(row.steals, summaryStatsMode) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'blocks') }">
                {{ formatCount(row.blocks, summaryStatsMode) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'fouls') }">
                {{ formatCount(row.fouls, summaryStatsMode) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'turnovers') }">
                {{ formatCount(row.turnovers, summaryStatsMode) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'fgPct') }">
                {{ formatPct(row.fgPct) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'tpPct') }">
                {{ formatPct(row.tpPct) }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'ftPct') }">
                {{ formatPct(row.ftPct) }}
              </td>
              <td>
                <button type="button" class="view-btn" @click="handlePlayerDetailClick(row.playerId)">
                  球员详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="showSingleTable()" class="stats-table-wrap">
        <div v-if="!matchId" class="stats-empty">请从比赛列表选择一场比赛查看单场数据</div>
        <div v-else-if="singleMatchState === 'loading'" class="stats-empty">加载中...</div>
        <div v-else-if="singleMatchState === 'error'" class="stats-empty is-error">
          {{ singleMatchErrorMessage || '加载单场数据失败' }}
        </div>
        <div v-else-if="!singleMatchStats" class="stats-empty">暂无单场数据</div>

        <template v-else>
          <p class="single-match-label">{{ singleMatchStats.label }}</p>

          <table class="stats-table">
            <thead>
              <tr>
                <th>球员</th>
                <th>评分</th>
                <th>得分</th>
                <th>篮板</th>
                <th>助攻</th>
                <th>抢断</th>
                <th>盖帽</th>
                <th>犯规</th>
                <th>失误</th>
                <th>投篮</th>
                <th>三分</th>
                <th>罚球</th>
                <th>查看</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in singleMatchStats.players" :key="row.playerId">
                <td>
                  <div class="player-cell">
                    <div class="player-cell__avatar">
                      <img
                        v-if="row.avatar"
                        :src="row.avatar"
                        :alt="row.nickname"
                        class="player-cell__avatar-img"
                      />
                      <span v-else class="player-cell__avatar-placeholder">
                        {{ getPlayerNumber(row.nickname) }}
                      </span>
                    </div>
                    <PlayerLink
                      :player-id="row.playerId"
                      :player-name="row.nickname"
                      tab="overview"
                    />
                  </div>
                </td>
                <td>{{ row.grade }}</td>
                <td :class="{ 'is-max': isSingleMax(row, 'points') }">{{ row.points }}</td>
                <td :class="{ 'is-max': isSingleMax(row, 'rebounds') }">{{ row.rebounds }}</td>
                <td :class="{ 'is-max': isSingleMax(row, 'assists') }">{{ row.assists }}</td>
                <td :class="{ 'is-max': isSingleMax(row, 'steals') }">{{ row.steals }}</td>
                <td :class="{ 'is-max': isSingleMax(row, 'blocks') }">{{ row.blocks }}</td>
                <td :class="{ 'is-max': isSingleMax(row, 'fouls') }">{{ row.fouls }}</td>
                <td :class="{ 'is-max': isSingleMax(row, 'turnovers') }">{{ row.turnovers }}</td>
                <td>{{ row.fg }}</td>
                <td>{{ row.tp }}</td>
                <td>{{ row.ft }}</td>
                <td>
                  <button type="button" class="view-btn" @click="handlePlayerDetailClick(row.playerId)">
                    球员详情
                  </button>
                </td>
              </tr>
              <tr class="team-total-row">
                <td>球队合计</td>
                <td>{{ singleMatchStats.teamTotal.grade }}</td>
                <td>{{ singleMatchStats.teamTotal.points }}</td>
                <td>{{ singleMatchStats.teamTotal.rebounds }}</td>
                <td>{{ singleMatchStats.teamTotal.assists }}</td>
                <td>{{ singleMatchStats.teamTotal.steals }}</td>
                <td>{{ singleMatchStats.teamTotal.blocks }}</td>
                <td>{{ singleMatchStats.teamTotal.fouls }}</td>
                <td>{{ singleMatchStats.teamTotal.turnovers }}</td>
                <td>{{ singleMatchStats.teamTotal.fg }}</td>
                <td>{{ singleMatchStats.teamTotal.tp }}</td>
                <td>{{ singleMatchStats.teamTotal.ft }}</td>
                <td />
              </tr>
            </tbody>
          </table>
        </template>
      </div>

      <p class="stats-note">
        数据来源：比赛结束后提交的游戏数据截图，经赛事审核后计入。
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
  margin-bottom: 20px;
}

.section-head-title--sm {
  font-size: clamp(18px, 1.8vw, 22px);
}

.stats-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-left: auto;
}

.filter-select select {
  min-width: 160px;
  height: 44px;
  padding: 0 12px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 2px;
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text);
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
}

.filter-select:first-child select {
  min-width: 210px;
  max-width: 240px;
}

.filter-select:last-child select {
  min-width: 150px;
  max-width: 170px;
}

.stats-section + .stats-section {
  margin-top: 40px;
}

.stats-empty {
  padding: 40px 16px;
  text-align: center;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: rgba(25, 25, 25, 0.025);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;

  &.is-error {
    color: var(--cnpa-red);
  }
}

.season-cards {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 14px;
}

.season-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 122px;
  padding: 24px 18px 22px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 2px;
  background: rgba(25, 25, 25, 0.03);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55);
  text-align: center;
  box-sizing: border-box;
}

.season-card__label {
  display: block;
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.62);
  text-align: center;
}

.season-card__value {
  margin: 0;
  font-size: clamp(38px, 3vw, 50px);
  line-height: 1;
  font-weight: 900;
  color: #111;
  letter-spacing: -0.02em;
}

.mode-tabs {
  display: inline-flex;
  border: 1px solid var(--cnpa-paper-border);
  background: var(--cnpa-paper-surface);
}

.mode-tab {
  min-width: 88px;
  height: 36px;
  padding: 0 14px;
  border: none;
  border-right: 1px solid rgba(17, 17, 17, 0.1);
  background: transparent;
  color: var(--cnpa-paper-text-muted);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

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
  min-width: 980px;
  border-collapse: collapse;
  border: 1px solid var(--cnpa-paper-border);
  background: rgba(25, 25, 25, 0.03);

  th,
  td {
    padding: 12px 10px;
    border-top: 1px solid rgba(0, 0, 0, 0.08);
    text-align: center;
    font-size: 12px;
    color: var(--cnpa-paper-text);
    white-space: nowrap;
  }

  th {
    background: var(--cnpa-paper-surface);
    color: var(--cnpa-paper-text-muted);
    font-weight: 800;
  }

  tbody tr {
    background: rgba(25, 25, 25, 0.025);
  }

  tbody tr:hover {
    background: rgba(25, 25, 25, 0.035);
  }
}

.is-max {
  color: var(--cnpa-red);
  font-weight: 900;
}

.player-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.player-cell__avatar {
  width: 32px;
  height: 32px;
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
  background: linear-gradient(135deg, #1a1a1a 0%, #3a3a3a 100%);
  color: #fff;
  font-size: 11px;
  font-weight: 900;
}

.view-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.team-total-row {
  background: var(--cnpa-paper-surface);
  font-weight: 800;
}

.stats-note {
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--cnpa-paper-text-muted);
}

@media (max-width: 1280px) {
  .season-cards {
    gap: 10px;
  }

  .season-card {
    min-height: 112px;
    padding: 20px 12px;
  }

  .season-card__value {
    font-size: 38px;
  }
}

@media (max-width: 992px) {
  .season-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .team-stats__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-filters {
    width: 100%;
    justify-content: flex-start;
    margin-left: 0;
  }

  .section-head--compact {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
