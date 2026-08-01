<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import CurrentMatchVersusCard from './CurrentMatchVersusCard.vue'
import MatchDetailTabs, { type MatchDetailTab } from './MatchDetailTabs.vue'
import MatchBothRostersPanel from './MatchBothRostersPanel.vue'
import MatchDataTabPanel from './MatchDataTabPanel.vue'
import { getEventTeamDetail, getEventTeamMatchDetail } from '@/api/eventParticipation'
import type { EventTeamDetailDto } from '@/api/types/eventParticipation'
import type { PlayerMatchTaskItem } from '@/api/matchData'
import { useAccountStore } from '@/stores/account'
import { buildMatchDataListRoute } from '@/utils/participationMappers'
import { resolveMatchDataActions } from '@/utils/matchDataActions'
import { resolveMatchDataTabMessage } from '@/utils/matchDataTabHelpers'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const loading = ref(false)
const loadError = ref('')
const eventTeamDetail = ref<EventTeamDetailDto | null>(null)
const match = ref<PlayerMatchTaskItem | null>(null)
const activeTab = ref<MatchDetailTab>('overview')

const eventTeamId = computed(() => String(route.params.eventTeamId || ''))
const matchId = computed(() => String(route.params.matchId || ''))

const matchActions = computed(() => resolveMatchDataActions(match.value))
const matchDataMessage = computed(() => resolveMatchDataTabMessage(match.value))
const identityText = computed(() => (match.value?.isCaptain ? '赛事队长' : '队员'))

const breadcrumbItems = computed(() => [
  { label: '首页', to: '/home' },
  { label: '个人中心', to: '/account/overview' },
  { label: '我的赛事', to: '/account/my-events?tab=joined' },
  {
    label: eventTeamDetail.value?.tournamentName || eventTeamDetail.value?.seasonName || '我的比赛',
    to: buildMatchDataListRoute(eventTeamId.value),
  },
  { label: '比赛详情' },
])

function parseTab(value: unknown): MatchDetailTab {
  if (value === 'roster' || value === 'data' || value === 'overview') {
    return value
  }
  return 'overview'
}

function setTab(tab: MatchDetailTab) {
  activeTab.value = tab
  router.replace({
    path: route.path,
    query: { ...route.query, tab },
  })
}

async function loadPage() {
  if (!eventTeamId.value || !matchId.value) {
    loadError.value = '缺少比赛参数'
    return
  }
  if (!accountStore.isVerifiedPlayer) {
    ElMessage.warning('请先完成球员身份认证')
    router.replace('/account/player-identity')
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    const [detailRes, matchRes] = await Promise.all([
      getEventTeamDetail(eventTeamId.value),
      getEventTeamMatchDetail(eventTeamId.value, matchId.value),
    ])
    eventTeamDetail.value = detailRes.data ?? null
    match.value = matchRes.data ?? null
    if (!match.value) {
      loadError.value = '未找到比赛信息'
    }
  } catch (error) {
    match.value = null
    loadError.value = error instanceof Error ? error.message : '加载失败'
    if (loadError.value.includes('403') || loadError.value.includes('无权')) {
      ElMessage.warning('无权查看该比赛')
      router.replace('/account/my-events?tab=joined')
    }
  } finally {
    loading.value = false
  }
}

function goAction(path?: string) {
  if (!path) return
  router.push(path)
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = parseTab(tab)
  },
  { immediate: true },
)

watch(() => `${eventTeamId.value}-${matchId.value}`, () => {
  void loadPage()
})

onMounted(() => {
  void loadPage()
})
</script>

<template>
  <div class="account-view match-detail-view">
    <AccountPageHeader
      zh-title="比赛详情"
      en-title="MATCH DETAIL"
      :breadcrumb-items="breadcrumbItems"
    />

    <div class="match-detail-view__hero-wrap">
      <EventTeamHeroStrip
        :tournament-name="match?.eventName || eventTeamDetail?.tournamentName || eventTeamDetail?.seasonName || '赛事'"
        :team-code="match?.myTeamNameSnapshot || eventTeamDetail?.eventTeamNo || eventTeamDetail?.teamName || '我的球队'"
        description="查看当前比赛基础信息、双方阵容与比赛数据入口"
        :status-text="`${identityText} · ${match?.matchStatusText || '赛程已发布'}`"
        status-type="success"
      />
      <MatchDetailTabs
        v-if="match && !loading && !loadError"
        class="match-detail-view__tabs"
        :active-tab="activeTab"
        @change="setTab"
      />
    </div>

    <div v-if="loading" class="match-detail-view__state">比赛详情加载中...</div>
    <AccountEmptyState v-else-if="loadError" :message="loadError" />

    <template v-else-if="match">
      <div class="match-detail-view__panel">
        <section v-show="activeTab === 'overview'" class="match-detail-view__section">
          <CurrentMatchVersusCard :match="match" :identity-text="identityText" />
          <div class="match-detail-view__section-footer">
            <button
              type="button"
              class="match-detail-view__back-btn"
              @click="router.push(buildMatchDataListRoute(eventTeamId))"
            >
              返回我的比赛
            </button>
          </div>
        </section>

        <section v-show="activeTab === 'roster'" class="match-detail-view__section">
          <p v-if="match.rosterStatusText" class="match-detail-view__section-note">
            阵容状态：{{ match.rosterStatusText }} · 数据来自本场比赛 EventPlayer 快照
          </p>
          <MatchBothRostersPanel
            :home-team-name="match.homeTeamNameSnapshot || match.homeTeamName"
            :away-team-name="match.awayTeamNameSnapshot || match.awayTeamName"
            :home-roster="match.homeRosterList"
            :away-roster="match.awayRosterList"
            :match-platform="match.platformCode"
          />
        </section>

        <section v-show="activeTab === 'data'" class="match-detail-view__section">
          <MatchDataTabPanel
            :match="match"
            :message="matchDataMessage"
            :actions="matchActions"
            :identity-text="identityText"
            @action="goAction"
            @submitted="loadPage"
          />
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.match-detail-view {
  &__hero-wrap {
    position: relative;
    margin-bottom: 0;
  }

  &__tabs {
    position: relative;
    margin-top: -1px;
    margin-left: 0;
  }

  &__state {
    padding: 24px 0;
    color: #666;
  }

  &__panel {
    margin-top: 16px;
    padding: 20px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 2px;
    background: rgba(255, 255, 255, 0.92);
  }

  &__section {
    min-height: 120px;
  }

  &__section-note {
    margin: 0 0 16px;
    font-size: 13px;
    color: #666;
    line-height: 1.6;
  }

  &__section-footer {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
  }

  &__back-btn {
    min-height: 36px;
    padding: 0 16px;
    border: 1px solid rgba(20, 20, 20, 0.22);
    border-radius: 2px;
    background: #fff;
    color: #222;
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease;

    &:hover {
      color: #e10613;
      border-color: rgba(225, 6, 19, 0.45);
      background: rgba(225, 6, 19, 0.04);
    }
  }
}

@media (max-width: 767px) {
  .match-detail-view__panel {
    padding: 16px 12px;
  }

  .match-detail-view__tabs {
    width: 100%;
  }
}
</style>
