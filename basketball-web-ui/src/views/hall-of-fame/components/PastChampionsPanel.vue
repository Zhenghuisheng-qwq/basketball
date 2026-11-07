<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import HallPagination from './HallPagination.vue'
import { getPublicHallTournamentHonorPage } from '@/api/publicHallOfFame'
import { getV4PublicSeasonList, getV4PublicSeriesList } from '@/api/v4Public'
import type { PublicHonorItemResp } from '@/api/types/public'
import type { V4PublicSeasonItem, V4PublicSeriesItem } from '@/api/types/v4Public'
import { displayText, formatDateTime } from '@/utils/publicDisplay'
import { teamDetailLocation } from '@/utils/eventContext'
import cnpaShield from '@/assets/icon/cnpa-shield.png'
import { pickLatestPublishedSeason, platformTag, seasonBelongsToSeries, seasonsNewestFirst } from '../hallFilters'

const props = defineProps<{
  year: string
  page: number
}>()

const emit = defineEmits<{
  'update:year': [value: string]
  'update:page': [value: number]
}>()

const router = useRouter()
const { t } = useI18n()
const panelRef = ref<HTMLElement | null>(null)
const pageSize = 8

const allChampions = ref<PublicHonorItemResp[]>([])
const seriesList = ref<V4PublicSeriesItem[]>([])
const v4Seasons = ref<V4PublicSeasonItem[]>([])
const platform = ref('ALL')
const series = ref('ALL')
const seasonId = ref('ALL')
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0
let defaultApplied = false

const yearOptions = computed(() => {
  const years = new Set<string>()
  allChampions.value.forEach((item) => {
    const year = String(item.awardDate || '').slice(0, 4)
    if (/^\d{4}$/.test(year)) years.add(year)
  })
  v4Seasons.value.forEach((item) => {
    if (item.seasonYear) years.add(String(item.seasonYear))
  })
  const sorted = Array.from(years).sort((a, b) => Number(b) - Number(a))
  return [
    { value: 'ALL', label: t('publicPages.hall.allYears') },
    ...sorted.map((year) => ({ value: year, label: t('publicPages.hall.yearLabel', { year }) })),
  ]
})

