<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TornEdge from '@/components/home/TornEdge.vue'
import PlayerLink from '@/components/player/PlayerLink.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import LeagueSystem from '@/components/home/LeagueSystem.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import {
  getPublicHome,
  getPublicHomeBanners,
  getPublicHomePlayerRanking,
  getPublicHomeRecentMatches,
  getPublicHomeStatSummary,
  getPublicHomeTeamRanking,
} from '@/api/publicHome'
import type {
  PublicAnnouncementItemResp,
  PublicHomeBannerResp,
  PublicMatchItemVo,
  PublicPlayerRankItemVo,
  PublicSeasonSummaryVo,
  PublicTeamRankItemVo,
} from '@/api/types/public'
import { formatAnnouncementDate } from '@/utils/announcementNavigate'
import {
  displayText,
  formatDateTime,
  formatPercent,
  formatStatValue,
  initials,
  mapMatchStatus,
} from '@/utils/publicDisplay'
type SectionState = 'loading' | 'success' | 'empty' | 'error'
type HomeCompetitionFilter = 'ALL' | 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'
type HomeFocusFilter = Exclude<HomeCompetitionFilter, 'ALL'>

type HomeBannerItem = {
  id: string
  image: string
}

type HomeCommunityItem = {
  id: string
  name: string
  description: string
  available: boolean
  url?: string
}

const router = useRouter()

const PREVIEW_MATCH_COUNT = 3
const MATCH_QUERY_LIMIT = 10
const PREVIEW_TEAM_COUNT = 6

const homeCompetitionFilters: Array<{ key: HomeCompetitionFilter; label: string }> = [
  { key: 'ALL', label: '全部赛事' },
  { key: 'LEAGUE', label: 'League' },
  { key: 'ARENA', label: 'Arena' },
  { key: 'CUP', label: 'Cup' },
  { key: 'OPEN', label: 'Open' },
]

const homeFocusFilters: Array<{ key: HomeFocusFilter; label: string }> = [
  { key: 'LEAGUE', label: 'League' },
  { key: 'ARENA', label: 'Arena' },
  { key: 'CUP', label: 'Cup' },
  { key: 'OPEN', label: 'Open' },
]

const communityList: HomeCommunityItem[] = [
  { id: 'kook', name: 'KOOK', description: '加入赛事实时交流', available: false },
  { id: 'wechat', name: '微信群', description: '赛事资讯与活动通知', available: false },
  { id: 'bilibili', name: 'B站', description: '关注赛事内容更新', available: false },
  { id: 'douyin', name: '抖音', description: '获取更多精彩片段', available: false },
]

const banners = ref<HomeBannerItem[]>([])
const matches = ref<PublicMatchItemVo[]>([])
const teams = ref<PublicTeamRankItemVo[]>([])
const teamRanks = ref<PublicTeamRankItemVo[]>([])
const playerRanks = ref<PublicPlayerRankItemVo[]>([])
const summary = ref<PublicSeasonSummaryVo | null>(null)
const announcements = ref<PublicAnnouncementItemResp[]>([])

const matchState = ref<SectionState>('loading')
const bannerState = ref<SectionState>('loading')
const teamState = ref<SectionState>('loading')
const rankState = ref<SectionState>('loading')
const playerState = ref<SectionState>('loading')
const summaryState = ref<SectionState>('loading')
const contentState = ref<SectionState>('loading')

const matchError = ref('')
const bannerError = ref('')
const teamError = ref('')
const rankError = ref('')
const playerError = ref('')
const summaryError = ref('')
const contentError = ref('')

const brokenLogos = ref<Record<string, boolean>>({})
const activeCompetitionFilter = ref<HomeCompetitionFilter>('ALL')

const featuredAnnouncement = computed(() => announcements.value[0] ?? null)
const sideAnnouncements = computed(() => announcements.value.slice(1))
const currentEventName = computed(
  () => summary.value?.tournamentName?.trim() || summary.value?.seasonName?.trim() || '',
)
const currentSeasonId = computed(() => {
  const summarySeasonId = summary.value?.seasonId?.trim()
  if (summarySeasonId) return summarySeasonId
  return matches.value.find((match) => match.seasonId?.trim())?.seasonId?.trim() || ''
})
const hasCurrentEvent = computed(() => Boolean(
  currentSeasonId.value
  || summary.value?.tournamentId?.trim()
  || currentEventName.value,
))

