<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import MyEventsTabs from '@/components/account/events/MyEventsTabs.vue'
import AvailableEventTable from '@/components/account/events/AvailableEventTable.vue'
import AvailableEventFilters from '@/components/account/events/AvailableEventFilters.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import MyApplicationList from '@/components/account/events/MyApplicationList.vue'
import ParticipatingEventList from '@/components/account/events/ParticipatingEventList.vue'
import { getAccountTournamentPage } from '@/api/accountTournament'
import { getMyEventTeamList } from '@/api/eventParticipation'
import { getMyEventsContext, getMyEventsCompetitionSeasonOptions, getMyEventsSeasonPage, getMemberTeamEventPage, remindCaptainToRegister } from '@/api/myEvents'
import type { CompetitionSeasonOptionDto } from '@/api/types/myEvents'
import type { MyEventsSeasonPageQuery } from '@/api/types/myEvents'
import { getOpenRegistrationPage } from '@/api/openRegistration'
import { pageMyMatches, type PlayerMatchTaskItem } from '@/api/matchData'
import { getTeamRegistrationPage } from '@/api/teamRegistration'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'
import type { MyEventsContextDto, MyEventsRoleMode } from '@/api/types/myEvents'
import { useTeamStore } from '@/stores/team'
import {
  mapEventTeamSummaryToParticipating,
  mapMyEventsSeasonToAvailableEvent,
  mapOpenRegistrationToApplication,
  mapTeamEventRecordToApplication,
  mapTeamRegistrationToApplication,
  mergeParticipatingEvents,
  formatParticipationIdentityText,
  deriveMatchDataEntry,
} from '@/utils/participationMappers'
import {
  getBannerCopy,
  getEmptyMessage,
  getFooterNote,
  getTabMeta,
  getTabsForRole,
} from '@/constants/accountEventsUi'
import type { AvailableEvent, MyApplication, MyEventsTabKey, ParticipatingEvent } from '@/types/accountEvents'
import type { MyEventsSeasonFilterValues } from '@/components/account/events/AvailableEventFilters.vue'
import { getOpenStatusRouteName } from '@/types/account'

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()

const loading = ref(false)
const loadError = ref('')
const eventsContext = ref<MyEventsContextDto | null>(null)
const participatingEvents = ref<ParticipatingEvent[]>([])
const availableEvents = ref<AvailableEvent[]>([])
const availableEmptyReason = ref('')
const availableTotal = ref(0)
const availablePage = ref(1)
const availablePageSize = ref(5)
const seasonOptions = ref<CompetitionSeasonOptionDto[]>([])
const availableFilters = ref<MyEventsSeasonFilterValues>({
  eventName: '',
  seasonId: '',
  platformCode: 'ALL',
  status: '',
  signupTimeRange: null,
})
const myApplications = ref<MyApplication[]>([])

const roleMode = computed<MyEventsRoleMode>(() => eventsContext.value?.currentRoleMode || 'NO_TEAM')
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
const emptyMessage = computed(() => availableEmptyReason.value || getEmptyMessage(activeTab.value, roleMode.value))

const bannerStatus = computed(() => {
  if (loading.value) return { text: '加载中', type: 'info' as const }
  if (loadError.value) return { text: '加载失败', type: 'danger' as const }
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
    ? `${item.seasonName || item.tournamentName || '赛事'} / ${platformLabel} / ${registrationStatusText(item.registrationStatus)}`
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
    actionText: '查看阵容',
    actionType: 'VIEW_EVENT_RELATION',
    actionRoute: `/account/my-tournaments/${seasonKey}/team/roster`,
    isCaptain: isCaptainRole,
    rosterLocked: false,
  }
}

async function loadSeasonMatches(seasonId: string): Promise<PlayerMatchTaskItem[]> {
  try {
    const res = await pageMyMatches({ seasonId, pageNum: 1, pageSize: 100 })
    return res.data?.records || []
  } catch {
    return []
  }
}

async function enrichParticipatingWithMatchData(events: ParticipatingEvent[]): Promise<ParticipatingEvent[]> {
  const seasonIds = [...new Set(events.map((item) => item.tournamentId).filter(Boolean))]
  const matchMap = new Map<string, PlayerMatchTaskItem[]>()

  await Promise.all(seasonIds.map(async (seasonId) => {
    matchMap.set(seasonId, await loadSeasonMatches(seasonId))
  }))

  return events.map((event) => {
    const matches = matchMap.get(event.tournamentId) || []
    const matchData = deriveMatchDataEntry(
      event.tournamentId,
      matches,
      Boolean(event.isCaptain),
      Boolean(event.rosterLocked),
    )
    return {
      ...event,
      ...matchData,
    }
  })
}

