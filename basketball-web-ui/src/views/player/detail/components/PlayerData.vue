<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { normalizeDataMode, type PlayerDataMode } from '@/utils/playerNavigate'
import type { PlayerCareerStatistics, PlayerDetailData } from '../mock/playerDetail'

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

const careerRows = computed(() => {
  let rows = props.detail.careerStats
  if (seasonFilter.value !== '全部赛季') {
    rows = rows.filter((row) => row.tournamentName.startsWith(seasonFilter.value.slice(0, 4)))
  }
  return rows
})

const careerAverageRow = computed(() => {
  const rows = careerRows.value
  if (rows.length === 0) return null
  const totalGames = rows.reduce((sum, row) => sum + row.games, 0)
  const avg = (key: keyof PlayerCareerStatistics) =>
    rows.reduce((sum, row) => sum + Number(row[key]), 0) / rows.length
  return {
    tournamentName: '生涯平均',
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
    fieldGoalPercentage: avg('fieldGoalPercentage'),
    threePointPercentage: avg('threePointPercentage'),
    freeThrowPercentage: avg('freeThrowPercentage'),
  }
})

const seasonGames = computed(() => props.detail.seasonGames[selectedStageId.value] ?? [])

const seasonAverageRow = computed(() => {
  const rows = seasonGames.value
  if (rows.length === 0) return null
  const count = rows.length
  const sum = (getter: (row: (typeof rows)[number]) => number) =>
    rows.reduce((total, row) => total + getter(row), 0) / count
  return {
    date: '赛事场均',
    opponentName: '-',
    result: '-',
    teamScore: 0,
    opponentScore: 0,
    points: sum((row) => row.points),
    rebounds: sum((row) => row.rebounds),
    assists: sum((row) => row.assists),
    steals: sum((row) => row.steals),
    blocks: sum((row) => row.blocks),
    turnovers: sum((row) => row.turnovers),
    fouls: sum((row) => row.fouls),
    fieldGoalsMade: sum((row) => row.fieldGoalsMade),
    fieldGoalsAttempted: sum((row) => row.fieldGoalsAttempted),
    threePointsMade: sum((row) => row.threePointsMade),
    threePointsAttempted: sum((row) => row.threePointsAttempted),
    freeThrowsMade: sum((row) => row.freeThrowsMade),
    freeThrowsAttempted: sum((row) => row.freeThrowsAttempted),
  }
})

function formatStat(value: number, isPct = false): string {
  if (isPct) return `${value.toFixed(1)}%`
  if (statMode.value === 'average') return Number.isInteger(value) ? String(value) : value.toFixed(1)
  return String(Math.round(value * 12))
}

