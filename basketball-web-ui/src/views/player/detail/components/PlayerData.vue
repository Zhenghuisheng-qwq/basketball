<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { normalizeDataMode, type PlayerDataMode } from '@/utils/playerNavigate'
import type { PlayerCareerStatistics, PlayerDetailData } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
}>()

const route = useRoute()
const router = useRouter()

const dataMode = ref<PlayerDataMode>(normalizeDataMode(route.query.dataMode))
const statMode = ref<'average' | 'total'>('average')
const seasonFilter = ref('全部赛季')
const selectedStageId = ref(props.detail.seasonStageOptions[0]?.id ?? '')
const dropdownOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

const selectedStage = computed(
  () =>
    props.detail.seasonStageOptions.find((item) => item.id === selectedStageId.value) ??
    props.detail.seasonStageOptions[0],
)

const seasonFilterOptions = computed(() => {
  const seen = new Map<string, string>()
  for (const row of props.detail.careerStats) {
    if (row.seasonId && !seen.has(row.seasonId)) {
      seen.set(row.seasonId, row.tournamentName || `赛季 ${row.seasonId}`)
    }
  }
  return [...seen.entries()].map(([id, label]) => ({ id, label }))
})

const careerRows = computed(() => {
  let rows = props.detail.careerStats
  if (seasonFilter.value !== '全部赛季') {
    rows = rows.filter((row) => row.seasonId === seasonFilter.value)
  }
  return rows
})

const careerAverageRow = computed(() => {
  const rows = careerRows.value
  if (rows.length === 0) return null
  const totalGames = rows.reduce((sum, row) => sum + row.games, 0)
  const avg = (key: keyof PlayerCareerStatistics) =>
    rows.reduce((sum, row) => sum + Number(row[key] ?? 0), 0) / rows.length
  const avgPct = (key: 'fieldGoalPercentage' | 'threePointPercentage' | 'freeThrowPercentage') => {
    const values = rows
      .map((row) => row[key])
      .filter((value): value is number => value !== null && value !== undefined && Number.isFinite(Number(value)))
      .map(Number)
    if (!values.length) return null
    return values.reduce((sum, value) => sum + value, 0) / values.length
  }
  return {
    seasonId: '',
    divisionId: '',
    tournamentName: '生涯平均',
    teamId: '',
    teamLogoUrl: '',
    teamName: '-',
    platform: '-',
    games: totalGames,
    points: avg('points'),
    rebounds: avg('rebounds'),
    assists: avg('assists'),
    steals: avg('steals'),
    blocks: avg('blocks'),
    turnovers: avg('turnovers'),
    fouls: avg('fouls'),
    fieldGoalPercentage: avgPct('fieldGoalPercentage'),
    threePointPercentage: avgPct('threePointPercentage'),
    freeThrowPercentage: avgPct('freeThrowPercentage'),
  }
})

const seasonStatRows = computed(() => {
  if (!selectedStage.value) return []
  return props.detail.careerStats.filter((row) => row.seasonId === selectedStage.value.id)
})

function formatStat(value: number | null | undefined, isPct = false): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  const num = Number(value)
  if (isPct) {
    // 后端可能返回 0~1 或 0~100
    const pct = num <= 1 ? num * 100 : num
    return `${pct.toFixed(1)}%`
  }
  if (statMode.value === 'average') return Number.isInteger(num) ? String(num) : num.toFixed(1)
  return String(Math.round(num))
}

function setDataMode(mode: PlayerDataMode) {
  dataMode.value = mode
  router.replace({
    query: {
      ...route.query,
      dataMode: mode,
    },
  })
}

function selectStage(id: string) {
  selectedStageId.value = id
  dropdownOpen.value = false
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value
}

function handleDocumentClick(event: MouseEvent) {
  if (!dropdownOpen.value || !dropdownRef.value) return
  if (!dropdownRef.value.contains(event.target as Node)) dropdownOpen.value = false
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') dropdownOpen.value = false
}

watch(
  () => route.query.dataMode,
  (value) => {
    dataMode.value = normalizeDataMode(value)
  },
)

