<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import { getPublicHallPlayerHonorPage } from '@/api/publicHallOfFame'
import { getPublicPlayerSeasonStat } from '@/api/publicPlayer'
import { getV4PublicSeasonList, getV4PublicSeriesList } from '@/api/v4Public'
import type { PublicHonorItemResp, PublicPlayerSeasonStatVo } from '@/api/types/public'
import type { V4PublicSeasonItem, V4PublicSeriesItem } from '@/api/types/v4Public'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, formatDateTime } from '@/utils/publicDisplay'
import { pickLatestPublishedSeason, platformTag, seasonBelongsToSeries, seasonsNewestFirst } from '../hallFilters'

defineProps<{
  page: number
}>()

const router = useRouter()
const { t } = useI18n()

const allItems = ref<PublicHonorItemResp[]>([])
const seriesList = ref<V4PublicSeriesItem[]>([])
const v4Seasons = ref<V4PublicSeasonItem[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const year = ref('ALL')
const platform = ref('ALL')
const seriesId = ref('ALL')
const seasonFilter = ref('ALL')
const weekCursor = ref(0)
const stat = ref<PublicPlayerSeasonStatVo | null>(null)
let reqId = 0
let statReq = 0
let defaultApplied = false

const weeklyItems = computed(() =>
  allItems.value.filter((item) => (item.honorCode || '').toUpperCase() === 'WEEKLY_BEST'),
)

const yearOptions = computed(() => {
  const years = new Set<string>()
  weeklyItems.value.forEach((item) => {
    const value = String(item.awardDate || '').slice(0, 4)
    if (/^\d{4}$/.test(value)) years.add(value)
  })
  v4Seasons.value.forEach((item) => {
    if (item.seasonYear) years.add(String(item.seasonYear))
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allYears') },
    ...Array.from(years).sort((a, b) => Number(b) - Number(a)).map((value) => ({
      value,
      label: t('publicPages.hall.yearLabel', { year: value }),
    })),
  ]
})

const platformOptions = computed(() => {
  const values = new Set<string>()
  weeklyItems.value.forEach((item) => {
    const tag = platformTag(item.platformCode)
    if (tag) values.add(tag)
  })
  v4Seasons.value.forEach((item) => {
    item.platformCodes?.forEach((code) => {
      const tag = platformTag(code)
      if (tag) values.add(tag)
    })
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allPlatforms') },
    ...Array.from(values).map((value) => ({ value, label: value })),
  ]
})

const seriesOptions = computed(() => [
  { value: 'ALL', label: t('publicPages.hall.allEvents') },
  ...seriesList.value.map((item) => ({
    value: String(item.seriesId),
    label: displayText(item.seriesName, t('publicPages.hall.publicEvent')),
  })),
])

const seasonOptions = computed(() => {
  const map = new Map<string, string>()
  seasonsNewestFirst(v4Seasons.value).forEach((item) => {
    const id = String(item.seasonId || '')
    if (!id) return
    map.set(id, displayText(item.displayTitle || item.seasonName, t('publicPages.hall.season')))
  })
  weeklyItems.value.forEach((item) => {
    const id = String(item.seasonId || '')
    if (!id || map.has(id)) return
    map.set(id, displayText(item.seasonName, t('publicPages.hall.season')))
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allSeasons') },
    ...Array.from(map, ([value, label]) => ({ value, label })),
  ]
})

const weeks = computed(() => {
  const list = weeklyItems.value.filter((item) => {
    if (year.value !== 'ALL' && !String(item.awardDate || '').startsWith(year.value)) return false
    if (platform.value !== 'ALL' && platformTag(item.platformCode) !== platform.value) return false
    if (seriesId.value !== 'ALL' && String(item.tournamentId || '') !== seriesId.value) return false
    if (seasonFilter.value !== 'ALL' && String(item.seasonId || '') !== seasonFilter.value) return false
    return true
  })
  return [...list].sort((a, b) => String(a.awardDate || '').localeCompare(String(b.awardDate || '')))
})

const current = computed(() => weeks.value[weekCursor.value] || null)

function weekLabel(item: PublicHonorItemResp): string {
  const subtitle = String(item.honorSubtitle || '').trim()
  if (subtitle) return subtitle
  const date = formatDateTime(item.awardDate || '').date
  if (date !== '--') return date
  return displayText(item.honorTitle, t('publicPages.hall.weeklyBest'))
}

const weekOptions = computed(() =>
  weeks.value.map((item, index) => ({
    value: String(index),
    label: weekLabel(item),
  })),
)

const statBoxes = computed(() => {
  const row = stat.value
  return [
    { key: 'games', label: t('publicPages.player.games'), value: formatCount(row?.games) },
    { key: 'pts', label: t('publicPages.player.avgPoints'), value: formatAvg(row?.avgPoints) },
    { key: 'reb', label: t('publicPages.player.avgRebounds'), value: formatAvg(row?.avgRebounds) },
    { key: 'ast', label: t('publicPages.player.avgAssists'), value: formatAvg(row?.avgAssists) },
    { key: 'stl', label: t('publicPages.team.avgSteals'), value: formatAvg(row?.avgSteals) },
    { key: 'fg', label: t('publicPages.hall.fieldGoalPct'), value: formatPct(row?.fieldGoalPct) },
  ]
})

async function loadAll() {
  const currentReq = ++reqId
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const [seriesRes, seasonRes] = await Promise.all([
      getV4PublicSeriesList(),
      getV4PublicSeasonList(),
    ])
    if (currentReq !== reqId) return
    seriesList.value = seriesRes.data || []
    v4Seasons.value = seasonRes.data || []

    const collected: PublicHonorItemResp[] = []
    let page = 1
    let pages = 1
    do {
      const res = await getPublicHallPlayerHonorPage({
        currentPage: page,
        pageSize: 100,
        honorCodes: 'WEEKLY_BEST',
        sortField: 'awardDate',
        sortOrder: 'asc',
      })
      if (currentReq !== reqId) return
      const data = res.data
      collected.push(...(data?.records ?? []))
      pages = Math.max(1, Number(data?.pages || 1))
      page += 1
    } while (page <= pages && page <= 8)
    const weekly = collected.filter((item) => (item.honorCode || '').toUpperCase() === 'WEEKLY_BEST')
    allItems.value = weekly
    applyRunningDefault()
    state.value = weekly.length ? 'success' : 'empty'
    weekCursor.value = Math.max(0, weeks.value.length - 1)
  } catch (err) {
    if (currentReq !== reqId) return
    allItems.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : t('publicPages.hall.loadPlayerHonorsFailed')
  }
}

async function loadStat(item: PublicHonorItemResp | null) {
  const currentReq = ++statReq
  stat.value = null
  if (!item?.playerId) return
  try {
    const res = await getPublicPlayerSeasonStat(item.playerId, {
      seasonId: item.seasonId || undefined,
    })
    if (currentReq !== statReq) return
    const rows = res.data ?? []
    stat.value = rows.find((row) => String(row.seasonId || '') === String(item.seasonId || '')) || rows[0] || null
  } catch {
    if (currentReq !== statReq) return
    stat.value = null
  }
}

watch(() => true, () => {
  void loadAll()
}, { immediate: true })

watch(weeks, (list) => {
  if (weekCursor.value > list.length - 1) {
    weekCursor.value = Math.max(0, list.length - 1)
  }
})

watch(current, (item) => {
  void loadStat(item)
})

function focusLatestWeek() {
  weekCursor.value = Math.max(0, weeks.value.length - 1)
}

function applyRunningDefault() {
  if (defaultApplied) return
  defaultApplied = true
  const active = pickLatestPublishedSeason(v4Seasons.value, allItems.value)
  if (!active) return
  seriesId.value = String(active.seriesId || 'ALL')
  seasonFilter.value = String(active.seasonId || 'ALL')
}

function updateYear(value: string | number) {
  year.value = String(value)
  focusLatestWeek()
}

function updatePlatform(value: string | number) {
  platform.value = String(value)
  focusLatestWeek()
}

function updateSeries(value: string | number) {
  seriesId.value = String(value)
  if (!seasonBelongsToSeries(v4Seasons.value, seasonFilter.value, seriesId.value)) {
    seasonFilter.value = 'ALL'
  }
  focusLatestWeek()
}

function updateSeason(value: string | number) {
  seasonFilter.value = String(value)
  const picked = v4Seasons.value.find((item) => String(item.seasonId || '') === seasonFilter.value)
  const nextSeries = String(picked?.seriesId || '')
  if (nextSeries && seriesId.value !== 'ALL' && nextSeries !== seriesId.value) {
    seriesId.value = nextSeries
  }
  focusLatestWeek()
}

function updateWeek(value: string | number) {
  const index = Number(value)
  if (Number.isNaN(index)) return
  weekCursor.value = Math.min(Math.max(index, 0), Math.max(0, weeks.value.length - 1))
}

function stepWeek(delta: number) {
  updateWeek(weekCursor.value + delta)
}

function openPlayer(playerId?: string | null) {
  if (!playerId) return
  goToPlayerDetail(router, playerId, 'overview')
}

function openTeam(teamId?: string | null) {
  if (!teamId) return
  router.push(`/tournament/team/${teamId}/overview`)
}

function awardRange(item: PublicHonorItemResp) {
  const date = formatDateTime(item.awardDate || '').date
  return date === '--' ? '' : date
}

function formatCount(value?: number | null) {
  if (value == null || Number.isNaN(Number(value))) return '--'
  return String(value)
}

function formatAvg(value?: number | string | null) {
  if (value == null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function formatPct(value?: number | string | null) {
  if (value == null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  const pct = num <= 1 ? num * 100 : num
  return `${pct.toFixed(1)}%`
}
</script>

<template>
  <section class="weekly-best-panel">
    <HallPanelHead :title="t('publicPages.hall.weeklyTab')" subtitle="WEEKLY BEST" layout="inline">
      <template #filters>
        <HallSelect
          width="filter"
          :model-value="year"
          :options="yearOptions"
          @update:model-value="updateYear"
        />
        <HallSelect
          width="filter"
          :model-value="platform"
          :options="platformOptions"
          @update:model-value="updatePlatform"
        />
        <HallSelect
          width="filter"
          trigger-variant="week"
          :model-value="seriesId"
          :options="seriesOptions"
          @update:model-value="updateSeries"
        />
        <HallSelect
          width="filter"
          :model-value="seasonFilter"
          :options="seasonOptions"
          @update:model-value="updateSeason"
        />
        <HallSelect
          v-if="weekOptions.length"
          width="filter"
          trigger-variant="week"
          :model-value="String(weekCursor)"
          :options="weekOptions"
          @update:model-value="updateWeek"
        />
      </template>
    </HallPanelHead>

    <div v-if="state === 'loading'" class="hall-empty">{{ t('publicPages.hall.loading') }}</div>
    <div v-else-if="state === 'error'" class="hall-empty">
      <p>{{ errorMessage }}</p>
      <button type="button" @click="loadAll">{{ t('publicPages.hall.retry') }}</button>
    </div>
    <div v-else-if="state === 'empty'" class="hall-empty">{{ t('publicPages.hall.noWeeklyBest') }}</div>
    <div v-else-if="!current" class="hall-empty">{{ t('publicPages.hall.noWeeklyBestSeason') }}</div>

    <template v-else>
      <article class="weekly-feature">
        <div class="weekly-feature__mark">
          <span>PLAYER</span>
          <strong>OF THE WEEK</strong>
        </div>
        <div class="weekly-feature__photo">
          <HallPlayerSilhouette
            :name="displayText(current.playerName, t('publicPages.hall.player'))"
            :avatar-url="current.playerAvatarUrl || ''"
            size="feature"
          />
        </div>
        <div class="weekly-feature__identity">
          <button
            type="button"
            class="weekly-feature__name"
            :disabled="!current.playerId"
            @click="openPlayer(current.playerId)"
          >
            {{ displayText(current.playerName, t('publicPages.hall.unknownPlayer')) }}
          </button>
          <button
            v-if="current.teamName || current.eventTeamId"
            type="button"
            class="weekly-feature__team"
            :disabled="!current.eventTeamId"
            @click="openTeam(current.eventTeamId)"
          >
            <TeamLogo
              :team="{
                id: String(current.eventTeamId || current.teamId || current.honorId),
                name: displayText(current.teamName, 'CN'),
                shortName: displayText(current.teamName, 'CN'),
                logoUrl: current.teamLogoUrl || '',
              }"
              size="sm"
            />
            <span>{{ displayText(current.teamName, t('publicPages.hall.championTeam')) }}</span>
          </button>
          <p class="weekly-feature__meta">{{ displayText(current.seasonName || current.tournamentName, t('publicPages.hall.season')) }}</p>
          <p v-if="awardRange(current)" class="weekly-feature__meta">{{ awardRange(current) }}</p>
        </div>
        <dl class="weekly-feature__stats">
          <div v-for="box in statBoxes" :key="box.key">
            <dt>{{ box.label }}</dt>
            <dd>{{ box.value }}</dd>
          </div>
        </dl>
      </article>

      <div class="weekly-nav">
        <button type="button" :disabled="weekCursor <= 0" @click="stepWeek(-1)">
          <span aria-hidden="true">‹</span>
          {{ t('publicPages.hall.prevRecord') }}
        </button>
        <strong>{{ weekLabel(current) }}</strong>
        <button type="button" :disabled="weekCursor >= weeks.length - 1" @click="stepWeek(1)">
          {{ t('publicPages.hall.nextRecord') }}
          <span aria-hidden="true">›</span>
        </button>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.weekly-best-panel {
  width: 100%;
}

.weekly-feature {
  display: grid;
  grid-template-columns: 180px 240px minmax(220px, 300px) minmax(0, 1fr);
  column-gap: 28px;
  align-items: center;
  min-height: 320px;
  padding: 0 12px 0 8px;
  overflow: hidden;
  background: #0c0c0c;
  color: #fff;
}

.weekly-feature__mark {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #e10613;
  font-size: 42px;
  font-style: italic;
  font-weight: 900;
  line-height: 0.9;

  strong {
    display: block;
    margin-top: 6px;
    color: #fff;
    font-size: 16px;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.06em;
    line-height: 1.1;
  }
}

.weekly-feature__photo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-width: 0;
  overflow: hidden;

  :deep(.hall-player-visual) {
    height: 280px;
    min-height: 0;
    border: 0;
    background: transparent;
  }

  :deep(.hall-player-visual__photo) {
    object-fit: contain;
    object-position: center center;
  }

  :deep(.hall-player-visual__silhouette) {
    top: 50%;
    bottom: auto;
    transform: translate(-50%, -50%);
  }

  :deep(.hall-player-visual__glow) {
    display: none;
  }
}

.weekly-feature__identity {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.weekly-feature__name,
.weekly-feature__team {
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  text-align: center;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }
}

.weekly-feature__name {
  font-size: clamp(36px, 3vw, 52px);
  font-weight: 900;
  letter-spacing: 0.02em;
  line-height: 1;
  text-transform: uppercase;
}

.weekly-feature__team {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 12px;
  font-size: 20px;
  font-weight: 800;

  :deep(.team-logo--sm) {
    width: 42px;
    height: 42px;
  }
}

.weekly-feature__meta,
.weekly-feature__code {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 14px;
  font-weight: 600;
}

.weekly-feature__code {
  color: var(--cnpa-red, #e10613);
}

.weekly-feature__stats {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  align-content: center;
  height: 100%;
  margin: 0;
  padding-left: 22px;

  &::before {
    content: '';
    position: absolute;
    top: 16%;
    bottom: 16%;
    left: 0;
    width: 2px;
    background: rgba(255, 255, 255, 0.78);
  }

  > div {
    position: relative;
    padding: 0 10px;
    text-align: center;
  }

  > div + div::before {
    content: '';
    position: absolute;
    top: 4px;
    bottom: 4px;
    left: 0;
    width: 1px;
    background: rgba(255, 255, 255, 0.28);
  }

  dt {
    color: rgba(255, 255, 255, 0.82);
    font-size: 14px;
    font-weight: 700;
  }

  dd {
    margin: 10px 0 0;
    color: #e10613;
    font-size: 36px;
    font-weight: 700;
    font-synthesis: none;
    font-variant-numeric: tabular-nums;
    line-height: 1;
    -webkit-font-smoothing: antialiased;
  }
}

.weekly-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 22px;

  button,
  strong {
    min-width: 108px;
    height: 40px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    border: 1px solid rgba(17, 17, 17, 0.16);
    background: #fff;
    color: #111;
    font-size: 14px;
    font-weight: 800;
  }

  strong {
    background: #111;
    color: #fff;
    border-color: #111;
  }

  button {
    cursor: pointer;

    &:disabled {
      opacity: 0.4;
      cursor: default;
    }
  }
}

@media (max-width: 1100px) {
  .weekly-feature {
    grid-template-columns: 140px 180px minmax(0, 1fr);
    grid-template-rows: auto auto;
    min-height: 0;
    padding: 16px;
  }

  .weekly-feature__stats {
    grid-column: 1 / -1;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