function formatShot(made: number, attempted: number): string {
  return `${made}/${attempted}`
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
    <div class="player-detail-data__mode-tabs">
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
        赛季数据
      </button>
    </div>

    <template v-if="dataMode === 'career'">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 class="player-detail-section-head__title">生涯数据</h2>
        </div>
        <div class="player-detail-stat-switch">
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

      <div class="player-detail-filter-row">
        <select v-model="seasonFilter">
          <option value="全部赛季">全部赛季</option>
          <option value="2026赛季">2026赛季</option>
          <option value="2025赛季">2025赛季</option>
          <option value="2024赛季">2024赛季</option>
        </select>
        <span class="player-detail-data__hint">命中率按累计命中数 ÷ 累计出手数计算</span>
      </div>

      <div class="player-detail-table-scroll">
        <table class="player-detail-table">
          <thead>
            <tr>
              <th>赛事</th>
              <th>球队</th>
              <th>平台</th>
              <th>出场</th>
              <th>得分</th>
              <th>篮板</th>
              <th>助攻</th>
              <th>抢断</th>
              <th>盖帽</th>
              <th>失误</th>
              <th>犯规</th>
              <th>投篮%</th>
              <th>三分%</th>
              <th>罚球%</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in careerRows" :key="row.tournamentName">
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
              <td>{{ formatStat(row.points) }}</td>
              <td>{{ formatStat(row.rebounds) }}</td>
              <td>{{ formatStat(row.assists) }}</td>
              <td>{{ formatStat(row.steals) }}</td>
              <td>{{ formatStat(row.blocks) }}</td>
              <td>{{ formatStat(row.turnovers) }}</td>
              <td>{{ formatStat(row.fouls) }}</td>
              <td>{{ formatStat(row.fieldGoalPercentage, true) }}</td>
              <td>{{ formatStat(row.threePointPercentage, true) }}</td>
              <td>{{ formatStat(row.freeThrowPercentage, true) }}</td>
            </tr>
            <tr v-if="careerAverageRow" class="is-summary">
              <td class="is-left">{{ careerAverageRow.tournamentName }}</td>
              <td colspan="2">-</td>
              <td>{{ careerAverageRow.games }}</td>
              <td>{{ formatStat(careerAverageRow.points) }}</td>
              <td>{{ formatStat(careerAverageRow.rebounds) }}</td>
              <td>{{ formatStat(careerAverageRow.assists) }}</td>
              <td>{{ formatStat(careerAverageRow.steals) }}</td>
              <td>{{ formatStat(careerAverageRow.blocks) }}</td>
              <td>{{ formatStat(careerAverageRow.turnovers) }}</td>
              <td>{{ formatStat(careerAverageRow.fouls) }}</td>
              <td>{{ formatStat(careerAverageRow.fieldGoalPercentage, true) }}</td>
              <td>{{ formatStat(careerAverageRow.threePointPercentage, true) }}</td>
              <td>{{ formatStat(careerAverageRow.freeThrowPercentage, true) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <template v-else>
      <div ref="dropdownRef" class="player-detail-stage-selector" :class="{ 'is-open': dropdownOpen }">
        <button type="button" class="player-detail-stage-selector__trigger" @click.stop="toggleDropdown">
          {{ selectedStage?.label }}
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

      <div class="player-detail-section-head">
        <div>
          <div class="player-detail-section-head__group">
            <h2 class="player-detail-section-head__title">{{ selectedStage?.tournamentName }}</h2>
            <span class="player-detail-section-head__sub">
              {{ selectedStage?.stageLabel }} - 单场数据 · 共 {{ selectedStage?.games }} 场
            </span>
          </div>
        </div>
      </div>

      <div class="player-detail-table-scroll">
        <table class="player-detail-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>对手</th>
              <th>结果</th>
              <th>比赛比分</th>
              <th>得分</th>
              <th>篮板</th>
              <th>助攻</th>
              <th>抢断</th>
              <th>盖帽</th>
              <th>失误</th>
              <th>犯规</th>
              <th>投篮</th>
              <th>三分</th>
              <th>罚球</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in seasonGames" :key="row.id">
              <td>{{ row.date }}</td>
              <td>{{ row.opponentName }}</td>
              <td :class="row.result === 'WIN' ? 'player-detail-result--win' : 'player-detail-result--loss'">
                {{ row.result === 'WIN' ? '胜利' : '失败' }}
              </td>
              <td>{{ row.teamScore }}:{{ row.opponentScore }}</td>
              <td>{{ row.points }}</td>
              <td>{{ row.rebounds }}</td>
              <td>{{ row.assists }}</td>
              <td>{{ row.steals }}</td>
              <td>{{ row.blocks }}</td>
              <td>{{ row.turnovers }}</td>
              <td>{{ row.fouls }}</td>
              <td>{{ formatShot(row.fieldGoalsMade, row.fieldGoalsAttempted) }}</td>
              <td>{{ formatShot(row.threePointsMade, row.threePointsAttempted) }}</td>
              <td>{{ formatShot(row.freeThrowsMade, row.freeThrowsAttempted) }}</td>
            </tr>
            <tr v-if="seasonAverageRow" class="is-summary">
              <td>{{ seasonAverageRow.date }}</td>
              <td colspan="3">-</td>
              <td>{{ seasonAverageRow.points.toFixed(1) }}</td>
              <td>{{ seasonAverageRow.rebounds.toFixed(1) }}</td>
              <td>{{ seasonAverageRow.assists.toFixed(1) }}</td>
              <td>{{ seasonAverageRow.steals.toFixed(1) }}</td>
              <td>{{ seasonAverageRow.blocks.toFixed(1) }}</td>
              <td>{{ seasonAverageRow.turnovers.toFixed(1) }}</td>
              <td>{{ seasonAverageRow.fouls.toFixed(1) }}</td>
              <td>
                {{
                  formatShot(
                    Math.round(seasonAverageRow.fieldGoalsMade),
                    Math.round(seasonAverageRow.fieldGoalsAttempted),
                  )
                }}
              </td>
              <td>
                {{
                  formatShot(
                    Math.round(seasonAverageRow.threePointsMade),
                    Math.round(seasonAverageRow.threePointsAttempted),
                  )
                }}
              </td>
              <td>
                {{
                  formatShot(
                    Math.round(seasonAverageRow.freeThrowsMade),
                    Math.round(seasonAverageRow.freeThrowsAttempted),
                  )
                }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-data__mode-tabs {
  display: inline-flex;
  gap: 28px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--cnpa-paper-border);
}

.player-detail-data__mode-tab {
  position: relative;
  margin: 0;
  padding: 0 2px 12px;
  border: 0;
  background: transparent;
  color: var(--cnpa-paper-text-secondary);
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;

  &.is-active {
    color: var(--cnpa-red);

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -1px;
      height: 3px;
      background: var(--cnpa-red);
    }
  }
}

.player-detail-data__hint {
  align-self: center;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-stage-selector {
  position: relative;
  width: min(100%, 320px);
  margin-bottom: 18px;
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
</style>
