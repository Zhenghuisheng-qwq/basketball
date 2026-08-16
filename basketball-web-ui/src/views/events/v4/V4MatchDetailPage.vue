<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicMatchDetail } from '@/api/v4Public'
import type { V4PublicMatchDetail } from '@/api/types/v4Public'
import SafeImage from '@/components/common/SafeImage.vue'
import { displayNumber, displayText, formatDateTime, initials } from '@/utils/publicDisplay'
import { formatScore, formatSeriesSetScore, v4BestOfLabel, v4MatchSeriesStatusLabel, v4MatchStatusLabel } from '@/utils/v4Display'
import { requiresPeriodScores } from '@/utils/fullMatchSubmissionRules'
import '../styles/v4-events.scss'

const route = useRoute()
const router = useRouter()

const matchId = computed(() => String(route.params.matchId || ''))
const loading = ref(true)
const error = ref('')
const match = ref<V4PublicMatchDetail | null>(null)

async function loadMatch() {
  if (!matchId.value) {
    match.value = null
    error.value = '比赛主键无效'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await getV4PublicMatchDetail(matchId.value)
    match.value = res.data
  } catch (err) {
    match.value = null
    error.value = err instanceof Error ? err.message : '加载比赛详情失败'
  } finally {
    loading.value = false
  }
}

function backToSeason() {
  if (match.value?.seasonId) {
    router.push({
      path: '/tournament',
      query: {
        seasonId: match.value.seasonId,
        divisionId: match.value.divisionId || undefined,
        tab: 'schedule',
      },
    })
    return
  }
  router.push('/tournament')
}

function goTeam(eventTeamId?: string | null) {
  const id = String(eventTeamId || '').trim()
  if (!id || id === '0' || id === 'null' || id === 'undefined') return
  const numeric = Number(id)
  if (!Number.isFinite(numeric) || numeric <= 0) return
  router.push({
    path: `/tournament/team/${id}`,
    query: {
      ...(match.value?.seriesCode ? { series: String(match.value.seriesCode).toUpperCase() } : {}),
      ...(match.value?.divisionId ? { divisionId: String(match.value.divisionId) } : {}),
    },
  })
}

function goPlayer(sourcePlayerId?: string | null) {
  const id = String(sourcePlayerId || '').trim()
  if (!id) return
  router.push({
    path: `/player/${id}`,
    query: {
      ...(match.value?.seriesCode ? { series: String(match.value.seriesCode).toUpperCase() } : {}),
      ...(match.value?.divisionId ? { divisionId: String(match.value.divisionId) } : {}),
    },
  })
}

/** 空值 / 无有效出手 → --，避免假 0% */
function formatShooting(made?: number | null, attempt?: number | null): string {
  if (made == null || attempt == null || attempt <= 0) return '--'
  return `${made}/${attempt}`
}

function statCell(value?: number | null): string {
  return displayNumber(value, '--')
}

const contextLine = computed(() => {
  const m = match.value
  if (!m) return ''
  return [m.seriesName, m.seasonName, m.divisionName, m.stageName]
    .map((item) => (item || '').trim())
    .filter(Boolean)
    .join(' · ')
})

const visiblePeriodScores = computed(() => {
  const m = match.value
  if (!m || !requiresPeriodScores(m.gameMode) || !m.periodScores?.length) return []
  return m.periodScores.filter((row) => row.periodNo != null)
})

watch(matchId, () => {
  void loadMatch()
})

onMounted(() => {
  void loadMatch()
})
</script>

