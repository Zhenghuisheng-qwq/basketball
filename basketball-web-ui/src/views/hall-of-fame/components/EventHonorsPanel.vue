<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import ChampionRoster from './ChampionRoster.vue'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import {
  getPublicHallHonorDetail,
  getPublicHallTournamentHonorPage,
} from '@/api/publicHallOfFame'
import { getV4PublicSeasonList, getV4PublicSeriesList } from '@/api/v4Public'
import type { V4PublicSeasonItem } from '@/api/types/v4Public'
import type {
  PublicHonorDetailResp,
  PublicHonorItemResp,
  PublicTournamentItemVo,
} from '@/api/types/public'
import { getPublicPlayerDetail, getPublicPlayerSeasonStat } from '@/api/publicPlayer'
import type { PublicPlayerSeasonStatVo } from '@/api/types/public'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, formatDateTime, localizedText } from '@/utils/publicDisplay'
import { pickLatestPublishedSeason, platformTag, seasonBelongsToSeries, seasonsNewestFirst } from '../hallFilters'
import type { ChampionRosterMember } from '../types'
import mrtMidBackground from '@/assets/icon/mrt/mrt_mid.png'

const props = defineProps<{
  tournamentId: string
  rosterExpanded: boolean
  preferredSeasonId?: string
}>()

const emit = defineEmits<{
  'update:tournamentId': [value: string]
  'update:rosterExpanded': [value: boolean]
  'update:seasonId': [value: string]
  'update:scope': [payload: { tournamentId: string, seasonId: string }]
}>()

const router = useRouter()
const { t } = useI18n()
const LINEUP_POSITIONS = ['PG', 'SG', 'SF', 'PF', 'C']
const playerPosition = ref<Record<string, string>>({})

const EVENT_HONOR_CODES = 'CHAMPION,RUNNER_UP,THIRD_PLACE,MVP,ALL_TOURNAMENT,ALL_DEFENSIVE'

const tournaments = ref<PublicTournamentItemVo[]>([])
const v4Seasons = ref<V4PublicSeasonItem[]>([])
const allHonors = ref<PublicHonorItemResp[]>([])
const year = ref('ALL')
const platform = ref('ALL')
const seasonId = ref(props.preferredSeasonId || 'ALL')
const championDetail = ref<PublicHonorDetailResp | null>(null)
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let loadReq = 0
let defaultApplied = false

const tournamentOptions = computed(() => [
  { value: 'ALL', label: t('publicPages.hall.allEvents') },
  ...tournaments.value.map((item) => ({
    value: item.tournamentId,
    label: displayText(item.tournamentName || item.shortName, t('publicPages.hall.publicEvent')),
  })),
])

