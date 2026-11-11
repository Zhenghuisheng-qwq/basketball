<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import TornEdge from '@/components/home/TornEdge.vue'
import { getPublicHomeDataFocus } from '@/api/publicHome'
import type { PublicHomeDataFocusVo, PublicHomeFocusTeamVo, PublicHomePlayerLeaderVo } from '@/api/types/public'
import { displayText, formatDecimal, initials } from '@/utils/publicDisplay'
import { iconAssets } from '@/constants/iconAssets'

const iconPts = iconAssets['home/basketball1.png']
const iconAst = iconAssets['home/basketball2.png']
const iconReb = iconAssets['home/basketball3.png']

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

const { t } = useI18n()

const seriesFilters = computed<Array<{ key: SeriesFilter; label: string }>>(() => [
  { key: 'LEAGUE', label: t('home.league') },
  { key: 'ARENA', label: t('home.arena') },
  { key: 'CUP', label: t('home.cup') },
  { key: 'OPEN', label: t('home.open') },
])

const seriesFilter = ref<SeriesFilter>('LEAGUE')
const userPickedSeries = ref(false)
const filterOpen = ref(false)
const state = ref<SectionState>('loading')
const errorMessage = ref('')
const brokenLogos = ref<Record<string, boolean>>({})
const focusData = ref<PublicHomeDataFocusVo | null>(null)
const featured = ref<PublicHomeFocusTeamVo | null>(null)

const filterLabel = computed(() =>
  seriesFilters.value.find((item) => item.key === seriesFilter.value)?.label || t('home.seriesFilter'),
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
    key: mark,
    title,
    statLabel,
    mark,
    value: Number.isFinite(numeric) ? formatDecimal(numeric, 1) : '--',
    playerName: displayText(row?.playerName, t('home.noPlayer')),
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
    focusData.value = data ?? null
    featured.value = data?.focusTeam ?? null
    state.value = 'success'
  } catch (err) {
    focusData.value = null
    featured.value = null
    state.value = 'error'
    errorMessage.value = err instanceof Error && err.message ? err.message : ''
  }
}

const leaders = computed(() => {
  if (state.value !== 'success') return []
  const data = focusData.value
  return [
    toLeaderCard(t('home.pointsLeader'), t('home.pointsAvg'), 'PTS', data?.pointsLeader),
    toLeaderCard(t('home.assistsLeader'), t('home.assistsAvg'), 'AST', data?.assistsLeader),
    toLeaderCard(t('home.reboundsLeader'), t('home.reboundsAvg'), 'REB', data?.reboundsLeader),
  ]
})

const focusErrorText = computed(() => errorMessage.value || t('home.focusError'))

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
            <section class="data-focus-box" :aria-label="t('home.dataFocus')">
              <div class="data-focus-header">
                <div class="combined-column-heading combined-column-heading--news">
                  <h2>{{ t('home.dataFocus') }}</h2>
                  <span class="home-kicker">DATA FOCUS</span>
                </div>
                <div class="data-focus-filter">
                  <button
                    type="button"
                    class="data-focus-filter__trigger"
                    :aria-expanded="filterOpen"
                    @click="filterOpen = !filterOpen"
                  >
                    <span>{{ t('home.seriesFilter') }}</span>
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

              <div v-if="state === 'loading'" class="home-state">{{ t('common.loading') }}</div>
              <div v-else-if="state === 'error'" class="home-state home-state--error">{{ focusErrorText }}</div>
              <div v-else-if="!leaders.length" class="home-state">{{ t('home.noFocus') }}</div>
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

            <section class="data-focus-box data-focus-box--featured" :aria-label="t('home.weeklyFocus')">
              <div class="data-focus-header">
                <div class="combined-column-heading combined-column-heading--news">
                  <h2>{{ t('home.weeklyFocus') }}</h2>
                  <span class="home-kicker">WEEKLY FOCUS</span>
                </div>
              </div>

              <div v-if="state === 'loading'" class="home-state">{{ t('common.loading') }}</div>
              <div v-else-if="state === 'error'" class="home-state home-state--error">{{ focusErrorText }}</div>
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
                    <small>{{ t('home.focusTeam') }}</small>
                  </div>
                </div>
                <div class="weekly-stat">
                  <span>{{ t('home.record') }}</span>
                  <b v-if="hasFeaturedRecord">
                    <em>{{ t('home.wins', { count: featuredWins }) }}</em>
                    <i>{{ t('home.losses', { count: featuredLosses }) }}</i>
                  </b>
                  <b v-else>--</b>
                  <small v-if="featuredWinRate">{{ t('home.winRate', { rate: featuredWinRate }) }}</small>
                </div>
                <div class="weekly-stat">
                  <span>{{ t('home.margin') }}</span>
                  <b class="is-accent">{{ featuredNet }}</b>
                </div>
                <div class="weekly-form">
                  <span>{{ t('home.recentForm') }}</span>
                  <div v-if="featuredForm.length" class="weekly-form__badges">
                    <i
                      v-for="(item, index) in featuredForm"
                      :key="`${item}-${index}`"
                      :class="item === 'W' ? 'is-win' : 'is-lose'"
                    >{{ item === 'W' ? t('home.win') : t('home.loss') }}</i>
                  </div>
                  <small v-else>{{ t('home.noRecentForm') }}</small>
                </div>
              </article>
              <p v-else class="weekly-focus-empty">{{ t('home.noWeeklyTeam') }}</p>
            </section>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
