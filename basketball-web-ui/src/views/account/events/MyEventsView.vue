<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import MyEventsTabs from '@/components/account/events/MyEventsTabs.vue'
import AvailableEventTable from '@/components/account/events/AvailableEventTable.vue'
import MyApplicationList from '@/components/account/events/MyApplicationList.vue'
import ParticipatingEventList from '@/components/account/events/ParticipatingEventList.vue'
import V4MySchedulePanel from '@/components/account/events/V4MySchedulePanel.vue'
import {
  getV4AvailableDivisions,
  getV4MyEvents,
  getV4MyRegistrations,
} from '@/api/v4PlayerEvents'
import { useTeamStore } from '@/stores/team'
import { useAccountStore } from '@/stores/account'
import type { MyEventsRoleMode } from '@/api/types/myEvents'
import {
  mapV4DivisionToAvailableEvent,
  mapV4EventToParticipating,
  mapV4RegistrationToApplication,
} from '@/utils/v4AccountEventsMappers'
import {
  getBannerCopy,
  getEmptyMessage,
  getFooterNote,
  getTabMeta,
  getTabsForRole,
} from '@/constants/accountEventsUi'
import type { AvailableEvent, MyApplication, MyEventsTabKey, ParticipatingEvent } from '@/types/accountEvents'

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()
const accountStore = useAccountStore()

const loading = ref(false)
const loadError = ref('')
const availableEvents = ref<AvailableEvent[]>([])
const myApplications = ref<MyApplication[]>([])
const participatingEvents = ref<ParticipatingEvent[]>([])

const roleMode = computed<MyEventsRoleMode>(() => {
  if (!teamStore.hasTeam) return 'NO_TEAM'
  if (teamStore.isCaptain) return 'TEAM_CAPTAIN'
  if (teamStore.hasTeam) return 'TEAM_MEMBER'
  return 'NO_TEAM'
})

const visibleTabs = computed(() => getTabsForRole(roleMode.value))

const activeTab = computed<MyEventsTabKey>(() => {
  const tab = route.query.tab
  const allowed = visibleTabs.value.map((item) => item.key)
  if (typeof tab === 'string' && allowed.includes(tab as MyEventsTabKey)) {
    return tab as MyEventsTabKey
  }
  return allowed[0] || 'available'
})

const tabMeta = computed(() => getTabMeta(activeTab.value, roleMode.value))
const bannerCopy = computed(() => getBannerCopy(activeTab.value, roleMode.value))
const footerNote = computed(() => getFooterNote(activeTab.value, roleMode.value))
const emptyMessage = computed(() => getEmptyMessage(activeTab.value, roleMode.value))

const bannerStatus = computed(() => {
  if (loading.value) return { text: '加载中', type: 'info' as const }
  if (loadError.value) return { text: '加载失败', type: 'danger' as const }
  if (!accountStore.isVerifiedPlayer) return { text: '未认证球员', type: 'warning' as const }
  if (roleMode.value === 'NO_TEAM') return { text: '未加入球队', type: 'info' as const }
  if (roleMode.value === 'TEAM_MEMBER') return { text: '球队成员', type: 'info' as const }
  if (roleMode.value === 'TEAM_CAPTAIN') return { text: '有效队长', type: 'success' as const }
  return { text: '球员', type: 'info' as const }
})

function setTab(tab: MyEventsTabKey) {
  router.replace({
    path: '/account/my-events',
    query: tab === visibleTabs.value[0]?.key ? {} : { tab },
  })
}

async function loadAvailableEvents() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getV4AvailableDivisions()
    availableEvents.value = (res.data || []).map(mapV4DivisionToAvailableEvent)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    availableEvents.value = []
  } finally {
    loading.value = false
  }
}

async function loadApplications() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getV4MyRegistrations()
    myApplications.value = (res.data || []).map(mapV4RegistrationToApplication)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    myApplications.value = []
  } finally {
    loading.value = false
  }
}

async function loadJoinedEvents() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getV4MyEvents()
    participatingEvents.value = (res.data || []).map(mapV4EventToParticipating)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    participatingEvents.value = []
  } finally {
    loading.value = false
  }
}

