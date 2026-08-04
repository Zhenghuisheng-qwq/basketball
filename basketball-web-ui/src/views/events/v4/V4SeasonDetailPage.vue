<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getV4PublicMatchList,
  getV4PublicPlayerStat,
  getV4PublicRank,
  getV4PublicSeasonDetail,
  getV4PublicStanding,
  getV4PublicStageList,
  getV4PublicStageStandings,
  getV4PublicStageBracket,
  getV4PublicTeamList,
  getV4PublicTeamStat,
} from '@/api/v4Public'
import type {
  V4PublicEventTeamItem,
  V4PublicMatchItem,
  V4PublicPlayerStatItem,
  V4PublicRankItem,
  V4PublicSeasonDetail,
  V4PublicStandingItem,
  V4PublicStageItem,
  V4PublicStageStandingScope,
  V4PublicTeamStatItem,
  V4SeasonDetailTab,
} from '@/api/types/v4Public'
import SafeImage from '@/components/common/SafeImage.vue'
import { displayText, formatDateTime, formatDecimal, initials } from '@/utils/publicDisplay'
import {
  formatScore,
  v4FormatTypeLabel,
  v4MatchStatusLabel,
  v4PlatformLabel,
  v4RankTypeLabel,
  v4RegistrationModeLabel,
  v4SeasonStatusLabel,
  v4SeriesLabel,
} from '@/utils/v4Display'
import { getV4PublicTeamDetail } from '@/api/v4Public'
import type { V4PublicEventTeamDetail } from '@/api/types/v4Public'
import '../styles/v4-events.scss'

const route = useRoute()
const router = useRouter()

const seasonId = computed(() => String(route.params.seasonId || ''))
const activeTab = ref<V4SeasonDetailTab>('overview')
const activeDivisionId = ref('')
const scheduleFilter = ref<'all' | 'upcoming' | 'finished'>('all')
const statsSubTab = ref<'team' | 'player' | 'rank'>('team')
const rankType = ref('PLAYER_SCORE')

const loading = ref(true)
const tabLoading = ref(false)
const error = ref('')
const season = ref<V4PublicSeasonDetail | null>(null)

const teams = ref<V4PublicEventTeamItem[]>([])
const matches = ref<V4PublicMatchItem[]>([])
const standings = ref<V4PublicStandingItem[]>([])
const stageList = ref<V4PublicStageItem[]>([])
const stageStandingScopes = ref<V4PublicStageStandingScope[]>([])
const activeStandingStageId = ref('')
const bracketMatches = ref<V4PublicMatchItem[]>([])
const teamStats = ref<V4PublicTeamStatItem[]>([])
const playerStats = ref<V4PublicPlayerStatItem[]>([])
const ranks = ref<V4PublicRankItem[]>([])

const expandedTeamId = ref('')
const expandedTeamDetail = ref<V4PublicEventTeamDetail | null>(null)
const rosterLoading = ref(false)

const tabs: Array<{ key: V4SeasonDetailTab; label: string }> = [
  { key: 'overview', label: '赛事概况' },
  { key: 'teams', label: '参赛队伍' },
  { key: 'schedule', label: '赛程' },
  { key: 'standing', label: '积分榜' },
  { key: 'stats', label: '数据榜单' },
]

const currentDivision = computed(() =>
  season.value?.divisions.find((item) => item.divisionId === activeDivisionId.value) ?? null,
)

const matchesByRound = computed(() => {
  const map = new Map<number, V4PublicMatchItem[]>()
  for (const match of matches.value) {
    const round = match.roundNo ?? 0
    const list = map.get(round) ?? []
    list.push(match)
    map.set(round, list)
  }
  return Array.from(map.entries()).sort((a, b) => a[0] - b[0])
})

async function loadSeason() {
  loading.value = true
  error.value = ''
  try {
    const res = await getV4PublicSeasonDetail(seasonId.value)
    season.value = res.data
    activeDivisionId.value =
      (route.query.divisionId as string) ||
      res.data?.defaultDivisionId ||
      res.data?.divisions?.[0]?.divisionId ||
      ''
    await loadTabData()
  } catch (err) {
    season.value = null
    error.value = err instanceof Error ? err.message : '加载赛事详情失败'
  } finally {
    loading.value = false
  }
}

