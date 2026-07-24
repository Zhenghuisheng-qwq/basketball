<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TornEdge from '@/components/home/TornEdge.vue'
import PlayerLink from '@/components/player/PlayerLink.vue'
import homeTop from '@/assets/icon/home_top.png'
import {
  getPublicHome,
  getPublicHomePlayerRanking,
  getPublicHomeRecentMatches,
  getPublicHomeStatSummary,
  getPublicHomeTeamRanking,
} from '@/api/publicHome'
import type {
  PublicAnnouncementItemResp,
  PublicHomeBannerResp,
  PublicMatchItemVo,
  PublicPartnerResp,
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

const matches = ref<PublicMatchItemVo[]>([])
const teams = ref<PublicTeamRankItemVo[]>([])
const teamRanks = ref<PublicTeamRankItemVo[]>([])
const playerRanks = ref<PublicPlayerRankItemVo[]>([])
const summary = ref<PublicSeasonSummaryVo | null>(null)
const banners = ref<PublicHomeBannerResp[]>([])
const announcements = ref<PublicAnnouncementItemResp[]>([])
const partners = ref<PublicPartnerResp[]>([])

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

const stats = computed(() => {
  const s = summary.value
  return [
    { value: displayText(s?.teamCount ?? 0, '0'), label: '支参赛战队' },
    { value: displayText(s?.playerStatCount ?? 0, '0'), label: '球员数据记录' },
    { value: displayText(s?.finishedMatchCount ?? 0, '0'), label: '已结束比赛' },
    { value: displayText(s?.standingCount ?? 0, '0'), label: '积分榜记录' },
  ]
})

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
    const res = await getPublicHomeRecentMatches(6)
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
    const res = await getPublicHomeTeamRanking(8)
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
    const data = res.data
    banners.value = data?.banners ?? []
    announcements.value = data?.latestAnnouncements ?? []
    partners.value = data?.partners ?? []
    contentState.value =
      banners.value.length || announcements.value.length || partners.value.length
        ? 'success'
        : 'empty'
  } catch (err) {
    banners.value = []
    announcements.value = []
    partners.value = []
    contentState.value = 'error'
    contentError.value = err instanceof Error ? err.message : '加载首页运营内容失败'
  }
}

function handleBannerClick(banner: PublicHomeBannerResp) {
  const type = (banner.targetType || '').toUpperCase()
  const value = (banner.targetValue || '').trim()
  if (!value) return
  if (type === 'URL' && /^https?:\/\//i.test(value)) {
    window.open(value, '_blank', 'noopener,noreferrer')
    return
  }
  if (value.startsWith('/')) {
    router.push(value)
  }
}

function openAnnouncement(id?: string | null) {
  if (!id) return
  router.push(`/announcements/${id}`)
}

