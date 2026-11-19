<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  getOwnPerformanceHonorPage,
  getOwnPerformancePlayer,
  getOwnPerformanceSeasonStat,
} from '@/api/accountPerformance'
import {
  getPublicPlayerDetail,
  getPublicPlayerHonorPage,
  getPublicPlayerSeasonStat,
} from '@/api/publicPlayer'
import type { PublicHonorItemResp, PublicPlayerDetailVo, PublicPlayerSeasonStatVo } from '@/api/types/public'
import PlayerHero from './components/PlayerHero.vue'
import PlayerTabs from './components/PlayerTabs.vue'
import PlayerOverview from './components/PlayerOverview.vue'
import PlayerMatches from './components/PlayerMatches.vue'
import PlayerData from './components/PlayerData.vue'
import PlayerHonors from './components/PlayerHonors.vue'
import PlayerCareer from './components/PlayerCareer.vue'
import {
  buildEmptyPlayerDetail,
  buildPlayerEventOptions,
  buildPlayerNameHistory,
  buildPlayerTeamHistory,
  mapPublicPlayerHonors,
  mapPublicPlayerProfile,
  mapPublicPlayerSeasonStats,
  resolvePlayerEventDivision,
} from './mapPlayerDetail'
import { fetchPlayerMatchesForEvents, hydratePlayerMatches } from './playerMatchLoader'
import type { PlayerDetailData, PlayerMatchRecord, PlayerProfile } from './types'
import { normalizePlayerTab, resolvePlayerId, type PlayerDetailTab } from '@/utils/playerNavigate'

const props = defineProps<{
  sourcePlayerId?: string
  selfView?: boolean
}>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const playerId = computed(() => props.sourcePlayerId || resolvePlayerId(route.params.playerId as string))
const queryDivisionId = computed(() => String(route.query.divisionId || '').trim())
const playerDetail = ref<PlayerDetailData | null>(null)
const loadState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const loadError = ref('')
const rawDetail = ref<PublicPlayerDetailVo | null>(null)
const seasonRows = ref<PublicPlayerSeasonStatVo[]>([])
const honorRows = ref<PublicHonorItemResp[]>([])
let reqId = 0
let matchReq = 0

const activeTab = computed<PlayerDetailTab>(() => normalizePlayerTab(route.query.tab))

const tabComponent = computed(() => {
  switch (activeTab.value) {
    case 'matches':
      return PlayerMatches
    case 'data':
      return PlayerData
    case 'honors':
      return PlayerHonors
    case 'career':
      return PlayerCareer
    default:
      return PlayerOverview
  }
})

function composeDetail(divisionQuery: string): PlayerDetailData | null {
  const source = rawDetail.value
  if (!source) return null
  const options = buildPlayerEventOptions(source.currentTeams, seasonRows.value)
  const divisionId = resolvePlayerEventDivision(divisionQuery, options)
  const option = options.find((item) => item.id === divisionId)
  const profile = mapPublicPlayerProfile(source, { divisionId })
  const selectedStat = seasonRows.value.find((row) => String(row.divisionId || '') === divisionId)
  if (selectedStat) {
    profile.eventPlayerId = String(selectedStat.eventPlayerId || '')
    profile.divisionId = String(selectedStat.divisionId || divisionId)
    profile.seasonId = selectedStat.seasonId ? String(selectedStat.seasonId) : profile.seasonId
    profile.seasonName = selectedStat.seasonName || profile.seasonName
    profile.divisionName = selectedStat.divisionName || profile.divisionName
    profile.seriesName = selectedStat.seriesName || profile.seriesName
    if (selectedStat.teamName) profile.currentTeamName = selectedStat.teamName
    if (selectedStat.eventTeamId) {
      profile.eventTeamId = String(selectedStat.eventTeamId)
      profile.currentTeamId = String(selectedStat.eventTeamId)
    }
  } else if (option) {
    profile.divisionId = option.id
    profile.seasonId = option.seasonId || profile.seasonId
    profile.seasonName = option.seasonName || profile.seasonName
    profile.divisionName = option.divisionName || profile.divisionName
    profile.seriesName = option.seriesName || profile.seriesName
    profile.eventPlayerId = option.eventPlayerId || ''
    if (option.teamName) profile.currentTeamName = option.teamName
    if (option.eventTeamId) {
      profile.eventTeamId = option.eventTeamId
      profile.currentTeamId = option.eventTeamId
    }
    if (option.teamLogoUrl) profile.currentTeamLogoUrl = option.teamLogoUrl
  }
  const detail = buildEmptyPlayerDetail(profile)
  detail.profile = profile
  detail.careerStats = mapPublicPlayerSeasonStats(seasonRows.value)
  detail.seasonStageOptions = options
  detail.teamHistory = buildPlayerTeamHistory(profile, seasonRows.value)
  detail.nameHistory = buildPlayerNameHistory(profile, seasonRows.value)
  detail.honors = mapPublicPlayerHonors(honorRows.value)
  return detail
}

function publishDetail(divisionQuery: string) {
  const detail = composeDetail(divisionQuery)
  playerDetail.value = detail
  return detail
}

