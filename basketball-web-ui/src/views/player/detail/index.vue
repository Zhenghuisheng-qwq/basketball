<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import { getPublicPlayerDetail, getPublicPlayerMatchPage, getPublicPlayerSeasonStat } from '@/api/publicPlayer'
import PlayerHero from './components/PlayerHero.vue'
import PlayerTabs from './components/PlayerTabs.vue'
import PlayerOverview from './components/PlayerOverview.vue'
import PlayerMatches from './components/PlayerMatches.vue'
import PlayerData from './components/PlayerData.vue'
import PlayerCareer from './components/PlayerCareer.vue'
import {
  buildEmptyPlayerDetail,
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
    const [detailRes, matchRes, statRes] = await Promise.all([
      getPublicPlayerDetail(id),
      getPublicPlayerMatchPage(id, { currentPage: 1, pageSize: 100 }),
      getPublicPlayerSeasonStat(id),
    ])
    if (current !== reqId) return
    const rawDetail = detailRes.data
    if (!rawDetail) {
      playerDetail.value = null
      loadState.value = 'empty'
      return
    }
    const profile = mapPublicPlayerProfile(rawDetail)
    const detail = buildEmptyPlayerDetail(profile)
    const matches = (matchRes.data?.records ?? []).map((item) =>
      mapPublicPlayerMatch(item, profile.currentTeamId, profile.currentTeamName),
    )
    detail.recentMatches = matches.slice(0, 5)
    detail.allMatches = matches
    detail.careerStats = mapPublicPlayerSeasonStats(statRes.data ?? [])
    detail.seasonStageOptions = mapSeasonStageOptions(statRes.data ?? [])
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
  playerId,
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