function openPartner(partner: PublicPartnerResp) {
  const url = partner.websiteUrl?.trim()
  if (!url) return
  if (/^https?:\/\//i.test(url)) {
    window.open(url, '_blank', 'noopener,noreferrer')
  }
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
    <section class="home-hero-section">
      <div class="site-shell">
        <img class="hero-image" :src="homeTop" alt="CNPA 篮球赛事平台" />
      </div>
    </section>

    <section
      v-if="contentState === 'loading' || banners.length || contentState === 'error'"
      class="home-ops-section"
    >
      <div class="site-shell">
        <div class="site-shell-inner">
          <div class="section-head">
            <h2>运营推荐</h2>
          </div>
          <div v-if="contentState === 'loading'" class="home-state home-state--light">加载中...</div>
          <div v-else-if="contentState === 'error'" class="home-state home-state--error home-state--light">
            {{ contentError }}
          </div>
          <div v-else-if="banners.length" class="home-banner-grid">
            <button
              v-for="banner in banners"
              :key="banner.bannerId"
              type="button"
              class="home-banner-card"
              @click="handleBannerClick(banner)"
            >
              <img
                v-if="showLogo(banner.imageUrl, `b-${banner.bannerId}`)"
                :src="banner.imageUrl!"
                :alt="displayText(banner.title, '运营位')"
                @error="markLogoBroken(`b-${banner.bannerId}`)"
              />
              <div class="home-banner-card__text">
                <strong>{{ displayText(banner.title, '运营推荐') }}</strong>
                <span v-if="banner.subtitle">{{ banner.subtitle }}</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </section>

    <section id="matches" class="match-section">
      <div class="site-shell">
        <div class="paper-band paper-band-light">
          <TornEdge position="top" tone="light" />

          <div class="site-shell-inner paper-band-inner">
            <div class="section-head section-head-dark">
              <h2>近期赛事</h2>
              <button type="button" class="section-more section-more-dark" @click="handleMoreMatches">
                查看全部赛事
                <span>›</span>
              </button>
            </div>

            <div v-if="matchState === 'loading'" class="home-state">加载中...</div>
            <div v-else-if="matchState === 'error'" class="home-state home-state--error">{{ matchError }}</div>
            <div v-else-if="matchState === 'empty'" class="home-state">暂无近期赛事</div>
            <div v-else class="match-grid">
              <article v-for="match in matches" :key="match.matchId" class="match-card">
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
                  <div class="match-vs">VS</div>
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
                  查看赛程
                </button>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="teams" class="team-section">
      <div class="site-shell">
        <div class="paper-band paper-band-dark">
          <TornEdge position="top" tone="dark" />

          <div class="site-shell-inner paper-band-inner">
            <div class="section-head section-head-light">
              <h2>参赛战队</h2>
              <button type="button" class="section-more section-more-light" @click="handleMoreTeams">
                查看数据中心
                <span>›</span>
              </button>
            </div>

            <div v-if="teamState === 'loading'" class="home-state home-state--light">加载中...</div>
            <div v-else-if="teamState === 'error'" class="home-state home-state--error home-state--light">
              {{ teamError }}
            </div>
            <div v-else-if="teamState === 'empty'" class="home-state home-state--light">暂无参赛战队</div>
            <div v-else class="team-grid">
              <article
                v-for="team in teams"
                :key="team.teamId"
                class="team-card"
                @click="handleTeamClick(team.teamId)"
              >
                <div class="team-logo">
                  <img
                    v-if="showLogo(team.teamLogoUrl, `t-${team.teamId}`)"
                    :src="team.teamLogoUrl!"
                    :alt="displayText(team.teamName)"
                    @error="markLogoBroken(`t-${team.teamId}`)"
                  />
                  <span v-else class="logo-fallback">{{ initials(team.teamName) }}</span>
                </div>
                <span class="team-name">{{ displayText(team.teamName) }}</span>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="about-paper-section">
      <div class="site-shell">
        <div class="paper-band paper-band-light">
          <TornEdge position="top" tone="light" />

          <div class="site-shell-inner paper-band-inner about-paper-inner">
            <div class="about-text">
              <h2>关于 CNPA <span>联赛体系</span></h2>
              <p>
                CNPA 中国职业电竞篮球联赛，致力于打造专业、公平、热血的线上篮球竞技舞台。
                通过全年多级赛事体系，推动电竞篮球运动的发展与普及。
              </p>
            </div>
            <div v-if="summaryState === 'loading'" class="home-state">加载中...</div>
            <div v-else-if="summaryState === 'error'" class="home-state home-state--error">{{ summaryError }}</div>
            <div v-else class="about-stats">
              <div v-for="item in stats" :key="item.label" class="stat-item">
                <strong>{{ item.value }}</strong>
                <span>{{ item.label }}</span>
              </div>
            </div>
          </div>

          <TornEdge position="bottom" tone="light" />
        </div>
      </div>
    </section>

    <section class="home-announce-section">
      <div class="site-shell">
        <div class="paper-band paper-band-light">
          <TornEdge position="top" tone="light" />
          <div class="site-shell-inner paper-band-inner">
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
            <div v-else class="home-announce-list">
              <button
                v-for="item in announcements"
                :key="item.announcementId"
                type="button"
                class="home-announce-item"
                @click="openAnnouncement(item.announcementId)"
              >
                <span class="home-announce-item__category">{{ displayText(item.categoryName, '公告') }}</span>
                <strong>{{ displayText(item.title) }}</strong>
                <time>{{ formatAnnouncementDate(item.publishTime) }}</time>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="ranking" class="ranking-section">
      <div class="site-shell">
        <div class="site-shell-inner">
          <div class="section-head">
            <h2>数据榜单</h2>
          </div>
          <div class="ranking-panel">
            <div class="ranking-card">
              <h3>球队排行</h3>
              <div v-if="rankState === 'loading'" class="home-state">加载中...</div>
              <div v-else-if="rankState === 'error'" class="home-state home-state--error">{{ rankError }}</div>
              <div v-else-if="rankState === 'empty'" class="home-state">暂无球队排行</div>
              <table v-else>
                <thead>
                  <tr>
                    <th>排名</th>
                    <th>战队</th>
                    <th>胜率</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in teamRanks" :key="row.teamId">
                    <td class="rank-num">{{ displayText(row.rankNo ?? 0, '0') }}</td>
                    <td>
                      <span class="team-name-cell">
                        <span class="tiny-logo">
                          <img
                            v-if="showLogo(row.teamLogoUrl, `r-${row.teamId}`)"
                            :src="row.teamLogoUrl!"
                            :alt="displayText(row.teamName)"
                            @error="markLogoBroken(`r-${row.teamId}`)"
                          />
                          <span v-else class="tiny-fallback">{{ initials(row.teamName) }}</span>
                        </span>
                        <router-link :to="`/tournament/team/${row.teamId}/overview`">
                          {{ displayText(row.teamName) }}
                        </router-link>
                      </span>
                    </td>
                    <td>{{ formatPercent(row.statValue) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="ranking-card">
              <h3>球员得分榜 TOP5</h3>
              <div v-if="playerState === 'loading'" class="home-state">加载中...</div>
              <div v-else-if="playerState === 'error'" class="home-state home-state--error">{{ playerError }}</div>
              <div v-else-if="playerState === 'empty'" class="home-state">暂无球员排行</div>
              <template v-else>
                <div class="player-row head">
                  <span>排名</span>
                  <span>球员</span>
                  <span>战队</span>
                  <span>场均得分 PPG</span>
                </div>
                <div v-for="player in playerRanks" :key="player.playerId" class="player-row">
                  <span class="rank-num">{{ displayText(player.rankNo ?? 0, '0') }}</span>
                  <span class="player-name">
                    <i
                      class="avatar"
                      :style="
                        showLogo(player.avatarUrl, `p-${player.playerId}`)
                          ? { backgroundImage: `url(${player.avatarUrl})`, backgroundSize: 'cover' }
                          : undefined
                      "
                    />
                    <PlayerLink
                      :player-id="player.playerId"
                      :player-name="displayText(player.playerName)"
                      tab="data"
                    />
                  </span>
                  <span>{{ displayText(player.teamName) }}</span>
                  <span>{{ formatStatValue(player.statValue, 'PPG') }}</span>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="home-partner-section">
      <div class="site-shell">
        <div class="site-shell-inner">
          <div class="section-head">
            <h2>合作伙伴</h2>
          </div>
          <div v-if="contentState === 'loading'" class="home-state home-state--light">加载中...</div>
          <div v-else-if="contentState === 'error'" class="home-state home-state--error home-state--light">
            {{ contentError }}
          </div>
          <div v-else-if="!partners.length" class="home-state home-state--light">暂无合作伙伴</div>
          <div v-else class="home-partner-grid">
            <button
              v-for="partner in partners"
              :key="partner.partnerId"
              type="button"
              class="home-partner-card"
              :disabled="!partner.websiteUrl"
              @click="openPartner(partner)"
            >
              <img
                v-if="showLogo(partner.logoUrl, `p-${partner.partnerId}`)"
                :src="partner.logoUrl!"
                :alt="displayText(partner.partnerName)"
                @error="markLogoBroken(`p-${partner.partnerId}`)"
              />
              <span v-else class="logo-fallback">{{ initials(partner.partnerName) }}</span>
              <strong>{{ displayText(partner.partnerName) }}</strong>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/assets/styles/home-page.scss';

.home-ops-section,
.home-partner-section {
  padding: 42px 0 28px;
  background: #050505;
}

.home-banner-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.home-banner-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 160px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: #111;
  color: #fff;
  text-align: left;
  cursor: pointer;

  img {
    width: 100%;
    height: 120px;
    object-fit: cover;
  }
}

.home-banner-card__text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;

  strong {
    font-size: 15px;
    font-weight: 800;
  }

  span {
    color: rgba(255, 255, 255, 0.65);
    font-size: 13px;
  }
}

.home-announce-list {
  display: flex;
  flex-direction: column;
}

.home-announce-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 96px;
  gap: 16px;
  align-items: center;
  width: 100%;
  padding: 16px 0;
  border: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: transparent;
  color: #171717;
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:hover strong {
    color: var(--cnpa-red, #e10613);
  }
}

.home-announce-item__category {
  color: var(--cnpa-red, #e10613);
  font-size: 12px;
  font-weight: 800;
}

.home-announce-item strong {
  font-size: 15px;
  font-weight: 800;
}

.home-announce-item time {
  color: #777;
  font-size: 13px;
  text-align: right;
}

.home-partner-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.home-partner-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-height: 110px;
  padding: 16px 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: #111;
  color: #fff;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }

  img,
  .logo-fallback {
    width: 56px;
    height: 56px;
    object-fit: contain;
  }

  strong {
    font-size: 12px;
    font-weight: 700;
    text-align: center;
  }
}

@media (max-width: 1023px) {
  .home-banner-grid {
    grid-template-columns: 1fr;
  }

  .home-partner-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .home-announce-item {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .home-announce-item time {
    text-align: left;
  }
}

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
.team-logo,
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

  :deep(.badge) {
    display: none;
  }
}

.team-name-cell a {
  color: inherit;
  text-decoration: none;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}
</style>
