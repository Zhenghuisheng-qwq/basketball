<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicPageLayout from '@/components/layout/PublicPageLayout.vue'
import PublicEmptyState from '@/components/common/PublicEmptyState.vue'
import {
  getV4PublicPlayerStat,
  getV4PublicRank,
  getV4PublicSeasonList,
  getV4PublicStanding,
  getV4PublicTeamStat,
} from '@/api/v4Public'
import type {
  V4PublicDivisionBrief,
  V4PublicPlayerStatItem,
  V4PublicRankItem,
  V4PublicSeasonItem,
  V4PublicStandingItem,
  V4PublicTeamStatItem,
} from '@/api/types/v4Public'
import { displayText, formatDecimal } from '@/utils/publicDisplay'
import { v4RankTypeLabel } from '@/utils/v4Display'
import V4DataHero from './components/V4DataHero.vue'
import './styles/v4-data-page.scss'

/** 遗留页（路由已切到 data/index）；保留本地 tab 类型避免污染公开菜单约定 */
const DATA_TAB_VALUES = ['standing', 'team', 'player', 'rank'] as const
type DataPanel = (typeof DATA_TAB_VALUES)[number]

const route = useRoute()
const router = useRouter()

const seasons = ref<V4PublicSeasonItem[]>([])
const selectedSeasonId = ref('')
const selectedDivisionId = ref('')
const activePanel = ref<DataPanel>('standing')
const rankType = ref('PLAYER_SCORE')
const loading = ref(true)
const panelLoading = ref(false)
const error = ref('')

const standings = ref<V4PublicStandingItem[]>([])
const teamStats = ref<V4PublicTeamStatItem[]>([])
const playerStats = ref<V4PublicPlayerStatItem[]>([])
const ranks = ref<V4PublicRankItem[]>([])

const currentSeason = computed(() =>
  seasons.value.find((item) => item.seasonId === selectedSeasonId.value) ?? null,
)

const divisions = computed<V4PublicDivisionBrief[]>(() => currentSeason.value?.divisions ?? [])

const mainTabs = [
  ['standing', '积分榜'],
  ['team', '球队统计'],
  ['player', '球员统计'],
  ['rank', '排行榜'],
] as const

const rankTabs = ['PLAYER_SCORE', 'PLAYER_ASSIST', 'PLAYER_REBOUND'] as const

const emptyState = computed(() => {
  if (activePanel.value === 'standing') {
    return { title: '暂无积分榜数据', description: '比赛数据审核通过后自动生成' }
  }
  if (activePanel.value === 'team') {
    return { title: '暂无球队统计数据', description: '比赛数据审核通过后自动生成' }
  }
  if (activePanel.value === 'player') {
    return { title: '暂无球员统计数据', description: '比赛数据审核通过后自动生成' }
  }
  return { title: '暂无排行榜数据', description: '比赛数据审核通过后自动生成' }
})

const hasPanelData = computed(() => {
  if (activePanel.value === 'standing') return standings.value.length > 0
  if (activePanel.value === 'team') return teamStats.value.length > 0
  if (activePanel.value === 'player') return playerStats.value.length > 0
  return ranks.value.length > 0
})

function normalizeTab(raw: unknown): DataPanel {
  if (typeof raw === 'string' && (DATA_TAB_VALUES as readonly string[]).includes(raw)) {
    return raw as DataPanel
  }
  return 'standing'
}

function applyRouteQuery() {
  activePanel.value = normalizeTab(route.query.tab)

  const series = typeof route.query.series === 'string'
    ? route.query.series.trim().toUpperCase()
    : ''
  if (!series || !seasons.value.length) return

  const matched = seasons.value.find((item) => (item.seriesCode || '').toUpperCase() === series)
  if (matched && matched.seasonId !== selectedSeasonId.value) {
    selectedSeasonId.value = matched.seasonId
    selectedDivisionId.value = matched.divisions?.[0]?.divisionId ?? ''
  }
}

function syncTabToRoute(tab: DataPanel) {
  const nextQuery: Record<string, string> = {}
  Object.entries(route.query).forEach(([key, value]) => {
    if (typeof value === 'string' && key !== 'tab') {
      nextQuery[key] = value
    }
  })
  nextQuery.tab = tab
  router.replace({ path: '/data', query: nextQuery })
}

async function loadSeasons() {
  loading.value = true
  error.value = ''
  try {
    const res = await getV4PublicSeasonList()
    seasons.value = res.data ?? []
    applyRouteQuery()
    if (!selectedSeasonId.value && seasons.value[0]) {
      selectedSeasonId.value = seasons.value[0].seasonId
      selectedDivisionId.value = seasons.value[0].divisions?.[0]?.divisionId ?? ''
    }
  } catch (err) {
    seasons.value = []
    error.value = err instanceof Error ? err.message : '加载赛季失败'
  } finally {
    loading.value = false
  }
}

