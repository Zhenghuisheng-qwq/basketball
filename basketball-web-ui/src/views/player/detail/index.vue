<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  getPublicPlayerDetail,
  getPublicPlayerHonorPage,
  getPublicPlayerMatchPage,
  getPublicPlayerSeasonStat,
} from '@/api/publicPlayer'
import { getV4PublicMatchList } from '@/api/v4Public'
import { mapV4MatchToTeamRecord } from '@/views/tournament/team/utils/mapTeamMatch'
import PlayerHero from './components/PlayerHero.vue'
import PlayerTabs from './components/PlayerTabs.vue'
import PlayerOverview from './components/PlayerOverview.vue'
import PlayerMatches from './components/PlayerMatches.vue'
import PlayerData from './components/PlayerData.vue'
import PlayerCareer from './components/PlayerCareer.vue'
import {
  buildEmptyPlayerDetail,
  mapPublicPlayerHonors,
  mapPublicPlayerMatch,
  mapPublicPlayerProfile,
  mapPublicPlayerSeasonStats,
  mapSeasonStageOptions,
} from './mapPlayerDetail'
import type { PlayerDetailData } from './types'
import { normalizePlayerTab, resolvePlayerId, type PlayerDetailTab } from '@/utils/playerNavigate'

const route = useRoute()
const router = useRouter()

const playerId = computed(() => resolvePlayerId(route.params.playerId as string))
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
    const [detailRes, matchRes, statRes, honorRes] = await Promise.all([
      getPublicPlayerDetail(id),
      getPublicPlayerMatchPage(id, { currentPage: 1, pageSize: 100 }),
      getPublicPlayerSeasonStat(id),
      getPublicPlayerHonorPage(id, { currentPage: 1, pageSize: 50 }),
    ])
    if (current !== reqId) return
    const rawDetail = detailRes.data
    if (!rawDetail) {
      playerDetail.value = null
      loadState.value = 'empty'
      return
    }
    const profile = mapPublicPlayerProfile(rawDetail, { divisionId: queryDivisionId.value })
    const detail = buildEmptyPlayerDetail(profile)
    let matches = (matchRes.data?.records ?? []).map((item) =>
      mapPublicPlayerMatch(item, profile.currentTeamId, profile.currentTeamName),
    )
    const contextDivisionId = queryDivisionId.value || profile.divisionId
    if (contextDivisionId && profile.eventTeamId) {
      try {
        const v4Res = await getV4PublicMatchList({
          divisionId: contextDivisionId,
          eventTeamId: profile.eventTeamId,
        })
        matches = (v4Res.data ?? []).map((item) => {
          const mapped = mapV4MatchToTeamRecord(item, profile.eventTeamId || profile.currentTeamId)
          return {
            id: mapped.id,
            matchId: mapped.id,
            date: mapped.date,
            tournamentName: profile.seriesName || profile.seasonName || '公开赛事',
            stageName: mapped.competition,
            teamName: profile.currentTeamName,
            teamId: profile.eventTeamId || profile.currentTeamId,
            teamLogoUrl: profile.currentTeamLogoUrl,
            opponentName: mapped.opponentName || '对手',
            opponentId: mapped.opponentId,
            opponentLogoUrl: mapped.opponentLogoUrl || '',
            result: mapped.result === 'win' ? 'WIN' : 'LOSS',
            resultLabel: mapped.resultType === 'EVENT_DISQUALIFICATION'
              ? (mapped.result === 'win' ? '对手取消资格判负' : '球队取消资格判负')
              : mapped.resultLabel,
            teamScore: mapped.teamScore ?? 0,
            opponentScore: mapped.opponentScore ?? 0,
            originalTeamScore: mapped.originalTeamScore,
            originalOpponentScore: mapped.originalOpponentScore,
            seriesScoreText: mapped.seriesScoreText,
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
          }
        })
      } catch {
        // keep global match fallback
      }
    }
    detail.recentMatches = matches.slice(0, 5)
    detail.allMatches = matches
    const seasonStats = (statRes.data ?? []).filter((row) =>
      queryDivisionId.value ? String(row.divisionId || '') === queryDivisionId.value : true,
    )
    detail.careerStats = mapPublicPlayerSeasonStats(seasonStats.length ? seasonStats : (statRes.data ?? []))
    detail.seasonStageOptions = mapSeasonStageOptions(seasonStats.length ? seasonStats : (statRes.data ?? []))
    detail.honors = mapPublicPlayerHonors(honorRes.data?.records ?? [])
    playerDetail.value = detail
    loadState.value = 'success'
    document.title = `${profile.displayName} - CNPA 篮球赛事平台`
  } catch (err) {
    if (current !== reqId) return
    playerDetail.value = null
    loadState.value = 'error'
    loadError.value = err instanceof Error ? err.message : '加载球员详情失败'
  }
}

function setTab(tab: PlayerDetailTab) {
  router.replace({
    name: 'PlayerDetail',
    params: { playerId: String(playerId.value) },
    query: {
      ...route.query,
      tab,
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
      <div v-if="loadState === 'loading'" class="player-detail-empty">加载中...</div>
      <div v-else-if="loadState === 'error'" class="player-detail-empty is-error">
        {{ loadError || '加载球员详情失败' }}
      </div>
      <template v-else-if="playerDetail">
        <PlayerHero :profile="playerDetail.profile" />
      </template>
      <div v-else class="player-detail-empty">球员不存在，请返回数据中心或球队页面重新选择。</div>
    </div>

    <div v-if="playerDetail" class="player-detail-page__paper">
      <PaperTear position="top" tone="light" />
      <div class="player-detail-page__paper-body">
        <div class="site-shell player-detail-page__paper-inner">
          <div class="site-shell-inner">
            <PlayerTabs :model-value="activeTab" @update:model-value="setTab" />
            <component :is="tabComponent" :detail="playerDetail" :player-id="playerId!" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use './styles/player-detail.scss';

.player-detail-page__paper {
  position: relative;

  :deep(.paper-tear) {
    height: 0;
  }

  :deep(.torn-edge-top) {
    transform: translateY(-48%);
  }

  :deep(.torn-edge-main) {
    fill: var(--cnpa-paper-bg) !important;
  }
}

.player-detail-empty.is-error {
  color: #e10613;
}
</style>