function matchCompetitionType(match: PublicMatchItemVo): Exclude<HomeCompetitionFilter, 'ALL'> | null {
  const record = match as PublicMatchItemVo & Record<string, unknown>
  const rawValue = ['seriesType', 'eventType', 'competitionType']
    .map((key) => record[key])
    .find((value): value is string => typeof value === 'string' && Boolean(value.trim()))
  if (!rawValue) return null

  const normalized = rawValue.trim().toUpperCase()
  if (normalized === 'LEAGUE' || normalized === 'ARENA' || normalized === 'CUP' || normalized === 'OPEN') {
    return normalized
  }
  return null
}

const hasCompetitionMetadata = computed(() => matches.value.some((match) => matchCompetitionType(match) !== null))
const competitionFilterUnavailable = computed(() => !hasCompetitionMetadata.value)
const filteredMatches = computed(() => {
  if (activeCompetitionFilter.value === 'ALL') return matches.value
  return matches.value.filter((match) => matchCompetitionType(match) === activeCompetitionFilter.value)
})
const visibleMatches = computed(() => filteredMatches.value.slice(0, PREVIEW_MATCH_COUNT))

function mapBanner(item: PublicHomeBannerResp): HomeBannerItem | null {
  const image = item.imageUrl?.trim()
  if (!image) return null
  return {
    id: item.bannerId,
    image,
  }
}

function matchCardTone(index: number): 'side' | 'featured' {
  if (visibleMatches.value.length === 1) return 'featured'
  if (visibleMatches.value.length === 2) return index === 0 ? 'featured' : 'side'
  return index === 1 ? 'featured' : 'side'
}

function matchCardNo(index: number): string {
  return String(index + 1).padStart(2, '0')
}

function teamMark(index: number): string {
  return String(index + 1).padStart(2, '0')
}

function winRateWidth(value: unknown): string {
  const num = Number(value)
  if (!Number.isFinite(num) || num <= 0) return '8%'
  if (num > 1) return `${Math.min(num, 100)}%`
  return `${Math.min(num * 100, 100)}%`
}

function markLogoBroken(key: string) {
  brokenLogos.value[key] = true
}

function showLogo(url: string | null | undefined, key: string): boolean {
  return Boolean(url?.trim()) && !brokenLogos.value[key]
}

function handleMoreMatches() {
  const seasonId = currentSeasonId.value
  if (!seasonId) return
  router.push({ path: `/events/${seasonId}`, query: { tab: 'schedule' } })
}

function handleMoreTeams() {
  router.push('/data')
}

function handleWatchMatch(match: PublicMatchItemVo) {
  if (!match.matchId) return
  router.push(`/events/match/${match.matchId}`)
}

function selectCompetitionFilter(filter: HomeCompetitionFilter) {
  if (filter !== 'ALL' && !hasCompetitionMetadata.value) return
  activeCompetitionFilter.value = filter
}

function matchStatus(match: PublicMatchItemVo) {
  const rawStatus = (match as unknown as Record<string, unknown>).matchStatus
  if (typeof rawStatus === 'string') {
    switch (rawStatus.trim().toUpperCase()) {
      case 'PLAYING':
      case 'LIVE':
        return 'live' as const
      case 'FINISHED':
      case 'FORFEITED':
        return 'finished' as const
      case 'CANCELLED':
        return 'cancelled' as const
      case 'POSTPONED':
        return 'postponed' as const
      default:
        return 'scheduled' as const
    }
  }
  if (match.actualEndTime) return 'finished' as const
  return mapMatchStatus(typeof rawStatus === 'number' ? rawStatus : null)
}

function matchStatusLabel(match: PublicMatchItemVo): string {
  switch (matchStatus(match)) {
    case 'live':
      return '进行中'
    case 'finished':
      return '已结束'
    case 'postponed':
      return '已延期'
    case 'cancelled':
      return '已取消'
    default:
      return '未开始'
  }
}

