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

const careerRows = computed(() => {
  let rows = props.detail.careerStats
  if (seasonFilter.value !== '全部赛季') {
    rows = rows.filter((row) => row.tournamentName.includes(seasonFilter.value.slice(0, 4)))
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
          <option
            v-for="row in detail.careerStats"
            :key="row.seasonId"
            :value="`${row.seasonId}赛季`"
          >
            赛季 {{ row.seasonId }}
          </option>
        </select>
        <span class="player-detail-data__hint">展示 API 返回的赛季场均统计</span>
      </div>

      <div v-if="careerRows.length === 0" class="player-detail-empty-inline">暂无生涯数据</div>

      <div v-else class="player-detail-table-scroll">
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
            <tr v-for="row in careerRows" :key="row.seasonId">
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

      <div class="player-detail-section-head">
        <div>
          <div class="player-detail-section-head__group">
            <h2 class="player-detail-section-head__title">{{ selectedStage?.tournamentName || '赛季数据' }}</h2>
            <span class="player-detail-section-head__sub">
              {{ selectedStage?.stageLabel || '常规赛' }} · 共 {{ selectedStage?.games || 0 }} 场
            </span>
          </div>
        </div>
      </div>

      <div v-if="seasonStatRows.length === 0" class="player-detail-empty-inline">暂无赛季数据</div>

      <div v-else class="player-detail-table-scroll">
        <table class="player-detail-table">
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
              <td>{{ formatStat(row.points) }}</td>
              <td>{{ formatStat(row.rebounds) }}</td>
              <td>{{ formatStat(row.assists) }}</td>
              <td>{{ formatStat(row.steals) }}</td>
              <td>{{ formatStat(row.blocks) }}</td>
              <td>{{ formatStat(row.fieldGoalPercentage, true) }}</td>
              <td>{{ formatStat(row.threePointPercentage, true) }}</td>
              <td>{{ formatStat(row.freeThrowPercentage, true) }}</td>
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

.player-detail-empty-inline {
  padding: 32px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
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
