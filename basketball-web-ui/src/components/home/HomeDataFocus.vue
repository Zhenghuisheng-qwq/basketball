<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import TornEdge from '@/components/home/TornEdge.vue'
import { getPublicHomeDataFocus } from '@/api/publicHome'
import type { PublicHomeFocusTeamVo, PublicHomePlayerLeaderVo } from '@/api/types/public'
import { displayText, formatDecimal, initials } from '@/utils/publicDisplay'
import iconPts from '@/assets/icon/home/basketball1.png'
import iconAst from '@/assets/icon/home/basketball2.png'
import iconReb from '@/assets/icon/home/basketball3.png'

type SeriesFilter = 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'
type SectionState = 'loading' | 'success' | 'empty' | 'error'

type LeaderCard = {
  key: string
  title: string
  statLabel: string
  mark: string
  value: string
  playerName: string
  avatarUrl: string
  teamName: string
  teamLogoUrl: string
}

const markIcons: Record<string, string> = {
  PTS: iconPts,
  AST: iconAst,
  REB: iconReb,
}

const props = defineProps<{
  defaultSeriesCode?: SeriesFilter
}>()

const seriesFilters: Array<{ key: SeriesFilter; label: string }> = [
  { key: 'LEAGUE', label: 'League' },
  { key: 'ARENA', label: 'Arena' },
  { key: 'CUP', label: 'Cup' },
  { key: 'OPEN', label: 'Open' },
]

const seriesFilter = ref<SeriesFilter>('LEAGUE')
const userPickedSeries = ref(false)
const filterOpen = ref(false)
const state = ref<SectionState>('loading')
const errorMessage = ref('')
const brokenLogos = ref<Record<string, boolean>>({})
const leaders = ref<LeaderCard[]>([])
const featured = ref<PublicHomeFocusTeamVo | null>(null)

const filterLabel = computed(() =>
  seriesFilters.find((item) => item.key === seriesFilter.value)?.label || '赛事筛选',
)

const resolvedSeriesCode = computed(() => seriesFilter.value)

const featuredRecord = computed(() => {
  if (!featured.value) return '--'
  const wins = featured.value.weekWinCount
  const losses = featured.value.weekLoseCount
  if (wins == null || losses == null) return '--'
  return `${wins}胜${losses}负`
})

const featuredNet = computed(() => {
  const diff = featured.value?.weekAvgPointDiff
  if (diff == null || diff === '') return '--'
  const numeric = Number(diff)
  if (!Number.isFinite(numeric)) return '--'
  return `${numeric > 0 ? '+' : ''}${formatDecimal(numeric, 1)}`
})

const featuredForm = computed(() =>
  (featured.value?.recentForm ?? []).filter((item) => item === 'W' || item === 'L'),
)

function toLeaderCard(
  title: string,
  statLabel: string,
  mark: string,
  row?: PublicHomePlayerLeaderVo | null,
): LeaderCard {
  const value = row?.value
  const numeric = value == null || value === '' ? Number.NaN : Number(value)
  return {
    key: title,
    title,
    statLabel,
    mark,
    value: Number.isFinite(numeric) ? formatDecimal(numeric, 1) : '--',
    playerName: displayText(row?.playerName, '暂无数据'),
    avatarUrl: row?.avatarUrl?.trim() || '',
    teamName: row?.teamName?.trim() || '',
    teamLogoUrl: row?.teamLogoUrl?.trim() || '',
  }
}

function showLogo(url: string, key: string) {
  return Boolean(url) && !brokenLogos.value[key]
}

async function load() {
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getPublicHomeDataFocus(resolvedSeriesCode.value)
    const data = res.data
    leaders.value = [
      toLeaderCard('得分王', '场均得分', 'PTS', data?.pointsLeader),
      toLeaderCard('助攻王', '场均助攻', 'AST', data?.assistsLeader),
      toLeaderCard('篮板王', '场均篮板', 'REB', data?.reboundsLeader),
    ]
    featured.value = data?.focusTeam ?? null
    const hasLeader = Boolean(data?.pointsLeader || data?.assistsLeader || data?.reboundsLeader)
    state.value = hasLeader || featured.value ? 'success' : 'empty'
  } catch (err) {
    leaders.value = []
    featured.value = null
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载数据焦点失败'
  }
}

function selectSeries(key: SeriesFilter) {
  userPickedSeries.value = true
  seriesFilter.value = key
  filterOpen.value = false
}

watch(
  () => props.defaultSeriesCode,
  (code) => {
    if (userPickedSeries.value) return
    if (code === 'LEAGUE' || code === 'CUP' || code === 'ARENA' || code === 'OPEN') {
      seriesFilter.value = code
    }
  },
  { immediate: true },
)

watch(
  resolvedSeriesCode,
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
                  :key="item.key"
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
                    v-if="showLogo(card.avatarUrl || card.teamLogoUrl, card.key)"
                    :src="card.avatarUrl || card.teamLogoUrl"
                    :alt="card.playerName"
                    @error="brokenLogos[card.key] = true"
                  />
                  <i v-else>{{ initials(card.playerName) }}</i>
                </span>
                <div>
                  <b>{{ card.playerName }}</b>
                  <small>{{ card.teamName || card.statLabel }}</small>
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
                  <div v-if="featuredForm.length">
                    <span>最近战绩</span>
                    <div class="data-featured-card__form">
                      <i
                        v-for="(item, index) in featuredForm"
                        :key="`${item}-${index}`"
                        :class="item === 'W' ? 'is-win' : 'is-lose'"
                      >{{ item }}</i>
                    </div>
                  </div>
                </div>
              </template>
              <p v-else class="home-state home-state--inline">本周暂无焦点战队</p>
            </article>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
