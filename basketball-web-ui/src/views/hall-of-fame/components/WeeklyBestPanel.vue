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
import type { PublicHonorItemResp, PublicPlayerSeasonStatVo } from '@/api/types/public'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, formatDateTime } from '@/utils/publicDisplay'

defineProps<{
  page: number
}>()

const router = useRouter()
const { t } = useI18n()

const allItems = ref<PublicHonorItemResp[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const seasonFilter = ref('ALL')
const weekCursor = ref(0)
const stat = ref<PublicPlayerSeasonStatVo | null>(null)
let reqId = 0
let statReq = 0

const weeklyItems = computed(() =>
  allItems.value.filter((item) => (item.honorCode || '').toUpperCase() === 'WEEKLY_BEST'),
)

const seasonOptions = computed(() => {
  const map = new Map<string, string>()
  weeklyItems.value.forEach((item) => {
    const id = String(item.seasonId || '')
    if (!id) return
    map.set(id, displayText(item.seasonName, t('publicPages.hall.season')))
  })
  return [
    { value: 'ALL', label: t('publicPages.hall.allSeasons') },
    ...Array.from(map, ([value, label]) => ({ value, label })),
  ]
})

const weeks = computed(() => {
  const list = weeklyItems.value.filter((item) => {
    if (seasonFilter.value === 'ALL') return true
    return String(item.seasonId || '') === seasonFilter.value
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
    { key: 'games', label: t('publicPages.player.games'), value: formatCount(row?.games), accent: false },
    { key: 'pts', label: t('publicPages.player.avgPoints'), value: formatAvg(row?.avgPoints), accent: true },
    { key: 'reb', label: t('publicPages.player.avgRebounds'), value: formatAvg(row?.avgRebounds), accent: true },
    { key: 'ast', label: t('publicPages.player.avgAssists'), value: formatAvg(row?.avgAssists), accent: true },
    { key: 'stl', label: t('publicPages.team.avgSteals'), value: formatAvg(row?.avgSteals), accent: true },
    { key: 'fg', label: t('publicPages.player.fgPct'), value: formatPct(row?.fieldGoalPct), accent: true },
  ]
})

async function loadAll() {
  const currentReq = ++reqId
  state.value = 'loading'
  errorMessage.value = ''
  try {
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

function updateSeason(value: string | number) {
  seasonFilter.value = String(value)
  weekCursor.value = Math.max(0, weeks.value.length - 1)
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
      <article class="weekly-feature hall-card-surface">
        <div class="weekly-feature__hero">
          <div class="weekly-feature__mark">
            <span>PLAYER</span>
            <span>OF THE</span>
            <strong>WEEK</strong>
          </div>
          <div class="weekly-feature__photo">
            <HallPlayerSilhouette
              :name="displayText(current.playerName, t('publicPages.hall.player'))"
              :avatar-url="current.playerAvatarUrl || ''"
              size="feature"
            />
          </div>
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
          <p v-if="current.honorTitle" class="weekly-feature__meta">{{ current.honorTitle }}</p>
          <p class="weekly-feature__meta">{{ displayText(current.seasonName || current.tournamentName, t('publicPages.hall.season')) }}</p>
          <p v-if="awardRange(current)" class="weekly-feature__meta">{{ awardRange(current) }}</p>
        </div>
        <dl class="weekly-feature__stats">
          <div v-for="box in statBoxes" :key="box.key">
            <dt>{{ box.label }}</dt>
            <dd :class="{ 'is-accent': box.accent }">{{ box.value }}</dd>
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
  grid-template-columns: minmax(360px, 1.2fr) minmax(170px, 0.75fr) minmax(240px, 1fr);
  gap: 12px 20px;
  align-items: center;
  min-height: 196px;
  padding: 8px 22px 8px 8px;
}

.weekly-feature__hero {
  display: grid;
  grid-template-columns: auto minmax(88px, 1fr);
  align-items: end;
  min-height: 180px;
  min-width: 0;
}

.weekly-feature__mark {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  padding: 0 12px 0 16px;
  color: #fff;
  font-size: 30px;
  font-weight: 900;
  line-height: 0.92;
  letter-spacing: 0;
  white-space: nowrap;

  strong {
    color: var(--cnpa-red, #e10613);
  }
}

.weekly-feature__photo {
  height: 180px;
  min-width: 0;
  overflow: hidden;
}

.weekly-feature__name,
.weekly-feature__team {
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }
}

.weekly-feature__name {
  font-size: clamp(28px, 3vw, 40px);
  font-weight: 900;
  letter-spacing: 0.04em;
}

.weekly-feature__team {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 14px;
  font-weight: 800;
}

.weekly-feature__meta,
.weekly-feature__code {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  font-weight: 700;
}

.weekly-feature__code {
  color: var(--cnpa-red, #e10613);
}

.weekly-feature__stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin: 0;

  dt {
    color: rgba(255, 255, 255, 0.72);
    font-size: 12px;
    font-weight: 700;
  }

  dd {
    margin: 8px 0 0;
    color: #fff;
    font-size: 32px;
    font-weight: 900;
    line-height: 1;
  }

  .is-accent {
    color: var(--cnpa-red, #e10613);
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
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .weekly-feature__mark {
    white-space: normal;
  }

  .weekly-feature__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .weekly-feature__hero {
    grid-template-columns: 1fr;
  }
}
</style>
