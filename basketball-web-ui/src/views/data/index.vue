<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import PaperTear from '@/components/common/PaperTear.vue'
import { getPublicDataCenterSeasons } from '@/api/publicDataCenter'
import {
  getPublicSeasonPlayerStatPage,
  getPublicSeasonTeamStatPage,
} from '@/api/publicSeasonStat'
import type { PublicPlayerSeasonStatVo, PublicTeamSeasonStatVo } from '@/api/types/public'
import type { PageResult } from '@/api/types/common'
import { displayText, toNumber } from '@/utils/publicDisplay'
import DataHero from './components/DataHero.vue'
import DataTypeTabs from './components/DataTypeTabs.vue'
import DataStatisticsTable from './components/DataStatisticsTable.vue'
import DataPagination from './components/DataPagination.vue'
import DataSearchBar from './components/DataSearchBar.vue'
import type {
  DataType,
  PlayerStatistics,
  StatisticType,
  TeamStatistics,
  TournamentGroup,
} from './types'

const activeDataType = ref<DataType>('team')
const activeStatisticType = ref<StatisticType>('average')
const selectedSeasonId = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const appliedKeyword = ref('')

const seasonGroups = ref<TournamentGroup[]>([])
const seasonLoading = ref(true)
const seasonError = ref('')

const rows = ref<Array<TeamStatistics | PlayerStatistics>>([])
const tableLoading = ref(false)
const tableError = ref('')
let reqId = 0

function toPct(value: unknown): number {
  const num = toNumber(value, 0)
  return num <= 1 ? num * 100 : num
}

function mapTeamRow(item: PublicTeamSeasonStatVo, index: number): TeamStatistics {
  const name = displayText(item.teamName, '未知球队')
  const games = toNumber(item.games, 0)
  return {
    id: String(item.teamId),
    rank: (currentPage.value - 1) * pageSize.value + index + 1,
    teamId: String(item.teamId),
    teamName: name,
    teamAbbr: name.slice(0, 6),
    logoUrl: item.teamLogoUrl?.trim() || '',
    logoColor: '#333',
    games,
    wins: toNumber(item.wins, 0),
    losses: toNumber(item.losses, 0),
    winRate: toPct(item.winRate),
    averagePoints: toNumber(item.avgPoints, 0),
    averageRebounds: toNumber(item.avgRebounds, 0),
    averageAssists: toNumber(item.avgAssists, 0),
    averageSteals: toNumber(item.avgSteals, 0),
    averageBlocks: toNumber(item.avgBlocks, 0),
    averageTurnovers: toNumber(item.avgTurnovers, 0),
    totalPoints: toNumber(item.pointsFor, 0),
    totalRebounds: Math.round(toNumber(item.avgRebounds, 0) * games),
    totalAssists: Math.round(toNumber(item.avgAssists, 0) * games),
    totalSteals: Math.round(toNumber(item.avgSteals, 0) * games),
    totalBlocks: Math.round(toNumber(item.avgBlocks, 0) * games),
    totalTurnovers: Math.round(toNumber(item.avgTurnovers, 0) * games),
    fieldGoalPct: toPct(item.fieldGoalPct),
    threePointPct: toPct(item.threePointPct),
    freeThrowPct: toPct(item.freeThrowPct),
  }
}

