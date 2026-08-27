<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  getV4PublicPlayerStat,
  getV4PublicSeasonList,
  getV4PublicTeamStat,
} from '@/api/v4Public'
import type { V4PublicPlayerStatItem, V4PublicTeamStatItem } from '@/api/types/v4Public'
import { displayText, toNumber } from '@/utils/publicDisplay'
import { v4PlatformLabel, v4SeriesLabel } from '@/utils/v4Display'
import DataHero from './components/DataHero.vue'
import DataTypeTabs from './components/DataTypeTabs.vue'
import DataStatisticsTable from './components/DataStatisticsTable.vue'
import DataPagination from './components/DataPagination.vue'
import type {
  DataType,
  PlayerStatistics,
  StatisticType,
  TeamStatistics,
  TournamentGroup,
} from './types'

const route = useRoute()
const router = useRouter()

const activeDataType = ref<DataType>('team')
const activeStatisticType = ref<StatisticType>('average')
/** master 选择器 id：这里存 divisionId（统计维度） */
const selectedSeasonId = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const appliedKeyword = ref('')

const seasonGroups = ref<TournamentGroup[]>([])
const seasonLoading = ref(true)
const seasonError = ref('')

const allRows = ref<Array<TeamStatistics | PlayerStatistics>>([])
const tableLoading = ref(false)
const tableError = ref('')
let reqId = 0

function avgOrZero(totalValue: number, games: number) {
  return games > 0 ? totalValue / games : 0
}

function mapTeamRow(item: V4PublicTeamStatItem, index: number): TeamStatistics {
  const name = displayText(item.teamName, '未知球队')
  const games = toNumber(item.matchCount, 0)
  const wins = toNumber(item.winCount, 0)
  const losses = toNumber(item.loseCount, 0)
  const rebound = toNumber(item.rebound, 0)
  const assist = toNumber(item.assist, 0)
  const steal = toNumber(item.steal, 0)
  const block = toNumber(item.block, 0)
  const turnover = toNumber(item.turnover, 0)
  return {
    id: String(item.eventTeamId),
    rank: index + 1,
    teamId: String(item.eventTeamId),
    teamName: name,
    teamAbbr: name.slice(0, 6),
    logoUrl: item.teamLogoUrl?.trim() || '',
    logoColor: '#333',
    games,
    wins,
    losses,
    winRate: games > 0 ? (wins / games) * 100 : 0,
    averagePoints: toNumber(item.avgPointsFor, avgOrZero(toNumber(item.pointsFor, 0), games)),
    averageRebounds: avgOrZero(rebound, games),
    averageAssists: avgOrZero(assist, games),
    averageSteals: avgOrZero(steal, games),
    averageBlocks: avgOrZero(block, games),
    averageTurnovers: avgOrZero(turnover, games),
    totalPoints: toNumber(item.pointsFor, 0),
    totalRebounds: rebound,
    totalAssists: assist,
    totalSteals: steal,
    totalBlocks: block,
    totalTurnovers: turnover,
    // 球队级投篮：当前 TeamStat 无 FG/3P/FT 正式字段
    fieldGoalPct: null,
    threePointPct: null,
    freeThrowPct: null,
  }
}

function mapPlayerRow(item: V4PublicPlayerStatItem, index: number): PlayerStatistics {
  const teamName = displayText(item.teamName, '--')
  const games = toNumber(item.matchCount, 0)
  const sourcePlayerId = String(item.sourcePlayerId || '').trim()
  return {
    id: sourcePlayerId || String(item.eventPlayerId),
    playerId: sourcePlayerId,
    rank: index + 1,
    playerName: displayText(item.playerName, '未知球员'),
    teamId: '',
    teamName,
    teamAbbr: teamName.slice(0, 6),
    teamLogoUrl: '',
    logoColor: '#333',
    position: '--',
    games,
    averagePoints: toNumber(item.avgPoints, 0),
    averageRebounds: toNumber(item.avgRebound, 0),
    averageAssists: toNumber(item.avgAssist, 0),
    averageSteals: avgOrZero(toNumber(item.steal, 0), games),
    averageBlocks: avgOrZero(toNumber(item.block, 0), games),
    averageTurnovers: avgOrZero(toNumber(item.turnover, 0), games),
    totalPoints: toNumber(item.points, 0),
    totalRebounds: toNumber(item.rebound, 0),
    totalAssists: toNumber(item.assist, 0),
    totalSteals: toNumber(item.steal, 0),
    totalBlocks: toNumber(item.block, 0),
    totalTurnovers: toNumber(item.turnover, 0),
    // 球员级投篮：正式 PlayerStat / MatchPlayerStat 无 FG/3P/FT 字段
    fieldGoalPct: null,
    threePointPct: null,
    freeThrowPct: null,
  }
}

function applyRouteQuery() {
  const tab = route.query.tab
  if (tab === 'player' || tab === 'team') {
    activeDataType.value = tab
  } else if (tab === 'standing' || tab === 'rank') {
    activeDataType.value = tab === 'rank' ? 'player' : 'team'
  }
}

