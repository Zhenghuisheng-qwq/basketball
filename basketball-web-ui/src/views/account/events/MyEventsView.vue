<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import MyEventsTabs from '@/components/account/events/MyEventsTabs.vue'
import AvailableEventTable from '@/components/account/events/AvailableEventTable.vue'
import MyApplicationList from '@/components/account/events/MyApplicationList.vue'
import ParticipatingEventList from '@/components/account/events/ParticipatingEventList.vue'
import { getAccountTournamentPage } from '@/api/accountTournament'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'
import { useAccountStore } from '@/stores/account'
import {
  getBannerCopy,
  getFooterNote,
  getTabMeta,
} from '@/mock/accountEventsMock'
import type { AvailableEvent, MyApplication, MyEventsTabKey, ParticipatingEvent } from '@/types/accountEvents'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const loading = ref(false)
const loadError = ref('')
const participatingEvents = ref<ParticipatingEvent[]>([])

/** 报名/可参与列表属后续阶段，本轮不使用 Mock 数据 */
const availableEvents = ref<AvailableEvent[]>([])
const myApplications = ref<MyApplication[]>([])

const activeTab = computed<MyEventsTabKey>(() => {
  const tab = route.query.tab
  if (tab === 'applications' || tab === 'joined' || tab === 'available') return tab
  return 'joined'
})

const tabMeta = computed(() => getTabMeta(activeTab.value))
const bannerCopy = computed(() => getBannerCopy(activeTab.value))
const footerNote = computed(() => getFooterNote(activeTab.value))

const bannerStatus = computed(() => {
  if (loading.value) return { text: '加载中', type: 'info' as const }
  if (loadError.value) return { text: '加载失败', type: 'danger' as const }
  if (activeTab.value === 'joined') {
    return {
      text: participatingEvents.value.length
        ? `参赛记录 ${participatingEvents.value.length} 项`
        : '暂无参赛记录',
      type: participatingEvents.value.length ? 'success' as const : 'info' as const,
    }
  }
  if (activeTab.value === 'applications') {
    return { text: '报名功能后续开放', type: 'info' as const }
  }
  return { text: '公开报名入口后续开放', type: 'info' as const }
})

function setTab(tab: MyEventsTabKey) {
  router.replace({
    path: '/account/my-events',
    query: tab === 'joined' ? { tab: 'joined' } : { tab },
  })
}

function registrationStatusText(status?: number): string {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已提交'
    case 2: return '审核中'
    case 3: return '已通过'
    case 4: return '已驳回'
    case 5: return '已撤回'
    case 6: return '已取消'
    default: return status == null ? '未知' : `状态 ${status}`
  }
}

function mapToParticipating(item: AccountTournamentItemDto): ParticipatingEvent {
  const role = item.rosterRole || '球员'
  return {
    relationId: item.registrationId,
    tournamentId: item.tournamentId || item.registrationId,
    tournamentName: item.tournamentName || '未命名赛事',
    platform: item.seasonName || '-',
    participationType: 'TEAM',
    participationTypeText: item.teamName ? '球队参赛' : '个人参赛',
    identityType: role,
    identityText: role,
    status: String(item.registrationStatus ?? ''),
    statusText: registrationStatusText(item.registrationStatus),
    actionText: '查看记录',
    actionType: 'VIEW_EVENT_RELATION',
    actionRoute: '/account/my-tournaments/list',
  }
}

async function loadJoinedTournaments() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getAccountTournamentPage({ currentPage: 1, pageSize: 50 })
    if (res.code !== 200) {
      throw new Error(res.msg || '加载失败')
    }
    participatingEvents.value = (res.data?.records || []).map(mapToParticipating)
    await accountStore.loadMyTournaments().catch(() => undefined)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    participatingEvents.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadJoinedTournaments()
})

function handleAvailableAction(_event: AvailableEvent) {
  ElMessage.info('赛事报名功能后续开放')
}

function handleApplicationAction(_application: MyApplication) {
  ElMessage.info('报名管理功能后续开放')
}

function handleParticipatingAction(event: ParticipatingEvent) {
  if (event.actionRoute) {
    router.push(event.actionRoute)
  }
}
</script>

<template>
  <div class="account-view account-view--my-events">
    <AccountPageHeader
      :zh-title="tabMeta.title"
      :en-title="tabMeta.subtitle"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事' },
      ]"
    />

    <AccountStatusBanner
      :title="bannerCopy.title"
      :description="bannerCopy.description"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <MyEventsTabs :active-tab="activeTab" @change="setTab" />

    <p v-if="loadError && activeTab === 'joined'" class="my-events-error">{{ loadError }}</p>

    <template v-if="activeTab === 'available'">
      <AccountEmptyState
        v-if="!availableEvents.length"
        message="暂无可参与赛事"
        subtitle="公开报名入口将在后续阶段开放"
        large
      />
      <AvailableEventTable
        v-else
        :events="availableEvents"
        @action="handleAvailableAction"
      />
    </template>

    <template v-else-if="activeTab === 'applications'">
      <AccountEmptyState
        v-if="!myApplications.length"
        message="暂无报名记录"
        subtitle="球队报名与个人报名将在后续阶段开放"
        large
      />
      <MyApplicationList
        v-else
        :applications="myApplications"
        @action="handleApplicationAction"
      />
    </template>

    <template v-else>
      <AccountEmptyState
        v-if="!loading && !loadError && !participatingEvents.length"
        message="暂无参赛记录"
        subtitle="完成球员认证并参赛后，记录将显示在这里"
        large
      />
      <ParticipatingEventList
        v-else-if="participatingEvents.length"
        :events="participatingEvents"
        @action="handleParticipatingAction"
      />
    </template>

    <p class="my-events-footnote">{{ footerNote }}</p>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view--my-events {
  width: 100%;
  min-width: 0;
}

.my-events-footnote {
  margin: 16px 0 0;
  font-size: 13px;
  color: #666;
  line-height: 1.7;
}

.my-events-error {
  margin: 0 0 12px;
  color: #b42318;
  font-size: 13px;
}
</style>