const yearOptions = computed(() => {
  const years = new Set<string>()
  scopedHonors.value.forEach((item) => {
    const value = String(item.awardDate || '').slice(0, 4)
    if (/^\d{4}$/.test(value)) years.add(value)
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
  scopedHonors.value.forEach((item) => {
    const tag = platformTag(item.platformCode)
    if (tag) values.add(tag)
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allPlatforms') },
    ...Array.from(values).map((value) => ({ value, label: value })),
  ]
})

const scopedHonors = computed(() => allHonors.value.filter((item) => {
  if (props.tournamentId && String(item.tournamentId || '') !== props.tournamentId) return false
  return true
}))

const seasonOptions = computed(() => {
  const map = new Map<string, string>()
  seasonsNewestFirst(v4Seasons.value).forEach((item) => {
    const id = String(item.seasonId || '')
    if (!id) return
    map.set(id, displayText(item.displayTitle || item.seasonName, t('publicPages.hall.season')))
  })
  allHonors.value.forEach((item) => {
    const id = String(item.seasonId || '')
    if (!id || map.has(id)) return
    map.set(id, displayText(item.seasonName, t('publicPages.hall.season')))
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allSeasons') },
    ...Array.from(map, ([value, label]) => ({ value, label })),
  ]
})

const honors = computed(() => scopedHonors.value.filter((item) => {
  if (year.value !== 'ALL' && !String(item.awardDate || '').startsWith(year.value)) return false
  if (platform.value !== 'ALL' && platformTag(item.platformCode) !== platform.value) return false
  if (seasonId.value && seasonId.value !== 'ALL' && String(item.seasonId || '') !== seasonId.value) return false
  return true
}))

const viewState = computed(() => {
  if (state.value === 'loading' || state.value === 'error') return state.value
  return honors.value.length ? 'success' : 'empty'
})

const currentTournament = computed(
  () => tournaments.value.find((item) => item.tournamentId === props.tournamentId) ?? null,
)

const championHonor = computed(
  () => honors.value.find((item) => (item.honorCode || '').toUpperCase() === 'CHAMPION') ?? null,
)

function lineupPositionCode(item: PublicHonorItemResp): string {
  const picked = String(item.honorValue || '').trim().toUpperCase()
  if (LINEUP_POSITIONS.includes(picked)) return picked
  const playerId = String(item.playerId || '')
  return playerPosition.value[playerId] || ''
}

function sortLineup(items: PublicHonorItemResp[]) {
  return [...items].sort((left, right) => {
    const leftIndex = LINEUP_POSITIONS.indexOf(lineupPositionCode(left))
    const rightIndex = LINEUP_POSITIONS.indexOf(lineupPositionCode(right))
    const leftOrder = leftIndex < 0 ? LINEUP_POSITIONS.length : leftIndex
    const rightOrder = rightIndex < 0 ? LINEUP_POSITIONS.length : rightIndex
    if (leftOrder !== rightOrder) return leftOrder - rightOrder
    return (left.displayOrder ?? 0) - (right.displayOrder ?? 0)
  })
}

const featuredGroups = computed(() => {
  const groups = [
    { key: 'MVP', title: t('publicPages.hall.mvp') },
    { key: 'ALL_TOURNAMENT', title: t('publicPages.hall.allTournament') },
    { key: 'ALL_DEFENSIVE', title: t('publicPages.hall.allDefensive') },
  ]
  return groups
    .map((group) => ({
      ...group,
      items: sortLineup(honors.value.filter((item) => (item.honorCode || '').toUpperCase() === group.key)),
    }))
    .filter((group) => group.items.length > 0)
})

const featuredIds = computed(() => new Set(featuredGroups.value.flatMap((group) => group.items.map((item) => item.honorId))))

const otherHonors = computed(() =>
  honors.value.filter((item) => item.honorId !== championHonor.value?.honorId && !featuredIds.value.has(item.honorId)),
)

const championTeam = computed(() => {
  const record = championDetail.value || championHonor.value
  if (!record?.teamId && !record?.teamName) return null
  return {
    id: String(record.teamId || ''),
    name: displayText(record.teamName, 'CN'),
    shortName: displayText(record.teamName, 'CN'),
    logoUrl: record.teamLogoUrl || '',
  }
})

const mvpHonor = computed(() => featuredGroups.value.find((group) => group.key === 'MVP')?.items[0] ?? null)
const mvpStat = ref<PublicPlayerSeasonStatVo | null>(null)

const mvpStats = computed(() => {
  const row = mvpStat.value
  return [
    { key: 'pts', label: t('publicPages.player.avgPoints'), value: formatAvg(row?.avgPoints) },
    { key: 'reb', label: t('publicPages.player.avgRebounds'), value: formatAvg(row?.avgRebounds) },
    { key: 'ast', label: t('publicPages.player.avgAssists'), value: formatAvg(row?.avgAssists) },
    { key: 'stl', label: t('publicPages.team.avgSteals'), value: formatAvg(row?.avgSteals) },
    { key: 'fg', label: t('publicPages.hall.fieldGoalPct'), value: formatPct(row?.fieldGoalPct) },
  ]
})

const rosterMembers = computed<ChampionRosterMember[]>(() => {
  const detail = championDetail.value
  if (!detail?.lineup?.length) return []
  return detail.lineup.map((item, index) => {
    const roleRaw = (item.roleType || '').toUpperCase()
    const role = roleRaw.includes('START') ? 'STARTER' : 'SUBSTITUTE'
    return {
      id: String(item.lineupId || `${item.playerId || index}`),
      tournamentId: String(detail.tournamentId || props.tournamentId),
      teamId: String(item.teamId || detail.teamId || ''),
      playerId: item.playerId || '',
      playerName: displayText(item.playerName, t('publicPages.hall.player')),
      playerAvatarUrl: item.avatarUrl || '',
      position: displayText(item.positionCode, '--'),
      role,
      captain: roleRaw.includes('CAPTAIN'),
      appearances: 0,
      sortOrder: item.sortNo ?? index,
    }
  })
})

async function loadPage() {
  const current = ++loadReq
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const [seriesRes, seasonRes] = await Promise.all([
      getV4PublicSeriesList(),
      getV4PublicSeasonList(),
    ])
    if (current !== loadReq) return
    tournaments.value = (seriesRes.data || []).map((item) => ({
      tournamentId: String(item.seriesId),
      tournamentName: item.seriesName,
      shortName: item.seriesCode || item.seriesName,
    }))
    v4Seasons.value = seasonRes.data || []

    const collected: PublicHonorItemResp[] = []
    let page = 1
    let pages = 1
    do {
      const res = await getPublicHallTournamentHonorPage({
        currentPage: page,
        pageSize: 100,
        honorCodes: EVENT_HONOR_CODES,
        sortField: 'awardDate',
        sortOrder: 'desc',
      })
      if (current !== loadReq) return
      const data = res.data
      collected.push(...(data?.records ?? []))
      pages = Math.max(1, Number(data?.pages || 1))
      page += 1
    } while (page <= pages && page <= 10)

    allHonors.value = collected
    applyRunningDefault()
    state.value = 'success'
  } catch (err) {
    if (current !== loadReq) return
    tournaments.value = []
    v4Seasons.value = []
    allHonors.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : t('publicPages.hall.loadEventHonorsFailed')
  }
}

watch(
  () => props.preferredSeasonId,
  (id) => {
    seasonId.value = id || 'ALL'
  },
)

function selectionHasHonors(seriesId: string, nextSeasonId: string) {
  return allHonors.value.some((item) => {
    if (seriesId && String(item.tournamentId || '') !== seriesId) return false
    if (nextSeasonId && String(item.seasonId || '') !== nextSeasonId) return false
    return true
  })
}

function applyRunningDefault() {
  if (defaultApplied) return
  defaultApplied = true
  const active = pickLatestPublishedSeason(v4Seasons.value, allHonors.value)
  if (!active) return
  const tournamentId = String(active.seriesId || '')
  const nextSeasonId = String(active.seasonId || '')
  if (!tournamentId || !nextSeasonId) return
  const pinnedSeries = props.tournamentId
  const pinnedSeason = props.preferredSeasonId || ''
  if ((pinnedSeries || pinnedSeason) && selectionHasHonors(pinnedSeries, pinnedSeason)) return
  if (pinnedSeries === tournamentId && pinnedSeason === nextSeasonId) return
  seasonId.value = nextSeasonId
  emit('update:scope', { tournamentId, seasonId: nextSeasonId })
}

watch(allHonors, (rows) => {
  const missing = new Set<string>()
  rows.forEach((item) => {
    const code = String(item.honorCode || '').toUpperCase()
    if (code !== 'ALL_TOURNAMENT' && code !== 'ALL_DEFENSIVE') return
    const picked = String(item.honorValue || '').trim().toUpperCase()
    const playerId = String(item.playerId || '')
    if (!playerId || LINEUP_POSITIONS.includes(picked) || playerPosition.value[playerId]) return
    missing.add(playerId)
  })
  missing.forEach((playerId) => {
    void getPublicPlayerDetail(playerId).then((res) => {
      const code = String(res.data?.positionCode || '').trim().toUpperCase()
      if (!code) return
      playerPosition.value = { ...playerPosition.value, [playerId]: code }
    }).catch(() => {})
  })
})

watch(
  () => true,
  () => {
    void loadPage()
  },
  { immediate: true },
)

function updateTournament(value: string | number) {
  const id = String(value)
  const nextSeason = seasonBelongsToSeries(v4Seasons.value, seasonId.value, id) ? seasonId.value : 'ALL'
  seasonId.value = nextSeason
  emit('update:scope', {
    tournamentId: id === 'ALL' ? '' : id,
    seasonId: nextSeason === 'ALL' ? '' : nextSeason,
  })
  emit('update:rosterExpanded', false)
}

function updateYear(value: string | number) {
  year.value = String(value)
  emit('update:rosterExpanded', false)
}

function updatePlatform(value: string | number) {
  platform.value = String(value)
  emit('update:rosterExpanded', false)
}

function updateSeason(value: string | number) {
  const next = String(value)
  seasonId.value = next
  emit('update:rosterExpanded', false)
  if (next === 'ALL') {
    emit('update:seasonId', '')
    return
  }
  const picked = v4Seasons.value.find((item) => String(item.seasonId || '') === next)
  const seriesId = String(picked?.seriesId || '')
  if (props.tournamentId && seriesId && seriesId !== props.tournamentId) {
    emit('update:scope', { tournamentId: seriesId, seasonId: next })
    return
  }
  emit('update:seasonId', next)
}

function retryLoad() {
  void loadPage()
}

function toggleRoster() {
  emit('update:rosterExpanded', !props.rosterExpanded)
}

function openTeam(teamId?: string | null) {
  if (!teamId) return
  router.push(`/tournament/team/${teamId}/overview`)
}

function openPlayer(playerId?: string | null) {
  if (!playerId) return
  goToPlayerDetail(router, playerId, 'overview')
}

function awardDateText(value?: string | null) {
  const dt = formatDateTime(value)
  return dt.date === '--' ? '--' : dt.date
}

function championDateText(value?: string | null) {
  return awardDateText(value).replace(/-/g, '.')
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

watch(championHonor, async (item) => {
  championDetail.value = null
  if (!item?.honorId) return
  const current = loadReq
  try {
    const detailRes = await getPublicHallHonorDetail(item.honorId)
    if (current !== loadReq) return
    championDetail.value = detailRes.data ?? null
  } catch {
    if (current !== loadReq) return
    championDetail.value = null
  }
})

watch(mvpHonor, async (item) => {
  mvpStat.value = null
  if (!item?.playerId) return
  try {
    const res = await getPublicPlayerSeasonStat(item.playerId, { seasonId: item.seasonId || undefined })
    const rows = res.data ?? []
    mvpStat.value = rows.find((row) => String(row.seasonId || '') === String(item.seasonId || '')) || rows[0] || null
  } catch {
    mvpStat.value = null
  }
})
</script>

<template>
  <section class="event-honors-panel">
    <HallPanelHead :title="t('publicPages.hall.eventHonors')" subtitle="EVENT HONORS" layout="inline">
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
          :model-value="tournamentId || 'ALL'"
          :options="tournamentOptions"
          @update:model-value="updateTournament"
        />
        <HallSelect
          width="filter"
          :model-value="seasonId || 'ALL'"
          :options="seasonOptions"
          @update:model-value="updateSeason"
        />
      </template>
    </HallPanelHead>

    <div v-if="viewState === 'loading'" class="hall-empty">{{ t('publicPages.hall.loading') }}</div>
    <div v-else-if="viewState === 'error'" class="hall-empty">
      <p>{{ errorMessage }}</p>
      <button type="button" @click="retryLoad">{{ t('publicPages.hall.retry') }}</button>
    </div>
    <div v-else-if="viewState === 'empty'" class="hall-empty">{{ t('publicPages.hall.noSeasonPublishedHonors') }}</div>

    <template v-else>
      <article
        v-if="championHonor"
        class="event-champion-card"
        :style="{ '--champion-bg': `url(${mrtMidBackground})` }"
      >
        <button
          type="button"
          class="event-champion-card__team"
          :disabled="!championHonor.eventTeamId"
          @click="openTeam(championHonor.eventTeamId)"
        >
          <p class="event-champion-card__label">{{ t('publicPages.hall.champion') }}</p>
          <span class="event-champion-card__label-en">CHAMPION</span>
          <span class="event-champion-card__logo">
            <TeamLogo v-if="championTeam" :team="championTeam" size="xl" />
          </span>
          <strong>{{ displayText(championHonor.teamName, t('publicPages.hall.championTeam')) }}</strong>
        </button>

        <div class="event-champion-card__info">
          <p class="event-champion-card__tournament">
            {{ displayText(championHonor.seasonName || currentTournament?.tournamentName || championHonor.tournamentName) }}
          </p>
          <p class="event-champion-card__desc">
            {{ localizedText(championHonor.honorSubtitle || championHonor.honorTitle, championHonor.honorSubtitleEn || championHonor.honorTitleEn, t('publicPages.hall.publishedHonor')) }}
          </p>
          <div class="event-champion-card__date-block">
            <span>{{ t('publicPages.hall.crownedDate') }}</span>
            <strong>{{ championDateText(championHonor.awardDate) }}</strong>
          </div>
          <button
            v-if="rosterMembers.length"
            type="button"
            class="event-champion-card__btn"
            @click="toggleRoster"
          >
            {{ rosterExpanded ? t('publicPages.hall.collapseRoster') : t('publicPages.hall.viewRoster') }}
            <span aria-hidden="true">›</span>
          </button>
        </div>
      </article>

      <ChampionRoster
        v-if="rosterExpanded && rosterMembers.length"
        :members="rosterMembers"
      />

      <template v-for="group in featuredGroups" :key="group.key">
        <section v-if="group.key === 'MVP'" class="event-mvp">
          <h3 class="event-mvp__title">
            {{ t('publicPages.hall.mvp').replace(/\s*MVP\s*$/i, '') }}
            <span>MVP</span>
          </h3>
          <article
            v-for="item in group.items"
            :key="item.honorId"
            class="event-mvp__card"
          >
            <div class="event-mvp__photo">
              <HallPlayerSilhouette
                :name="displayText(item.playerName, t('publicPages.hall.player'))"
                :avatar-url="item.playerAvatarUrl || ''"
                size="mvp"
              />
            </div>
            <div class="event-mvp__identity">
              <button type="button" class="event-mvp__name" :disabled="!item.playerId" @click="openPlayer(item.playerId)">
                {{ displayText(item.playerName, t('publicPages.hall.unknownPlayer')) }}
              </button>
              <p v-if="item.teamName" class="event-mvp__team">
                <TeamLogo
                  :team="{
                    id: String(item.eventTeamId || item.teamId || item.honorId),
                    name: displayText(item.teamName, 'CN'),
                    shortName: displayText(item.teamName, 'CN'),
                    logoUrl: item.teamLogoUrl || '',
                  }"
                  size="xs"
                />
                <span>{{ displayText(item.teamName) }}</span>
              </p>
              <p class="event-mvp__meta">
                {{ localizedText(item.seasonName, item.seasonNameEn, t('publicPages.hall.season')) }}
                <span>{{ t('publicPages.hall.seasonScope') }}</span>
              </p>
            </div>
            <dl class="event-mvp__stats">
              <div v-for="box in mvpStats" :key="box.key">
                <dt>{{ box.label }}</dt>
                <dd>{{ box.value }}</dd>
              </div>
            </dl>
          </article>
        </section>
        <section v-else class="event-lineup">
          <h3 class="event-lineup__title">
            {{ group.title }}
            <span>{{ group.key === 'ALL_DEFENSIVE' ? 'ALL-DEFENSIVE TEAM' : 'ALL-TOURNAMENT TEAM' }}</span>
          </h3>
          <div class="event-lineup__grid">
            <article
              v-for="item in group.items"
              :key="item.honorId"
              class="event-lineup__card"
            >
              <div class="event-lineup__photo">
                <HallPlayerSilhouette
                  :name="displayText(item.playerName, t('publicPages.hall.player'))"
                  :avatar-url="item.playerAvatarUrl || ''"
                  size="lineup"
                />
              </div>
              <div class="event-lineup__meta">
                <button type="button" class="event-lineup__name" :disabled="!item.playerId" @click="openPlayer(item.playerId)">
                  {{ displayText(item.playerName, t('publicPages.hall.unknownPlayer')) }}
                </button>
                <p class="event-lineup__team">
                  <TeamLogo
                    v-if="item.teamName || item.teamLogoUrl"
                    :team="{
                      id: String(item.eventTeamId || item.teamId || item.honorId),
                      name: displayText(item.teamName, 'CN'),
                      shortName: displayText(item.teamName, 'CN'),
                      logoUrl: item.teamLogoUrl || '',
                    }"
                    size="xs"
                  />
                  <span>{{ displayText(item.teamName) }}</span>
                </p>
              </div>
            </article>
          </div>
        </section>
      </template>

      <div v-if="otherHonors.length" class="event-other-honors">
        <h3 class="event-other-honors__title">{{ t('publicPages.hall.otherHonors') }}</h3>
        <div class="event-other-honors__grid">
          <article
            v-for="item in otherHonors"
            :key="item.honorId"
            class="event-other-honor hall-card-surface"
          >
            <div v-if="item.playerId" class="event-other-honor__avatar">
              <HallPlayerSilhouette
                :name="displayText(item.playerName, t('publicPages.hall.player'))"
                :avatar-url="item.playerAvatarUrl || ''"
                size="roster"
              />
            </div>
            <div v-else-if="item.teamId" class="event-other-honor__logo">
              <TeamLogo
                :team="{
                  id: String(item.teamId),
                  name: displayText(item.teamName, 'CN'),
                  shortName: displayText(item.teamName, 'CN'),
                  logoUrl: item.teamLogoUrl || '',
                }"
                size="md"
              />
            </div>
            <div class="event-other-honor__body">
              <p class="event-other-honor__code">{{ displayText(item.honorName) }}</p>
              <button
                v-if="item.playerId"
                type="button"
                class="event-other-honor__name"
                @click="openPlayer(item.playerId)"
              >
                {{ displayText(item.playerName) }}
              </button>
              <button
                v-else-if="item.teamId"
                type="button"
                class="event-other-honor__name"
                @click="openTeam(item.eventTeamId)"
              >
                {{ displayText(item.teamName) }}
              </button>
              <p class="event-other-honor__meta">
                {{ localizedText(item.seasonName, item.seasonNameEn, t('publicPages.hall.season')) }} · {{ awardDateText(item.awardDate) }}
              </p>
            </div>
          </article>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.event-honors-panel {
  width: 100%;
}

.event-champion-card {
  display: grid;
  grid-template-columns: 25% minmax(260px, 440px);
  align-items: center;
  aspect-ratio: 2048 / 768;
  overflow: hidden;
  background-color: #070707;
  background-image: var(--champion-bg);
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  color: #fff;
}

.event-champion-card__team {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 24px 16px;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: center;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }

  strong {
    font-size: 48px;
    font-weight: 900;
    line-height: 1;
  }
}

