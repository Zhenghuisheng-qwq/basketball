<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTeamPlayerAverageStats,
  getTeamPlayerTotalStats,
  getTeamSeasonStats,
  getTeamSingleMatchStats,
} from '@/mock/team'
import type {
  PlayerStatsMode,
  PlayoffStatsMode,
  TeamPlayerAverageStat,
  TeamPlayerTotalStat,
  TeamSingleMatchPlayerStat,
  TeamStage,
} from '@/types/team'

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

const route = useRoute()

const competitionFilter = ref('2026 CNPA League')
const stageFilter = ref<TeamStage>('regular')
const playerStatsMode = ref<PlayerStatsMode>('average')
const playoffStatsMode = ref<PlayoffStatsMode>('summary')

const teamId = computed(() => route.params.teamId as string)
const matchId = computed(() => (route.query.matchId as string) || 'rng-po-g1')

const seasonStats = computed(() => getTeamSeasonStats(teamId.value, stageFilter.value))

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

const averagePlayerStats = computed(() =>
  getTeamPlayerAverageStats(teamId.value, stageFilter.value),
)

const totalPlayerStats = computed(() => getTeamPlayerTotalStats(teamId.value, stageFilter.value))

const summaryPlayerStats = computed(() => {
  if (stageFilter.value === 'regular') {
    return playerStatsMode.value === 'average' ? averagePlayerStats.value : totalPlayerStats.value
  }
  return playoffStatsMode.value === 'summary'
    ? totalPlayerStats.value
    : averagePlayerStats.value
})

const singleMatchStats = computed(() =>
  getTeamSingleMatchStats(teamId.value, matchId.value),
)

const summaryMaxMap = computed(() => buildMaxMap(summaryPlayerStats.value))

const singleMaxMap = computed(() => {
  if (!singleMatchStats.value) return {}
  return buildSingleMaxMap(singleMatchStats.value.players)
})

function buildMaxMap(rows: Array<TeamPlayerAverageStat | TeamPlayerTotalStat>) {
  const maxes: Partial<Record<NumericColumn, number>> = {}
  for (const col of numericColumns) {
    maxes[col] = Math.max(...rows.map((row) => row[col]))
  }
  return maxes
}

function buildSingleMaxMap(rows: TeamSingleMatchPlayerStat[]) {
  const maxes: Partial<Record<(typeof singleNumericColumns)[number], number>> = {}
  for (const col of singleNumericColumns) {
    maxes[col] = Math.max(...rows.map((row) => row[col]))
  }
  return maxes
}

function isSummaryMax(
  row: TeamPlayerAverageStat | TeamPlayerTotalStat,
  col: NumericColumn,
): boolean {
  return row[col] === summaryMaxMap.value[col]
}

function isSingleMax(row: TeamSingleMatchPlayerStat, col: (typeof singleNumericColumns)[number]) {
  return row[col] === singleMaxMap.value[col]
}

function getPlayerNumber(nickname: string): string {
  const match = nickname.match(/(\d+)$/)
  return match ? match[1].padStart(2, '0') : nickname.slice(-2)
}

function formatCount(value: number, mode: PlayerStatsMode): string {
  return mode === 'average' ? value.toFixed(1) : String(Math.round(value))
}

function formatPct(value: number): string {
  return `${value.toFixed(1)}%`
}