async function loadSeasons() {
  seasonLoading.value = true
  seasonError.value = ''
  try {
    const seriesFilter = typeof route.query.series === 'string'
      ? route.query.series.trim().toUpperCase()
      : ''
    const res = await getV4PublicSeasonList(seriesFilter ? { seriesCode: seriesFilter } : undefined)
    const list = res.data ?? []
    const groupMap = new Map<string, TournamentGroup>()
    for (const season of list) {
      const seasonKey = displayText(
        season.seasonYear
          ? `${v4SeriesLabel(season.seriesCode)} ${season.seasonYear}`
          : season.displayTitle || season.seriesName,
        '赛季',
      )
      if (!groupMap.has(seasonKey)) {
        groupMap.set(seasonKey, { season: seasonKey, tournaments: [] })
      }
      const divisions = season.divisions?.length
        ? season.divisions
        : [{ divisionId: '', divisionName: season.displayTitle, platformCode: '' } as any]
      for (const division of divisions) {
        if (!division.divisionId) continue
        groupMap.get(seasonKey)!.tournaments.push({
          id: division.divisionId,
          name: displayText(division.divisionName, division.divisionId),
          season: seasonKey,
          label: `${v4SeriesLabel(season.seriesCode)} · ${displayText(season.displayTitle)} · ${v4PlatformLabel(division.platformCode)}`,
        })
      }
    }
    seasonGroups.value = [...groupMap.values()].filter((group) => group.tournaments.length > 0)
    if (!selectedSeasonId.value) {
      selectedSeasonId.value = seasonGroups.value[0]?.tournaments[0]?.id || ''
    }
  } catch (err) {
    seasonGroups.value = []
    seasonError.value = err instanceof Error ? err.message : '加载赛季失败'
  } finally {
    seasonLoading.value = false
  }
}

async function loadTable() {
  const divisionId = selectedSeasonId.value
  const current = ++reqId
  if (!divisionId) {
    allRows.value = []
    return
  }
  tableLoading.value = true
  tableError.value = ''
  try {
    if (activeDataType.value === 'team') {
      const res = await getV4PublicTeamStat(divisionId)
      if (current !== reqId) return
      allRows.value = (res.data ?? []).map((item, index) => mapTeamRow(item, index))
    } else {
      const res = await getV4PublicPlayerStat(divisionId)
      if (current !== reqId) return
      allRows.value = (res.data ?? []).map((item, index) => mapPlayerRow(item, index))
    }
  } catch (err) {
    if (current !== reqId) return
    allRows.value = []
    tableError.value = err instanceof Error ? err.message : '加载数据失败'
  } finally {
    if (current === reqId) tableLoading.value = false
  }
}

const filteredRows = computed(() => {
  const keyword = appliedKeyword.value.trim().toLowerCase()
  let list = [...allRows.value]
  if (keyword) {
    list = list.filter((row) => {
      if ('playerName' in row) {
        return (
          row.playerName.toLowerCase().includes(keyword)
          || row.teamName.toLowerCase().includes(keyword)
          || row.playerId.toLowerCase().includes(keyword)
        )
      }
      return (
        row.teamName.toLowerCase().includes(keyword)
        || row.teamId.toLowerCase().includes(keyword)
      )
    })
  }
  list.sort((a, b) => {
    if (activeDataType.value === 'team') {
      const left = a as TeamStatistics
      const right = b as TeamStatistics
      if (activeStatisticType.value === 'total') return right.wins - left.wins
      return right.averagePoints - left.averagePoints
    }
    const left = a as PlayerStatistics
    const right = b as PlayerStatistics
    if (activeStatisticType.value === 'total') return right.totalPoints - left.totalPoints
    return right.averagePoints - left.averagePoints
  })
  return list.map((row, index) => ({ ...row, rank: index + 1 }))
})

const total = computed(() => filteredRows.value.length)

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

watch(activeDataType, (tab) => {
  currentPage.value = 1
  searchKeyword.value = ''
  appliedKeyword.value = ''
  router.replace({ path: '/data', query: { ...route.query, tab } })
})

watch([activeDataType, selectedSeasonId], () => {
  currentPage.value = 1
  searchKeyword.value = ''
  appliedKeyword.value = ''
})

watch(activeStatisticType, () => {
  currentPage.value = 1
})

watch([selectedSeasonId, activeDataType], () => {
  void loadTable()
})

watch(
  () => [route.query.tab, route.query.series],
  () => {
    applyRouteQuery()
    void loadSeasons()
  },
)

onMounted(async () => {
  applyRouteQuery()
  await loadSeasons()
  await loadTable()
})
</script>
<template>
  <div class="data-center-page">
    <div class="site-shell data-center-page__shell">
      <DataHero
        v-model:tournament-id="selectedSeasonId"
        :groups="seasonGroups"
        :loading="seasonLoading"
        :error="seasonError"
      />

      <section class="data-center-page__main">
        <div class="data-center-page__tabs">
          <DataTypeTabs v-model="activeDataType" />
        </div>

        <div class="data-center-panel">
          <div class="data-center-panel__tear-top" aria-hidden="true">
            <PaperTear position="top" tone="light" />
          </div>

          <div class="data-center-panel__shell">
            <div class="site-shell-inner data-center-panel__body">
              <div class="data-center-panel__toolbar">
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
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="total"
              />
            </div>

            <div class="data-center-panel__edge-bottom" aria-hidden="true" />
          </div>
        </div>
      </section>
    </div>

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

.data-center-page__shell {
  position: relative;
}

.data-center-page__main {
  width: 100%;
  padding: 8px 0 0;
  background: #050505;
}

.data-center-page__tabs {
  width: 100%;
  padding-inline: var(--cnpa-public-content-gutter);
  box-sizing: border-box;
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
  overflow: hidden;
}

.data-center-panel__body {
  position: relative;
  z-index: 1;
  padding: 20px var(--cnpa-public-content-gutter) 0;
  box-sizing: border-box;
  background: transparent;
}

.data-center-panel__toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
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