.event-champion-card__label {
  margin: 0;
  color: #e10613;
  font-size: 52px;
  font-weight: 900;
  line-height: 1;
}

.event-champion-card__label-en {
  margin: 6px 0 8px;
  color: #fff;
  font-size: 28px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.event-champion-card__logo {
  width: 210px;
  height: 210px;

  :deep(.team-logo--xl) {
    width: 210px;
    height: 210px;
  }
}

.event-champion-card__info {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 240px;
  padding: 28px 20px;
  text-align: center;

  &::before {
    content: '';
    position: absolute;
    top: 8%;
    bottom: 8%;
    left: 0;
    width: 1px;
    background: rgba(255, 255, 255, 0.55);
  }
}

.event-champion-card__tournament {
  margin: 0;
  font-size: clamp(36px, 3.2vw, 52px);
  font-style: italic;
  font-weight: 900;
  line-height: 1.15;
}

.event-champion-card__desc {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 20px;
  line-height: 1.5;
}

.event-champion-card__date-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  margin-top: 8px;

  span {
    color: rgba(255, 255, 255, 0.6);
    font-size: 16px;
  }

  strong {
    font-size: 28px;
  }
}

.event-champion-card__btn {
  align-self: center;
  margin-top: 16px;
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;

  span {
    margin-left: 6px;
  }
}

