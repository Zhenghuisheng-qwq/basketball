<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicMatchDetail, getV4PublicMatchSeries } from '@/api/v4Public'
import type { V4PublicMatchDetail, V4PublicMatchSeriesDetail } from '@/api/types/v4Public'
import PaperSection from '@/components/common/PaperSection.vue'
import { businessKickoffLabel } from '@/utils/businessDateTime'
import { requiresPeriodScores } from '@/utils/fullMatchSubmissionRules'
import {
  publicContestDetailLabel,
  publicContestDetailPath,
  v4BestOfLabel,
  v4MatchStatusLabel,
  v4PublicRoundLabel,
  v4SeriesHeroStatusLabel,
  v4StageLabel,
} from '@/utils/v4Display'
import MatchScoreboard from '../match-detail/MatchScoreboard.vue'
import SeriesProgress from '../match-detail/SeriesProgress.vue'
import SeriesGameTabs from '../match-detail/SeriesGameTabs.vue'
import PeriodScoreBoard from '../match-detail/PeriodScoreBoard.vue'
import BoxScorePanel from '../match-detail/BoxScorePanel.vue'
import heroBg from '@/assets/icon/score/detail_top_background.png'
import {
  SERIES_TOTAL_TAB,
  formatMatchWhen,
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
const { t } = useI18n()

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
const seriesRoundName = ref('')

const visibleGames = computed(() => publicSeriesGames(series.value))
const progressGames = computed(() =>
  [...(series.value?.games || [])].sort((a, b) => (a.seriesGameNo || 0) - (b.seriesGameNo || 0)),
)

const seriesTabs = computed(() => {
  if (!seriesMode.value) return []
  return [
    { key: SERIES_TOTAL_TAB, label: t('eventsPage.match.seriesStats') },
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
  if (seriesMode.value) {
    const s = series.value
    return toDetailTeam(s?.awayEventTeamId, s?.awayTeamName, s?.awayTeamLogoUrl, s?.awayTeamShortName)
  }
  if (match.value) return teamFromMatchSide(match.value, 'away')
  return toDetailTeam()
})

const rightTeam = computed(() => {
  if (seriesMode.value) {
    const s = series.value
    return toDetailTeam(s?.homeEventTeamId, s?.homeTeamName, s?.homeTeamLogoUrl, s?.homeTeamShortName)
  }
  if (match.value) return teamFromMatchSide(match.value, 'home')
  return toDetailTeam()
})

const score = computed(() => {
  if (seriesMode.value) {
    return seriesWinScore(series.value)
  }
  return matchBasketballScore(match.value)
})

const winnerEventTeamId = computed(() => {
  if (seriesMode.value) {
    return series.value?.winnerEventTeamId || ''
  }
  return match.value?.winnerEventTeamId || ''
})

const statusText = computed(() => {
  if (seriesMode.value) {
    return v4SeriesHeroStatusLabel(series.value?.seriesStatus)
  }
  return v4MatchStatusLabel(match.value?.matchStatus)
})

const caption = computed(() => {
  if (seriesMode.value) return ''
  return ''
})

const advanceLabel = computed(() => {
  if (!seriesMode.value) return ''
  const status = String(series.value?.seriesStatus || '').toUpperCase()
  if (status !== 'COMPLETED' || !series.value?.winnerEventTeamId) return ''
  if (series.value?.finalStage) return t('eventsPage.match.champion')
  return t('eventsPage.match.advance')
})

const seriesNote = computed(() => {
  if (!seriesMode.value) return ''
  const type = String(series.value?.resolutionType || '').toUpperCase()
  if (type === 'FORFEIT') return t('events.resolution.FORFEIT')
  if (type === 'MANUAL') return String(series.value?.resultReason || '').trim()
  return ''
})

function seriesStartText(date?: string | null, time?: string | null): string {
  const label = businessKickoffLabel(date, time)
  if (label) return t('publicPages.team.seriesStart', { time: label })
  return formatMatchWhen(null, date, null)
}

const metaText = computed(() => {
  if (seriesMode.value) {
    const s = series.value
    const stage = v4StageLabel(undefined, s?.stageName)
    const round = v4PublicRoundLabel(seriesRoundName.value, s?.roundNo)
    const when = seriesStartText(s?.scheduledDate, s?.scheduledStartTime)
    return [stage, round, v4BestOfLabel(s?.bestOf), when].filter(Boolean).join(' · ')
  }
  const m = match.value
  if (!m) return ''
  const stage = v4StageLabel(m.stageType, m.stageName) || t('events.regularSeason')
  const round = m.roundNo != null ? t('events.round.OTHER', { round: m.roundNo }) : ''
  const when = m.matchTime
    ? formatMatchWhen(m.matchTime)
    : (m.bestOf && m.bestOf > 1 ? t('publicPages.team.followsSeries') : '')
  return [stage, round, v4BestOfLabel(m.bestOf || 1), when].filter(Boolean).join(' · ')
})

const breadcrumb = computed(() => {
  const eventName = series.value?.eventSeriesName
    || match.value?.seriesName
    || t('events.title')
  return [t('events.title'), eventName, seriesMode.value ? publicContestDetailLabel('MATCH_SERIES') : publicContestDetailLabel('MATCH')]
})

const parentSeriesPath = computed(() => {
  if (seriesMode.value) return ''
  return publicContestDetailPath({
    unitType: 'MATCH_SERIES',
    matchSeriesId: match.value?.matchSeriesId,
  })
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

const boxTitle = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) return t('eventsPage.match.seriesBox')
  return t('eventsPage.match.boxScore')
})

const boxSubtitle = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) return 'SERIES TOTALS'
  return 'BOX SCORE'
})

