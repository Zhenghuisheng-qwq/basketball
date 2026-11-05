<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  getV4PublicFeaturedDivision,
  getV4PublicLeaderboard,
  getV4PublicSeasonList,
  getV4PublicStageList,
} from '@/api/v4Public'
import type { V4PublicLeaderboardRow, V4PublicStageItem } from '@/api/types/v4Public'
import { displayText, localizedText, toNumber } from '@/utils/publicDisplay'
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
const { t, locale } = useI18n()

const activeDataType = ref<DataType>('team')
const activeStatisticType = ref<StatisticType>('average')
const sortKey = ref('winLoss')
const sortDir = ref<'asc' | 'desc'>('desc')
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
const scopes = ref<Array<{ key: string; label: string }>>([])
const selectedScope = ref('OVERALL')
const serverTotal = ref(0)
const emptyReason = ref('')
const stale = ref(false)
let reqId = 0

function teamShootingPct(made?: number | null, attempt?: number | null): number | null {
  const attempts = toNumber(attempt, 0)
  if (attempts <= 0) return null
  return (toNumber(made, 0) / attempts) * 100
}

function mapTeamRow(item: V4PublicLeaderboardRow): TeamStatistics {
  const name = displayText(item.teamName, t('publicPages.data.unknownTeam'))
  const games = toNumber(item.games, 0)
  const wins = toNumber(item.wins, 0)
  const losses = toNumber(item.losses, 0)
  return {
    id: String(item.eventTeamId),
    rank: toNumber(item.rank, 0),
    teamId: String(item.eventTeamId),
    teamName: name,
    teamAbbr: name.slice(0, 6),
    logoUrl: item.teamLogoUrl?.trim() || '',
    logoColor: '#333',
    games,
    wins,
    losses,
    winRate: games > 0 ? (wins / games) * 100 : 0,
    averagePoints: toNumber(item.averagePoints, 0),
    averageRebounds: toNumber(item.averageRebounds, 0),
    averageAssists: toNumber(item.averageAssists, 0),
    averageSteals: toNumber(item.averageSteals, 0),
    averageBlocks: toNumber(item.averageBlocks, 0),
    averageTurnovers: toNumber(item.averageTurnovers, 0),
    totalPoints: toNumber(item.points, 0),
    totalRebounds: toNumber(item.rebounds, 0),
    totalAssists: toNumber(item.assists, 0),
    totalSteals: toNumber(item.steals, 0),
    totalBlocks: toNumber(item.blocks, 0),
    totalTurnovers: toNumber(item.turnovers, 0),
    fieldGoalPct: teamShootingPct(item.fieldGoalMade, item.fieldGoalAttempt),
    threePointPct: teamShootingPct(item.threePointMade, item.threePointAttempt),
    freeThrowPct: teamShootingPct(item.freeThrowMade, item.freeThrowAttempt),
  }
}

function mapPlayerRow(item: V4PublicLeaderboardRow): PlayerStatistics {
  const teamName = displayText(item.teamName, '--')
  const games = toNumber(item.games, 0)
  const sourcePlayerId = String(item.sourcePlayerId || '').trim()
  return {
    id: sourcePlayerId || String(item.eventPlayerId),
    playerId: sourcePlayerId,
    rank: toNumber(item.rank, 0),
    playerName: displayText(item.playerName, t('publicPages.data.unknownPlayer')),
    avatarUrl: item.avatarUrl?.trim() || '',
    teamId: String(item.eventTeamId || ''),
    teamName,
    teamAbbr: teamName.slice(0, 6),
    teamLogoUrl: item.teamLogoUrl?.trim() || '',
    logoColor: '#333',
    position: item.position?.trim() || '--',
    games,
    averagePoints: toNumber(item.averagePoints, 0),
    averageRebounds: toNumber(item.averageRebounds, 0),
    averageAssists: toNumber(item.averageAssists, 0),
    averageSteals: toNumber(item.averageSteals, 0),
    averageBlocks: toNumber(item.averageBlocks, 0),
    averageTurnovers: toNumber(item.averageTurnovers, 0),
    totalPoints: toNumber(item.points, 0),
    totalRebounds: toNumber(item.rebounds, 0),
    totalAssists: toNumber(item.assists, 0),
    totalSteals: toNumber(item.steals, 0),
    totalBlocks: toNumber(item.blocks, 0),
    totalTurnovers: toNumber(item.turnovers, 0),
    averageFouls: null,
    totalFouls: null,
    fieldGoalPct: teamShootingPct(item.fieldGoalMade, item.fieldGoalAttempt),
    threePointPct: teamShootingPct(item.threePointMade, item.threePointAttempt),
    freeThrowPct: teamShootingPct(item.freeThrowMade, item.freeThrowAttempt),
  }
}