.event-other-honors {
  margin-top: 28px;
}

.event-other-honors__title {
  margin: 0 0 14px;
  color: #111;
  font-size: 18px;
  font-weight: 900;
}

.event-other-honors__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.event-other-honor {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
}

.event-other-honor__avatar,
.event-other-honor__logo {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}

.event-other-honor__code {
  margin: 0;
  color: var(--cnpa-red, #e10613);
  font-size: 12px;
  font-weight: 800;
}

.event-other-honor__name {
  display: block;
  margin: 6px 0 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: 18px;
  font-weight: 900;
  text-align: left;
  cursor: pointer;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}

.event-other-honor__meta {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.event-mvp {
  margin-top: 28px;
  container-type: inline-size;
}

.event-mvp__title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 8px;
  color: #111;
  font-size: 26px;
  font-weight: 900;
  line-height: 1;

  span {
    color: #e10613;
    font-size: 26px;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.01em;
  }
}

.event-mvp__card {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: stretch;
  height: calc((100cqw - 64px) / 5 * 3.7 / 3);
  overflow: hidden;
  background: #070707;
  color: #fff;
}

.event-mvp__photo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #070707;

  :deep(.hall-player-visual) {
    position: relative;
    inset: auto;
    display: flex;
    width: max-content;
    max-width: calc(100% - 16px);
    height: auto;
    max-height: calc(100% - 16px);
    min-height: 0;
    padding: 7px;
    border: 3px solid #e6c56b;
    box-shadow:
      inset 0 0 0 3px #070707,
      inset 0 0 0 6px #e6c56b;
    background: #070707;
  }

  :deep(.hall-player-visual__photo) {
    position: relative;
    inset: auto;
    display: block;
    width: auto;
    height: auto;
    max-width: calc(100cqw / 3 - 36px);
    max-height: calc((100cqw - 64px) / 5 * 3.7 / 3 - 48px);
    object-fit: contain;
    object-position: center center;
  }

  :deep(.hall-player-visual:not(:has(img))) {
    width: calc((100cqw - 64px) / 5 - 16px);
    height: calc((100cqw - 64px) / 5 * 3.7 / 3 - 16px);
  }
}