async function loadContext() {
  const res = await getMyEventsContext()
  eventsContext.value = res.data ?? null
  await teamStore.bootstrap().catch(() => undefined)
}

async function loadSeasonOptions() {
  try {
    const res = await getMyEventsCompetitionSeasonOptions()
    seasonOptions.value = res.data || []
  } catch {
    seasonOptions.value = []
  }
}

function buildAvailableQuery(): MyEventsSeasonPageQuery {
  const [startTime, endTime] = availableFilters.value.signupTimeRange || []
  return {
    eventName: availableFilters.value.eventName.trim() || undefined,
    seasonId: availableFilters.value.seasonId || undefined,
    platformCode: availableFilters.value.platformCode || 'ALL',
    status: availableFilters.value.status || undefined,
    startTime: startTime || undefined,
    endTime: endTime || undefined,
    pageNum: availablePage.value,
    pageSize: availablePageSize.value,
  }
}

async function loadAvailableEvents() {
  loading.value = true
  loadError.value = ''
  availableEmptyReason.value = ''
  try {
    const res = await getMyEventsSeasonPage(buildAvailableQuery())
    availableEvents.value = (res.data?.records || []).map(mapMyEventsSeasonToAvailableEvent)
    availableTotal.value = res.data?.total ?? availableEvents.value.length
    availableEmptyReason.value = res.data?.emptyReason || ''
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    availableEvents.value = []
    availableTotal.value = 0
  } finally {
    loading.value = false
  }
}

function handleAvailableSearch() {
  availablePage.value = 1
  void loadAvailableEvents()
}

function handleAvailableReset() {
  availableFilters.value = {
    eventName: '',
    seasonId: '',
    platformCode: 'ALL',
    status: '',
    signupTimeRange: null,
  }
  availablePage.value = 1
  void loadAvailableEvents()
}

function handleAvailablePageChange(page: number) {
  availablePage.value = page
  void loadAvailableEvents()
}

function handleAvailablePageSizeChange(pageSize: number) {
  availablePageSize.value = pageSize
  availablePage.value = 1
  void loadAvailableEvents()
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

    const teamId = eventsContext.value?.teamId || teamStore.myTeamSummary?.teamId
    if (teamId && roleMode.value === 'TEAM_CAPTAIN') {
      const teamRes = await getTeamRegistrationPage(teamId, { currentPage: 1, pageSize: 20 })
      teamApps = (teamRes.data?.records || []).map((item) =>
        mapTeamRegistrationToApplication(item, true))
    } else if (teamId && roleMode.value === 'TEAM_MEMBER') {
      const teamRes = await getMemberTeamEventPage(teamId, { currentPage: 1, pageSize: 20 })
      teamApps = (teamRes.data?.records || []).map(mapTeamEventRecordToApplication)
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
    participatingEvents.value = await enrichParticipatingWithMatchData(
      mergeParticipatingEvents(fromEventTeams, fromTournaments),
    )
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
  await loadJoinedTournaments()
}

onMounted(async () => {
  try {
    await loadContext()
    if (activeTab.value === 'available') {
      await loadSeasonOptions()
    }
    await loadActiveTab()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  }
})

watch(activeTab, (tab) => {
  if (tab === 'available' && !seasonOptions.value.length) {
    void loadSeasonOptions()
  }
  void loadActiveTab()
})

watch(visibleTabs, (tabs) => {
  if (!tabs.some((tab) => tab.key === activeTab.value)) {
    setTab(tabs[0]?.key || 'available')
  }
})

function showActionMessage(event: AvailableEvent) {
  if (event.disabledReason) {
    ElMessage.warning(event.disabledReason)
  }
}

async function handleAvailableAction(event: AvailableEvent, overrideAction?: AvailableEvent['actionType']) {
  const actionType = overrideAction || event.actionType

  if (actionType === 'VIEW_EVENT') {
    router.push(`/events/${event.tournamentId || event.seasonId}`)
    return
  }
  if (actionType === 'VIEW_ROSTER') {
    setTab('joined')
    return
  }
  if (actionType === 'VIEW_REGISTRATION') {
    if (roleMode.value === 'TEAM_MEMBER') {
      setTab('applications')
      return
    }
    router.replace('/account/my-events?tab=applications')
    return
  }
  if (actionType === 'JOIN_TEAM') {
    showActionMessage(event)
    router.push('/account/team?tab=joinableTeams')
    return
  }
  if (actionType === 'CONTACT_CAPTAIN') {
    ElMessage.warning(event.disabledReason || '当前用户不是球队队长，请联系队长报名。')
    return
  }
  if (actionType === 'ROSTER_NOT_SELECTED') {
    ElMessage.info('你尚未入选该赛事阵容，请联系队长确认。')
    return
  }
  if (actionType === 'RE_REGISTER') {
    goTeamRegistration(event, event.platformCode)
    return
  }
  if (actionType === 'REMIND_CAPTAIN') {
    const teamId = eventsContext.value?.teamId
    const seasonId = event.seasonId
    if (!teamId || !seasonId) {
      ElMessage.warning('暂无法提醒队长')
      return
    }
    try {
      await remindCaptainToRegister(teamId, { seasonId })
      ElMessage.success('已提醒队长')
      await loadAvailableEvents()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '提醒失败')
    }
    return
  }
  if (actionType === 'TEAM_REGISTRATION' || actionType === 'MANAGE_REGISTRATION') {
    goTeamRegistration(event, event.platformCode)
    return
  }
  if (event.disabledReason) {
    showActionMessage(event)
    return
  }
  ElMessage.warning('当前不可操作')
}