async function loadActiveTab() {
  if (activeTab.value === 'available') {
    await loadAvailableEvents()
    return
  }
  if (activeTab.value === 'applications') {
    await loadApplications()
    return
  }
  if (activeTab.value === 'schedule') {
    return
  }
  await loadJoinedEvents()
}

onMounted(async () => {
  try {
    await teamStore.bootstrap().catch(() => undefined)
    await loadActiveTab()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  }
})

watch(activeTab, () => {
  void loadActiveTab()
})

watch(visibleTabs, (tabs) => {
  if (!tabs.some((tab) => tab.key === activeTab.value)) {
    setTab(tabs[0]?.key || 'available')
  }
})

function handleAvailableAction(event: AvailableEvent) {
  if (event.actionType === 'GO_JOINED') {
    setTab('joined')
    return
  }
  if (event.actionType === 'TEAM_REGISTRATION' || event.actionType === 'MANAGE_REGISTRATION' || event.actionType === 'RE_REGISTER') {
    router.push({
      name: 'AccountV4Registration',
      params: { divisionId: event.divisionId },
    })
    return
  }
  if (event.actionType === 'VIEW_REGISTRATION') {
    setTab('applications')
    return
  }
  if (event.disabledReason) {
    ElMessage.warning(event.disabledReason)
    return
  }
  ElMessage.warning('当前不可操作')
}

function handleApplicationAction(application: MyApplication) {
  if (application.status === 'APPROVED') {
    setTab('joined')
    return
  }
  router.push({
    name: 'AccountV4Registration',
    params: { divisionId: application.divisionId },
    query: application.applicationId ? { registrationId: application.applicationId } : undefined,
  })
}

function handleParticipatingAction(event: ParticipatingEvent) {
  if (event.actionRoute) {
    router.push(event.actionRoute)
  }
}

function handleParticipatingMatchDataAction(event: ParticipatingEvent) {
  if (event.matchDataActionRoute) {
    router.push(event.matchDataActionRoute)
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
      v-if="activeTab !== 'schedule'"
      :title="bannerCopy.title"
      :description="bannerCopy.description"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <MyEventsTabs :active-tab="activeTab" :tabs="visibleTabs" @change="setTab" />

    <p v-if="loadError" class="my-events-error">{{ loadError }}</p>
    <p v-else-if="loading && activeTab !== 'schedule'" class="my-events-loading">加载中…</p>

    <template v-else-if="activeTab === 'available'">
      <AccountEmptyState
        v-if="!availableEvents.length"
        :message="emptyMessage"
        :subtitle="roleMode === 'NO_TEAM' ? '加入球队后，可由队长统一报名赛事' : undefined"
        large
      />
      <AvailableEventTable
        v-else
        :events="availableEvents"
        :role-mode="roleMode"
        @action="handleAvailableAction"
      />
    </template>

    <template v-else-if="activeTab === 'applications'">
      <AccountEmptyState
        v-if="!myApplications.length"
        :message="emptyMessage"
        :subtitle="roleMode === 'TEAM_MEMBER' ? '请等待队长代表球队报名赛事' : '队长提交赛事报名后，记录将显示在这里'"
        large
      />
      <MyApplicationList
        v-else
        :applications="myApplications"
        @action="handleApplicationAction"
      />
    </template>

    <template v-else-if="activeTab === 'joined'">
      <AccountEmptyState
        v-if="!participatingEvents.length"
        :message="emptyMessage"
        subtitle="报名审核通过并进入正式阵容后，记录将显示在这里"
        large
      />
      <ParticipatingEventList
        v-else
        :events="participatingEvents"
        @action="handleParticipatingAction"
        @match-data-action="handleParticipatingMatchDataAction"
      />
    </template>

    <V4MySchedulePanel v-else-if="activeTab === 'schedule'" :active="activeTab === 'schedule'" />

    <p v-if="footerNote && activeTab !== 'schedule'" class="my-events-footnote">{{ footerNote }}</p>
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

.my-events-loading {
  margin: 0 0 12px;
  color: #696969;
  font-size: 14px;
}
</style>
