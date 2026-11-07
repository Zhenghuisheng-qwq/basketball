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
import { getV4PublicMatchDetail, getV4PublicMatchList } from '@/api/v4Public'
import { mapV4MatchToTeamRecord } from '@/views/tournament/team/utils/mapTeamMatch'
import PlayerHero from './components/PlayerHero.vue'
import PlayerTabs from './components/PlayerTabs.vue'
import PlayerOverview from './components/PlayerOverview.vue'
import PlayerMatches from './components/PlayerMatches.vue'
import PlayerData from './components/PlayerData.vue'
import PlayerHonors from './components/PlayerHonors.vue'
import PlayerCareer from './components/PlayerCareer.vue'
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
} from './mapPlayerDetail'
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
let reqId = 0

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

async function loadPlayer() {
  const id = playerId.value
  const current = ++reqId
  if (!id) {
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
    const rawDetail = detailRes.data
    if (!rawDetail) {
      playerDetail.value = null
      loadState.value = 'empty'
      return
    }
    const seasonRows = statRes.data ?? []
    const preferredDivision = queryDivisionId.value || String(seasonRows[0]?.divisionId || '')
    const profile = mapPublicPlayerProfile(rawDetail, { divisionId: preferredDivision })
    const scopedStats = seasonRows.filter((row) =>
      preferredDivision ? String(row.divisionId || '') === preferredDivision : true,
    )
    const selectedStat = scopedStats[0] || seasonRows[0]
    const eventPlayerId = String(selectedStat?.eventPlayerId || '').trim()
    if (eventPlayerId) {
      profile.eventPlayerId = eventPlayerId
    }
    if (selectedStat) {
      profile.divisionId = String(selectedStat.divisionId || preferredDivision)
      profile.seasonId = selectedStat.seasonId ? String(selectedStat.seasonId) : profile.seasonId
      profile.seasonName = selectedStat.seasonName || profile.seasonName
      profile.divisionName = selectedStat.divisionName || profile.divisionName
      profile.seriesName = selectedStat.seriesName || profile.seriesName
      if (selectedStat.teamName) {
        profile.currentTeamName = selectedStat.teamName
      }
      if (selectedStat.eventTeamId) {
        profile.eventTeamId = String(selectedStat.eventTeamId)
        profile.currentTeamId = String(selectedStat.eventTeamId)
      }
    }
    const detail = buildEmptyPlayerDetail(profile)
    detail.profile = profile
    detail.careerStats = mapPublicPlayerSeasonStats(seasonRows)
    detail.seasonStageOptions = mapSeasonStageOptions(seasonRows)
    detail.teamHistory = buildPlayerTeamHistory(profile, seasonRows)
    detail.nameHistory = buildPlayerNameHistory(profile, seasonRows)
    detail.honors = mapPublicPlayerHonors(honorRes.data?.records ?? [])
    playerDetail.value = detail
    loadState.value = 'success'
    if (!props.sourcePlayerId) {
      document.title = t('common.documentTitle', { page: profile.displayName })
    }
    void loadPlayerMatches(current, profile)
  } catch (err) {
    if (current !== reqId) return
    playerDetail.value = null
    loadState.value = 'error'
    loadError.value = err instanceof Error ? err.message : t('publicPages.player.loadFailed')
  }
}

async function loadPlayerMatches(current: number, profile: PlayerProfile) {
  const contextDivisionId = queryDivisionId.value || profile.divisionId
  if (!contextDivisionId || !profile.eventTeamId) return
  try {
    const v4Res = await getV4PublicMatchList({
      divisionId: contextDivisionId,
      eventTeamId: profile.eventTeamId,
    })
    if (current !== reqId) return
    const finishedMatchIds = new Set<string>()
    let inPlayoffs = false
    let hasLiveMatch = false
    const sourceMatches = v4Res.data ?? []
    const matches = sourceMatches.flatMap((item) => {
      const status = String(item.matchStatus || '').toUpperCase()
      const seriesStatus = String(item.seriesStatus || '').toUpperCase()
      const played = status === 'FINISHED' || status === 'FORFEITED' || status === 'FORFEIT'
      const seriesSkipped = Boolean(item.matchSeriesId) && seriesStatus === 'COMPLETED' && !played
      const mapped = mapV4MatchToTeamRecord(item, profile.eventTeamId || profile.currentTeamId)
      if (mapped.stage === 'playoffs') inPlayoffs = true
      if (!played && !seriesSkipped) hasLiveMatch = true
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
        ].filter(Boolean).join(' · ')
          || mapped.stageHeadline
          || mapped.competition
          || '--',
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
    if (sourceMatches.length > 0 && !hasLiveMatch) {
      profile.currentStage = ''
    } else if (inPlayoffs) {
      profile.currentStage = t('publicPages.player.playoffs')
    }
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
        patchMatchRow(
          String(row.matchId),
          applyPlayerBoxScore(row, findPlayerBoxScore(res.data?.playerStats, profile)),
        )
      } catch {
        // 单场失败不影响页面
      }
    }
  }))
}

const selectedDivisionId = computed(() => queryDivisionId.value || playerDetail.value?.profile.divisionId || '')

function setTab(tab: PlayerDetailTab) {
  router.replace({
    query: {
      ...route.query,
      tab,
    },
  })
}

function selectDivision(event: Event) {
  const divisionId = (event.target as HTMLSelectElement).value
  router.replace({
    query: {
      ...route.query,
      divisionId: divisionId || undefined,
    },
  })
}

watch(
  [playerId, queryDivisionId],
  () => {
    void loadPlayer()
  },
  { immediate: true },
)
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
              <label v-if="playerDetail.seasonStageOptions.length" class="player-event-scope">
                <span>{{ t('publicPages.player.eventScope') }}</span>
                <select :value="selectedDivisionId" @change="selectDivision">
                  <option
                    v-for="option in playerDetail.seasonStageOptions"
                    :key="option.id"
                    :value="option.id"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </label>
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

.player-event-scope {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  color: #777;
  font-size: 12px;

  select {
    min-width: 220px;
    max-width: 100%;
    padding: 8px 12px;
    border: 1px solid #ccc;
    background: #fff;
    color: #191919;
    font: inherit;
  }
}
</style>