async function loadPanel() {
  if (!selectedDivisionId.value) return
  panelLoading.value = true
  try {
    if (activePanel.value === 'standing') {
      standings.value = (await getV4PublicStanding(selectedDivisionId.value)).data ?? []
    } else if (activePanel.value === 'team') {
      teamStats.value = (await getV4PublicTeamStat(selectedDivisionId.value)).data ?? []
    } else if (activePanel.value === 'player') {
      playerStats.value = (await getV4PublicPlayerStat(selectedDivisionId.value)).data ?? []
    } else {
      ranks.value = (await getV4PublicRank(selectedDivisionId.value, rankType.value)).data ?? []
    }
  } catch {
    standings.value = []
    teamStats.value = []
    playerStats.value = []
    ranks.value = []
  } finally {
    panelLoading.value = false
  }
}

function selectPanel(tab: DataPanel) {
  activePanel.value = tab
  syncTabToRoute(tab)
}

watch(selectedSeasonId, (value) => {
  const season = seasons.value.find((item) => item.seasonId === value)
  selectedDivisionId.value = season?.divisions?.[0]?.divisionId ?? ''
})

watch([selectedDivisionId, activePanel, rankType], () => {
  void loadPanel()
})

watch(
  () => [route.query.tab, route.query.series],
  () => {
    applyRouteQuery()
  },
)

onMounted(async () => {
  await loadSeasons()
  await loadPanel()
})
</script>

<template>
  <PublicPageLayout>
    <template #hero>
      <V4DataHero
        :loading="loading"
        :error="error"
        :seasons="seasons"
        :divisions="divisions"
        :selected-season-id="selectedSeasonId"
        :selected-division-id="selectedDivisionId"
        @update:selected-season-id="selectedSeasonId = $event"
        @update:selected-division-id="selectedDivisionId = $event"
      />
    </template>

    <template #before>
      <nav class="v4-data-type-tabs" role="tablist" aria-label="数据分类">
        <button
          v-for="item in mainTabs"
          :key="item[0]"
          type="button"
          role="tab"
          class="v4-data-type-tabs__item"
          :class="{ 'is-active': activePanel === item[0] }"
          :aria-selected="activePanel === item[0]"
          @click="selectPanel(item[0])"
        >
          {{ item[1] }}
        </button>
      </nav>
    </template>

    <div class="v4-data-page__shell">
      <div v-if="activePanel === 'rank'" class="v4-data-subtabs" aria-label="排行榜类型">
        <button
          v-for="type in rankTabs"
          :key="type"
          type="button"
          class="v4-data-subtabs__btn"
          :class="{ 'is-active': rankType === type }"
          @click="rankType = type"
        >
          {{ v4RankTypeLabel(type) }}
        </button>
      </div>

      <div class="v4-data-panel">
        <div v-if="panelLoading" class="v4-data-panel__loading">加载中...</div>
        <template v-else-if="!hasPanelData">
          <PublicEmptyState
            :title="emptyState.title"
            :description="emptyState.description"
          />
        </template>
        <template v-else>
          <div v-if="activePanel === 'standing'" class="v4-data-table-wrap">
            <table class="v4-data-table">
              <thead>
                <tr>
                  <th>排名</th><th>队伍</th><th>胜</th><th>负</th><th>净胜分</th><th>积分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in standings" :key="row.eventTeamId">
                  <td>{{ row.rankNo ?? '--' }}</td>
                  <td>{{ displayText(row.teamName) }}</td>
                  <td>{{ row.winCount ?? 0 }}</td>
                  <td>{{ row.loseCount ?? 0 }}</td>
                  <td>{{ row.pointDiff ?? 0 }}</td>
                  <td>{{ row.standingPoints ?? 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else-if="activePanel === 'team'" class="v4-data-table-wrap">
            <table class="v4-data-table">
              <thead>
                <tr>
                  <th>队伍</th><th>场次</th><th>场均得分</th><th>篮板</th><th>助攻</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in teamStats" :key="row.eventTeamId">
                  <td>{{ displayText(row.teamName) }}</td>
                  <td>{{ row.matchCount ?? 0 }}</td>
                  <td>{{ formatDecimal(row.avgPointsFor) }}</td>
                  <td>{{ row.rebound ?? 0 }}</td>
                  <td>{{ row.assist ?? 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else-if="activePanel === 'player'" class="v4-data-table-wrap">
            <table class="v4-data-table">
              <thead>
                <tr>
                  <th>球员</th><th>球队</th><th>场均得分</th><th>场均篮板</th><th>场均助攻</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in playerStats" :key="row.eventPlayerId">
                  <td>{{ displayText(row.playerName) }}</td>
                  <td>{{ displayText(row.teamName) }}</td>
                  <td>{{ formatDecimal(row.avgPoints) }}</td>
                  <td>{{ formatDecimal(row.avgRebound) }}</td>
                  <td>{{ formatDecimal(row.avgAssist) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="v4-data-table-wrap">
            <table class="v4-data-table">
              <thead>
                <tr><th>排名</th><th>名称</th><th>数值</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in ranks" :key="`${row.rankNo}-${row.eventPlayerId || row.eventTeamId}`">
                  <td>{{ row.rankNo ?? '--' }}</td>
                  <td>{{ displayText(row.playerName || row.teamName) }}</td>
                  <td>{{ formatDecimal(row.rankValue) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>
  </PublicPageLayout>
</template>