.event-mvp__photo,
.event-mvp__identity,
.event-mvp__stats {
  min-width: 0;
}

.event-mvp__identity {
  container-type: inline-size;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  text-align: center;
}

.event-mvp__name {
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: clamp(36px, 24cqw, 60px);
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1;
  text-align: center;
  text-transform: uppercase;
  white-space: nowrap;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }
}

.event-mvp__team {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 14px 0 0;
  color: #fff;
  font-size: clamp(20px, 9cqw, 28px);
  font-weight: 800;
  line-height: 1;

  :deep(.team-logo--xs) {
    width: 52px;
    height: 52px;
  }
}

.event-mvp__meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  margin: 18px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  text-align: center;
}

.event-mvp__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-content: center;
  gap: 28px 0;
  margin: 0;
  padding: 16px 8px;
  text-align: center;

  > div {
    position: relative;
    padding: 0 10px;
    text-align: center;
  }

  > div:not(:nth-child(3n + 1))::before {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    width: 1px;
    background: #e6c56b;
  }

  dt {
    color: rgba(255, 255, 255, 0.88);
    font-size: 18px;
    font-weight: 700;
  }

  dd {
    margin: 12px 0 0;
    color: #e10613;
    font-size: clamp(36px, 3.2vw, 52px);
    font-weight: 900;
    line-height: 1;
  }
}