function mapPlayerRow(item: PublicPlayerSeasonStatVo, index: number): PlayerStatistics {
  const teamName = displayText(item.teamName, '--')
  return {
    id: String(item.playerId),
    playerId: String(item.playerId),
    rank: (currentPage.value - 1) * pageSize.value + index + 1,
    playerName: displayText(item.playerName, '未知球员'),
    avatarUrl: item.avatarUrl?.trim() || '',
    teamId: String(item.teamId || ''),
    teamName,
    teamAbbr: teamName.slice(0, 6),
    teamLogoUrl: item.teamLogoUrl?.trim() || '',
    logoColor: '#333',
    position: '--',
    games: toNumber(item.games, 0),
    averagePoints: toNumber(item.avgPoints, 0),
    averageRebounds: toNumber(item.avgRebounds, 0),
    averageAssists: toNumber(item.avgAssists, 0),
    averageSteals: toNumber(item.avgSteals, 0),
    averageBlocks: toNumber(item.avgBlocks, 0),
    averageTurnovers: 0,
    totalPoints: toNumber(item.points, 0),
    totalRebounds: toNumber(item.rebounds, 0),
    totalAssists: toNumber(item.assists, 0),
    totalSteals: toNumber(item.steals, 0),
    totalBlocks: toNumber(item.blocks, 0),
    totalTurnovers: 0,
    fieldGoalPct: toPct(item.fieldGoalPct),
    threePointPct: toPct(item.threePointPct),
    freeThrowPct: toPct(item.freeThrowPct),
  }
}

function extractPageResult<T>(data: PageResult<T> | undefined | null) {
  const records = data?.records ?? []
  const totalCount = Number(data?.total ?? 0)
  const current = Number(data?.current ?? 0)
  const size = Number(data?.size ?? 0)
  return {
    records,
    total: Number.isFinite(totalCount) ? totalCount : records.length,
    current: Number.isFinite(current) ? current : 1,
    size: Number.isFinite(size) ? size : records.length,
  }
}

const sortField = computed(() => {
  if (activeStatisticType.value === 'total') {
    return activeDataType.value === 'team' ? 'wins' : 'points'
  }
  return 'avgPoints'
})

function buildQueryParams() {
  const params: {
    currentPage: number
    pageSize: number
    sortField: string
    sortOrder: string
    keyword?: string
  } = {
    currentPage: currentPage.value,
    pageSize: pageSize.value,
    sortField: sortField.value,
    sortOrder: 'desc',
  }
  const keyword = appliedKeyword.value.trim()
  if (keyword) {
    params.keyword = keyword
  }
  return params
}

function onSearch() {
  appliedKeyword.value = searchKeyword.value.trim()
  currentPage.value = 1
}

async function loadSeasons() {
  seasonLoading.value = true
  seasonError.value = ''
  try {
    const res = await getPublicDataCenterSeasons()
    const list = res.data ?? []
    const groupMap = new Map<string, TournamentGroup>()
    for (const item of list) {
      const seasonKey = displayText(
        item.competitionSeasonName
          ? `${item.competitionSeasonName}${item.competitionSeasonYear ? ` (${item.competitionSeasonYear})` : ''}`
          : item.seasonYear || item.tournamentName,
        '赛季',
      )
      if (!groupMap.has(seasonKey)) {
        groupMap.set(seasonKey, { season: seasonKey, tournaments: [] })
      }
      groupMap.get(seasonKey)!.tournaments.push({
        id: item.seasonId,
        name: displayText(item.seasonName, item.seasonId),
        season: seasonKey,
        label: displayText(
          item.tournamentName
            ? `${item.tournamentName} · ${item.seasonName || item.seasonId}`
            : item.seasonName,
          item.seasonId,
        ),
      })
    }
    seasonGroups.value = [...groupMap.values()]
    if (!selectedSeasonId.value && list[0]) {
      selectedSeasonId.value = list[0].seasonId
    }
  } catch (err) {
    seasonGroups.value = []
    seasonError.value = err instanceof Error ? err.message : '加载赛季失败'
  } finally {
    seasonLoading.value = false
  }
}

