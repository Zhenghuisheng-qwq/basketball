<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import TornEdge from '@/components/home/TornEdge.vue'
import {
  getV4PublicSeasonDetail,
  getV4PublicSeasonList,
  getV4PublicStanding,
  getV4PublicTeamStat,
} from '@/api/v4Public'
import type { V4PublicStandingItem, V4PublicTeamStatItem } from '@/api/types/v4Public'
import { displayText, formatDecimal, initials } from '@/utils/publicDisplay'
import { calculateAverage } from '@/utils/statAverage'
import iconPts from '@/assets/icon/home/basketball1.png'
import iconAst from '@/assets/icon/home/basketball2.png'
import iconReb from '@/assets/icon/home/basketball3.png'

type SeriesFilter = '' | 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'
type SectionState = 'loading' | 'success' | 'empty' | 'error'

type LeaderCard = {
  key: string
  title: string
  statLabel: string
  mark: string
  value: string
  teamId: string
  teamName: string
  teamLogoUrl: string
}

const props = defineProps<{
  seasonId?: string
}>()

const markIcons: Record<string, string> = {
  PTS: iconPts,
  AST: iconAst,
  REB: iconReb,
}

const seriesFilters: Array<{ key: SeriesFilter; label: string }> = [
  { key: '', label: '全部赛事' },
  { key: 'LEAGUE', label: 'League' },
  { key: 'ARENA', label: 'Arena' },
  { key: 'CUP', label: 'Cup' },
  { key: 'OPEN', label: 'Open' },
]

const seriesFilter = ref<SeriesFilter>('')
const filterOpen = ref(false)
const state = ref<SectionState>('loading')
const errorMessage = ref('')
const brokenLogos = ref<Record<string, boolean>>({})
const leaders = ref<LeaderCard[]>([])
const featured = ref<V4PublicStandingItem | null>(null)
const featuredStat = ref<V4PublicTeamStatItem | null>(null)

const filterLabel = computed(() =>
  seriesFilters.find((item) => item.key === seriesFilter.value)?.label || '赛事筛选',
)

const featuredRecord = computed(() => {
  if (!featured.value) return '--'
  const wins = featured.value.winCount ?? 0
  const losses = featured.value.loseCount ?? 0
  return `${wins}胜${losses}负`
})

const featuredNet = computed(() => {
  const stat = featuredStat.value
  if (stat?.avgPointsFor != null && stat?.avgPointsAgainst != null) {
    const diff = Number(stat.avgPointsFor) - Number(stat.avgPointsAgainst)
    if (!Number.isFinite(diff)) return '--'
    return `${diff > 0 ? '+' : ''}${formatDecimal(diff, 1)}`
  }
  const total = featured.value?.pointDiff
  const games = (featured.value?.winCount ?? 0) + (featured.value?.loseCount ?? 0)
  if (total == null || games <= 0) return '--'
  const avg = Number(total) / games
  return `${avg > 0 ? '+' : ''}${formatDecimal(avg, 1)}`
})

function pickLeader(
  rows: V4PublicTeamStatItem[],
  title: string,
  statLabel: string,
  mark: string,
  valueOf: (row: V4PublicTeamStatItem) => number,
): LeaderCard {
  let best: V4PublicTeamStatItem | null = null
  let bestValue = Number.NEGATIVE_INFINITY
  for (const row of rows) {
    if ((row.matchCount ?? 0) <= 0) continue
    const value = valueOf(row)
    if (value > bestValue) {
      best = row
      bestValue = value
    }
  }
  return {
    key: title,
    title,
    statLabel,
    mark,
    value: best ? formatDecimal(bestValue, 1) : '--',
    teamId: best?.eventTeamId || '',
    teamName: displayText(best?.teamName, '暂无数据'),
    teamLogoUrl: best?.teamLogoUrl?.trim() || '',
  }
}

function showLogo(url: string, key: string) {
  return Boolean(url) && !brokenLogos.value[key]
}

async function resolveDivisionId(): Promise<string> {
  if (seriesFilter.value) {
    const res = await getV4PublicSeasonList({ seriesCode: seriesFilter.value })
    const season = res.data?.[0]
    if (!season?.seasonId) return ''
    if (season.divisions?.[0]?.divisionId) return season.divisions[0].divisionId
    const detail = await getV4PublicSeasonDetail(season.seasonId)
    return detail.data?.defaultDivisionId || detail.data?.divisions?.[0]?.divisionId || ''
  }
  if (props.seasonId) {
    const detail = await getV4PublicSeasonDetail(props.seasonId)
    return detail.data?.defaultDivisionId || detail.data?.divisions?.[0]?.divisionId || ''
  }
  const res = await getV4PublicSeasonList()
  const season = res.data?.[0]
  if (!season?.seasonId) return ''
  if (season.divisions?.[0]?.divisionId) return season.divisions[0].divisionId
  const detail = await getV4PublicSeasonDetail(season.seasonId)
  return detail.data?.defaultDivisionId || detail.data?.divisions?.[0]?.divisionId || ''
}