<template>
  <div class="v4-events-page">
    <section class="v4-events-hero">
      <div class="site-shell">
        <button type="button" class="v4-btn v4-btn--ghost" @click="backToSeason">← 返回赛程</button>
        <div v-if="loading" class="v4-state">加载中...</div>
        <div v-else-if="error" class="v4-state v4-state--error">{{ error }}</div>
        <template v-else-if="match">
          <div v-if="match.seriesContext" class="v4-series-context">
            <span>{{ v4BestOfLabel(match.bestOf) }}</span>
            <span>Game {{ match.seriesGameNo }} / {{ match.seriesContext.bestOf }}</span>
            <span>局分 {{ formatSeriesSetScore({
              homeWinCount: match.seriesContext.homeWinCount,
              awayWinCount: match.seriesContext.awayWinCount,
              bestOf: match.seriesContext.bestOf || match.bestOf,
              seriesStatus: match.seriesContext.seriesStatus,
              winnerEventTeamId: match.seriesContext.winnerEventTeamId,
              homeEventTeamId: match.seriesContext.homeEventTeamId || match.homeTeam?.eventTeamId,
              awayEventTeamId: match.seriesContext.awayEventTeamId || match.awayTeam?.eventTeamId,
            }) }}</span>
            <span>{{ match.seriesContext.seriesStatusName || v4MatchSeriesStatusLabel(match.seriesContext.seriesStatus) }}</span>
          </div>
          <p v-if="contextLine" class="v4-events-hero__eyebrow">{{ contextLine }}</p>
          <p class="v4-events-hero__eyebrow">
            第 {{ match.roundNo ?? '--' }} 轮 · {{ formatDateTime(match.matchTime).full }}
          </p>
          <div class="v4-match-row" style="margin-top: 20px">
            <button
              type="button"
              class="v4-match-row__team"
              style="cursor: pointer; background: transparent; border: 0; color: inherit"
              @click="goTeam(match.homeTeam?.eventTeamId)"
            >
              <SafeImage
                :src="match.homeTeam?.teamLogoUrl"
                :alt="match.homeTeam?.teamName"
                :initials="initials(match.homeTeam?.teamName, 2)"
                :size="56"
              />
              <span>{{ displayText(match.homeTeam?.teamName) }}</span>
            </button>
            <div class="v4-match-row__center">
              <div class="v4-match-row__score">
                {{ formatScore(match.homeScore, match.awayScore, match.matchStatus, {
                  homeLabel: match.homeResultLabel,
                  awayLabel: match.awayResultLabel,
                  resultType: match.resultType,
                }) }}
              </div>
              <div class="v4-match-row__status">
                {{ match.matchStatusName || v4MatchStatusLabel(match.matchStatus) }}
              </div>
              <div class="v4-match-row__status">{{ displayText(match.venueText, '暂无场地信息') }}</div>
            </div>
            <button
              type="button"
              class="v4-match-row__team v4-match-row__team--away"
              style="cursor: pointer; background: transparent; border: 0; color: inherit"
              @click="goTeam(match.awayTeam?.eventTeamId)"
            >
              <span>{{ displayText(match.awayTeam?.teamName) }}</span>
              <SafeImage
                :src="match.awayTeam?.teamLogoUrl"
                :alt="match.awayTeam?.teamName"
                :initials="initials(match.awayTeam?.teamName, 2)"
                :size="56"
              />
            </button>
          </div>
        </template>
      </div>
    </section>

    <section v-if="match && !loading" class="v4-events-list-section">
      <div class="site-shell">
        <div v-if="match.seriesContext?.games?.length" class="v4-series-games-panel">
          <h2 class="v4-schedule-round__title">系列赛各局</h2>
          <div
            v-for="game in match.seriesContext.games"
            :key="game.matchId || `g-${game.seriesGameNo}`"
            class="v4-match-row"
            :class="{ 'v4-match-row--active': game.currentGame }"
            @click="game.matchId && game.matchId !== match.matchId && !game.skipped && router.push(`/events/match/${game.matchId}`)"
          >
            <span>Game {{ game.seriesGameNo }}</span>
            <span v-if="game.skipped">{{ game.skippedReason || '无需进行' }}</span>
            <span v-else>{{ formatScore(game.homeScore, game.awayScore, game.matchStatus, {
              resultType: game.matchStatus,
            }) }}</span>
            <span>{{ game.matchStatusName || v4MatchStatusLabel(game.matchStatus) }}</span>
          </div>
        </div>

        <template v-if="visiblePeriodScores.length">
          <h2 class="v4-schedule-round__title">单节比分</h2>
          <div class="v4-table-wrap">
            <table class="v4-table">
              <thead>
                <tr>
                  <th>节次</th>
                  <th>{{ displayText(match.homeTeam?.teamName, '主队') }}</th>
                  <th>{{ displayText(match.awayTeam?.teamName, '客队') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in visiblePeriodScores" :key="row.periodNo">
                  <td>Q{{ row.periodNo }}</td>
                  <td>{{ statCell(row.homeScore) }}</td>
                  <td>{{ statCell(row.awayScore) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <h2 class="v4-schedule-round__title">单场球队数据</h2>
        <div v-if="!match.teamStats?.length" class="v4-state">暂无数据</div>
        <div v-else class="v4-table-wrap">
          <table class="v4-table">
            <thead>
              <tr>
                <th>球队</th>
                <th>得分</th>
                <th>篮板</th>
                <th>助攻</th>
                <th>抢断</th>
                <th>盖帽</th>
                <th>失误</th>
                <th>FG</th>
                <th>3P</th>
                <th>FT</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in match.teamStats" :key="row.eventTeamId">
                <td>
                  <button
                    type="button"
                    class="v4-linkish"
                    @click="goTeam(row.eventTeamId)"
                  >
                    {{ displayText(row.teamName) }}
                  </button>
                </td>
                <td>{{ statCell(row.score ?? row.pointsFor) }}</td>
                <td>{{ statCell(row.rebound) }}</td>
                <td>{{ statCell(row.assist) }}</td>
                <td>{{ statCell(row.steal) }}</td>
                <td>{{ statCell(row.block) }}</td>
                <td>{{ statCell(row.turnover) }}</td>
                <td>{{ formatShooting(row.fieldGoalMade, row.fieldGoalAttempt) }}</td>
                <td>{{ formatShooting(row.threePointMade, row.threePointAttempt) }}</td>
                <td>{{ formatShooting(row.freeThrowMade, row.freeThrowAttempt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2 class="v4-schedule-round__title" style="margin-top: 32px">单场球员数据</h2>
        <div v-if="!match.playerStats?.length" class="v4-state">暂无已确认的球员技术统计</div>
        <div v-else class="v4-table-wrap">
          <table class="v4-table">
            <thead>
              <tr>
                <th>球员</th>
                <th>球队</th>
                <th>得分</th>
                <th>篮板</th>
                <th>助攻</th>
                <th>抢断</th>
                <th>盖帽</th>
                <th>失误</th>
                <th>犯规</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in match.playerStats" :key="row.eventPlayerId">
                <td>
                  <button
                    v-if="row.sourcePlayerId"
                    type="button"
                    class="v4-linkish"
                    @click="goPlayer(row.sourcePlayerId)"
                  >
                    {{ displayText(row.playerName) }}
                  </button>
                  <span v-else>{{ displayText(row.playerName) }}</span>
                </td>
                <td>{{ displayText(row.teamName) }}</td>
                <td>{{ statCell(row.points) }}</td>
                <td>{{ statCell(row.rebound) }}</td>
                <td>{{ statCell(row.assist) }}</td>
                <td>{{ statCell(row.steal) }}</td>
                <td>{{ statCell(row.block) }}</td>
                <td>{{ statCell(row.turnover) }}</td>
                <td>{{ statCell(row.foul) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.v4-linkish {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-decoration: underline;
  cursor: pointer;
}
</style>