function handlePlayerDetailClick() {
  ElMessage.info('球员详情暂未开放')
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
    <div class="section-head">
      <div class="section-head-group">
        <h2 class="section-head-title">球队数据</h2>
        <span class="section-head-sub">Team Stats</span>
      </div>
    </div>

    <div class="stats-filters">
      <label class="filter-select">
        <span class="filter-label">赛事</span>
        <select v-model="competitionFilter">
          <option value="2026 CNPA League">2026 CNPA League</option>
        </select>
      </label>

      <label class="filter-select">
        <span class="filter-label">赛段</span>
        <select v-model="stageFilter">
          <option value="regular">常规赛</option>
          <option value="playoffs">季后赛</option>
        </select>
      </label>
    </div>

    <section class="stats-section">
      <div class="section-head section-head--compact">
        <div class="section-head-group">
          <h3 class="section-head-title section-head-title--sm">{{ seasonCardTitle.cn }}</h3>
          <span class="section-head-sub">{{ seasonCardTitle.en }}</span>
        </div>
      </div>

      <div v-if="!seasonStats" class="stats-empty">暂无场均数据</div>

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
        <div v-if="summaryPlayerStats.length === 0" class="stats-empty">暂无球员数据</div>

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
                  <span>{{ row.nickname }}</span>
                </div>
              </td>
              <td>{{ row.games }}</td>
              <td :class="{ 'is-max': isSummaryMax(row, 'points') }">
                {{ formatCount(row.points, stageFilter === 'regular' ? playerStatsMode : 'total') }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'rebounds') }">
                {{
                  formatCount(row.rebounds, stageFilter === 'regular' ? playerStatsMode : 'total')
                }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'assists') }">
                {{ formatCount(row.assists, stageFilter === 'regular' ? playerStatsMode : 'total') }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'steals') }">
                {{ formatCount(row.steals, stageFilter === 'regular' ? playerStatsMode : 'total') }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'blocks') }">
                {{ formatCount(row.blocks, stageFilter === 'regular' ? playerStatsMode : 'total') }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'fouls') }">
                {{ formatCount(row.fouls, stageFilter === 'regular' ? playerStatsMode : 'total') }}
              </td>
              <td :class="{ 'is-max': isSummaryMax(row, 'turnovers') }">
                {{
                  formatCount(row.turnovers, stageFilter === 'regular' ? playerStatsMode : 'total')
                }}
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
                <button type="button" class="view-btn" @click="handlePlayerDetailClick">
                  球员详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="showSingleTable()" class="stats-table-wrap">
        <div v-if="!singleMatchStats" class="stats-empty">暂无单场数据</div>

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
                      <div class="player-cell__avatar-placeholder">
                        {{ getPlayerNumber(row.nickname) }}
                      </div>
                    </div>
                    <span>{{ row.nickname }}</span>
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
                  <button type="button" class="view-btn" @click="handlePlayerDetailClick">
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
  .section-head-title {
    color: var(--cnpa-paper-text);
  }

  .section-head-sub {
    color: var(--cnpa-paper-text-muted);
  }
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
  gap: 16px;
  margin-bottom: 32px;
}

.filter-select {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
}

.filter-select select {
  min-width: 160px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.85);
  color: var(--cnpa-paper-text);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
}

.stats-section + .stats-section {
  margin-top: 40px;
}

.stats-empty {
  padding: 40px 16px;
  text-align: center;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.55);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.season-cards {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.season-card {
  padding: 18px 12px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.72);
  text-align: center;
}

.season-card__label {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
}

.season-card__value {
  font-size: clamp(22px, 2vw, 28px);
  font-weight: 900;
  color: var(--cnpa-paper-text);
}

.mode-tabs {
  display: inline-flex;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.72);
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
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.72);

  th,
  td {
    padding: 12px 10px;
    border-top: 1px solid rgba(17, 17, 17, 0.08);
    text-align: center;
    font-size: 12px;
    color: var(--cnpa-paper-text);
    white-space: nowrap;
  }

  th {
    background: rgba(255, 255, 255, 0.55);
    color: var(--cnpa-paper-text-muted);
    font-weight: 800;
  }

  tbody tr:hover {
    background: rgba(255, 255, 255, 0.85);
  }
}

.is-max {
  color: var(--cnpa-red);
  font-weight: 900;
}

.player-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
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
  border: 1px solid rgba(17, 17, 17, 0.16);
  background: #fff;
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
  background: rgba(255, 255, 255, 0.55);
  font-weight: 800;
}

.stats-note {
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--cnpa-paper-text-muted);
}

@media (max-width: 1200px) {
  .season-cards {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .season-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-head--compact {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-select select {
    width: 100%;
  }
}
</style>
