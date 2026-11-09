<script setup lang="ts">
import { computed, onMounted, provide, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import TeamLogo from '@/components/team/TeamLogo.vue'
import {
  getOwnPerformanceHonorPage,
  getOwnPerformancePlayer,
  getOwnPerformanceSeasonStat,
} from '@/api/accountPerformance'
import { getV4PublicMatchDetail, getV4PublicMatchList } from '@/api/v4Public'
import type { PublicPlayerSeasonStatVo } from '@/api/types/public'
import { useAccountStore } from '@/stores/account'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'
import { goToPlayerDetail, normalizePlayerTab, type PlayerDetailTab } from '@/utils/playerNavigate'
import PlayerTabs from '@/views/player/detail/components/PlayerTabs.vue'
import PlayerOverview from '@/views/player/detail/components/PlayerOverview.vue'
import PlayerMatches from '@/views/player/detail/components/PlayerMatches.vue'
import PlayerData from '@/views/player/detail/components/PlayerData.vue'
import PlayerHonors from '@/views/player/detail/components/PlayerHonors.vue'
import PlayerCareer from '@/views/player/detail/components/PlayerCareer.vue'
import {
  applyPlayerBoxScore,
  buildEmptyPlayerDetail,
  buildPlayerNameHistory,
  buildPlayerTeamHistory,
  findPlayerBoxScore,
  mapPublicPlayerHonors,
  mapPublicPlayerProfile,
  mapPublicPlayerSeasonStats,
  mapSeasonStageOptions,
} from '@/views/player/detail/mapPlayerDetail'
import type { PlayerDetailData, PlayerMatchRecord, PlayerProfile } from '@/views/player/detail/types'
import { mapV4MatchToTeamRecord } from '@/views/tournament/team/utils/mapTeamMatch'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const loading = ref(true)
const loadError = ref('')
const playerDetail = ref<PlayerDetailData | null>(null)
const selectedStatus = ref('')
const selectedEndTime = ref('')
let reqId = 0

provide('accountPerformanceFrame', true)

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter'), to: '/account/overview' },
  { label: t('accountShell.sidebar.performance') },
])

const playerId = computed(() => {
  const cert = accountStore.playerCert
  if (!cert?.playerId) return ''
  if (cert.certificationStatus !== 3 && !accountStore.isVerifiedPlayer) return ''
  return String(cert.playerId).trim()
})

const queryDivisionId = computed(() => String(route.query.divisionId || '').trim())
const activeTab = computed<PlayerDetailTab>(() => normalizePlayerTab(route.query.tab))

const selectedOptionId = computed(() => {
  const options = playerDetail.value?.seasonStageOptions || []
  if (options.some((item) => item.id === queryDivisionId.value)) return queryDivisionId.value
  return options[0]?.id || ''
})

const selectedEnded = computed(() => {
  const status = selectedStatus.value.toUpperCase()
  const endAt = Date.parse(selectedEndTime.value)
  return status === 'FINISHED' || status === 'ARCHIVED' || (Number.isFinite(endAt) && endAt < Date.now())
})

function officialStat(row: PublicPlayerSeasonStatVo) {
  return row.hasOfficialStat !== false && row.games != null
}

function resolveDivision(rows: PublicPlayerSeasonStatVo[], query: string) {
  const ids = rows.map((row) => String(row.divisionId || '')).filter(Boolean)
  if (query && ids.includes(query)) return query
  return ids[0] || ''
}

async function loadAccount() {
  if (!accountStore.currentAccount) {
    await accountStore.loadOverview()
  }
  if (!accountStore.playerCert?.playerId) {
    await accountStore.loadPlayerCertification()
  }
}