async function loadPlayer() {
  const id = playerId.value
  const current = ++reqId
  matchReq += 1
  if (!id) {
    rawDetail.value = null
    playerDetail.value = null
    loadState.value = 'empty'
    return
  }
  loadState.value = 'loading'
  loadError.value = ''
  try {
    const [detailRes, statRes, honorRes] = props.selfView
      ? await Promise.all([
          getOwnPerformancePlayer(),
          getOwnPerformanceSeasonStat(),
          getOwnPerformanceHonorPage({ currentPage: 1, pageSize: 200 }),
        ])
      : await Promise.all([
          getPublicPlayerDetail(id),
          getPublicPlayerSeasonStat(id),
          getPublicPlayerHonorPage(id, { currentPage: 1, pageSize: 200 }),
        ])
    if (current !== reqId) return
    if (!detailRes.data) {
      rawDetail.value = null
      playerDetail.value = null
      loadState.value = 'empty'
      return
    }
    rawDetail.value = detailRes.data
    seasonRows.value = statRes.data ?? []
    honorRows.value = honorRes.data?.records ?? []
    const detail = publishDetail(queryDivisionId.value)
    loadState.value = 'success'
    if (!props.sourcePlayerId && detail) {
      document.title = t('common.documentTitle', { page: detail.profile.displayName })
    }
    if (detail) void loadPlayerMatches(detail)
  } catch (err) {
    if (current !== reqId) return
    playerDetail.value = null
    loadState.value = 'error'
    loadError.value = err instanceof Error ? err.message : t('publicPages.player.loadFailed')
  }
}

async function loadPlayerMatches(detail: PlayerDetailData) {
  const current = ++matchReq
  const profile = detail.profile
  const option = detail.seasonStageOptions.find((item) => item.id === String(profile.divisionId || ''))
  if (!option?.eventTeamId) {
    applyPlayerMatches([], profile)
    return
  }
  try {
    const matches = await fetchPlayerMatchesForEvents([option])
    if (current !== matchReq) return
    const hasUpcoming = matches.some((row) => row.status === 'scheduled')
    const inPlayoffs = matches.some((row) => row.stageName.includes(t('publicPages.player.playoffs')))
    if (matches.length > 0 && !hasUpcoming) profile.currentStage = ''
    else if (inPlayoffs) profile.currentStage = t('publicPages.player.playoffs')
    const ordered = [...matches].sort((a, b) => (b.sortDate || '').localeCompare(a.sortDate || ''))
    applyPlayerMatches(ordered, profile)
    const recent = ordered.slice(0, 5)
    void hydratePlayerMatches(
      recent,
      profile,
      () => current === matchReq,
      patchMatchRow,
    )
  } catch {
    if (current !== matchReq) return
    applyPlayerMatches([], profile)
  }
}

function applyPlayerMatches(matches: PlayerMatchRecord[], profile: PlayerProfile) {
  if (!playerDetail.value || playerDetail.value.profile.id !== profile.id) return
  playerDetail.value = {
    ...playerDetail.value,
    profile,
    recentMatches: matches.slice(0, 5),
    allMatches: matches,
  }
}

function patchMatchRow(matchId: string, nextRow: PlayerMatchRecord) {
  const detail = playerDetail.value
  if (!detail) return
  const allMatches = detail.allMatches.map((row) => (
    String(row.matchId) === matchId ? nextRow : row
  ))
  const recentIds = new Set(detail.recentMatches.map((row) => String(row.matchId)))
  playerDetail.value = {
    ...detail,
    recentMatches: allMatches.filter((row) => recentIds.has(String(row.matchId))),
    allMatches,
  }
}

function setTab(tab: PlayerDetailTab) {
  router.replace({
    query: {
      ...route.query,
      tab,
    },
  })
}

watch(playerId, () => {
  void loadPlayer()
}, { immediate: true })

watch(queryDivisionId, () => {
  if (loadState.value !== 'success' || !rawDetail.value) return
  const detail = publishDetail(queryDivisionId.value)
  if (detail) void loadPlayerMatches(detail)
})
</script>

<template>
  <div class="player-detail-page">
    <div class="site-shell player-detail-page__shell">
      <div v-if="loadState === 'loading'" class="player-detail-empty">{{ t('publicPages.player.loading') }}</div>
      <div v-else-if="loadState === 'error'" class="player-detail-empty is-error">
        {{ loadError || t('publicPages.player.loadFailed') }}
      </div>
      <div v-else-if="!playerDetail" class="player-detail-empty">
        {{ t('publicPages.player.notFound') }}
      </div>
      <template v-else>
        <PlayerHero :profile="playerDetail.profile" />
        <div class="player-detail-page__paper">
          <div class="player-detail-page__paper-body">
            <div class="site-shell-inner player-detail-page__paper-inner">
              <PlayerTabs :model-value="activeTab" @update:model-value="setTab" />
              <component :is="tabComponent" :detail="playerDetail" :player-id="playerId!" />
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use './styles/player-detail.scss';

.player-detail-empty.is-error {
  color: #e10613;
}
</style>