function goTeamRegistration(event: AvailableEvent, platformCode?: string) {
  const seasonKey = event.seasonId || event.tournamentId
  router.push({
    path: `/account/my-events/${seasonKey}/team-registration`,
    query: platformCode ? { platformCode } : {},
  })
}

function handleApplicationAction(application: MyApplication) {
  if (application.applicationType === 'INDIVIDUAL') {
    const routeName = getOpenStatusRouteName(application.status as any)
    router.push({ name: routeName })
    return
  }
  if (application.actionType === 'VIEW_APPLICATION_STATUS') {
    if (roleMode.value === 'TEAM_MEMBER') {
      ElMessage.info('当前为球队成员只读视图')
      return
    }
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
      :title="bannerCopy.title"
      :description="bannerCopy.description"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <MyEventsTabs :active-tab="activeTab" :tabs="visibleTabs" @change="setTab" />

    <p v-if="loadError" class="my-events-error">{{ loadError }}</p>
    <p v-else-if="loading" class="my-events-loading">加载中…</p>

    <template v-else-if="activeTab === 'available'">
      <AvailableEventFilters
        v-model="availableFilters"
        :season-options="seasonOptions"
        :loading="loading"
        @search="handleAvailableSearch"
        @reset="handleAvailableReset"
      />
      <AccountEmptyState
        v-if="!availableEvents.length"
        :message="emptyMessage"
        :subtitle="roleMode === 'NO_TEAM' ? '可先前往「我的球队」申请加入球队' : undefined"
        large
      />
      <template v-else>
        <div class="my-events-available-list">
          <AvailableEventTable
            :events="availableEvents"
            :role-mode="roleMode"
            @action="handleAvailableAction"
          />
          <MyEventsPagination
            :page="availablePage"
            :page-size="availablePageSize"
            :total="availableTotal"
            @update:page="handleAvailablePageChange"
            @update:page-size="handleAvailablePageSizeChange"
          />
        </div>
      </template>
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

    <template v-else>
      <AccountEmptyState
        v-if="!participatingEvents.length"
        :message="emptyMessage"
        subtitle="完成球员认证并进入阵容后，记录将显示在这里"
        large
      />
      <ParticipatingEventList
        v-else
        :events="participatingEvents"
        @action="handleParticipatingAction"
        @match-data-action="handleParticipatingMatchDataAction"
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

.my-events-loading {
  margin: 0 0 12px;
  color: #696969;
  font-size: 14px;
}

.my-events-available-list {
  display: flex;
  flex-direction: column;
}

.my-events-dialog {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 20px;
}

.my-events-dialog__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.my-events-dialog__panel {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg);

  h3 {
    margin: 0 0 8px;
  }
}

.my-events-dialog__tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: #696969;
}

.my-events-dialog__platforms {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.my-events-dialog__platform {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
}

.my-events-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
