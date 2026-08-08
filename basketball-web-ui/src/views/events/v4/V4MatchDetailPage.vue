<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicMatchDetail } from '@/api/v4Public'
import type { V4PublicMatchDetail } from '@/api/types/v4Public'
import SafeImage from '@/components/common/SafeImage.vue'
import { displayText, formatDateTime, initials } from '@/utils/publicDisplay'
import { formatScore, v4BestOfLabel, v4MatchSeriesStatusLabel, v4MatchStatusLabel } from '@/utils/v4Display'
import '../styles/v4-events.scss'

const route = useRoute()
const router = useRouter()

const matchId = computed(() => String(route.params.matchId || ''))
const loading = ref(true)
const error = ref('')
const match = ref<V4PublicMatchDetail | null>(null)

async function loadMatch() {
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
    router.push(`/events/${match.value.seasonId}?tab=schedule&divisionId=${match.value.divisionId}`)
    return
  }
  router.push('/tournament')
}

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
            <span>局分 {{ match.seriesContext.homeWinCount ?? 0 }} : {{ match.seriesContext.awayWinCount ?? 0 }}</span>
            <span>{{ match.seriesContext.seriesStatusName || v4MatchSeriesStatusLabel(match.seriesContext.seriesStatus) }}</span>
          </div>
          <p class="v4-events-hero__eyebrow">第 {{ match.roundNo ?? '--' }} 轮 · {{ formatDateTime(match.matchTime).full }}</p>
          <div class="v4-match-row" style="cursor: default; margin-top: 20px">
            <div class="v4-match-row__team">
              <SafeImage
                :src="match.homeTeam?.teamLogoUrl"
                :alt="match.homeTeam?.teamName"
                :initials="initials(match.homeTeam?.teamName, 2)"
                :size="56"
              />
              <span>{{ displayText(match.homeTeam?.teamName) }}</span>
            </div>
            <div class="v4-match-row__center">
              <div class="v4-match-row__score">
                {{ formatScore(match.homeScore, match.awayScore, match.matchStatus) }}
              </div>
              <div class="v4-match-row__status">{{ v4MatchStatusLabel(match.matchStatus) }}</div>
              <div class="v4-match-row__status">{{ displayText(match.venueText, '暂无场地信息') }}</div>
            </div>
            <div class="v4-match-row__team v4-match-row__team--away">
              <span>{{ displayText(match.awayTeam?.teamName) }}</span>
              <SafeImage
                :src="match.awayTeam?.teamLogoUrl"
                :alt="match.awayTeam?.teamName"
                :initials="initials(match.awayTeam?.teamName, 2)"
                :size="56"
              />
            </div>
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
            :key="game.matchId"
            class="v4-match-row"
            :class="{ 'v4-match-row--active': game.currentGame }"
            @click="game.matchId && game.matchId !== match.matchId && router.push(`/events/match/${game.matchId}`)"
          >
            <span>Game {{ game.seriesGameNo }}</span>
            <span v-if="game.skipped">{{ game.skippedReason || '无需进行' }}</span>
            <span v-else>{{ formatScore(game.homeScore, game.awayScore, game.matchStatus) }}</span>
            <span>{{ game.matchStatusName || v4MatchStatusLabel(game.matchStatus) }}</span>
          </div>
        </div>
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
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in match.teamStats" :key="row.eventTeamId">
                <td>{{ displayText(row.teamName) }}</td>
                <td>{{ row.score ?? row.pointsFor ?? 0 }}</td>
                <td>{{ row.rebound ?? 0 }}</td>
                <td>{{ row.assist ?? 0 }}</td>
                <td>{{ row.steal ?? 0 }}</td>
                <td>{{ row.block ?? 0 }}</td>
                <td>{{ row.turnover ?? 0 }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2 class="v4-schedule-round__title" style="margin-top: 32px">单场球员数据</h2>
        <div v-if="!match.playerStats?.length" class="v4-state">暂无数据</div>
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
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in match.playerStats" :key="row.eventPlayerId">
                <td>{{ displayText(row.playerName) }}</td>
                <td>{{ displayText(row.teamName) }}</td>
                <td>{{ row.points ?? 0 }}</td>
                <td>{{ row.rebound ?? 0 }}</td>
                <td>{{ row.assist ?? 0 }}</td>
                <td>{{ row.steal ?? 0 }}</td>
                <td>{{ row.block ?? 0 }}</td>
                <td>{{ row.turnover ?? 0 }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>