function matchScore(match: PublicMatchItemVo): string {
  if (match.homeScore === null || match.homeScore === undefined) return 'VS'
  if (match.awayScore === null || match.awayScore === undefined) return 'VS'
  return `${match.homeScore} : ${match.awayScore}`
}

function matchActionLabel(match: PublicMatchItemVo): string {
  switch (matchStatus(match)) {
    case 'live':
      return '进入比赛'
    case 'finished':
      return '查看比赛详情'
    default:
      return '未开始'
  }
}

function matchActionDisabled(match: PublicMatchItemVo): boolean {
  return matchStatus(match) !== 'live' && matchStatus(match) !== 'finished'
}

function handleCurrentEvent() {
  if (!hasCurrentEvent.value) return
  const seasonId = currentSeasonId.value
  if (seasonId) {
    router.push(`/events/${seasonId}`)
  }
}

function handleTeamClick(teamId: string) {
  router.push(`/tournament/team/${teamId}/overview`)
}

function matchRound(match: PublicMatchItemVo): string {
  return displayText(match.stageName || match.groupName, '')
}

async function loadBanners() {
  bannerState.value = 'loading'
  bannerError.value = ''
  try {
    const res = await getPublicHomeBanners()
    banners.value = (res.data ?? [])
      .map((item) => mapBanner(item))
      .filter((item): item is HomeBannerItem => Boolean(item))
    bannerState.value = banners.value.length ? 'success' : 'empty'
  } catch (err) {
    banners.value = []
    bannerState.value = 'error'
    bannerError.value = err instanceof Error ? err.message : '加载首页 Banner 失败'
  }
}

async function loadMatches() {
  matchState.value = 'loading'
  matchError.value = ''
  try {
    const res = await getPublicHomeRecentMatches(MATCH_QUERY_LIMIT)
    matches.value = res.data ?? []
    matchState.value = matches.value.length ? 'success' : 'empty'
  } catch (err) {
    matches.value = []
    matchState.value = 'error'
    matchError.value = err instanceof Error ? err.message : '加载近期赛事失败'
  }
}

async function loadTeams() {
  teamState.value = 'loading'
  teamError.value = ''
  try {
    const res = await getPublicHomeTeamRanking(PREVIEW_TEAM_COUNT)
    teams.value = res.data ?? []
    teamState.value = teams.value.length ? 'success' : 'empty'
  } catch (err) {
    teams.value = []
    teamState.value = 'error'
    teamError.value = err instanceof Error ? err.message : '加载参赛战队失败'
  }
}

async function loadTeamRanks() {
  rankState.value = 'loading'
  rankError.value = ''
  try {
    const res = await getPublicHomeTeamRanking(5)
    teamRanks.value = res.data ?? []
    rankState.value = teamRanks.value.length ? 'success' : 'empty'
  } catch (err) {
    teamRanks.value = []
    rankState.value = 'error'
    rankError.value = err instanceof Error ? err.message : '加载球队排行失败'
  }
}

async function loadPlayerRanks() {
  playerState.value = 'loading'
  playerError.value = ''
  try {
    const res = await getPublicHomePlayerRanking(5)
    playerRanks.value = res.data ?? []
    playerState.value = playerRanks.value.length ? 'success' : 'empty'
  } catch (err) {
    playerRanks.value = []
    playerState.value = 'error'
    playerError.value = err instanceof Error ? err.message : '加载球员排行失败'
  }
}

async function loadSummary() {
  summaryState.value = 'loading'
  summaryError.value = ''
  try {
    const res = await getPublicHomeStatSummary()
    summary.value = res.data ?? null
    summaryState.value = summary.value ? 'success' : 'empty'
  } catch (err) {
    summary.value = null
    summaryState.value = 'error'
    summaryError.value = err instanceof Error ? err.message : '加载统计摘要失败'
  }
}

async function loadHomeContent() {
  contentState.value = 'loading'
  contentError.value = ''
  try {
    const res = await getPublicHome()
    announcements.value = res.data?.latestAnnouncements ?? []
    contentState.value = announcements.value.length ? 'success' : 'empty'
  } catch (err) {
    announcements.value = []
    contentState.value = 'error'
    contentError.value = err instanceof Error ? err.message : '加载首页公告失败'
  }
}