.event-lineup {
  margin-top: 28px;
}

.event-lineup__title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 0 0 14px;
  color: #111;
  font-size: 22px;
  font-weight: 900;

  span {
    color: var(--cnpa-red, #e10613);
    font-size: 13px;
    font-style: italic;
    letter-spacing: 0.06em;
  }
}

.event-lineup__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.event-lineup__card {
  position: relative;
  aspect-ratio: 3 / 3.7;
  overflow: hidden;
  background: #070707;
  color: #fff;
  border: 3px solid #e6c56b;
  box-shadow:
    inset 0 0 0 3px #070707,
    inset 0 0 0 6px #e6c56b;
}

.event-lineup__photo {
  position: absolute;
  inset: 7px;
}

.event-lineup__photo :deep(.hall-player-visual) {
  width: 100%;
  height: 100%;
  min-height: 100%;
}

.event-lineup__meta {
  position: absolute;
  right: 7px;
  bottom: 7px;
  left: 7px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 56px 12px 18px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.78) 38%, #070707 100%);
  text-align: center;
}

.event-lineup__name {
  width: 100%;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font-family: inherit;
  font-size: clamp(22px, 1.8vw, 32px);
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: 0.02em;
  text-align: center;
  text-transform: uppercase;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }
}

.event-lineup__team {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin: 0;
  color: rgba(255, 255, 255, 0.94);
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.04em;
  line-height: 1.1;
  text-transform: uppercase;

  :deep(.team-logo--xs) {
    width: 32px;
    height: 32px;
  }
}

@media (max-width: 1023px) {
  .event-champion-card {
    grid-template-columns: 1fr;
  }

  .event-champion-card__info::before {
    display: none;
  }

  .event-other-honors__grid {
    grid-template-columns: 1fr;
  }

  .event-lineup__grid {
    grid-template-columns: repeat(5, minmax(148px, 1fr));
    overflow-x: auto;
  }
}
</style>
