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
import { getAccountTournamentPage } from '@/api/accountTournament'
import { getMyEventTeamList } from '@/api/eventParticipation'
import { getOpenAvailableSeasonPage, getOpenRegistrationPage } from '@/api/openRegistration'
import { getAvailableSeasonPage } from '@/api/teamRegistration'
import { getTeamRegistrationPage } from '@/api/teamRegistration'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'
import { useAccountStore } from '@/stores/account'
import { useTeamStore } from '@/stores/team'
import {
  mapEventTeamSummaryToParticipating,
  mapOpenRegistrationToApplication,
  mapOpenSeasonToAvailableEvent,
  mapTeamRegistrationToApplication,
  mapTeamSeasonToAvailableEvent,
  mergeParticipatingEvents,
  formatParticipationIdentityText,
} from '@/utils/participationMappers'
import {
  getBannerCopy,
  getFooterNote,
  getTabMeta,
} from '@/constants/accountEventsUi'
import type { AvailableEvent, MyApplication, MyEventsTabKey, ParticipatingEvent } from '@/types/accountEvents'
import { getOpenStatusRouteName } from '@/types/account'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const teamStore = useTeamStore()

const loading = ref(false)
const loadError = ref('')
const participatingEvents = ref<ParticipatingEvent[]>([])
const availableEvents = ref<AvailableEvent[]>([])
const availableEmptyReason = ref('')
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
    return {
      text: myApplications.value.length
        ? `报名记录 ${myApplications.value.length} 项`
        : '暂无报名记录',
      type: myApplications.value.length ? 'warning' as const : 'info' as const,
    }
  }
  return {
    text: availableEvents.value.length
      ? `可参与 ${availableEvents.value.length} 项`
      : '暂无可参与赛事',
    type: availableEvents.value.length ? 'success' as const : 'info' as const,
  }
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
    case 1: return '审核中'
    case 2: return '审核中'
    case 3: return '已通过'
    case 4: return '已驳回'
    case 5: return '已撤回'
    case 6: return '已取消'
    default: return status == null ? '未知' : `状态 ${status}`
  }
}

function mapToParticipating(item: AccountTournamentItemDto): ParticipatingEvent {
  const isCaptainRole = item.rosterRole?.toUpperCase() === 'CAPTAIN' || teamStore.canManageCurrentTeam
  const identityText = formatParticipationIdentityText(item.rosterRole, isCaptainRole)
  const seasonKey = item.seasonId || item.tournamentId || item.registrationId
  const platformLabel = item.platformName || item.platformCode
  const platformDisplay = platformLabel
    ? `${item.seasonName || item.tournamentName || '赛事'} / ${platformLabel} 组 / ${registrationStatusText(item.registrationStatus)}`
    : item.seasonName || '-'
  return {
    relationId: item.registrationId,
    tournamentId: seasonKey,
    tournamentName: item.tournamentName || '未命名赛事',
    platform: platformDisplay,
    participationType: 'TEAM',
    participationTypeText: item.teamName ? '球队参赛' : '个人参赛',
    identityType: identityText,
    identityText,
    status: String(item.registrationStatus ?? ''),
    statusText: registrationStatusText(item.registrationStatus),
    actionText: '查看记录',
    actionType: 'VIEW_EVENT_RELATION',
    actionRoute: `/account/my-tournaments/${seasonKey}/team/roster`,
  }
}

