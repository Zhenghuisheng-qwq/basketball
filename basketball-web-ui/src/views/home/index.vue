<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TornEdge from '@/components/home/TornEdge.vue'
import HomeCommunity from '@/components/home/HomeCommunity.vue'
import HomeDataFocus from '@/components/home/HomeDataFocus.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import {
  getPublicHomeBanners,
  getPublicHomeRecentMatches,
  getPublicHomeStatSummary,
} from '@/api/publicHome'
import { getPublicAnnouncementPage } from '@/api/publicAnnouncement'
import type {
  PublicAnnouncementItemResp,
  PublicHomeBannerResp,
  PublicMatchItemVo,
  PublicSeasonSummaryVo,
} from '@/api/types/public'
import { formatAnnouncementDate, goToAnnouncementList } from '@/utils/announcementNavigate'
import {
  displayText,
  formatDateTime,
  initials,
  mapMatchStatus,
} from '@/utils/publicDisplay'
type SectionState = 'loading' | 'success' | 'empty' | 'error'
type HomeCompetitionFilter = 'ALL' | 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'
type HomeNewsTab = 'EVENT' | 'NOTICE'

type HomeBannerItem = {
  id: string
  image: string
}

const router = useRouter()

const PREVIEW_MATCH_COUNT = 3
const MATCH_QUERY_LIMIT = 10
const PREVIEW_NEWS_COUNT = 7

const homeCompetitionFilters: Array<{ key: HomeCompetitionFilter; label: string }> = [
  { key: 'ALL', label: '全部赛事' },
  { key: 'LEAGUE', label: 'League' },
  { key: 'ARENA', label: 'Arena' },
  { key: 'CUP', label: 'Cup' },
  { key: 'OPEN', label: 'Open' },
]

const homeNewsTabs: Array<{ key: HomeNewsTab; label: string }> = [
  { key: 'EVENT', label: '赛事' },
  { key: 'NOTICE', label: '公告' },
]

const banners = ref<HomeBannerItem[]>([])
const matches = ref<PublicMatchItemVo[]>([])
const summary = ref<PublicSeasonSummaryVo | null>(null)
const eventNews = ref<PublicAnnouncementItemResp[]>([])
const noticeNews = ref<PublicAnnouncementItemResp[]>([])

const matchState = ref<SectionState>('loading')
const bannerState = ref<SectionState>('loading')
const contentState = ref<SectionState>('loading')

const matchError = ref('')
const bannerError = ref('')
const contentError = ref('')

const brokenLogos = ref<Record<string, boolean>>({})
const matchCarousel = ref<HTMLElement | null>(null)
const activeCompetitionFilter = ref<HomeCompetitionFilter>('ALL')
const activeNewsTab = ref<HomeNewsTab>('EVENT')

const visibleNews = computed(() => {
  const source = activeNewsTab.value === 'EVENT' ? eventNews.value : noticeNews.value
  return source.slice(0, PREVIEW_NEWS_COUNT)
})
const currentSeasonId = computed(() => {
  const summarySeasonId = summary.value?.seasonId?.trim()
  if (summarySeasonId) return summarySeasonId
  return matches.value.find((match) => match.seasonId?.trim())?.seasonId?.trim() || ''
})

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

function handleWatchMatch(match: PublicMatchItemVo) {
  if (!match.matchId) return
  router.push(`/events/match/${match.matchId}`)
}

function selectCompetitionFilter(filter: HomeCompetitionFilter) {
  activeCompetitionFilter.value = filter
}

function scrollMatchesPrev() {
  if (!matchCarousel.value) return
  matchCarousel.value.scrollBy({
    left: -400,
    behavior: 'smooth'
  })
}