function applyRouteQuery() {
  const tab = route.query.tab
  if (tab === 'player' || tab === 'team') {
    activeDataType.value = tab
  } else if (tab === 'standing' || tab === 'rank') {
    activeDataType.value = tab === 'rank' ? 'player' : 'team'
  }
  const mode = route.query.mode
  if (mode === 'average' || mode === 'total') {
    activeStatisticType.value = mode
  }
  const page = Number(route.query.page)
  if (Number.isFinite(page) && page > 0) {
    currentPage.value = page
  }
  const scope = route.query.scope
  if (typeof scope === 'string' && scope) {
    selectedScope.value = scope
  }
  const sort = route.query.sort
  const dir = route.query.dir
  if (typeof sort === 'string' && sort && !(activeDataType.value === 'player' && sort === 'games')) {
    sortKey.value = sort
  } else {
    sortKey.value = activeDataType.value === 'team' ? 'winLoss' : 'totalPoints'
    sortDir.value = 'desc'
  }
  if (dir === 'asc' || dir === 'desc') {
    sortDir.value = dir
  }
}

function onColumnSort(key: string, dir: 'asc' | 'desc') {
  if (sortKey.value === key && sortDir.value === dir) return
  sortKey.value = key
  sortDir.value = dir
  if (currentPage.value !== 1) currentPage.value = 1
}

function leaderboardMode() {
  if (activeDataType.value !== 'player') return activeStatisticType.value
  if (sortKey.value === 'totalPoints' || sortKey.value === 'rank') return 'total'
  return activeStatisticType.value
}

function apiSortBy(tab: DataType, key: string) {
  if (key === 'rank' || key === 'totalPoints') return tab === 'team' ? 'WINS' : 'PTS'
  const metrics: Record<string, string> = {
    team: 'TEAM_NAME',
    player: 'PLAYER_NAME',
    position: 'POSITION',
    games: 'GAMES',
    winLoss: 'WINS',
    winRate: 'WIN_RATE',
    points: 'PTS',
    rebounds: 'REB',
    assists: 'AST',
    steals: 'STL',
    blocks: 'BLK',
    turnovers: 'TOV',
    fouls: 'FOUL',
    fieldGoals: 'FG_PCT',
    threePoints: 'THREE_PCT',
    freeThrows: 'FT_PCT',
  }
  return metrics[key] || (tab === 'team' ? 'WINS' : 'PTS')
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
        t('publicPages.data.season'),
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
          name: localizedText(division.divisionName, division.divisionNameEn, division.divisionId),
          season: seasonKey,
          label: `${v4SeriesLabel(season.seriesCode)} · ${localizedText(season.displayTitle, season.displayTitleEn)} · ${v4PlatformLabel(division.platformCode)}`,
        })
      }
    }
    seasonGroups.value = [...groupMap.values()].filter((group) => group.tournaments.length > 0)
    const queryDivision = typeof route.query.divisionId === 'string' ? route.query.divisionId : ''
    if (queryDivision) {
      selectedSeasonId.value = queryDivision
    } else if (!selectedSeasonId.value) {
      try {
        const featured = await getV4PublicFeaturedDivision()
        const featuredId = String(featured.data?.divisionId || '')
        if (featuredId && featuredId !== '0') {
          selectedSeasonId.value = featuredId
        }
      } catch {
        selectedSeasonId.value = ''
      }
      if (!selectedSeasonId.value) {
        selectedSeasonId.value = seasonGroups.value[0]?.tournaments[0]?.id || ''
      }
    }
  } catch (err) {
    seasonGroups.value = []
    seasonError.value = err instanceof Error ? err.message : t('publicPages.data.loadSeasonsFailed')
  } finally {
    seasonLoading.value = false
  }
}