async function loadTabData() {
  if (!activeDivisionId.value) return
  tabLoading.value = true
  try {
    if (activeTab.value === 'teams') {
      const res = await getV4PublicTeamList(activeDivisionId.value)
      teams.value = res.data ?? []
    } else if (activeTab.value === 'schedule') {
      const params: Parameters<typeof getV4PublicMatchList>[0] = {
        divisionId: activeDivisionId.value,
      }
      if (scheduleFilter.value === 'upcoming') params.upcomingOnly = true
      if (scheduleFilter.value === 'finished') params.finishedOnly = true
      const res = await getV4PublicMatchList(params)
      matches.value = res.data ?? []
    } else if (activeTab.value === 'standing') {
      const stageRes = await getV4PublicStageList(activeDivisionId.value)
      stageList.value = stageRes.data ?? []
      if (stageList.value.length) {
        activeStandingStageId.value = stageList.value.find(s => s.currentStage)?.stageId
          || stageList.value[0]?.stageId
          || ''
        if (activeStandingStageId.value) {
          const scopeRes = await getV4PublicStageStandings(activeStandingStageId.value)
          stageStandingScopes.value = scopeRes.data ?? []
          standings.value = stageStandingScopes.value.flatMap(s => s.rows ?? [])
        } else {
          stageStandingScopes.value = []
          standings.value = []
        }
      } else {
        const res = await getV4PublicStanding(activeDivisionId.value)
        standings.value = res.data ?? []
        stageStandingScopes.value = []
      }
    } else if (activeTab.value === 'stats') {
      if (statsSubTab.value === 'team') {
        const res = await getV4PublicTeamStat(activeDivisionId.value)
        teamStats.value = res.data ?? []
      } else if (statsSubTab.value === 'player') {
        const res = await getV4PublicPlayerStat(activeDivisionId.value)
        playerStats.value = res.data ?? []
      } else {
        const res = await getV4PublicRank(activeDivisionId.value, rankType.value)
        ranks.value = res.data ?? []
      }
    }
  } catch {
    teams.value = []
    matches.value = []
    standings.value = []
    teamStats.value = []
    playerStats.value = []
    ranks.value = []
  } finally {
    tabLoading.value = false
  }
}

async function toggleTeamRoster(team: V4PublicEventTeamItem) {
  if (expandedTeamId.value === team.eventTeamId) {
    expandedTeamId.value = ''
    expandedTeamDetail.value = null
    return
  }
  expandedTeamId.value = team.eventTeamId
  rosterLoading.value = true
  try {
    const res = await getV4PublicTeamDetail(team.eventTeamId)
    expandedTeamDetail.value = res.data ?? null
  } catch {
    expandedTeamDetail.value = null
  } finally {
    rosterLoading.value = false
  }
}

function setTab(tab: V4SeasonDetailTab) {
  activeTab.value = tab
  router.replace({
    path: route.path,
    query: { ...route.query, tab, divisionId: activeDivisionId.value },
  })
}

function setDivision(divisionId: string) {
  activeDivisionId.value = divisionId
  expandedTeamId.value = ''
  expandedTeamDetail.value = null
  router.replace({
    path: route.path,
    query: { ...route.query, divisionId },
  })
  void loadTabData()
}

function goMatch(matchId: string) {
  router.push(`/events/match/${matchId}`)
}

function backToList() {
  router.push('/tournament')
}

watch(
  () => route.params.seasonId,
  () => {
    activeTab.value = (route.query.tab as V4SeasonDetailTab) || 'overview'
    void loadSeason()
  },
)

watch([activeTab, scheduleFilter, statsSubTab, rankType], () => {
  if (activeTab.value !== 'overview') {
    void loadTabData()
  }
})

onMounted(() => {
  activeTab.value = (route.query.tab as V4SeasonDetailTab) || 'overview'
  void loadSeason()
})
</script>