const boxHint = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    const n = visibleGames.value.length || progressGames.value.filter((game) => !game.skipped).length
    return n ? t('eventsPage.count.seriesTotals', { count: n }) : ''
  }
  if (seriesMode.value && activeGame.value) {
    const no = activeGame.value.seriesGameNo
    const when = activeGame.value.scheduledTime
      ? formatMatchWhen(activeGame.value.scheduledTime)
      : t('publicPages.team.followsSeries')
    return ['G' + no, when].filter(Boolean).join(' · ')
  }
  return ''
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

function contestQuery() {
  const seasonId = series.value?.seasonId || match.value?.seasonId || String(route.query.seasonId || '')
  const divisionId = series.value?.divisionId || match.value?.divisionId || String(route.query.divisionId || '')
  return {
    ...(seasonId ? { seasonId } : {}),
    ...(divisionId ? { divisionId } : {}),
  }
}

function goRealMatchDetail(id: string) {
  return router.push({
    path: `/events/match/${id}`,
    query: contestQuery(),
  })
}

function applyTabFromRoute() {
  if (!seriesMode.value) {
    activeTab.value = SERIES_TOTAL_TAB
    return
  }
  if (gameQuery.value && visibleGames.value.some((game) => String(game.matchId) === gameQuery.value)) {
    activeTab.value = SERIES_TOTAL_TAB
    void router.replace({
      path: `/events/match/${gameQuery.value}`,
      query: contestQuery(),
    })
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
        throw new Error(t('eventsPage.match.invalidSeries'))
      }
      const res = await getV4PublicMatchSeries(seriesId.value)
      series.value = res.data ?? null
      if (!series.value) {
        throw new Error(t('eventsPage.match.noSeries'))
      }
      applyTabFromRoute()
      seriesRoundName.value = String(series.value.roundName || '').trim()
      if (activeTab.value !== SERIES_TOTAL_TAB) {
        await loadMatchById(activeTab.value)
      } else {
        match.value = null
      }
    } else {
      series.value = null
      if (!matchId.value) {
        throw new Error(t('eventsPage.match.invalidMatch'))
      }
      await loadMatchById(matchId.value)
      if (!match.value) {
        throw new Error(t('eventsPage.match.noMatch'))
      }
    }
    syncSelectedTeam()
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('eventsPage.match.loadFailed')
    if (seriesMode.value) series.value = null
    else match.value = null
  } finally {
    loading.value = false
  }
}

