<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TornEdge from '@/components/home/TornEdge.vue'
import PlayerLink from '@/components/player/PlayerLink.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import homeTop from '@/assets/icon/home_top.png'
import LeagueSystem from '@/components/home/LeagueSystem.vue'
import {
  getPublicHome,
  getPublicHomePlayerRanking,
  getPublicHomeRecentMatches,
  getPublicHomeStatSummary,
  getPublicHomeTeamRanking,
} from '@/api/publicHome'
import type {
  PublicAnnouncementItemResp,
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
} from '@/utils/publicDisplay'

type SectionState = 'loading' | 'success' | 'empty' | 'error'

const router = useRouter()

const PREVIEW_MATCH_COUNT = 3
const PREVIEW_TEAM_COUNT = 6

const matches = ref<PublicMatchItemVo[]>([])
const teams = ref<PublicTeamRankItemVo[]>([])
const teamRanks = ref<PublicTeamRankItemVo[]>([])
const playerRanks = ref<PublicPlayerRankItemVo[]>([])
const summary = ref<PublicSeasonSummaryVo | null>(null)
const announcements = ref<PublicAnnouncementItemResp[]>([])

const matchState = ref<SectionState>('loading')
const teamState = ref<SectionState>('loading')
const rankState = ref<SectionState>('loading')
const playerState = ref<SectionState>('loading')
const summaryState = ref<SectionState>('loading')
const contentState = ref<SectionState>('loading')

const matchError = ref('')
const teamError = ref('')
const rankError = ref('')
const playerError = ref('')
const summaryError = ref('')
const contentError = ref('')

const brokenLogos = ref<Record<string, boolean>>({})

const featuredAnnouncement = computed(() => announcements.value[0] ?? null)
const sideAnnouncements = computed(() => announcements.value.slice(1))

function matchCardTone(index: number): 'side' | 'featured' {
  if (matches.value.length === 1) return 'featured'
  if (matches.value.length === 2) return index === 0 ? 'featured' : 'side'
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
  router.push('/tournament')
}

function handleMoreTeams() {
  router.push('/data')
}

function handleWatchMatch(matchId: string) {
  router.push({ path: '/tournament', query: { tab: 'schedule', matchId } })
}

function handleTeamClick(teamId: string) {
  router.push(`/tournament/team/${teamId}/overview`)
}

function matchRound(match: PublicMatchItemVo): string {
  return displayText(match.stageName || match.groupName, '公开赛事')
}

async function loadMatches() {
  matchState.value = 'loading'
  matchError.value = ''
  try {
    const res = await getPublicHomeRecentMatches(PREVIEW_MATCH_COUNT)
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
    <section class="site-section home-hero-section">
      <div class="site-shell">
        <img class="hero-image" :src="homeTop" alt="CNPA 篮球赛事平台" />
      </div>
    </section>

    <section id="matches" class="site-section home-match-section">
      <div class="site-shell home-match-shell">
        <div class="home-match-paper paper-band paper-band-light">
          <TornEdge position="top" tone="light" variant="sharp" />
          <div class="home-match-inner">
            <div class="section-head section-head-dark home-match-header">
              <h2>近期赛事</h2>
              <button type="button" class="section-more section-more-dark" @click="handleMoreMatches">
                查看更多
                <span>›</span>
              </button>
            </div>

            <div v-if="matchState === 'loading'" class="home-state">加载中...</div>
            <div v-else-if="matchState === 'error'" class="home-state home-state--error">{{ matchError }}</div>
            <div v-else-if="matchState === 'empty'" class="home-state">暂无近期赛事</div>
            <div v-else class="home-match-grid">
              <div
                v-for="(match, index) in matches"
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
                    <span>{{ matchRound(match) }}</span>
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
                      <span class="match-vs">VS</span>
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
                  <button type="button" class="watch-btn" @click="handleWatchMatch(match.matchId)">
                    <span>查看赛程</span>
                  </button>
                </article>
              </div>
            </div>
          </div>
          <TornEdge position="bottom" tone="light" variant="sharp" />
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
