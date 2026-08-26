<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicMatchDetail, getV4PublicMatchSeries } from '@/api/v4Public'
import type { V4PublicMatchDetail, V4PublicMatchSeriesDetail } from '@/api/types/v4Public'
import PaperSection from '@/components/common/PaperSection.vue'
import { requiresPeriodScores } from '@/utils/fullMatchSubmissionRules'
import {
  v4BestOfLabel,
  v4MatchStatusLabel,
  v4PublicRoundLabel,
  v4SeriesHeroStatusLabel,
} from '@/utils/v4Display'
import MatchScoreboard from '../match-detail/MatchScoreboard.vue'
import SeriesGameTabs from '../match-detail/SeriesGameTabs.vue'
import PeriodScoreBoard from '../match-detail/PeriodScoreBoard.vue'
import BoxScorePanel from '../match-detail/BoxScorePanel.vue'
import {
  SERIES_TOTAL_TAB,
  formatMatchWhen,
  gameCaption,
  gameTabLabel,
  matchBasketballScore,
  publicSeriesGames,
  seriesWinScore,
  teamFromMatchSide,
  toDetailTeam,
} from '../match-detail/matchDetailDisplay'
import '../styles/events.scss'

const route = useRoute()
const router = useRouter()

const seriesMode = computed(() => route.name === 'V4MatchSeriesDetail')
const seriesId = computed(() => String(route.params.seriesId || ''))
const matchId = computed(() => String(route.params.matchId || ''))
const gameQuery = computed(() => String(route.query.game || '').trim())

const loading = ref(true)
const error = ref('')
const series = ref<V4PublicMatchSeriesDetail | null>(null)
const match = ref<V4PublicMatchDetail | null>(null)
const matchCache = ref<Record<string, V4PublicMatchDetail>>({})
const selectedTeamId = ref('')
const activeTab = ref(SERIES_TOTAL_TAB)

const visibleGames = computed(() => publicSeriesGames(series.value))

const seriesTabs = computed(() => {
  if (!seriesMode.value) return []
  return [
    { key: SERIES_TOTAL_TAB, label: '系列赛总计' },
    ...visibleGames.value.map((game) => ({
      key: String(game.matchId),
      label: gameTabLabel(game),
    })),
  ]
})

const activeGame = computed(() => {
  if (activeTab.value === SERIES_TOTAL_TAB) return null
  return visibleGames.value.find((game) => String(game.matchId) === activeTab.value) || null
})

const leftTeam = computed(() => {
  if (match.value) return teamFromMatchSide(match.value, 'away')
  const s = series.value
  return toDetailTeam(s?.awayEventTeamId, s?.awayTeamName, s?.awayTeamLogoUrl, s?.awayTeamShortName)
})

const rightTeam = computed(() => {
  if (match.value && !seriesMode.value) return teamFromMatchSide(match.value, 'home')
  if (match.value && activeGame.value) return teamFromMatchSide(match.value, 'home')
  const s = series.value
  return toDetailTeam(s?.homeEventTeamId, s?.homeTeamName, s?.homeTeamLogoUrl, s?.homeTeamShortName)
})

const score = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return seriesWinScore(series.value)
  }
  return matchBasketballScore(match.value)
})

const winnerEventTeamId = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return series.value?.winnerEventTeamId || ''
  }
  return match.value?.winnerEventTeamId || ''
})

const statusText = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return v4SeriesHeroStatusLabel(series.value?.seriesStatus)
  }
  if (seriesMode.value && activeGame.value) {
    return ''
  }
  return match.value?.matchStatusName || v4MatchStatusLabel(match.value?.matchStatus)
})

const caption = computed(() => {
  if (seriesMode.value && activeGame.value) {
    return gameCaption(activeGame.value)
  }
  return ''
})

const metaText = computed(() => {
  if (seriesMode.value) {
    const s = series.value
    const stage = v4PublicRoundLabel(s?.stageName, s?.roundNo) || s?.stageName || ''
    return [stage, v4BestOfLabel(s?.bestOf)].filter(Boolean).join(' · ')
  }
  const m = match.value
  if (!m) return ''
  const stage = m.stageName || '常规赛'
  const round = m.roundNo != null ? `第${m.roundNo}轮` : ''
  const when = formatMatchWhen(m.matchTime)
  return [stage, round, v4BestOfLabel(m.bestOf || 1), when].filter(Boolean).join(' · ')
})