async function loadPerformance() {
  const current = ++reqId
  const id = playerId.value
  if (!id) {
    playerDetail.value = null
    return
  }
  const [detailRes, statRes, honorRes] = await Promise.all([
    getOwnPerformancePlayer(),
    getOwnPerformanceSeasonStat(),
    getOwnPerformanceHonorPage({ currentPage: 1, pageSize: 200 }),
  ])
  if (current !== reqId) return
  const rawDetail = detailRes.data
  if (!rawDetail) {
    playerDetail.value = null
    return
  }
  const seasonRows = statRes.data ?? []
  const preferredDivision = resolveDivision(seasonRows, queryDivisionId.value)
  const selected = seasonRows.find((row) => String(row.divisionId || '') === preferredDivision) || seasonRows[0]
  const profile = mapPublicPlayerProfile(rawDetail, { divisionId: preferredDivision })
  selectedStatus.value = ''
  selectedEndTime.value = ''
  if (selected) {
    profile.divisionId = String(selected.divisionId || preferredDivision)
    profile.seasonId = selected.seasonId ? String(selected.seasonId) : ''
    profile.seasonName = selected.seasonName || ''
    profile.divisionName = selected.divisionName || ''
    profile.seriesName = selected.seriesName || profile.seriesName
    selectedStatus.value = selected.divisionStatus || ''
    selectedEndTime.value = selected.matchEndTime || ''
    profile.currentTeamName = selected.teamName || t('accountShell.performancePage.noTeamSnapshot')
    profile.currentTeamLogoUrl = selected.teamLogoUrl || ''
    profile.eventTeamId = selected.eventTeamId ? String(selected.eventTeamId) : ''
    profile.currentTeamId = profile.eventTeamId
    profile.eventPlayerId = selected.eventPlayerId ? String(selected.eventPlayerId) : ''
  }
  const officialRows = seasonRows.filter(officialStat)
  const detail = buildEmptyPlayerDetail(profile)
  detail.profile = profile
  detail.careerStats = mapPublicPlayerSeasonStats(officialRows)
  detail.seasonStageOptions = mapSeasonStageOptions(seasonRows)
  detail.teamHistory = buildPlayerTeamHistory(profile, seasonRows)
  detail.nameHistory = buildPlayerNameHistory(profile, officialRows)
  detail.honors = mapPublicPlayerHonors(honorRes.data?.records ?? [])
  playerDetail.value = detail
  void loadPlayerMatches(current, profile)
}

async function loadPage() {
  loading.value = true
  loadError.value = ''
  try {
    await loadAccount()
    await loadPerformance()
  } catch (error) {
    playerDetail.value = null
    loadError.value = error instanceof Error ? error.message : t('accountShell.overview.loadFailed')
  } finally {
    loading.value = false
  }
}

