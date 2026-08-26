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
type SectionState = 'loading' | 'success' | 'error'

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

const featuredWins = computed(() => featured.value?.weekWinCount)
const featuredLosses = computed(() => featured.value?.weekLoseCount)
const hasFeaturedRecord = computed(() => featuredWins.value != null && featuredLosses.value != null)

const featuredWinRate = computed(() => {
  const rate = featured.value?.weekWinRate
  if (rate == null || rate === '') return ''
  const numeric = Number(rate)
  if (!Number.isFinite(numeric)) return ''
  const percent = numeric <= 1 ? numeric * 100 : numeric
  return `${Math.round(percent)}%`
})

const featuredNet = computed(() => {
  const diff = featured.value?.weekAvgPointDiff
  if (diff == null || diff === '') return '--'
  const numeric = Number(diff)
  if (!Number.isFinite(numeric)) return '--'
  return `${numeric > 0 ? '+' : ''}${formatDecimal(numeric, 1)}`
})

const featuredForm = computed(() =>
  (featured.value?.recentForm ?? []).filter((item) => item === 'W' || item === 'L').slice(0, 5),
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
    state.value = 'success'
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
          <div class="data-focus-stack">
            <section class="data-focus-box" aria-label="数据焦点">
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
                    <span>赛事</span>
                    <small>{{ filterLabel }} ▼</small>
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
              <div v-else-if="!leaders.length" class="home-state">暂无数据焦点</div>
              <div v-else class="data-leader-grid">
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
              </div>
            </section>

            <section class="data-focus-box data-focus-box--featured" aria-label="本周焦点">
              <div class="data-focus-header">
                <div class="combined-column-heading combined-column-heading--news">
                  <h2>本周焦点</h2>
                  <span class="home-kicker">WEEKLY FOCUS</span>
                </div>
              </div>

              <div v-if="state === 'loading'" class="home-state">加载中...</div>
              <div v-else-if="state === 'error'" class="home-state home-state--error">{{ errorMessage }}</div>
              <article v-else-if="featured" class="weekly-focus-card">
                <div class="weekly-team">
                  <span class="weekly-team__logo">
                    <img
                      v-if="showLogo(featured.teamLogoUrl || '', 'featured')"
                      :src="featured.teamLogoUrl!"
                      :alt="displayText(featured.teamName)"
                      @error="brokenLogos.featured = true"
                    />
                    <i v-else>{{ initials(featured.teamName) }}</i>
                  </span>
                  <div class="weekly-team__copy">
                    <strong :title="displayText(featured.teamName)">{{ displayText(featured.teamName) }}</strong>
                    <small>焦点战队</small>
                  </div>
                </div>
                <div class="weekly-stat">
                  <span>战绩</span>
                  <b v-if="hasFeaturedRecord">
                    <em>{{ featuredWins }}胜</em>
                    <i>{{ featuredLosses }}负</i>
                  </b>
                  <b v-else>--</b>
                  <small v-if="featuredWinRate">胜率 {{ featuredWinRate }}</small>
                </div>
                <div class="weekly-stat">
                  <span>场均净胜</span>
                  <b class="is-accent">{{ featuredNet }}</b>
                </div>
                <div class="weekly-form">
                  <span>最近战绩</span>
                  <div v-if="featuredForm.length" class="weekly-form__badges">
                    <i
                      v-for="(item, index) in featuredForm"
                      :key="`${item}-${index}`"
                      :class="item === 'W' ? 'is-win' : 'is-lose'"
                    >{{ item === 'W' ? '胜' : '负' }}</i>
                  </div>
                  <small v-else>暂无近期战绩</small>
                </div>
              </article>
              <p v-else class="weekly-focus-empty">本周暂无焦点战队</p>
            </section>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