const platformOptions = computed(() => {
  const values = new Set<string>()
  allChampions.value.forEach((item) => {
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

const seriesOptions = computed(() => {
  const map = new Map<string, string>()
  seriesList.value.forEach((item) => {
    map.set(String(item.seriesId), displayText(item.seriesName, t('publicPages.hall.publicEvent')))
  })
  allChampions.value.forEach((item) => {
    const id = String(item.tournamentId || '')
    if (!id || map.has(id)) return
    map.set(id, displayText(item.tournamentName, t('publicPages.hall.publicEvent')))
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allEvents') },
    ...Array.from(map, ([value, label]) => ({ value, label })),
  ]
})

const seasonOptions = computed(() => {
  const map = new Map<string, string>()
  seasonsNewestFirst(v4Seasons.value).forEach((item) => {
    const id = String(item.seasonId || '')
    if (!id) return
    map.set(id, displayText(item.displayTitle || item.seasonName, t('publicPages.hall.season')))
  })
  allChampions.value.forEach((item) => {
    const id = String(item.seasonId || '')
    if (!id || map.has(id)) return
    map.set(id, displayText(item.seasonName, t('publicPages.hall.season')))
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allSeasons') },
    ...Array.from(map, ([value, label]) => ({ value, label })),
  ]
})

const filtered = computed(() => allChampions.value.filter((item) => {
  if (props.year !== 'ALL' && !String(item.awardDate || '').startsWith(props.year)) return false
  if (platform.value !== 'ALL' && platformTag(item.platformCode) !== platform.value) return false
  if (series.value !== 'ALL' && String(item.tournamentId || '') !== series.value) return false
  if (seasonId.value !== 'ALL' && String(item.seasonId || '') !== seasonId.value) return false
  return true
}))

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))

const pageItems = computed(() => {
  const start = (props.page - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const groupedByYear = computed(() => {
  const map = new Map<number, PublicHonorItemResp[]>()
  pageItems.value.forEach((item) => {
    const year = Number(String(item.awardDate || '').slice(0, 4)) || 0
    const list = map.get(year) || []
    list.push(item)
    map.set(year, list)
  })
  return Array.from(map.entries())
    .sort((a, b) => b[0] - a[0])
    .map(([year, items]) => ({ year, items }))
})

async function loadAll() {
  const current = ++reqId
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const [seriesRes, seasonRes] = await Promise.all([
      getV4PublicSeriesList(),
      getV4PublicSeasonList(),
    ])
    if (current !== reqId) return
    seriesList.value = seriesRes.data || []
    v4Seasons.value = seasonRes.data || []

    const collected: PublicHonorItemResp[] = []
    let page = 1
    let pages = 1
    do {
      const res = await getPublicHallTournamentHonorPage({
        currentPage: page,
        pageSize: 100,
        sortField: 'awardDate',
        sortOrder: 'desc',
      })
      if (current !== reqId) return
      const data = res.data
      const records = data?.records ?? []
      collected.push(...records)
      pages = Math.max(1, Number(data?.pages || 1))
      page += 1
    } while (page <= pages && page <= 10)

    allChampions.value = collected.filter(
      (item) => (item.honorCode || '').toUpperCase() === 'CHAMPION',
    )
    applyRunningDefault()
    state.value = allChampions.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    allChampions.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : t('publicPages.hall.loadChampionsFailed')
  }
}

watch(
  () => true,
  () => {
    void loadAll()
  },
  { immediate: true },
)

watch(
  () => [props.year, props.page, filtered.value.length, totalPages.value] as const,
  () => {
    if (props.page > totalPages.value) {
      emit('update:page', totalPages.value)
    }
  },
)

function updateYear(value: string | number) {
  emit('update:year', String(value))
  emit('update:page', 1)
}

function updatePlatform(value: string | number) {
  platform.value = String(value)
  emit('update:page', 1)
}

function updateSeries(value: string | number) {
  series.value = String(value)
  if (!seasonBelongsToSeries(v4Seasons.value, seasonId.value, series.value)) {
    seasonId.value = 'ALL'
  }
  emit('update:page', 1)
}

function updateSeason(value: string | number) {
  seasonId.value = String(value)
  const picked = v4Seasons.value.find((item) => String(item.seasonId || '') === seasonId.value)
  const nextSeries = String(picked?.seriesId || '')
  if (nextSeries && series.value !== 'ALL' && nextSeries !== series.value) {
    series.value = nextSeries
  }
  emit('update:page', 1)
}

function applyRunningDefault() {
  if (defaultApplied) return
  defaultApplied = true
  const active = pickLatestPublishedSeason(v4Seasons.value, allChampions.value)
  if (!active) return
  series.value = String(active.seriesId || 'ALL')
  seasonId.value = String(active.seasonId || 'ALL')
}

function teamLabel(record: PublicHonorItemResp): string {
  return String(record.teamName || '').trim()
}

function cardTitle(record: PublicHonorItemResp): string {
  return displayText(record.seasonName || record.tournamentName, t('publicPages.hall.publicEvent'))
}

function updatePage(page: number) {
  emit('update:page', page)
  panelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openTeam(record: PublicHonorItemResp) {
  const teamId = record.eventTeamId
  if (!teamId) return
  router.push(teamDetailLocation(teamId, {
    seasonId: record.seasonId,
    tab: 'overview',
  }))
}

function openEventHonor(tournamentId?: string | null, showRoster = false, seasonId?: string | number | null) {
  if (!tournamentId) return
  router.replace({
    path: '/hall-of-fame',
    query: {
      tab: 'event',
      tournamentId,
      ...(seasonId ? { seasonId: String(seasonId) } : {}),
      ...(showRoster ? { showRoster: '1' } : {}),
    },
  })
}

function awardDateText(value?: string | null) {
  const matched = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (matched) return `${matched[1]}.${matched[2]}.${matched[3]}`
  const dt = formatDateTime(value)
  return dt.date === '--' ? '--' : dt.date.replace(/-/g, '.')
}

function teamNameText(record: PublicHonorItemResp) {
  const name = teamLabel(record)
  return name ? name.toLocaleUpperCase() : t('publicPages.hall.championTeam')
}

function seriesIcon(record: PublicHonorItemResp) {
  const id = String(record.tournamentId || '')
  if (!id) return ''
  return seriesList.value.find((item) => String(item.seriesId) === id)?.iconUrl || ''
}
</script>

<template>
  <section ref="panelRef" class="past-champions-panel">
    <HallPanelHead
      layout="inline"
      :title="t('publicPages.hall.pastChampions')"
      subtitle="PAST CHAMPIONS"
    >
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
          :model-value="series"
          :options="seriesOptions"
          @update:model-value="updateSeries"
        />
        <HallSelect
          width="filter"
          :model-value="seasonId"
          :options="seasonOptions"
          @update:model-value="updateSeason"
        />
      </template>
    </HallPanelHead>

    <div v-if="state === 'loading'" class="hall-empty">{{ t('publicPages.hall.loading') }}</div>
    <div v-else-if="state === 'error'" class="hall-empty">
      <p>{{ errorMessage }}</p>
      <button type="button" @click="loadAll">{{ t('publicPages.hall.retry') }}</button>
    </div>
    <div v-else-if="state === 'empty' || filtered.length === 0" class="hall-empty">
      {{ t('publicPages.hall.noChampions') }}
    </div>

    <template v-else>
      <div v-for="group in groupedByYear" :key="group.year" class="past-champions-group">
        <h3 class="past-champions-group__year">{{ group.year || t('publicPages.hall.unknownYear') }}</h3>
        <div class="past-champions-grid">
          <article
            v-for="record in group.items"
            :key="record.honorId"
            class="past-champion-card"
          >
            <div class="past-champion-card__copy">
              <div class="past-champion-card__main">
                <div class="past-champion-card__series">
                  <img class="past-champion-card__series-icon" :src="seriesIcon(record) || cnpaShield" alt="" />
                  <span>{{ displayText(record.tournamentName, t('publicPages.hall.championFallback')) }}</span>
                </div>
                <h4 class="past-champion-card__name">{{ cardTitle(record) }}</h4>
                <span v-if="platformTag(record.platformCode)" class="past-champion-card__platform">
                  {{ platformTag(record.platformCode) }}
                </span>
                <p class="past-champion-card__date">
                  <span>{{ t('publicPages.hall.crownedDate') }}</span>
                  <strong>{{ awardDateText(record.awardDate) }}</strong>
                </p>
              </div>
            </div>

            <button
              type="button"
              class="past-champion-card__team"
              :disabled="!record.eventTeamId"
              @click="openTeam(record)"
            >
              <TeamLogo
                :team="{
                  id: String(record.eventTeamId || record.teamId || record.honorId),
                  name: teamLabel(record) || 'CN',
                  shortName: teamLabel(record) || 'CN',
                  logoUrl: record.teamLogoUrl || '',
                }"
                size="xl"
              />
              <strong>{{ teamNameText(record) }}</strong>
            </button>
            <div class="past-champion-card__actions">
              <button type="button" :disabled="!record.eventTeamId" @click="openTeam(record)">
                {{ t('publicPages.hall.viewChampionRoster') }}
                <span aria-hidden="true">›</span>
              </button>
              <button type="button" @click="openEventHonor(record.tournamentId, false, record.seasonId)">
                {{ t('publicPages.hall.enterEventHonors') }}
                <span aria-hidden="true">›</span>
              </button>
            </div>
          </article>
        </div>
      </div>

      <HallPagination
        :page="page"
        :total-pages="totalPages"
        @update:page="updatePage"
      />
    </template>
  </section>
</template>

<style scoped lang="scss">
.past-champions-panel {
  width: 100%;
}

.past-champions-group + .past-champions-group {
  margin-top: 28px;
}

.past-champions-group__year {
  margin: 2px 0 18px;
  color: #e10613;
  font-size: 42px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.02em;
}

.past-champions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 680px));
  justify-content: start;
  gap: 18px;
}

.past-champion-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr auto;
  align-items: stretch;
  min-height: 400px;
  padding: 28px 0 0;
  background: #111;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  overflow: hidden;
}

.past-champion-card__copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 18px;
  min-width: 0;
  height: 100%;
  padding: 0 20px 0 28px;
}

.past-champion-card__main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.past-champion-card__series {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  color: rgba(255, 255, 255, 0.72);
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.past-champion-card__mark {
  width: 14px;
  height: 16px;
  flex-shrink: 0;
  background: #e10613;
  clip-path: polygon(50% 0, 100% 18%, 82% 100%, 18% 100%, 0 18%);
}

.past-champion-card__series-icon {
  width: 52px;
  height: 58px;
  flex-shrink: 0;
  object-fit: contain;
}

.past-champion-card__name {
  margin: 12px 0 0;
  max-width: 100%;
  color: #fff;
  font-size: 32px;
  font-weight: 900;
  line-height: 1.15;
}

.past-champion-card__platform {
  display: inline-flex;
  align-items: center;
  align-self: center;
  justify-content: center;
  height: 30px;
  margin-top: 14px;
  padding: 0 12px;
  border: 1px solid #e10613;
  background: #e10613;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.past-champion-card__date {
  align-self: center;
  margin: 12px 0 0;
  text-align: center;

  span {
    display: block;
    color: rgba(255, 255, 255, 0.55);
    font-size: 16px;
    font-weight: 700;
  }

  strong {
    display: block;
    margin-top: 6px;
    color: #fff;
    font-size: 24px;
    font-weight: 800;
    letter-spacing: 0.04em;
  }
}

.past-champion-card__actions {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: 1fr 1fr;
  background: transparent;
  border-top: 1px solid rgba(255, 255, 255, 0.12);

  button {
    padding: 16px 8px;
    border: 0;
    background: transparent;
    color: rgba(255, 255, 255, 0.88);
    font: inherit;
    font-size: 18px;
    font-weight: 800;
    text-align: center;
    cursor: pointer;

    & + button {
      border-left: 1px solid rgba(255, 255, 255, 0.22);
    }

    span {
      margin-left: 4px;
    }

    &:hover:not(:disabled) {
      color: #e10613;
    }

    &:disabled {
      color: rgba(255, 255, 255, 0.35);
      cursor: default;
    }
  }
}

.past-champion-card__team {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  cursor: pointer;
  overflow: hidden;

  &:disabled {
    cursor: default;
  }

  strong {
    max-width: 100%;
    color: #fff;
    font-size: 36px;
    font-weight: 900;
    letter-spacing: 0.08em;
    line-height: 1.1;
    text-align: center;
  }

  :deep(.team-logo.team-logo--xl) {
    width: 200px;
    height: 180px;
    background: transparent;
  }

  :deep(.team-logo__img) {
    object-fit: contain;
  }
}

@media (max-width: 1023px) {
  .past-champions-grid {
    grid-template-columns: minmax(0, 680px);
  }
}

@media (max-width: 560px) {
  .past-champions-group__year {
    font-size: 28px;
  }

  .past-champion-card {
    min-height: 260px;
    padding: 16px 0 0;
  }

  .past-champion-card__name {
    font-size: 22px;
  }

  .past-champion-card__team {
    strong {
      font-size: 20px;
      letter-spacing: 0.04em;
    }

    :deep(.team-logo.team-logo--xl) {
      width: 120px;
      height: 108px;
    }
  }
}
</style>