async function loadTable() {
  const seasonId = selectedSeasonId.value
  const current = ++reqId
  if (!seasonId) {
    rows.value = []
    total.value = 0
    return
  }
  tableLoading.value = true
  tableError.value = ''
  try {
    const params = buildQueryParams()
    if (activeDataType.value === 'team') {
      const res = await getPublicSeasonTeamStatPage(seasonId, params)
      if (current !== reqId) return
      const page = extractPageResult(res.data)
      rows.value = page.records.map(mapTeamRow)
      total.value = page.total
    } else {
      const res = await getPublicSeasonPlayerStatPage(seasonId, params)
      if (current !== reqId) return
      const page = extractPageResult(res.data)
      rows.value = page.records.map(mapPlayerRow)
      total.value = page.total
    }
  } catch (err) {
    if (current !== reqId) return
    rows.value = []
    total.value = 0
    tableError.value = err instanceof Error ? err.message : '加载数据失败'
  } finally {
    if (current === reqId) tableLoading.value = false
  }
}

watch([activeDataType, selectedSeasonId], () => {
  currentPage.value = 1
  searchKeyword.value = ''
  appliedKeyword.value = ''
})

watch(activeStatisticType, () => {
  currentPage.value = 1
})

watch([activeDataType, selectedSeasonId, currentPage, pageSize, activeStatisticType, appliedKeyword], () => {
  void loadTable()
})

onMounted(() => {
  void loadSeasons()
})

const pagedRows = computed(() => rows.value)
</script>

<template>
  <div class="data-center-page">
    <DataHero
      v-model:tournament-id="selectedSeasonId"
      :groups="seasonGroups"
      :loading="seasonLoading"
      :error="seasonError"
    />

    <section class="data-center-page__main">
      <div class="site-shell">
        <DataTypeTabs v-model="activeDataType" />

        <div class="data-center-panel">
          <div class="data-center-panel__tear-top" aria-hidden="true">
            <PaperTear position="top" tone="light" />
          </div>

          <div class="data-center-panel__shell">
            <div class="data-center-panel__body">
              <div class="data-center-panel__toolbar">
                <DataSearchBar
                  v-model="searchKeyword"
                  :placeholder="activeDataType === 'team' ? '搜索战队名称或编号' : '搜索球员昵称或编号'"
                  @search="onSearch"
                />
                <div class="data-stat-switch" role="group" aria-label="统计类型">
                  <button
                    type="button"
                    class="data-stat-switch__btn"
                    :class="{ 'is-active': activeStatisticType === 'average' }"
                    @click="activeStatisticType = 'average'"
                  >
                    场均
                  </button>
                  <button
                    type="button"
                    class="data-stat-switch__btn"
                    :class="{ 'is-active': activeStatisticType === 'total' }"
                    @click="activeStatisticType = 'total'"
                  >
                    总计
                  </button>
                </div>
              </div>

              <DataStatisticsTable
                :data-type="activeDataType"
                :statistic-type="activeStatisticType"
                :rows="pagedRows"
                :loading="tableLoading"
                :error="tableError"
              />

              <DataPagination
                v-if="!tableLoading && total > 0"
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                class="data-center-panel__pagination"
              />
            </div>

            <div class="data-center-panel__edge-bottom" aria-hidden="true" />
          </div>
        </div>
      </div>
    </section>

    <div class="data-center-page__red-tear" aria-hidden="true">
      <svg
        class="data-center-page__red-tear-svg"
        viewBox="0 0 1440 14"
        preserveAspectRatio="none"
        focusable="false"
      >
        <path
          d="M0 8.2
             C18 3.1 34 11.4 52 6.8
             C71 1.8 88 10.6 109 5.4
             C128 0.8 146 9.9 167 4.6
             C189 -0.4 208 10.2 231 6.1
             C252 2.4 271 11.8 295 7.2
             C318 2.7 338 12.1 362 6.4
             C384 1.2 404 10.8 428 5.9
             C451 1.4 471 11.6 496 7.0
             C519 2.8 540 12.4 565 6.6
             C588 1.1 609 10.9 634 5.2
             C656 0.4 677 11.2 702 6.8
             C725 2.9 746 12.6 771 7.4
             C794 2.5 815 11.0 840 5.7
             C862 1.0 883 10.4 908 6.2
             C931 2.3 952 12.0 977 7.1
             C999 2.6 1020 11.5 1045 5.8
             C1067 0.9 1088 10.7 1113 6.5
             C1135 2.7 1156 12.2 1181 7.3
             C1203 2.9 1224 11.1 1249 5.6
             C1271 0.7 1292 10.5 1317 6.3
             C1338 2.4 1358 11.8 1382 7.0
             C1401 3.2 1418 10.2 1440 6.1
             L1440 14 L0 14 Z"
        />
      </svg>
    </div>
  </div>