async function loadScopes(divisionId: string) {
  const res = await getV4PublicStageList(divisionId)
  const stages = (res.data ?? []) as V4PublicStageItem[]
  const next = stages
    .filter((stage) => stage.stageId)
    .sort((a, b) => toNumber(a.stageOrder, 0) - toNumber(b.stageOrder, 0))
    .map((stage) => ({
      key: `STAGE:${stage.stageId}`,
      label: stage.stageName || stage.stageTypeName || stage.stageCode || String(stage.stageId),
    }))
  next.push({ key: 'OVERALL', label: t('publicPages.data.overall') })
  scopes.value = next
  const queryScope = typeof route.query.scope === 'string' ? route.query.scope : ''
  if (queryScope && next.some((item) => item.key === queryScope)) {
    selectedScope.value = queryScope
  } else if (!next.some((item) => item.key === selectedScope.value)) {
    selectedScope.value = next[0]?.key || 'OVERALL'
  }
}

function emptyTextFor(reason: string) {
  if (reason === 'NOT_PUBLISHED') return t('publicPages.data.notPublished')
  if (reason === 'NO_ELIGIBLE_SUBJECTS') return t('publicPages.data.noEligible')
  return ''
}

async function loadTable(soft = false) {
  const divisionId = selectedSeasonId.value
  const current = ++reqId
  if (!divisionId) {
    allRows.value = []
    serverTotal.value = 0
    emptyReason.value = ''
    return
  }
  const keepRows = soft && allRows.value.length > 0
  if (!keepRows) {
    tableLoading.value = true
    tableError.value = ''
  }
  try {
    if (!keepRows) {
      await loadScopes(divisionId)
      if (current !== reqId) return
    }
    const res = await getV4PublicLeaderboard({
      divisionId,
      scope: selectedScope.value,
      tab: activeDataType.value,
      mode: leaderboardMode(),
      sortBy: apiSortBy(activeDataType.value, sortKey.value),
      sortDir: sortDir.value,
      page: currentPage.value,
      keyword: appliedKeyword.value.trim() || undefined,
    })
    if (current !== reqId) return
    const board = res.data
    emptyReason.value = board?.emptyReason || ''
    stale.value = Boolean(board?.stale)
    serverTotal.value = toNumber(board?.totalEligible, 0)
    const rows = board?.rows ?? []
    allRows.value = rows.map((item) => activeDataType.value === 'team' ? mapTeamRow(item) : mapPlayerRow(item))
  } catch (err) {
    if (current !== reqId) return
    allRows.value = []
    serverTotal.value = 0
    emptyReason.value = ''
    tableError.value = err instanceof Error ? err.message : t('publicPages.data.loadDataFailed')
  } finally {
    if (current === reqId) tableLoading.value = false
  }
}

const filteredRows = computed(() => allRows.value)

const total = computed(() => serverTotal.value)

const pagedRows = computed(() => filteredRows.value)

const boardEmptyText = computed(() => emptyTextFor(emptyReason.value) || (stale.value ? t('publicPages.data.stale') : ''))

function syncBoardQuery() {
  const query = {
    ...route.query,
    divisionId: selectedSeasonId.value || undefined,
    scope: selectedScope.value || undefined,
    tab: activeDataType.value,
    mode: activeStatisticType.value,
    sort: sortKey.value,
    dir: sortDir.value,
    page: String(currentPage.value),
  }
  router.replace({ path: '/data', query })
}