function openAnnouncement(id?: string | null) {
  if (!id) return
  router.push(`/announcements/${id}`)
}

onMounted(() => {
  void loadBanners()
  void loadMatches()
  void loadTeams()
  void loadTeamRanks()
  void loadPlayerRanks()
  void loadSummary()
  void loadHomeContent()
})
</script>

<template>
  <div class="home-page">
    <HomeHero
      :banners="banners"
      :loading="bannerState === 'loading'"
      :error-message="bannerState === 'error' ? bannerError : ''"
    />

    <section id="matches" class="site-section home-match-section">
      <div class="site-shell home-match-shell">
        <div class="home-match-paper paper-band paper-band-light">
          <TornEdge position="top" tone="light" variant="sharp" />
          <div class="home-match-inner">
            <div class="section-head section-head-dark home-match-header">
              <h2>近期赛事</h2>
              <button
                type="button"
                class="section-more section-more-dark"
                :disabled="!currentSeasonId"
                @click="handleMoreMatches"
              >
                查看更多
                <span>›</span>
              </button>
            </div>

            <div class="home-competition-filter" aria-label="近期赛事筛选">
              <button
                v-for="filter in homeCompetitionFilters"
                :key="filter.key"
                type="button"
                class="home-competition-filter__item"
                :class="{ 'home-competition-filter__item--active': activeCompetitionFilter === filter.key }"
                :disabled="filter.key !== 'ALL' && !hasCompetitionMetadata"
                @click="selectCompetitionFilter(filter.key)"
              >
                {{ filter.label }}
              </button>
            </div>
            <p v-if="matchState === 'success' && competitionFilterUnavailable" class="home-competition-filter__note">
              当前接口未返回赛事类型，League / Arena / Cup / Open 筛选暂不可用。
            </p>

            <div v-if="matchState === 'loading'" class="home-state">加载中...</div>
            <div v-else-if="matchState === 'error'" class="home-state home-state--error">{{ matchError }}</div>
            <div v-else-if="matchState === 'empty'" class="home-state">暂无近期赛事</div>
            <div v-else-if="!filteredMatches.length" class="home-state">暂无该赛事类型比赛</div>
            <div v-else class="home-match-grid">
              <div
                v-for="(match, index) in visibleMatches"
                :key="match.matchId"
                class="home-match-column"
              >
                <article
                  class="home-match-card"
                  :class="`home-match-card--${matchCardTone(index)}`"
                >
                  <span class="match-card-no" aria-hidden="true">{{ matchCardNo(index) }}</span>
                  <div class="match-card-header">
                    <span>{{ formatDateTime(match.scheduledStartTime).full }}</span>
                    <span v-if="matchRound(match)">{{ matchRound(match) }}</span>
                    <span class="home-match-card__status" :class="`home-match-card__status--${matchStatus(match)}`">
                      {{ matchStatusLabel(match) }}
                    </span>
                  </div>
                  <div class="match-versus">
                    <div class="match-team">
                      <div class="match-logo">
                        <img
                          v-if="showLogo(match.homeTeamLogoUrl, `m-h-${match.matchId}`)"
                          :src="match.homeTeamLogoUrl!"
                          :alt="displayText(match.homeTeamName)"
                          @error="markLogoBroken(`m-h-${match.matchId}`)"
                        />
                        <span v-else class="logo-fallback">{{ initials(match.homeTeamName) }}</span>
                      </div>
                      <span class="match-team-name">{{ displayText(match.homeTeamName) }}</span>
                    </div>
                    <div class="match-vs-wrap">
                      <span class="match-vs-glow" aria-hidden="true" />
                      <span class="match-vs">{{ matchScore(match) }}</span>
                    </div>
                    <div class="match-team">
                      <div class="match-logo">
                        <img
                          v-if="showLogo(match.awayTeamLogoUrl, `m-a-${match.matchId}`)"
                          :src="match.awayTeamLogoUrl!"
                          :alt="displayText(match.awayTeamName)"
                          @error="markLogoBroken(`m-a-${match.matchId}`)"
                        />
                        <span v-else class="logo-fallback">{{ initials(match.awayTeamName) }}</span>
                      </div>
                      <span class="match-team-name">{{ displayText(match.awayTeamName) }}</span>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="watch-btn"
                    :class="{ 'watch-btn--disabled': matchActionDisabled(match) }"
                    :disabled="matchActionDisabled(match)"
                    @click="handleWatchMatch(match)"
                  >
                    <span>{{ matchActionLabel(match) }}</span>
                  </button>
                </article>
              </div>
            </div>
          </div>
          <TornEdge position="bottom" tone="light" variant="sharp" />
        </div>
      </div>
    </section>

    <section class="site-section home-current-event-section">
      <div class="site-shell">
        <div v-if="summaryState === 'loading'" class="home-current-event-card home-current-event-card--state">
          <div class="home-state">加载中...</div>
        </div>
        <div v-else-if="summaryState === 'error'" class="home-current-event-card home-current-event-card--state">
          <div class="home-state home-state--error">{{ summaryError }}</div>
        </div>
        <div v-else-if="!hasCurrentEvent" class="home-current-event-card home-current-event-card--state">
          <div class="home-state">暂无当前赛事</div>
        </div>
        <div v-else class="home-current-event-card">
          <div class="home-current-event-card__intro">
            <span class="home-kicker">CURRENT EVENT</span>
            <h2>当前赛事</h2>
            <p>聚焦正在发生的 CNPA 赛事，快速进入赛事主页。</p>
          </div>
          <div class="home-current-event-card__main">
            <div class="home-current-event-card__heading">
              <div>
                <span class="home-current-event-card__status">当前赛事</span>
                <h3 v-if="currentEventName">{{ currentEventName }}</h3>
                <p v-if="summary?.seasonName && summary.seasonName !== currentEventName">
                  {{ summary.seasonName }}
                </p>
              </div>
              <button
                v-if="currentSeasonId"
                type="button"
                class="home-outline-button"
                @click="handleCurrentEvent"
              >
                进入赛事
                <span aria-hidden="true">↗</span>
              </button>
            </div>
            <div class="home-current-event-card__stats">
              <div v-if="summary?.teamCount !== null && summary?.teamCount !== undefined">
                <strong>{{ summary.teamCount }}</strong>
                <span>参赛队伍</span>
              </div>
              <div v-if="summary?.matchCount !== null && summary?.matchCount !== undefined">
                <strong>{{ summary.matchCount }}</strong>
                <span>比赛场次</span>
              </div>
              <div v-if="summary?.finishedMatchCount !== null && summary?.finishedMatchCount !== undefined">
                <strong>{{ summary.finishedMatchCount }}</strong>
                <span>已完成比赛</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="teams" class="site-section home-team-section">
      <div class="site-shell home-team-shell">
        <div class="section-head section-head-light home-team-header">
          <h2>参赛战队</h2>
          <button type="button" class="section-more section-more-light" @click="handleMoreTeams">
            查看更多战队
            <span>›</span>
          </button>
        </div>

        <div v-if="teamState === 'loading'" class="home-state home-state--light">加载中...</div>
        <div v-else-if="teamState === 'error'" class="home-state home-state--error home-state--light">
          {{ teamError }}
        </div>
        <div v-else-if="teamState === 'empty'" class="home-state home-state--light">暂无参赛战队</div>
        <div v-else class="team-showcase-grid">
          <article
            v-for="(team, index) in teams"
            :key="team.teamId"
            class="team-showcase-item"
            @click="handleTeamClick(team.teamId)"
          >
            <span class="team-showcase-mark" aria-hidden="true">{{ teamMark(index) }}</span>
            <div class="team-logo-wrap">
              <img
                v-if="showLogo(team.teamLogoUrl, `t-${team.teamId}`)"
                :src="team.teamLogoUrl!"
                :alt="displayText(team.teamName)"
                @error="markLogoBroken(`t-${team.teamId}`)"
              />
              <span v-else class="logo-fallback">{{ initials(team.teamName) }}</span>
            </div>
            <span class="team-showcase-name">{{ displayText(team.teamName) }}</span>
          </article>
        </div>
      </div>
    </section>

    <LeagueSystem
      :summary="summary"
      :state="summaryState"
      :error-message="summaryError"
      @retry="loadSummary"
    />

    <section class="site-section home-focus-section">
      <div class="site-shell">
        <div class="home-focus-header">
          <div>
            <span class="home-kicker">FOCUS TEAMS</span>
            <h2 class="home-section-title">本周焦点战队</h2>
          </div>
          <p>当前接口未提供焦点战队运营配置。</p>
        </div>
        <div class="home-focus-filter" aria-label="焦点战队赛事筛选">
          <button
            v-for="filter in homeFocusFilters"
            :key="filter.key"
            type="button"
            class="home-focus-filter__item"
            disabled
            :aria-label="`${filter.label} 焦点战队筛选暂不可用`"
          >
            {{ filter.label }}
          </button>
        </div>
        <div class="home-focus-empty home-state">暂无焦点战队</div>
      </div>
    </section>

    <section id="ranking" class="site-section site-section--dark ranking-section">
      <div class="site-shell">
        <div class="site-shell-inner">
          <div class="section-head">
            <h2>数据榜单</h2>
          </div>
          <div class="ranking-stage">
            <article class="ranking-board ranking-board--teams">
              <header class="ranking-board-head">
                <h3>球队排行</h3>
                <span>WIN RATE</span>
              </header>
              <div v-if="rankState === 'loading'" class="home-state home-state--light">加载中...</div>
              <div v-else-if="rankState === 'error'" class="home-state home-state--error home-state--light">
                {{ rankError }}
              </div>
              <div v-else-if="rankState === 'empty'" class="home-state home-state--light">暂无球队排行</div>
              <ul v-else class="team-rank-list">
                <li
                  v-for="row in teamRanks"
                  :key="row.teamId"
                  class="team-rank-item"
                  :class="{ 'team-rank-item--lead': row.rankNo === 1 }"
                >
                  <span class="rank-badge">{{ displayText(row.rankNo ?? 0, '0') }}</span>
                  <router-link :to="`/tournament/team/${row.teamId}/overview`" class="team-rank-main">
                    <span class="tiny-logo">
                      <img
                        v-if="showLogo(row.teamLogoUrl, `r-${row.teamId}`)"
                        :src="row.teamLogoUrl!"
                        :alt="displayText(row.teamName)"
                        @error="markLogoBroken(`r-${row.teamId}`)"
                      />
                      <span v-else class="tiny-fallback">{{ initials(row.teamName) }}</span>
                    </span>
                    <span class="team-rank-name">{{ displayText(row.teamName) }}</span>
                  </router-link>
                  <div class="team-rank-meter" aria-hidden="true">
                    <i :style="{ width: winRateWidth(row.statValue) }" />
                  </div>
                  <strong class="team-rank-value">{{ formatPercent(row.statValue) }}</strong>
                </li>
              </ul>
            </article>

            <article class="ranking-board ranking-board--players">
              <header class="ranking-board-head">
                <h3>球员得分榜</h3>
                <span>TOP 5 PPG</span>
              </header>
              <div v-if="playerState === 'loading'" class="home-state home-state--light">加载中...</div>
              <div v-else-if="playerState === 'error'" class="home-state home-state--error home-state--light">
                {{ playerError }}
              </div>
              <div v-else-if="playerState === 'empty'" class="home-state home-state--light">暂无球员排行</div>
              <ul v-else class="player-rank-list">
                <li
                  v-for="player in playerRanks"
                  :key="player.playerId"
                  class="player-rank-item"
                  :class="{ 'player-rank-item--podium': (player.rankNo ?? 99) <= 3 }"
                >
                  <span class="rank-badge">{{ displayText(player.rankNo ?? 0, '0') }}</span>
                  <div class="player-rank-main">
                    <PlayerAvatar
                      :name="displayText(player.playerName)"
                      :avatar-url="player.avatarUrl || ''"
                      :player-id="player.playerId"
                      size="sm"
                      tone="dark"
                    />
                    <div class="player-rank-copy">
                      <PlayerLink
                        :player-id="player.playerId"
                        :player-name="displayText(player.playerName)"
                        tab="data"
                      />
                      <small>{{ displayText(player.teamName) }}</small>
                    </div>
                  </div>
                  <strong class="player-rank-value">{{ formatStatValue(player.statValue, 'PPG') }}</strong>
                </li>
              </ul>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section class="site-section home-announce-section">
      <div class="site-shell">
        <div class="paper-band paper-band-light">
          <TornEdge position="top" tone="light" variant="wave" />
          <div class="paper-band-inner">
            <div class="section-head section-head-dark">
              <h2>最新公告</h2>
              <button type="button" class="section-more section-more-dark" @click="router.push('/announcements')">
                查看全部公告
                <span>›</span>
              </button>
            </div>
            <div v-if="contentState === 'loading'" class="home-state">加载中...</div>
            <div v-else-if="contentState === 'error'" class="home-state home-state--error">{{ contentError }}</div>
            <div v-else-if="!announcements.length" class="home-state">暂无公告</div>
            <div v-else class="announce-board">
              <button
                v-if="featuredAnnouncement"
                type="button"
                class="announce-feature"
                @click="openAnnouncement(featuredAnnouncement.announcementId)"
              >
                <div class="announce-feature-media">
                  <img
                    v-if="showLogo(featuredAnnouncement.coverUrl, `a-${featuredAnnouncement.announcementId}`)"
                    :src="featuredAnnouncement.coverUrl!"
                    :alt="displayText(featuredAnnouncement.title)"
                    @error="markLogoBroken(`a-${featuredAnnouncement.announcementId}`)"
                  />
                  <div v-else class="announce-feature-fallback">
                    <span>{{ displayText(featuredAnnouncement.categoryName, '公告') }}</span>
                  </div>
                </div>
                <div class="announce-feature-body">
                  <span class="announce-feature-tag">{{ displayText(featuredAnnouncement.categoryName, '公告') }}</span>
                  <strong>{{ displayText(featuredAnnouncement.title) }}</strong>
                  <p v-if="featuredAnnouncement.summary">{{ displayText(featuredAnnouncement.summary) }}</p>
                  <time>{{ formatAnnouncementDate(featuredAnnouncement.publishTime) }}</time>
                </div>
              </button>

              <div class="announce-feed">
                <button
                  v-for="item in sideAnnouncements"
                  :key="item.announcementId"
                  type="button"
                  class="announce-feed-item"
                  @click="openAnnouncement(item.announcementId)"
                >
                  <span class="announce-feed-item__category">{{ displayText(item.categoryName, '公告') }}</span>
                  <strong>{{ displayText(item.title) }}</strong>
                  <time>{{ formatAnnouncementDate(item.publishTime) }}</time>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="site-section home-community-section">
      <div class="site-shell">
        <div class="home-community-card">
          <div class="home-community-card__intro">
            <span class="home-kicker">CNPA COMMUNITY</span>
            <h2 class="home-section-title">加入篮球电竞社区</h2>
            <p>关注赛事动态、精彩内容与玩家故事。</p>
          </div>
          <div class="home-community-grid">
            <component
              :is="community.available && community.url ? 'a' : 'div'"
              v-for="community in communityList"
              :key="community.id"
              class="home-community-item"
              :class="{ 'home-community-item--disabled': !community.available || !community.url }"
              :aria-disabled="!community.available || !community.url"
              v-bind="community.available && community.url
                ? { href: community.url, target: '_blank', rel: 'noreferrer' }
                : {}"
            >
              <span class="home-community-item__icon">{{ community.name.slice(0, 1) }}</span>
              <div>
                <strong>{{ community.name }}</strong>
                <span>{{ community.description }}</span>
              </div>
              <small v-if="!community.available || !community.url">未配置</small>
            </component>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.home-state {
  padding: 28px 0;
  text-align: center;
  color: rgba(17, 17, 17, 0.55);
  font-size: 14px;
  font-weight: 600;
}

.home-state--light {
  color: rgba(255, 255, 255, 0.72);
}

.home-state--error {
  color: #e10613;
}

.match-logo,
.team-logo-wrap,
.tiny-logo {
  position: relative;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.logo-fallback,
.tiny-fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  background: #151515;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.tiny-logo {
  .tiny-fallback {
    position: absolute;
    inset: 0;
    font-size: 9px;
  }
}

.team-name-cell a,
.team-rank-main,
.player-rank-copy :deep(a) {
  color: inherit;
  text-decoration: none;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}
</style>