async function loadPlayerMatches(current: number, profile: PlayerProfile) {
  if (!profile.divisionId || !profile.eventTeamId) {
    applyPlayerMatches([], profile)
    return
  }
  try {
    const v4Res = await getV4PublicMatchList({
      divisionId: profile.divisionId,
      eventTeamId: profile.eventTeamId,
    })
    if (current !== reqId) return
    const finishedMatchIds = new Set<string>()
    const matches = (v4Res.data ?? []).flatMap((item) => {
      const status = String(item.matchStatus || '').toUpperCase()
      const seriesStatus = String(item.seriesStatus || '').toUpperCase()
      const played = status === 'FINISHED' || status === 'FORFEITED' || status === 'FORFEIT'
      const seriesSkipped = Boolean(item.matchSeriesId) && seriesStatus === 'COMPLETED' && !played
      const mapped = mapV4MatchToTeamRecord(item, profile.eventTeamId || profile.currentTeamId)
      if (!played || seriesSkipped) return []
      finishedMatchIds.add(String(item.matchId))
      return [{
        id: mapped.id,
        matchId: mapped.id,
        date: mapped.date,
        tournamentName: profile.seriesName || profile.seasonName || t('publicPages.player.publicEvent'),
        stageName: [
          mapped.stage === 'playoffs' && mapped.stageHeadline && mapped.stageHeadline !== t('publicPages.team.playoffs')
            ? mapped.stageHeadline
            : '',
          mapped.playoffGame || '',
        ].filter(Boolean).join(' · ') || mapped.stageHeadline || mapped.competition || '--',
        teamName: profile.currentTeamName,
        teamId: profile.eventTeamId || profile.currentTeamId,
        teamLogoUrl: profile.currentTeamLogoUrl,
        opponentName: mapped.opponentName || t('publicPages.player.opponent'),
        opponentId: mapped.opponentId,
        opponentLogoUrl: mapped.opponentLogoUrl || '',
        result: mapped.result === 'win' ? 'WIN' : mapped.result === 'loss' ? 'LOSS' : 'UNDECIDED',
        resultLabel: mapped.resultType === 'EVENT_DISQUALIFICATION'
          ? (mapped.result === 'win' ? t('publicPages.player.opponentDqLoss') : t('publicPages.player.teamDqLoss'))
          : mapped.resultLabel,
        teamScore: mapped.teamScore ?? 0,
        opponentScore: mapped.opponentScore ?? 0,
        originalTeamScore: mapped.originalTeamScore,
        originalOpponentScore: mapped.originalOpponentScore,
        seriesScoreText: mapped.seriesScoreText,
        matchSeriesId: mapped.matchSeriesId,
        minutes: 0,
        points: 0,
        rebounds: 0,
        assists: 0,
        steals: 0,
        blocks: 0,
        turnovers: 0,
        fouls: 0,
        fieldGoalsMade: null,
        fieldGoalsAttempted: null,
        threePointsMade: null,
        threePointsAttempted: null,
        freeThrowsMade: null,
        freeThrowsAttempted: null,
      } satisfies PlayerMatchRecord]
    })
    matches.sort((a, b) => (b.date || '').localeCompare(a.date || ''))
    applyPlayerMatches(matches, profile)
    void hydrateMatchStatsInBackground(current, matches, profile, finishedMatchIds)
  } catch {
    if (current !== reqId) return
    applyPlayerMatches([], profile)
  }
}

function applyPlayerMatches(matches: PlayerMatchRecord[], profile: PlayerProfile) {
  if (!playerDetail.value) return
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
  playerDetail.value = {
    ...detail,
    recentMatches: allMatches.slice(0, 5),
    allMatches,
  }
}

async function hydrateMatchStatsInBackground(
  current: number,
  rows: PlayerMatchRecord[],
  profile: PlayerProfile,
  finishedMatchIds: Set<string>,
) {
  const queue = rows.filter((row) => finishedMatchIds.has(String(row.matchId)))
  const workerCount = Math.min(4, queue.length)
  if (!workerCount) return
  await Promise.all(Array.from({ length: workerCount }, async () => {
    while (queue.length) {
      const row = queue.shift()
      if (!row || current !== reqId) return
      try {
        const res = await getV4PublicMatchDetail(row.matchId)
        if (current !== reqId) return
        patchMatchRow(String(row.matchId), applyPlayerBoxScore(row, findPlayerBoxScore(res.data?.playerStats, profile)))
      } catch {
        // 单场失败不影响页面
      }
    }
  }))
}

function setTab(tab: PlayerDetailTab) {
  router.replace({ query: { ...route.query, tab } })
}

function selectDivision(event: Event) {
  const divisionId = (event.target as HTMLSelectElement).value
  if (!divisionId) return
  router.replace({ query: { ...route.query, divisionId } })
}

function openPublicPage() {
  if (!playerId.value) return
  goToPlayerDetail(router, playerId.value, 'overview')
}

onMounted(() => {
  void loadPage()
})

watch(queryDivisionId, () => {
  if (!playerId.value || loading.value) return
  void loadPerformance().catch((error) => {
    loadError.value = error instanceof Error ? error.message : t('accountShell.overview.loadFailed')
  })
})
</script>