watch(
  () => props.detail.seasonStageOptions,
  (options) => {
    if (!options.some((item) => item.id === selectedStageId.value)) {
      selectedStageId.value = options[0]?.id ?? ''
    }
  },
  { immediate: true },
)

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="player-detail-data">
    <header class="player-detail-data__intro">
      <h2 class="player-detail-data__title">
        球员数据
        <span>PLAYER DATA</span>
      </h2>
      <p class="player-detail-data__desc">
        汇总球员在 CNPA 公开赛事中的生涯与赛季表现，仅统计已审核的正式比赛数据
      </p>
    </header>

    <div class="player-detail-data__mode-tabs" role="tablist" aria-label="数据范围">
      <button
        type="button"
        class="player-detail-data__mode-tab"
        :class="{ 'is-active': dataMode === 'career' }"
        @click="setDataMode('career')"
      >
        生涯数据
      </button>
      <button
        type="button"
        class="player-detail-data__mode-tab"
        :class="{ 'is-active': dataMode === 'season' }"
        @click="setDataMode('season')"
      >
        赛事数据
      </button>
    </div>

    <template v-if="dataMode === 'career'">
      <section class="player-detail-data__panel" aria-label="生涯数据表">
        <div class="player-detail-data__panel-head">
          <strong>生涯数据</strong>
          <div class="player-detail-stat-switch" role="group" aria-label="统计类型">
            <button
              type="button"
              class="player-detail-stat-switch__btn"
              :class="{ 'is-active': statMode === 'average' }"
              @click="statMode = 'average'"
            >
              场均
            </button>
            <button
              type="button"
              class="player-detail-stat-switch__btn"
              :class="{ 'is-active': statMode === 'total' }"
              @click="statMode = 'total'"
            >
              累计
            </button>
          </div>
        </div>

        <div class="player-detail-data__panel-filter">
          <label class="player-detail-data__filter-label" for="player-career-stage-filter">统计赛段</label>
          <select id="player-career-stage-filter" v-model="seasonFilter">
            <option value="全部赛季">全部赛段</option>
            <option
              v-for="row in detail.careerStats"
              :key="row.seasonId"
              :value="`${row.seasonId}赛季`"
            >
              {{ row.tournamentName || `赛季 ${row.seasonId}` }}
            </option>
          </select>
          <span class="player-detail-data__hint">场均按有效出场场次计算，投篮百分比仅统计有投篮数据的场次</span>
        </div>

        <div v-if="careerRows.length === 0" class="player-detail-empty-inline">暂无生涯数据</div>

        <div v-else class="player-detail-table-scroll">
          <table class="player-detail-table player-detail-table--career">
            <thead>
              <tr>
                <th class="is-left">赛事</th>
                <th class="is-left">球队</th>
                <th>平台</th>
                <th>出场 <small>GP</small></th>
                <th>得分 <small>PTS</small></th>
                <th>篮板 <small>REB</small></th>
                <th>助攻 <small>AST</small></th>
                <th>抢断 <small>STL</small></th>
                <th>盖帽 <small>BLK</small></th>
                <th>失误 <small>TOV</small></th>
                <th>犯规 <small>PF</small></th>
                <th>投篮% <small>FG</small></th>
                <th>三分% <small>3P</small></th>
                <th>罚球% <small>FT</small></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in careerRows" :key="`${row.seasonId}-${row.teamId}`">
                <td class="is-left">{{ row.tournamentName }}</td>
                <td class="is-left">
                  <router-link :to="`/tournament/team/${row.teamId}/overview`" class="player-detail-team-link">
                    <TeamLogo
                      :team="{ id: row.teamId, name: row.teamName, shortName: row.teamName, logoUrl: row.teamLogoUrl }"
                      size="xs"
                    />
                    <span>{{ row.teamName }}</span>
                  </router-link>
                </td>
                <td>{{ row.platform }}</td>
                <td>{{ row.games }}</td>
                <td class="is-accent">{{ formatStat(row.points) }}</td>
                <td>{{ formatStat(row.rebounds) }}</td>
                <td class="is-accent">{{ formatStat(row.assists) }}</td>
                <td class="is-accent">{{ formatStat(row.steals) }}</td>
                <td>{{ formatStat(row.blocks) }}</td>
                <td>{{ formatStat(row.turnovers) }}</td>
                <td>{{ formatStat(row.fouls) }}</td>
                <td class="is-accent">{{ formatStat(row.fieldGoalPercentage, true) }}</td>
                <td class="is-accent">{{ formatStat(row.threePointPercentage, true) }}</td>
                <td class="is-accent">{{ formatStat(row.freeThrowPercentage, true) }}</td>
              </tr>
              <tr v-if="careerAverageRow" class="is-summary">
                <td class="is-left">{{ careerAverageRow.tournamentName }}</td>
                <td colspan="2">-</td>
                <td>{{ careerAverageRow.games }}</td>
                <td class="is-accent">{{ formatStat(careerAverageRow.points) }}</td>
                <td>{{ formatStat(careerAverageRow.rebounds) }}</td>
                <td class="is-accent">{{ formatStat(careerAverageRow.assists) }}</td>
                <td class="is-accent">{{ formatStat(careerAverageRow.steals) }}</td>
                <td>{{ formatStat(careerAverageRow.blocks) }}</td>
                <td>{{ formatStat(careerAverageRow.turnovers) }}</td>
                <td>{{ formatStat(careerAverageRow.fouls) }}</td>
                <td class="is-accent">{{ formatStat(careerAverageRow.fieldGoalPercentage, true) }}</td>
                <td class="is-accent">{{ formatStat(careerAverageRow.threePointPercentage, true) }}</td>
                <td class="is-accent">{{ formatStat(careerAverageRow.freeThrowPercentage, true) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-if="careerRows.length" class="player-detail-data__source">
          数据来源：比赛结束后提交并通过赛事审核的比赛数据。
        </p>
      </section>
    </template>

    <template v-else>
      <section class="player-detail-data__panel" aria-label="赛事数据表">
        <div class="player-detail-data__panel-head">
          <strong>{{ selectedStage?.tournamentName || '赛事数据' }}</strong>
          <span class="player-detail-data__panel-meta">
            {{ selectedStage?.stageLabel || '常规赛' }} · 共 {{ selectedStage?.games || 0 }} 场
          </span>
        </div>

        <div class="player-detail-data__panel-filter">
          <div ref="dropdownRef" class="player-detail-stage-selector" :class="{ 'is-open': dropdownOpen }">
            <button type="button" class="player-detail-stage-selector__trigger" @click.stop="toggleDropdown">
              {{ selectedStage?.label || '暂无赛季' }}
              <span class="player-detail-stage-selector__arrow" aria-hidden="true" />
            </button>
            <div v-show="dropdownOpen" class="player-detail-stage-selector__dropdown">
              <button
                v-for="option in detail.seasonStageOptions"
                :key="option.id"
                type="button"
                class="player-detail-stage-selector__option"
                :class="{ 'is-selected': option.id === selectedStageId }"
                @click="selectStage(option.id)"
              >
                {{ option.label }}
                <span class="player-detail-stage-selector__check" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>

        <div v-if="seasonStatRows.length === 0" class="player-detail-empty-inline">暂无赛事数据</div>

        <div v-else class="player-detail-table-scroll">
          <table class="player-detail-table player-detail-table--career">
          <thead>
            <tr>
              <th>球队</th>
              <th>出场</th>
              <th>得分</th>
              <th>篮板</th>
              <th>助攻</th>
              <th>抢断</th>
              <th>盖帽</th>
              <th>投篮%</th>
              <th>三分%</th>
              <th>罚球%</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in seasonStatRows" :key="row.seasonId">
              <td class="is-left">{{ row.teamName }}</td>
              <td>{{ row.games }}</td>
              <td class="is-accent">{{ formatStat(row.points) }}</td>
              <td>{{ formatStat(row.rebounds) }}</td>
              <td class="is-accent">{{ formatStat(row.assists) }}</td>
              <td class="is-accent">{{ formatStat(row.steals) }}</td>
              <td>{{ formatStat(row.blocks) }}</td>
              <td class="is-accent">{{ formatStat(row.fieldGoalPercentage, true) }}</td>
              <td class="is-accent">{{ formatStat(row.threePointPercentage, true) }}</td>
              <td class="is-accent">{{ formatStat(row.freeThrowPercentage, true) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-data {
  box-sizing: border-box;
}

.player-detail-data__intro {
  margin-bottom: 22px;
}

.player-detail-data__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0;
  font-size: clamp(28px, 2.6vw, 36px);
  font-weight: 900;
  letter-spacing: 0.02em;
  color: var(--cnpa-paper-text);

  span {
    color: var(--cnpa-red);
    font-size: 0.42em;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.14em;
  }
}

.player-detail-data__desc {
  margin: 8px 0 0;
  max-width: 640px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.player-detail-data__mode-tabs {
  display: inline-flex;
  margin: 0 0 22px;
  overflow: hidden;
  border: 1px solid var(--cnpa-public-border);
  border-radius: 4px;
  background: #fff;
}

.player-detail-data__mode-tab {
  position: relative;
  min-width: 124px;
  height: 42px;
  margin: 0;
  padding: 0 22px;
  border: 0;
  background: #fff;
  color: #666;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;

  &.is-active {
    background: #171717;
    color: #fff;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--cnpa-red);
    }
  }
}

.player-detail-data__panel {
  overflow: hidden;
  border: 1px solid var(--cnpa-public-border);
  background: #fff;
}

.player-detail-data__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 56px;
  padding: 0 20px;
  background: #171717;
  color: #fff;
}

.player-detail-data__panel-head strong {
  font-size: 16px;
  font-weight: 800;
}

.player-detail-data__panel-meta {
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
  font-weight: 700;
}

.player-detail-data__panel-head .player-detail-stat-switch {
  gap: 6px;
}

.player-detail-data__panel-head .player-detail-stat-switch__btn {
  min-width: 64px;
  height: 32px;
  padding: 0 14px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: rgba(255, 255, 255, 0.55);

  &.is-active {
    background: var(--cnpa-red);
    color: #fff;
    box-shadow: none;
  }
}

.player-detail-data__panel-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 20px;
  background: #f6f5f4;
  border-bottom: 1px solid var(--cnpa-public-border);
}

.player-detail-data__filter-label {
  color: #555;
  font-size: 13px;
  font-weight: 800;
}

.player-detail-data__panel-filter select {
  min-width: 180px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--cnpa-public-border);
  border-radius: 3px;
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
}