async function load() {
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const divisionId = await resolveDivisionId()
    if (!divisionId) {
      leaders.value = []
      featured.value = null
      featuredStat.value = null
      state.value = 'empty'
      return
    }
    const [statRes, standingRes] = await Promise.all([
      getV4PublicTeamStat(divisionId),
      getV4PublicStanding(divisionId),
    ])
    const stats = statRes.data ?? []
    const standings = standingRes.data ?? []
    leaders.value = [
      pickLeader(stats, '得分王', '场均得分', 'PTS', (row) => Number(row.avgPointsFor ?? calculateAverage(row.pointsFor, row.matchCount))),
      pickLeader(stats, '助攻王', '场均助攻', 'AST', (row) => calculateAverage(row.assist, row.matchCount)),
      pickLeader(stats, '篮板王', '场均篮板', 'REB', (row) => calculateAverage(row.rebound, row.matchCount)),
    ]
    featured.value = standings[0] ?? null
    featuredStat.value = featured.value
      ? stats.find((item) => item.eventTeamId === featured.value?.eventTeamId) ?? null
      : null
    state.value = leaders.value.some((item) => item.teamId) || featured.value ? 'success' : 'empty'
  } catch (err) {
    leaders.value = []
    featured.value = null
    featuredStat.value = null
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载数据焦点失败'
  }
}

function selectSeries(key: SeriesFilter) {
  seriesFilter.value = key
  filterOpen.value = false
}

watch(
  () => [props.seasonId, seriesFilter.value],
  () => {
    void load()
  },
  { immediate: true },
)
</script>

<template>
  <section class="site-section home-data-focus-section">
    <div class="site-shell">
      <div class="paper-band paper-band-light">
        <TornEdge position="top" tone="light" variant="soft" />
        <div class="paper-band-inner home-data-focus-inner">
          <div class="data-focus-header">
            <div class="combined-column-heading combined-column-heading--news">
              <h2>数据焦点</h2>
              <span class="home-kicker">DATA FOCUS</span>
            </div>
            <div class="data-focus-filter">
              <button
                type="button"
                class="data-focus-filter__trigger"
                :aria-expanded="filterOpen"
                @click="filterOpen = !filterOpen"
              >
                赛事筛选
                <small>{{ filterLabel }}</small>
              </button>
              <div v-if="filterOpen" class="data-focus-filter__menu" role="listbox">
                <button
                  v-for="item in seriesFilters"
                  :key="item.key || 'ALL'"
                  type="button"
                  role="option"
                  :aria-selected="seriesFilter === item.key"
                  @click="selectSeries(item.key)"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="state === 'loading'" class="home-state">加载中...</div>
          <div v-else-if="state === 'error'" class="home-state home-state--error">{{ errorMessage }}</div>
          <div v-else-if="state === 'empty'" class="home-state">暂无数据焦点</div>
          <div v-else class="data-focus-grid">
            <article
              v-for="card in leaders"
              :key="card.key"
              class="data-leader-card"
              :class="`data-leader-card--${card.mark.toLowerCase()}`"
            >
              <h3>{{ card.title }}</h3>
              <strong>{{ card.value }}</strong>
              <img
                class="data-leader-card__mark"
                :src="markIcons[card.mark]"
                alt=""
                aria-hidden="true"
                draggable="false"
              />
              <div class="data-leader-card__team">
                <span class="data-leader-card__logo">
                  <img
                    v-if="showLogo(card.teamLogoUrl, card.key)"
                    :src="card.teamLogoUrl"
                    :alt="card.teamName"
                    @error="brokenLogos[card.key] = true"
                  />
                  <i v-else>{{ initials(card.teamName) }}</i>
                </span>
                <div>
                  <b>{{ card.teamName }}</b>
                  <small>{{ card.statLabel }}</small>
                </div>
              </div>
            </article>

            <article class="data-featured-card">
              <h3>本周焦点战队</h3>
              <template v-if="featured">
                <div class="data-featured-card__main">
                  <span class="data-featured-card__logo">
                    <img
                      v-if="showLogo(featured.teamLogoUrl || '', 'featured')"
                      :src="featured.teamLogoUrl!"
                      :alt="displayText(featured.teamName)"
                      @error="brokenLogos.featured = true"
                    />
                    <i v-else>{{ initials(featured.teamName) }}</i>
                  </span>
                  <strong>{{ displayText(featured.teamName) }}</strong>
                </div>
                <div class="data-featured-card__stats">
                  <div>
                    <span>战绩</span>
                    <b>{{ featuredRecord }}</b>
                  </div>
                  <div>
                    <span>净胜分</span>
                    <b>{{ featuredNet }}</b>
                  </div>
                  <div>
                    <span>最近战绩</span>
                    <div class="data-featured-card__form" aria-hidden="true">
                      <i v-for="n in 5" :key="n">-</i>
                    </div>
                  </div>
                </div>
              </template>
              <p v-else class="home-state home-state--inline">暂无焦点战队</p>
            </article>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