<template>
  <div class="account-view account-performance">
    <AccountPageHeader
      :zh-title="t('accountShell.sidebar.performance')"
      en-title="MY PERFORMANCE"
      :breadcrumb-items="breadcrumbs"
    />

    <div v-if="loading" class="account-view__loading">{{ t('accountShell.common.loading') }}</div>

    <AccountEmptyState
      v-else-if="loadError"
      :message="t('accountShell.overview.loadFailed')"
      :subtitle="loadError"
    >
      <button type="button" class="account-btn-primary" @click="loadPage">{{ t('accountShell.common.retry') }}</button>
    </AccountEmptyState>

    <AccountEmptyState
      v-else-if="!playerId"
      :message="t('accountShell.performancePage.needVerification')"
    >
      <button type="button" class="account-btn-primary" @click="router.push(resolvePlayerIdentityRoute(accountStore))">
        {{ t('accountShell.performancePage.goVerify') }}
      </button>
    </AccountEmptyState>

    <template v-else-if="playerDetail">
      <div class="account-performance__toolbar">
        <label v-if="playerDetail.seasonStageOptions.length" class="account-performance__scope">
          <span>{{ t('publicPages.player.eventScope') }}</span>
          <select :value="selectedOptionId" @change="selectDivision">
            <option v-for="option in playerDetail.seasonStageOptions" :key="option.id" :value="option.id">
              {{ option.label }}
            </option>
          </select>
        </label>
        <p v-else class="account-performance__empty">{{ t('accountShell.performancePage.noParticipation') }}</p>
        <div v-if="playerDetail.seasonStageOptions.length" class="account-performance__team">
          <TeamLogo
            :team="{
              id: playerDetail.profile.eventTeamId || '',
              name: playerDetail.profile.currentTeamName,
              shortName: playerDetail.profile.currentTeamName,
              logoUrl: playerDetail.profile.currentTeamLogoUrl,
            }"
            size="xs"
          />
          <strong>{{ playerDetail.profile.currentTeamName }}</strong>
          <span>{{ selectedEnded ? t('accountEvents.common.finished') : t('accountEvents.common.inProgress') }}</span>
        </div>
        <button type="button" class="account-btn-secondary" @click="openPublicPage">
          {{ t('accountShell.performancePage.openPublic') }}
        </button>
      </div>

      <PlayerTabs :model-value="activeTab" @update:model-value="setTab" />
      <PlayerOverview v-if="activeTab === 'overview'" :detail="playerDetail" blank-without-games />
      <PlayerMatches v-else-if="activeTab === 'matches'" :detail="playerDetail" :player-id="playerId" />
      <PlayerData v-else-if="activeTab === 'data'" :detail="playerDetail" />
      <PlayerHonors v-else-if="activeTab === 'honors'" :detail="playerDetail" />
      <PlayerCareer v-else :detail="playerDetail" cumulative />
    </template>
  </div>
</template>

<style scoped lang="scss">
.account-performance__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 24px;
  margin-bottom: 20px;
}

.account-performance__scope,
.account-performance__team {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: #666;
  font-size: 13px;
}

.account-performance__scope select {
  min-width: 220px;
  max-width: 100%;
  padding: 8px 12px;
  border: 1px solid #111;
  border-radius: 0;
  background: #fff;
  color: #191919;
  font: inherit;
}

.account-performance__team strong {
  color: #171717;
  font-size: 15px;
}

.account-performance__empty {
  margin: 0;
  color: #666;
}

.account-performance__toolbar .account-btn-secondary {
  margin-left: auto;
}

.account-performance :deep(.player-detail-section-head__title),
.account-performance :deep(.player-detail-data__title),
.account-performance :deep(.player-detail-career__title),
.account-performance :deep(.player-honors__head h2) {
  font-size: 22px;
}

.account-performance :deep(.player-event-stats__item),
.account-performance :deep(.career-timeline__card),
.account-performance :deep(.player-detail-table) {
  border-radius: 0;
}
</style>