.player-detail-data__hint {
  margin-left: auto;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-data__source {
  margin: 0;
  padding: 12px 20px 16px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-data__panel .player-detail-table-scroll {
  border: 0;
  box-shadow: none;
}

.player-detail-data__panel .player-detail-table--career {
  min-width: 1080px;
}

.player-detail-data__panel .player-detail-table--career th,
.player-detail-data__panel .player-detail-table--career td {
  text-align: center;
}

.player-detail-data__panel .player-detail-table--career .is-left {
  text-align: left;
}

.player-detail-data__panel .player-detail-table--career thead th {
  height: 42px;
  background: #f0eeec;
  color: #555;
  font-size: 12px;
  font-weight: 800;
  border-bottom: 1px solid var(--cnpa-public-border);

  small {
    margin-left: 4px;
    color: #999;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.04em;
  }
}

.player-detail-data__panel .player-detail-table--career tbody td {
  background: #fff;
}

.player-detail-data__panel .player-detail-table--career tbody tr:nth-child(even) td {
  background: #f7f6f5;
}

.player-detail-data__panel .player-detail-table--career tbody tr:hover td {
  background: #f3f1ee;
}

.player-detail-data__panel .player-detail-table--career .is-accent {
  color: var(--cnpa-red);
  font-weight: 800;
}

.player-detail-data__panel .player-detail-table--career tbody tr.is-summary td {
  background: #ebe6dc;
  font-weight: 800;
}

.player-detail-empty-inline {
  margin: 16px 20px 20px;
  padding: 32px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.player-detail-stage-selector {
  position: relative;
  width: min(100%, 320px);
}

.player-detail-stage-selector__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 4px;
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.player-detail-stage-selector__arrow {
  width: 8px;
  height: 8px;
  border-right: 1.5px solid #666;
  border-bottom: 1.5px solid #666;
  transform: rotate(45deg);
}

.player-detail-stage-selector__dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 20;
  width: 100%;
  padding: 8px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 4px;
  background: var(--cnpa-paper-surface);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
}

.player-detail-stage-selector__option {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 34px;
  margin: 0 0 4px;
  padding: 0 34px 0 10px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  text-align: left;
  cursor: pointer;

  &:hover {
    background: rgba(25, 25, 25, 0.04);
  }

  &.is-selected {
    background: rgba(225, 6, 19, 0.08);
    color: var(--cnpa-red);
  }
}

.player-detail-stage-selector__check {
  position: absolute;
  right: 10px;
  display: none;
  width: 14px;
  height: 14px;
  border: 1.5px solid var(--cnpa-red);
  border-radius: 50%;

  &::before {
    content: '';
    position: absolute;
    left: 3px;
    top: 2px;
    width: 5px;
    height: 3px;
    border-left: 1.5px solid var(--cnpa-red);
    border-bottom: 1.5px solid var(--cnpa-red);
    transform: rotate(-45deg);
  }
}

.player-detail-stage-selector__option.is-selected .player-detail-stage-selector__check {
  display: block;
}

.player-detail-data__mode-tab:focus-visible,
.player-detail-data__panel-filter select:focus-visible,
.player-detail-stage-selector__trigger:focus-visible,
.player-detail-stage-selector__option:focus-visible {
  outline: 2px solid rgba(225, 6, 19, 0.4);
  outline-offset: 2px;
}

@media (max-width: 767px) {
  .player-detail-data__panel-head,
  .player-detail-data__panel-filter {
    align-items: flex-start;
    flex-direction: column;
  }

  .player-detail-data__hint {
    margin-left: 0;
  }

  .player-detail-data__panel-filter select,
  .player-detail-stage-selector {
    width: 100%;
    min-width: 0;
  }
}
</style>