watch(activeDataType, (tab) => {
  currentPage.value = 1
  searchKeyword.value = ''
  appliedKeyword.value = ''
  sortKey.value = tab === 'team' ? 'winLoss' : 'totalPoints'
  sortDir.value = 'desc'
})

watch([activeDataType, selectedSeasonId, activeStatisticType], () => {
  if (currentPage.value !== 1) currentPage.value = 1
})

function applySearch() {
  appliedKeyword.value = searchKeyword.value.trim()
  if (currentPage.value !== 1) currentPage.value = 1
}

watch([selectedSeasonId, activeDataType, activeStatisticType, selectedScope, currentPage, appliedKeyword, sortKey, sortDir], (next, prev) => {
  syncBoardQuery()
  const [division, tab, , scope] = next
  const [prevDivision, prevTab, , prevScope] = prev
  const soft = division === prevDivision && tab === prevTab && scope === prevScope
  void loadTable(soft)
})

watch(
  () => [route.query.tab, route.query.series],
  () => {
    applyRouteQuery()
    void loadSeasons()
  },
)

watch(locale, () => {
  void loadSeasons()
})

onMounted(async () => {
  applyRouteQuery()
  await loadSeasons()
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
                <label class="data-scope-select">
                  <span>{{ t('publicPages.data.scope') }}</span>
                  <select v-model="selectedScope">
                    <option v-for="scope in scopes" :key="scope.key" :value="scope.key">{{ scope.label }}</option>
                  </select>
                </label>
                <p v-if="stale" class="data-scope-stale">{{ t('publicPages.data.stale') }}</p>
                <form class="data-scope-search" @submit.prevent="applySearch">
                  <input v-model="searchKeyword" :placeholder="t('publicPages.data.search')" />
                  <button type="submit">{{ t('publicPages.data.search') }}</button>
                </form>
                <div class="data-stat-switch" role="group" :aria-label="t('publicPages.data.statType')">
                  <button
                    type="button"
                    class="data-stat-switch__btn"
                    :class="{ 'is-active': activeStatisticType === 'average' }"
                    @click="activeStatisticType = 'average'"
                  >
                    {{ t('publicPages.data.average') }}
                  </button>
                  <button
                    type="button"
                    class="data-stat-switch__btn"
                    :class="{ 'is-active': activeStatisticType === 'total' }"
                    @click="activeStatisticType = 'total'"
                  >
                    {{ t('publicPages.data.total') }}
                  </button>
                </div>
              </div>

              <DataStatisticsTable
                :data-type="activeDataType"
                :statistic-type="activeStatisticType"
                :rows="pagedRows"
                :loading="tableLoading"
                :error="tableError"
                :empty-text="boardEmptyText"
                :sort-key="sortKey"
                :sort-dir="sortDir"
                @sort="onColumnSort"
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
  display: none; /* 临时隐藏撕纸，勿删 */
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
  gap: 16px;
  margin-bottom: 18px;
}

.data-scope-select {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-right: auto;
  color: var(--cnpa-paper-text);
  font-size: 14px;
  font-weight: 700;

  select {
    min-width: 180px;
    height: 40px;
    padding: 0 10px;
    border: 1px solid var(--cnpa-paper-line, #ddd);
    border-radius: 4px;
    background: #fff;
    color: inherit;
  }
}

.data-scope-stale {
  margin: 0;
  color: #9a3412;
  font-size: 13px;
}

.data-scope-search {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  input, button {
    height: 40px;
    border: 1px solid var(--cnpa-paper-line, #ddd);
    border-radius: 4px;
    background: #fff;
  }

  input {
    width: 180px;
    padding: 0 10px;
  }

  button {
    padding: 0 14px;
    font-weight: 700;
    cursor: pointer;
  }
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
  display: none; /* 临时隐藏撕纸，勿删 */
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
  display: none; /* 临时隐藏撕纸，勿删 */
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