function scrollMatchesNext() {
  if (!matchCarousel.value) return
  matchCarousel.value.scrollBy({
    left: 400,
    behavior: 'smooth'
  })
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

async function loadSummary() {
  try {
    const res = await getPublicHomeStatSummary()
    summary.value = res.data ?? null
  } catch {
    summary.value = null
  }
}

async function loadNewsByCategory(categoryCode: 'EVENT' | 'NOTICE') {
  const res = await getPublicAnnouncementPage({
    currentPage: 1,
    pageSize: PREVIEW_NEWS_COUNT,
    categoryCode,
    sortField: 'publishTime',
    sortOrder: 'desc',
  })
  return res.data?.records ?? []
}

async function loadHomeContent() {
  contentState.value = 'loading'
  contentError.value = ''
  try {
    const [eventItems, noticeItems] = await Promise.all([
      loadNewsByCategory('EVENT'),
      loadNewsByCategory('NOTICE'),
    ])
    eventNews.value = eventItems
    noticeNews.value = noticeItems
    contentState.value = eventItems.length || noticeItems.length ? 'success' : 'empty'
  } catch (err) {
    eventNews.value = []
    noticeNews.value = []
    contentState.value = 'error'
    contentError.value = err instanceof Error ? err.message : '加载首页资讯失败'
  }
}

function openAnnouncement(id?: string | null) {
  if (!id) return
  router.push(`/announcements/${id}`)
}

function selectNewsTab(tab: HomeNewsTab) {
  activeNewsTab.value = tab
}

function formatNewsDate(value?: string | null): string {
  const full = formatAnnouncementDate(value)
  if (full === '--' || full.length < 10) return full
  return full.slice(5)
}

function handleMoreNews() {
  goToAnnouncementList(router, { category: activeNewsTab.value, page: 1 })
}

onMounted(() => {
  void loadBanners()
  void loadMatches()
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
          <div class="home-match-inner">
            <div class="section-head section-head-dark home-match-header">
              <div>
                <span class="home-kicker">UPCOMING MATCHES</span>
                <h2>近期比赛</h2>
              </div>
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
                @click="selectCompetitionFilter(filter.key)"
              >
                {{ filter.label }}
              </button>
            </div>

            <div v-if="matchState === 'loading'" class="home-state">加载中...</div>
            <div v-else-if="matchState === 'error'" class="home-state home-state--error">{{ matchError }}</div>
            <div v-else-if="matchState === 'empty'" class="home-state">暂无近期赛事</div>
            <div v-else-if="!filteredMatches.length" class="home-state">暂无该赛事类型比赛</div>
            <div v-else class="home-match-carousel-wrapper">
              <button
                type="button"
                class="home-match-arrow home-match-arrow--prev"
                aria-label="查看上一组比赛"
                @click="scrollMatchesPrev"
              >
                ‹
              </button>
              <div ref="matchCarousel" class="home-match-grid">
                <div
                  v-for="(match, index) in visibleMatches"
                  :key="match.matchId"
                  class="home-match-column"
                >
                  <article
                    class="home-match-card"
                    :class="`home-match-card--${matchCardTone(index)}`"
                  >
                    <div class="match-card-header">
                      <span class="match-competition-tag">{{ matchRound(match) || 'LEAGUE' }}</span>
                      <span class="match-date">{{ formatDateTime(match.scheduledStartTime).date }}</span>
                    </div>
                    <div class="match-time">{{ formatDateTime(match.scheduledStartTime).time }}</div>
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
                      <img
                        class="match-vs"
                        src="@/assets/icon/home/vs.png"
                        alt="VS"
                        width="56"
                        height="56"
                        draggable="false"
                      />
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
            <button
              type="button"
              class="home-match-arrow home-match-arrow--next"
              aria-label="查看下一组比赛"
              @click="scrollMatchesNext"
            >
              ›
            </button>
          </div>
          </div>
          <TornEdge position="bottom" tone="light" variant="sharp" />
        </div>
      </div>
    </section>

    <!-- 版心链路：site-section → site-shell → paper-band → *-inner（与近期比赛一致，勿改 width） -->
    <section class="site-section home-combined-section">
      <div class="site-shell home-combined-shell">
        <div class="home-combined-paper paper-band paper-band-light">
          <TornEdge position="top" tone="light" variant="wave" />
          <div class="home-combined-inner">
            <div class="combined-layout">
              <div class="combined-left" aria-label="资讯">
                <div class="combined-column-heading combined-column-heading--news">
                  <h2>最新资讯</h2>
                  <span class="home-kicker">LATEST NEWS</span>
                </div>
                <div class="news-tabs" role="tablist" aria-label="资讯分类">
                  <button
                    v-for="tab in homeNewsTabs"
                    :key="tab.key"
                    type="button"
                    role="tab"
                    class="news-tabs__item"
                    :class="{ 'news-tabs__item--active': activeNewsTab === tab.key }"
                    :aria-selected="activeNewsTab === tab.key"
                    @click="selectNewsTab(tab.key)"
                  >
                    {{ tab.label }}
                  </button>
                </div>
                <div v-if="contentState === 'loading'" class="home-state home-state--inline">加载中...</div>
                <div v-else-if="contentState === 'error'" class="home-state home-state--inline home-state--error">
                  {{ contentError }}
                </div>
                <div v-else-if="!visibleNews.length" class="home-state home-state--inline">
                  {{ activeNewsTab === 'EVENT' ? '暂无赛事资讯' : '暂无公告' }}
                </div>
                <ul v-else class="news-list">
                  <li v-for="item in visibleNews" :key="item.announcementId">
                    <button type="button" class="news-list__item" @click="openAnnouncement(item.announcementId)">
                      <span class="news-list__icon" aria-hidden="true" />
                      <strong>{{ displayText(item.title) }}</strong>
                      <time>{{ formatNewsDate(item.publishTime) }}</time>
                    </button>
                  </li>
                </ul>
                <button type="button" class="news-more" @click="handleMoreNews">
                  查看更多新闻
                  <span>→</span>
                </button>
              </div>

              <div class="combined-right">
                <div class="combined-column-heading">
                  <span class="home-kicker">CURRENT EVENTS</span>
                  <h2>当前赛事</h2>
                </div>

                <div class="current-events-grid">
                  <article class="current-event-card">
                    <span class="event-status-tag event-status-tag--active">进行中</span>
                    <div class="event-icon">
                      <img src="@/assets/icon/home/league.png" alt="CNPA LEAGUE" />
                    </div>
                    <div class="event-body">
                      <h3 class="event-title">CNPA LEAGUE</h3>
                      <div class="event-details">
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/stage.png" alt="" class="event-detail-icon" />
                          <span>当前地区：深圳赛区 第2期</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/user.png" alt="" class="event-detail-icon" />
                          <span>赛事状态：已结束</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/platform.png" alt="" class="event-detail-icon" />
                          <span>比赛阶段：CNPA 季后</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/date.png" alt="" class="event-detail-icon" />
                          <span>时间：09-10 ~ 11-30</span>
                        </div>
                      </div>
                    </div>
                  </article>

                  <article class="current-event-card">
                    <span class="event-status-tag event-status-tag--upcoming">报名中</span>
                    <div class="event-icon">
                      <img src="@/assets/icon/home/cup.png" alt="CNPA CUP" />
                    </div>
                    <div class="event-body">
                      <h3 class="event-title">CNPA CUP</h3>
                      <div class="event-details">
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/stage.png" alt="" class="event-detail-icon" />
                          <span>当前地区：挑金杯积</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/user.png" alt="" class="event-detail-icon" />
                          <span>赛事状态：进行中</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/platform.png" alt="" class="event-detail-icon" />
                          <span>比赛阶段：CNPA 季后</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/date.png" alt="" class="event-detail-icon" />
                          <span>时间：06-15 ~ 10-10</span>
                        </div>
                      </div>
                    </div>
                  </article>

                  <article class="current-event-card">
                    <span class="event-status-tag event-status-tag--upcoming">即将开始</span>
                    <div class="event-icon">
                      <img src="@/assets/icon/home/arena.png" alt="CNPA ARENA" />
                    </div>
                    <div class="event-body">
                      <h3 class="event-title">CNPA ARENA</h3>
                      <div class="event-details">
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/stage.png" alt="" class="event-detail-icon" />
                          <span>当前地区：竞技场黄金</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/user.png" alt="" class="event-detail-icon" />
                          <span>赛事状态：进行中</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/platform.png" alt="" class="event-detail-icon" />
                          <span>比赛阶段：CNPA 季后</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/date.png" alt="" class="event-detail-icon" />
                          <span>时间：10-01 ~ 10-31</span>
                        </div>
                      </div>
                    </div>
                  </article>

                  <article class="current-event-card">
                    <span class="event-status-tag">未开始</span>
                    <div class="event-icon">
                      <img src="@/assets/icon/home/open.png" alt="CNPA OPEN" />
                    </div>
                    <div class="event-body">
                      <h3 class="event-title">CNPA OPEN</h3>
                      <div class="event-details">
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/stage.png" alt="" class="event-detail-icon" />
                          <span>当前地区：挑金杯积</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/user.png" alt="" class="event-detail-icon" />
                          <span>赛事状态：未开始</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/platform.png" alt="" class="event-detail-icon" />
                          <span>比赛阶段：CNPA 季后</span>
                        </div>
                        <div class="event-detail-item">
                          <img src="@/assets/icon/home/date.png" alt="" class="event-detail-icon" />
                          <span>时间：09-01 ~ 09-30</span>
                        </div>
                      </div>
                    </div>
                  </article>
                </div>
              </div>
            </div>
          </div>
          <TornEdge position="bottom" tone="light" variant="wave" />
        </div>
      </div>
    </section>

    <HomeDataFocus :season-id="currentSeasonId" />

    <HomeCommunity />

  </div>
</template>

<style scoped lang="scss">
.match-logo {
  position: relative;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.logo-fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  background: #151515;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}
</style>