async function loadAvailableEvents() {
  loading.value = true
  loadError.value = ''
  availableEmptyReason.value = ''
  try {
    const isVerified = accountStore.isVerifiedPlayer
    const teamId = teamStore.myTeamSummary?.teamId
    const teamPromise = teamStore.hasTeam && teamStore.canManageCurrentTeam && teamId
      ? getAvailableSeasonPage(teamId, { currentPage: 1, pageSize: 20 })
      : Promise.resolve({ data: { records: [], emptyReason: teamStore.canManageCurrentTeam ? '' : '只有具备有效队长权限的用户可以代表球队报名赛事' } })

    const [openResult, teamResult] = await Promise.allSettled([
      getOpenAvailableSeasonPage({ currentPage: 1, pageSize: 20 }),
      teamPromise,
    ])

    const openEvents = openResult.status === 'fulfilled'
      ? (openResult.value.data?.records || []).map((item) => mapOpenSeasonToAvailableEvent(item, isVerified))
      : []

    let teamEvents: AvailableEvent[] = []
    if (teamResult.status === 'fulfilled') {
      teamEvents = (teamResult.value.data?.records || []).map((item) =>
        mapTeamSeasonToAvailableEvent(item, Boolean(teamStore.canManageCurrentTeam)))
      availableEmptyReason.value = teamResult.value.data?.emptyReason || ''
    } else if (teamStore.hasTeam && !teamStore.canManageCurrentTeam) {
      availableEmptyReason.value = '只有具备有效队长权限的用户可以代表球队报名赛事'
    }

    availableEvents.value = [...openEvents, ...teamEvents]
    if (!availableEvents.value.length && !availableEmptyReason.value && openResult.status === 'rejected' && teamResult.status === 'rejected') {
      loadError.value = '加载失败'
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    availableEvents.value = []
  } finally {
    loading.value = false
  }
}

function dedupeApplications(apps: MyApplication[]): MyApplication[] {
  const seen = new Set<string>()
  return apps.filter((app) => {
    const key = app.applicationId || `${app.applicationType}-${app.tournamentId}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

async function loadApplications() {
  loading.value = true
  loadError.value = ''
  try {
    const openRes = await getOpenRegistrationPage({ currentPage: 1, pageSize: 20 })
    const openApps = (openRes.data?.records || []).map(mapOpenRegistrationToApplication)
    let teamApps: MyApplication[] = []
    if (teamStore.myTeamSummary?.teamId && teamStore.canManageCurrentTeam) {
      const teamRes = await getTeamRegistrationPage(teamStore.myTeamSummary.teamId, { currentPage: 1, pageSize: 20 })
      teamApps = (teamRes.data?.records || [])
        .map((item) => mapTeamRegistrationToApplication(item, teamStore.canManageCurrentTeam))
    }
    myApplications.value = dedupeApplications([...openApps, ...teamApps])
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    myApplications.value = []
  } finally {
    loading.value = false
  }
}

async function loadJoinedTournaments() {
  loading.value = true
  loadError.value = ''
  try {
    const [tournamentRes, eventTeamRes] = await Promise.all([
      getAccountTournamentPage({ currentPage: 1, pageSize: 50 }),
      getMyEventTeamList(),
    ])
    const fromTournaments = (tournamentRes.data?.records || [])
      .filter((item) => item.registrationStatus === 3)
      .map(mapToParticipating)
    const fromEventTeams = (eventTeamRes.data || []).map(mapEventTeamSummaryToParticipating)
    participatingEvents.value = mergeParticipatingEvents(fromEventTeams, fromTournaments)
    await accountStore.loadMyTournaments().catch(() => undefined)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    participatingEvents.value = []
  } finally {
    loading.value = false
  }
}

async function loadActiveTab() {
  if (activeTab.value === 'available') {
    await teamStore.loadMyTeam().catch(() => undefined)
    await loadAvailableEvents()
    return
  }
  if (activeTab.value === 'applications') {
    await teamStore.bootstrap().catch(() => undefined)
    await loadApplications()
    return
  }
  await teamStore.loadQualification().catch(() => undefined)
  await loadJoinedTournaments()
}

onMounted(async () => {
  await teamStore.bootstrap().catch(() => undefined)
  void loadActiveTab()
})

watch(activeTab, () => {
  void loadActiveTab()
})

function handleAvailableAction(event: AvailableEvent) {
  if (event.actionType === 'INDIVIDUAL_REGISTRATION') {
    router.push({ name: 'AccountOpenApplicationApply' })
    return
  }
  if (event.actionType === 'TEAM_REGISTRATION') {
    const seasonKey = event.seasonId || event.tournamentId
    router.push(`/account/my-events/${seasonKey}/team-registration`)
    return
  }
  if (event.actionType === 'GO_APPLICATIONS') {
    router.replace('/account/my-events?tab=applications')
    return
  }
  ElMessage.warning('当前不可报名')
}

function handleApplicationAction(application: MyApplication) {
  if (application.applicationType === 'INDIVIDUAL') {
    const routeName = getOpenStatusRouteName(application.status as any)
    router.push({ name: routeName })
    return
  }
  if (application.actionType === 'VIEW_APPLICATION_STATUS') {
    router.push({
      path: '/account/team',
      query: {
        tab: 'registration',
        registrationId: application.applicationId,
      },
    })
    return
  }
  router.push(`/account/my-events/${application.tournamentId}/team-registration`)
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

    <p v-if="loadError" class="my-events-error">{{ loadError }}</p>

    <template v-if="activeTab === 'available'">
      <AccountEmptyState
        v-if="!loading && !availableEvents.length"
        message="暂无可参与赛事"
        :subtitle="availableEmptyReason || '当前暂无可报名赛事，请确认球队已认证且处于报名期'"
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
        v-if="!loading && !myApplications.length"
        message="暂无报名记录"
        subtitle="队长提交赛事报名后，记录将显示在这里"
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