async function onTabChange(tab: string) {
  if (tab !== SERIES_TOTAL_TAB) {
    await goRealMatchDetail(tab)
    return
  }
  activeTab.value = SERIES_TOTAL_TAB
  const query = { ...route.query }
  delete query.game
  await router.replace({ path: route.path, query })
  match.value = null
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
}, { immediate: true })
</script>

<template>
  <div class="match-detail-page">
    <section class="site-section match-detail-hero">
      <div class="site-shell">
        <div class="match-detail-hero__frame">
          <img
            class="match-detail-hero__image"
            :src="heroBg"
            alt=""
            draggable="false"
          />
          <div class="match-detail-hero__shade" aria-hidden="true" />
          <div class="match-detail-hero__body">
        <nav class="match-detail-crumb">
          <router-link to="/events">{{ t('events.title') }}</router-link>
          <span>/</span>
          <router-link :to="seasonPath">{{ breadcrumb[1] }}</router-link>
          <span>/</span>
          <strong>{{ breadcrumb[2] }}</strong>
          <template v-if="parentSeriesPath">
            <span>/</span>
            <router-link :to="parentSeriesPath">{{ publicContestDetailLabel('MATCH_SERIES') }}</router-link>
          </template>
        </nav>
        <div v-if="loading" class="match-detail-state">{{ t('common.loading') }}</div>
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
            :advance-label="advanceLabel"
          />
          <SeriesProgress
            v-if="seriesMode && progressGames.length"
            :games="progressGames"
            :left-team="leftTeam"
            :right-team="rightTeam"
            :active-match-id="activeTab === SERIES_TOTAL_TAB ? '' : activeTab"
            @select="onTabChange"
          />
          <p
            v-if="seriesMode && activeTab === SERIES_TOTAL_TAB && seriesNote"
            class="match-detail-reason"
          >
            {{ seriesNote }}
          </p>
          <SeriesGameTabs
            v-if="showSeriesTabs"
            :tabs="seriesTabs"
            :model-value="activeTab"
            :primary-key="SERIES_TOTAL_TAB"
            @update:model-value="onTabChange"
          />
        </template>
          </div>
        </div>
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
              :winner-event-team-id="match.winnerEventTeamId"
            />
            <BoxScorePanel
              v-if="showBoxScore"
              :teams="boxTeams"
              :selected-team-id="selectedTeamId"
              :players="boxPlayers"
              :team-stats="boxTeamStats"
              :title="boxTitle"
              :subtitle="boxSubtitle"
              :hint="boxHint"
              :variant="seriesMode ? 'series' : 'default'"
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
  background: #050505;
  padding: 0;
  color: #fff;
}

.match-detail-hero__frame {
  position: relative;
  overflow: hidden;
}

.match-detail-hero__image {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 28%;
  pointer-events: none;
  user-select: none;
}

.match-detail-hero__shade {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(180deg, rgba(5, 5, 5, 0.16) 0%, rgba(5, 5, 5, 0.22) 68%, #050505 100%);
  pointer-events: none;
}

.match-detail-hero__body {
  position: relative;
  z-index: 2;
  padding: 28px 0 36px;
}

.match-detail-hero :deep(.series-game-tabs) {
  margin-top: 22px;
}

.match-detail-hero :deep(.series-progress) {
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
  padding: 28px var(--cnpa-public-content-gutter) 56px;
}

@media (max-width: 640px) {
  .match-detail-hero__body {
    padding-top: 18px;
  }

  .match-detail-paper__inner {
    padding-top: 24px;
  }
}
</style>