</template>

<style scoped lang="scss">
.data-center-page {
  width: 100%;
  background: #050505;
  overflow-x: clip;
  color: #fff;
}

.data-center-page__main {
  width: 100%;
  padding: 8px 0 0;
  background: #050505;
}

.data-center-panel {
  --paper-tear-color: var(--cnpa-paper-bg);
  position: relative;
  z-index: 2;
  width: 100%;
  color: var(--cnpa-paper-text);
  overflow: visible;
}

.data-center-panel__tear-top {
  position: relative;
  z-index: 4;
  height: 0;
  pointer-events: none;

  :deep(.paper-tear) {
    height: 0;
  }

  :deep(.torn-edge-top) {
    top: 0;
    transform: translateY(-48%);
  }

  :deep(.torn-edge-main) {
    fill: var(--cnpa-paper-bg) !important;
  }

  :deep(.torn-edge-shadow) {
    display: none;
  }
}

.data-center-panel__shell {
  position: relative;
  z-index: 2;
  width: 100%;
  background-color: var(--cnpa-paper-bg);
  background-image: linear-gradient(
    180deg,
    #ebe9e9 0%,
    var(--cnpa-paper-bg) 45%,
    #e7e5e5 100%
  );
  border-radius: 2px 2px 0 0;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
  overflow: hidden;
}

.data-center-panel__body {
  position: relative;
  z-index: 1;
  padding: 20px clamp(14px, 1.8vw, 24px) 12px;
  box-sizing: border-box;
  background: transparent;
}

.data-center-panel__pagination {
  margin-top: 8px;
  padding-bottom: 8px;
}

.data-center-panel__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.data-stat-switch {
  display: inline-flex;
  align-items: center;
}

.data-stat-switch__btn {
  min-width: 86px;
  height: 45px;
  margin: 0;
  padding: 0 18px;
  border: 0;
  border-radius: 4px;
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text-secondary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;

  & + & {
    margin-left: 8px;
  }

  &.is-active {
    background: #ff1717;
    color: #fff;
    box-shadow: 0 2px 6px rgba(255, 23, 23, 0.18);
  }
}

.data-center-panel__edge-bottom {
  position: relative;
  z-index: 3;
  width: 100%;
  height: 11px;
  margin-top: -1px;
  background: var(--cnpa-paper-bg);
  pointer-events: none;
  clip-path: polygon(
    0 0,
    100% 0,
    100% 42%,
    98.6% 78%,
    96.8% 24%,
    94.1% 86%,
    91.7% 31%,
    88.2% 69%,
    85.9% 18%,
    82.4% 81%,
    78.6% 36%,
    75.1% 72%,
    71.3% 22%,
    67.8% 88%,
    63.4% 29%,
    59.7% 76%,
    55.2% 15%,
    51.6% 83%,
    47.1% 33%,
    43.4% 70%,
    38.9% 21%,
    34.6% 86%,
    30.2% 27%,
    26.5% 74%,
    21.8% 19%,
    17.9% 81%,
    13.4% 35%,
    9.6% 68%,
    5.8% 24%,
    2.7% 79%,
    0 46%
  );
}

.data-center-page__red-tear {
  position: relative;
  z-index: 3;
  width: 100%;
  height: 12px;
  margin-top: 36px;
  line-height: 0;
  overflow: hidden;
  background: transparent;
}

.data-center-page__red-tear-svg {
  display: block;
  width: 100%;
  height: 12px;

  path {
    fill: #e10613;
  }
}
</style>
