<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getV4PublicStageList } from '@/api/v4Public'
import PlayerMatchTable from './PlayerMatchTable.vue'
import { fetchPlayerMatchesForEvents, hydratePlayerMatches } from '../playerMatchLoader'
import type { PlayerDetailData, PlayerMatchRecord } from '../types'

const props = withDefaults(defineProps<{
  detail: PlayerDetailData
  playerId: string
  scopeQuery?: boolean
}>(), {
  scopeQuery: true,
})

const pageSize = 10
const currentPage = ref(1)
const statusFilter = ref<'all' | 'scheduled' | 'finished'>('all')
const eventId = ref(props.detail.profile.eventTeamId || 'all')
const stageId = ref('all')
const stageOptions = ref<Array<{ value: string; label: string }>>([])
const matches = ref<PlayerMatchRecord[]>([])
const loadState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const loadError = ref('')
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
let reqId = 0
let stageReq = 0

const statusOptions = computed(() => [
  { value: 'all' as const, label: t('publicPages.player.allResults') },
  { value: 'scheduled' as const, label: t('publicPages.player.notStarted') },
  { value: 'finished' as const, label: t('publicPages.player.finished') },
])

const eventOptions = computed(() => props.detail.seasonStageOptions.filter((item) => item.eventTeamId))

const targets = computed(() => {
  if (eventId.value === 'all') return eventOptions.value
  return eventOptions.value.filter((item) => item.eventTeamId === eventId.value)
})

const filteredMatches = computed(() => {
  return [...matches.value]
    .filter((row) => {
      if (statusFilter.value !== 'all' && row.status !== statusFilter.value) return false
      if (stageId.value !== 'all' && String(row.stageId || '') !== stageId.value) return false
      return true
    })
    .sort((a, b) => (a.sortDate || '9999').localeCompare(b.sortDate || '9999'))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredMatches.value.length / pageSize)))
const pageMatches = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMatches.value.slice(start, start + pageSize)
})

function syncEventQuery(id: string) {
  if (id === 'all') return
  const option = eventOptions.value.find((item) => item.eventTeamId === id)
  if (!option || String(route.query.divisionId || '') === option.id) return
  router.replace({
    query: {
      ...route.query,
      divisionId: option.id,
    },
  })
}

async function loadStages(id: string) {
  const current = ++stageReq
  stageId.value = 'all'
  if (id === 'all') {
    stageOptions.value = []
    return
  }
  const event = eventOptions.value.find((item) => item.eventTeamId === id)
  if (!event) {
    stageOptions.value = []
    return
  }
  try {
    const res = await getV4PublicStageList(event.id)
    if (current !== stageReq) return
    stageOptions.value = [...(res.data ?? [])]
      .sort((a, b) => (Number(a.stageOrder) || 0) - (Number(b.stageOrder) || 0))
      .map((stage) => ({
        value: String(stage.stageId),
        label: stage.stageName || stage.stageTypeName || stage.competitionFormatName || String(stage.stageId),
      }))
  } catch {
    if (current !== stageReq) return
    stageOptions.value = []
  }
}

async function loadMatches() {
  const current = ++reqId
  const selected = targets.value
  if (!selected.length) {
    matches.value = []
    loadState.value = 'empty'
    return
  }
  loadState.value = 'loading'
  loadError.value = ''
  try {
    const rows = await fetchPlayerMatchesForEvents(selected)
    if (current !== reqId) return
    matches.value = rows
    loadState.value = rows.length ? 'success' : 'empty'
    void hydratePlayerMatches(
      rows,
      props.detail.profile,
      () => current === reqId,
      (matchId, nextRow) => {
        matches.value = matches.value.map((row) => (
          String(row.matchId) === matchId ? nextRow : row
        ))
      },
    )
  } catch (err) {
    if (current !== reqId) return
    matches.value = []
    loadState.value = 'error'
    loadError.value = err instanceof Error ? err.message : t('publicPages.team.loadMatchesFailed')
  }
}

watch([statusFilter, eventId, stageId], () => {
  currentPage.value = 1
})

watch(eventId, (id) => {
  void loadStages(id)
  if (props.scopeQuery) syncEventQuery(id)
})

watch(
  () => props.detail.profile.eventTeamId,
  (id) => {
    if (props.scopeQuery) return
    const next = id || 'all'
    if (eventId.value !== next) eventId.value = next
  },
)

watch(
  () => targets.value.map((item) => item.eventTeamId).join(','),
  () => {
    void loadMatches()
  },
  { immediate: true },
)

watch(totalPages, (pages) => {
  if (currentPage.value > pages) currentPage.value = pages
})

if (eventId.value !== 'all') void loadStages(eventId.value)

function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}
</script>

<template>
  <div class="player-detail-matches">
    <div class="player-detail-section-head">
      <div class="player-detail-section-head__group">
        <h2 class="player-detail-section-head__title">{{ t('publicPages.player.matchHistory') }}</h2>
        <span class="player-detail-section-head__sub">Match History</span>
      </div>
      <div class="player-detail-filter-row player-match-filters">
        <select v-model="statusFilter">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-if="scopeQuery" v-model="eventId">
          <option value="all">{{ t('publicPages.player.allEvents') }}</option>
          <option v-for="option in eventOptions" :key="option.eventTeamId" :value="option.eventTeamId">
            {{ option.label }}
          </option>
        </select>
        <select v-model="stageId">
          <option value="all">{{ t('publicPages.player.allStages') }}</option>
          <option v-for="option in stageOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
    </div>

    <div v-if="loadState === 'loading'" class="player-detail-empty-inline">{{ t('publicPages.player.loading') }}</div>
    <div v-else-if="loadState === 'error'" class="player-detail-empty-inline is-error">
      {{ loadError || t('publicPages.team.loadMatchesFailed') }}
    </div>
    <div v-else-if="filteredMatches.length === 0" class="player-detail-empty-inline">{{ t('publicPages.player.noMatchRecords') }}</div>

    <template v-else>
      <PlayerMatchTable :rows="pageMatches" />

      <div class="player-detail-pagination">
        <button
          type="button"
          class="player-detail-pagination__btn"
          :disabled="currentPage <= 1"
          @click="goPage(currentPage - 1)"
        >
          ‹
        </button>
        <button
          v-for="page in totalPages"
          :key="page"
          type="button"
          class="player-detail-pagination__btn"
          :class="{ 'is-active': page === currentPage }"
          @click="goPage(page)"
        >
          {{ page }}
        </button>
        <button
          type="button"
          class="player-detail-pagination__btn"
          :disabled="currentPage >= totalPages"
          @click="goPage(currentPage + 1)"
        >
          ›
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-empty-inline {
  padding: 32px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;

  &.is-error {
    color: #e10613;
  }
}

.player-detail-matches {
  padding: 8px 0 0;
  box-sizing: border-box;
}

.player-match-filters {
  flex: 1 1 320px;
  justify-content: flex-end;
  margin-bottom: 0;

  select {
    max-width: 280px;
  }
}

@media (max-width: 767px) {
  .player-detail-matches {
    padding: 20px 16px;
  }
}
</style>