<template>
  <div class="v4-events-page">
    <section class="v4-events-hero">
      <div class="site-shell">
        <button type="button" class="v4-btn v4-btn--ghost" @click="backToList">← 返回赛事列表</button>
        <div v-if="loading" class="v4-state">加载中...</div>
        <div v-else-if="error" class="v4-state v4-state--error">{{ error }}</div>
        <template v-else-if="season">
          <p class="v4-events-hero__eyebrow">{{ v4SeriesLabel(season.seriesCode) }} · {{ season.seasonYear }}</p>
          <h1 class="v4-events-hero__title">{{ displayText(season.displayTitle) }}</h1>
          <p class="v4-events-hero__desc">{{ displayText(season.description, '暂无赛事简介') }}</p>
          <div class="v4-season-card__tags">
            <span class="v4-tag">{{ v4SeasonStatusLabel(season.seasonStatus) }}</span>
            <span class="v4-tag">队伍 {{ season.eventTeamCount ?? 0 }}</span>
            <span class="v4-tag">比赛 {{ season.matchCount ?? 0 }}</span>
          </div>
          <div class="v4-division-switch">
            <button
              v-for="division in season.divisions"
              :key="division.divisionId"
              type="button"
              class="v4-series-filter__btn"
              :class="{ 'v4-series-filter__btn--active': activeDivisionId === division.divisionId }"
              @click="setDivision(division.divisionId)"
            >
              {{ v4PlatformLabel(division.platformCode) }}
            </button>
          </div>
          <div class="v4-detail-tabs">
            <button
              v-for="item in tabs"
              :key="item.key"
              type="button"
              class="v4-detail-tabs__btn"
              :class="{ 'v4-detail-tabs__btn--active': activeTab === item.key }"
              @click="setTab(item.key)"
            >
              {{ item.label }}
            </button>
          </div>
        </template>
      </div>
    </section>

    <section v-if="season && !loading" class="v4-events-list-section">
      <div class="site-shell">
        <div v-if="activeTab === 'overview'" class="v4-overview-grid">
          <div class="v4-overview-item">
            <dt>报名方式</dt>
            <dd>{{ v4RegistrationModeLabel(currentDivision?.registrationMode || season.registrationMode) }}</dd>
          </div>
          <div class="v4-overview-item">
            <dt>赛制</dt>
            <dd>{{ v4FormatTypeLabel(season.formatType) }}</dd>
          </div>
          <div class="v4-overview-item">
            <dt>队伍上限</dt>
            <dd>{{ currentDivision?.maxTeamCount ?? '--' }}</dd>
          </div>
          <div class="v4-overview-item">
            <dt>阵容人数</dt>
            <dd>{{ currentDivision?.lineupMinCount ?? '--' }} - {{ currentDivision?.lineupMaxCount ?? '--' }}</dd>
          </div>
          <div class="v4-overview-item">
            <dt>报名时间</dt>
            <dd>{{ formatDateTime(season.registrationStartTime).full }} — {{ formatDateTime(season.registrationEndTime).full }}</dd>
          </div>
          <div class="v4-overview-item">
            <dt>比赛时间</dt>
            <dd>{{ formatDateTime(season.matchStartTime).full }} — {{ formatDateTime(season.matchEndTime).full }}</dd>
          </div>
          <div class="v4-overview-item">
            <dt>当前赛区</dt>
            <dd>{{ displayText(currentDivision?.divisionName, v4PlatformLabel(currentDivision?.platformCode)) }}</dd>
          </div>
        </div>

        <div v-else-if="activeTab === 'teams'">
          <div v-if="tabLoading" class="v4-state">加载中...</div>
          <div v-else-if="!teams.length" class="v4-state">暂无参赛队伍</div>
          <div v-else class="v4-team-grid">
            <div v-for="team in teams" :key="team.eventTeamId">
              <article class="v4-team-card" @click="toggleTeamRoster(team)">
                <div class="v4-team-card__head">
                  <SafeImage
                    :src="team.teamLogoUrl"
                    :alt="team.teamName"
                    :initials="initials(team.teamName, 2)"
                    :size="48"
                  />
                  <div>
                    <h3 class="v4-team-card__name">{{ displayText(team.teamName) }}</h3>
                    <p class="v4-team-card__meta">
                      队长 {{ displayText(team.captainName) }} · {{ v4PlatformLabel(team.platformCode) }}
                    </p>
                  </div>
                </div>
                <p class="v4-team-card__meta">点击查看阵容</p>
              </article>
              <div
                v-if="expandedTeamId === team.eventTeamId"
                class="v4-roster-panel"
              >
                <div v-if="rosterLoading" class="v4-state">加载阵容...</div>
                <div v-else-if="!expandedTeamDetail?.players?.length" class="v4-state">暂无阵容数据</div>
                <div v-else class="v4-table-wrap">
                  <table class="v4-table">
                    <thead>
                      <tr>
                        <th>球员</th>
                        <th>位置</th>
                        <th>号码</th>
                        <th>角色</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="player in expandedTeamDetail.players" :key="player.eventPlayerId">
                        <td>{{ displayText(player.playerName) }}</td>
                        <td>{{ displayText(player.position) }}</td>
                        <td>{{ displayText(player.jerseyNo) }}</td>
                        <td>{{ player.isCaptain ? '队长' : player.isStarter ? '首发' : '替补' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'schedule'">
          <div class="v4-stats-subtabs">
            <button
              v-for="item in ([['all', '全部'], ['upcoming', '未开始'], ['finished', '已结束']] as const)"
              :key="item[0]"
              type="button"
              class="v4-detail-tabs__btn"
              :class="{ 'v4-detail-tabs__btn--active': scheduleFilter === item[0] }"
              @click="scheduleFilter = item[0]"
            >
              {{ item[1] }}
            </button>
          </div>
          <div v-if="tabLoading" class="v4-state">加载中...</div>
          <div v-else-if="!matches.length" class="v4-state">暂无已发布赛程</div>
          <div v-else>
            <section v-for="[round, roundMatches] in matchesByRound" :key="round" class="v4-schedule-round">
              <h3 class="v4-schedule-round__title">第 {{ round }} 轮</h3>
              <div
                v-for="match in roundMatches"
                :key="match.matchId"
                class="v4-match-row"
                @click="goMatch(match.matchId)"
              >
                <div class="v4-match-row__team">
                  <SafeImage
                    :src="match.homeTeamLogoUrl"
                    :alt="match.homeTeamName"
                    :initials="initials(match.homeTeamName, 2)"
                    :size="36"
                  />
                  <span>{{ displayText(match.homeTeamName) }}</span>
                </div>
                <div class="v4-match-row__center">
                  <div class="v4-match-row__score">
                    {{ formatScore(match.homeScore, match.awayScore, match.matchStatus) }}
                  </div>
                  <div class="v4-match-row__status">{{ v4MatchStatusLabel(match.matchStatus) }}</div>
                  <div class="v4-match-row__status">{{ formatDateTime(match.matchTime).full }}</div>
                </div>
                <div class="v4-match-row__team v4-match-row__team--away">
                  <span>{{ displayText(match.awayTeamName) }}</span>
                  <SafeImage
                    :src="match.awayTeamLogoUrl"
                    :alt="match.awayTeamName"
                    :initials="initials(match.awayTeamName, 2)"
                    :size="36"
                  />
                </div>
              </div>
            </section>
          </div>
        </div>

        <div v-else-if="activeTab === 'standing'">
          <div v-if="tabLoading" class="v4-state">加载中...</div>
          <template v-else-if="stageStandingScopes.length">
            <div v-for="scope in stageStandingScopes" :key="scope.groupScopeKey || 'overall'" class="v4-standing-scope">
              <h4>{{ scope.groupName || (scope.groupId ? `小组 ${scope.groupId}` : '总积分榜') }}</h4>
              <div class="v4-table-wrap">
                <table class="v4-table">
                  <thead>
                    <tr>
                      <th>排名</th><th>队伍</th><th>胜</th><th>负</th><th>净胜分</th><th>积分</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in scope.rows || []" :key="row.eventTeamId">
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
            </div>
          </template>
          <div v-else-if="!standings.length" class="v4-state">暂无积分榜数据</div>
          <div v-else class="v4-table-wrap">
            <table class="v4-table">
              <thead>
                <tr>
                  <th>排名</th>
                  <th>队伍</th>
                  <th>胜</th>
                  <th>负</th>
                  <th>得分</th>
                  <th>失分</th>
                  <th>净胜分</th>
                  <th>积分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in standings" :key="row.eventTeamId">
                  <td>{{ row.rankNo ?? '--' }}</td>
                  <td>{{ displayText(row.teamName) }}</td>
                  <td>{{ row.winCount ?? 0 }}</td>
                  <td>{{ row.loseCount ?? 0 }}</td>
                  <td>{{ row.pointFor ?? 0 }}</td>
                  <td>{{ row.pointAgainst ?? 0 }}</td>
                  <td>{{ row.pointDiff ?? 0 }}</td>
                  <td>{{ row.standingPoints ?? 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-else-if="activeTab === 'stats'">
          <div class="v4-stats-subtabs">
            <button
              type="button"
              class="v4-detail-tabs__btn"
              :class="{ 'v4-detail-tabs__btn--active': statsSubTab === 'team' }"
              @click="statsSubTab = 'team'"
            >
              球队统计
            </button>
            <button
              type="button"
              class="v4-detail-tabs__btn"
              :class="{ 'v4-detail-tabs__btn--active': statsSubTab === 'player' }"
              @click="statsSubTab = 'player'"
            >
              球员统计
            </button>
            <button
              type="button"
              class="v4-detail-tabs__btn"
              :class="{ 'v4-detail-tabs__btn--active': statsSubTab === 'rank' }"
              @click="statsSubTab = 'rank'"
            >
              排行榜
            </button>
          </div>
          <div v-if="statsSubTab === 'rank'" class="v4-stats-subtabs">
            <button
              v-for="type in ['PLAYER_SCORE', 'PLAYER_ASSIST', 'PLAYER_REBOUND']"
              :key="type"
              type="button"
              class="v4-detail-tabs__btn"
              :class="{ 'v4-detail-tabs__btn--active': rankType === type }"
              @click="rankType = type"
            >
              {{ v4RankTypeLabel(type) }}
            </button>
          </div>
          <div v-if="tabLoading" class="v4-state">加载中...</div>
          <template v-else>
            <div v-if="statsSubTab === 'team'">
              <div v-if="!teamStats.length" class="v4-state">暂无球队统计数据</div>
              <div v-else class="v4-table-wrap">
                <table class="v4-table">
                  <thead>
                    <tr>
                      <th>队伍</th>
                      <th>场次</th>
                      <th>胜</th>
                      <th>负</th>
                      <th>得分</th>
                      <th>失分</th>
                      <th>篮板</th>
                      <th>助攻</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in teamStats" :key="row.eventTeamId">
                      <td>{{ displayText(row.teamName) }}</td>
                      <td>{{ row.matchCount ?? 0 }}</td>
                      <td>{{ row.winCount ?? 0 }}</td>
                      <td>{{ row.loseCount ?? 0 }}</td>
                      <td>{{ formatDecimal(row.avgPointsFor) }}</td>
                      <td>{{ formatDecimal(row.avgPointsAgainst) }}</td>
                      <td>{{ row.rebound ?? 0 }}</td>
                      <td>{{ row.assist ?? 0 }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-else-if="statsSubTab === 'player'">
              <div v-if="!playerStats.length" class="v4-state">暂无球员统计数据</div>
              <div v-else class="v4-table-wrap">
                <table class="v4-table">
                  <thead>
                    <tr>
                      <th>球员</th>
                      <th>球队</th>
                      <th>场次</th>
                      <th>得分</th>
                      <th>篮板</th>
                      <th>助攻</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in playerStats" :key="row.eventPlayerId">
                      <td>{{ displayText(row.playerName) }}</td>
                      <td>{{ displayText(row.teamName) }}</td>
                      <td>{{ row.matchCount ?? 0 }}</td>
                      <td>{{ formatDecimal(row.avgPoints) }}</td>
                      <td>{{ formatDecimal(row.avgRebound) }}</td>
                      <td>{{ formatDecimal(row.avgAssist) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-else>
              <div v-if="!ranks.length" class="v4-state">暂无排行榜数据</div>
              <div v-else class="v4-table-wrap">
                <table class="v4-table">
                  <thead>
                    <tr>
                      <th>排名</th>
                      <th>球员/球队</th>
                      <th>数值</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in ranks" :key="`${row.rankType}-${row.rankNo}-${row.eventPlayerId || row.eventTeamId}`">
                      <td>{{ row.rankNo ?? '--' }}</td>
                      <td>{{ displayText(row.playerName || row.teamName) }}</td>
                      <td>{{ formatDecimal(row.rankValue) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>