const breadcrumb = computed(() => {
  const eventName = series.value?.eventSeriesName
    || match.value?.seriesName
    || '赛事'
  return ['赛事', eventName, seriesMode.value ? '系列赛详情' : '比赛详情']
})

const showSeriesTabs = computed(() => seriesMode.value && seriesTabs.value.length > 0)

const showPeriodBoard = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) return false
  const gameMode = match.value?.gameMode || series.value?.gameMode
  if (!requiresPeriodScores(gameMode)) return false
  return Boolean(match.value?.periodScores?.length)
})

const boxPlayers = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return series.value?.playerStats || []
  }
  return match.value?.playerStats || []
})

const boxTeamStats = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return series.value?.teamStats || []
  }
  return match.value?.teamStats || []
})

const showBoxScore = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return Boolean(boxPlayers.value.length || boxTeamStats.value.length)
  }
  return true
})

const boxTeams = computed(() => [leftTeam.value, rightTeam.value].filter((team) => team.name))

const seasonPath = computed(() => {
  const seasonId = series.value?.seasonId || match.value?.seasonId || String(route.query.seasonId || '')
  const divisionId = series.value?.divisionId || match.value?.divisionId || String(route.query.divisionId || '')
  if (!seasonId) return '/events'
  return {
    path: `/events/${seasonId}`,
    query: {
      tab: 'schedule',
      ...(divisionId ? { divisionId } : {}),
    },
  }
})

function syncSelectedTeam() {
  const first = boxTeams.value[0]?.teamId
  if (!selectedTeamId.value || !boxTeams.value.some((team) => String(team.teamId) === String(selectedTeamId.value))) {
    selectedTeamId.value = first ? String(first) : ''
  }
}

function applyTabFromRoute() {
  if (!seriesMode.value) {
    activeTab.value = SERIES_TOTAL_TAB
    return
  }
  if (gameQuery.value && visibleGames.value.some((game) => String(game.matchId) === gameQuery.value)) {
    activeTab.value = gameQuery.value
    return
  }
  activeTab.value = SERIES_TOTAL_TAB
}

async function loadMatchById(id: string) {
  if (!id) {
    match.value = null
    return
  }
  if (matchCache.value[id]) {
    match.value = matchCache.value[id]
    return
  }
  const res = await getV4PublicMatchDetail(id)
  if (res.data) {
    matchCache.value[id] = res.data
    match.value = res.data
  } else {
    match.value = null
  }
}

async function loadPage() {
  loading.value = true
  error.value = ''
  try {
    if (seriesMode.value) {
      if (!seriesId.value) {
        throw new Error('系列赛主键无效')
      }
      const res = await getV4PublicMatchSeries(seriesId.value)
      series.value = res.data ?? null
      if (!series.value) {
        throw new Error('暂无系列赛详情')
      }
      applyTabFromRoute()
      if (activeTab.value !== SERIES_TOTAL_TAB) {
        await loadMatchById(activeTab.value)
      } else {
        match.value = null
      }
    } else {
      series.value = null
      if (!matchId.value) {
        throw new Error('比赛主键无效')
      }
      await loadMatchById(matchId.value)
      if (!match.value) {
        throw new Error('暂无比赛详情')
      }
    }
    syncSelectedTeam()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
    if (seriesMode.value) series.value = null
    else match.value = null
  } finally {
    loading.value = false
  }
}

async function onTabChange(tab: string) {
  activeTab.value = tab
  const query = { ...route.query }
  if (tab === SERIES_TOTAL_TAB) {
    delete query.game
  } else {
    query.game = tab
  }
  await router.replace({ path: route.path, query })
  if (tab === SERIES_TOTAL_TAB) {
    match.value = null
    syncSelectedTeam()
    return
  }
  try {
    await loadMatchById(tab)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载场次失败'
  }
  syncSelectedTeam()
}

