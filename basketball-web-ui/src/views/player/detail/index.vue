<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import PlayerHero from './components/PlayerHero.vue'
import PlayerTabs from './components/PlayerTabs.vue'
import PlayerOverview from './components/PlayerOverview.vue'
import PlayerMatches from './components/PlayerMatches.vue'
import PlayerData from './components/PlayerData.vue'
import PlayerCareer from './components/PlayerCareer.vue'
import { getPlayerDetail } from './mock/playerDetail'
import { normalizePlayerTab, resolvePlayerId, type PlayerDetailTab } from '@/utils/playerNavigate'

const route = useRoute()
const router = useRouter()

const playerId = computed(() => resolvePlayerId(route.params.playerId as string))
const playerDetail = computed(() => getPlayerDetail(playerId.value))
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
  () => route.params.playerId,
  () => {
    if (playerId.value) {
      document.title = `${playerDetail.value?.profile.displayName ?? '球员详情'} - CNPA 篮球赛事平台`
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="player-detail-page">
    <div class="site-shell player-detail-page__shell">
      <template v-if="playerDetail">
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
            <component :is="tabComponent" :detail="playerDetail" />
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
</style>