function goPlayer(sourcePlayerId?: string) {
  const id = String(sourcePlayerId || '').trim()
  if (!id) return
  router.push({
    path: `/player/${id}`,
    query: {
      ...(match.value?.seriesCode || series.value?.eventSeriesCode
        ? { series: String(match.value?.seriesCode || series.value?.eventSeriesCode).toUpperCase() }
        : {}),
      ...(match.value?.divisionId || series.value?.divisionId
        ? { divisionId: String(match.value?.divisionId || series.value?.divisionId) }
        : {}),
    },
  })
}

watch([seriesId, matchId, () => route.name], () => {
  void loadPage()
})

onMounted(() => {
  void loadPage()
})
</script>

<template>
  <div class="match-detail-page">
    <section class="site-section match-detail-hero">
      <div class="site-shell">
        <nav class="match-detail-crumb">
          <router-link to="/events">赛事</router-link>
          <span>/</span>
          <router-link :to="seasonPath">{{ breadcrumb[1] }}</router-link>
          <span>/</span>
          <strong>{{ breadcrumb[2] }}</strong>
        </nav>
        <div v-if="loading" class="match-detail-state">加载中...</div>
        <div v-else-if="error" class="match-detail-state is-error">{{ error }}</div>
        <template v-else>
          <MatchScoreboard
            :left-team="leftTeam"
            :right-team="rightTeam"
            :score="score"
            :winner-event-team-id="winnerEventTeamId"
            :status-text="statusText"
            :meta-text="metaText"
            :caption="caption"
          />
          <p
            v-if="seriesMode && activeTab === SERIES_TOTAL_TAB && (series?.resultReason || series?.resultSummary)"
            class="match-detail-reason"
          >
            {{ series?.resultReason || series?.resultSummary }}
          </p>
          <SeriesGameTabs
            v-if="showSeriesTabs"
            :tabs="seriesTabs"
            :model-value="activeTab"
            @update:model-value="onTabChange"
          />
        </template>
      </div>
    </section>

    <section
      v-if="!loading && !error && (series || match)"
      class="site-section match-detail-content"
    >
      <div class="site-shell">
        <PaperSection edge="none" tone="light" class="match-detail-paper">
          <div class="match-detail-paper__inner">
            <PeriodScoreBoard
              v-if="showPeriodBoard && match"
              :left-team="leftTeam"
              :right-team="rightTeam"
              :periods="match.periodScores || []"
              :left-total="match.awayScore"
              :right-total="match.homeScore"
            />
            <BoxScorePanel
              v-if="showBoxScore"
              :teams="boxTeams"
              :selected-team-id="selectedTeamId"
              :players="boxPlayers"
              :team-stats="boxTeamStats"
              @update:selected-team-id="selectedTeamId = $event"
              @go-player="goPlayer"
            />
          </div>
        </PaperSection>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.match-detail-page {
  width: 100%;
  background: #050505;
  overflow-x: clip;
}

.match-detail-hero {
  background:
    radial-gradient(circle at 50% 0%, rgba(229, 20, 34, 0.22), transparent 42%),
    linear-gradient(180deg, #141414 0%, #050505 100%);
  padding: 28px 0 36px;
  color: #fff;
}

.match-detail-hero :deep(.series-game-tabs) {
  margin-top: 28px;
}

.match-detail-crumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 28px;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  font-weight: 700;

  a,
  strong {
    color: inherit;
    text-decoration: none;
  }

  strong {
    color: #fff;
  }
}

.match-detail-state {
  min-height: 180px;
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.72);
  font-weight: 700;
}

.match-detail-state.is-error {
  color: #ff8a8a;
}

.match-detail-reason {
  margin: 16px 0 0;
  text-align: center;
  color: #ffb38a;
  font-size: 13px;
  font-weight: 700;
}

.match-detail-paper {
  margin-top: 0;
}

.match-detail-paper__inner {
  padding: 36px var(--cnpa-public-content-gutter) 72px;
}

@media (max-width: 640px) {
  .match-detail-hero {
    padding-top: 18px;
  }

  .match-detail-paper__inner {
    padding-top: 24px;
  }
}
</style>
