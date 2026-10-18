<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import MyEventsTabs from '@/components/account/events/MyEventsTabs.vue'
import AvailableEventTable from '@/components/account/events/AvailableEventTable.vue'
import MyApplicationList from '@/components/account/events/MyApplicationList.vue'
import ParticipatingEventList from '@/components/account/events/ParticipatingEventList.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import V4MySchedulePanel from '@/components/account/events/V4MySchedulePanel.vue'
import V4PersonalFormationPanel from '@/components/account/events/V4PersonalFormationPanel.vue'
import {
  getV4MyAvailableEventsPage,
  getV4MyEvents,
  getV4MyPersonalRegistrations,
  getV4MyRegistrations,
} from '@/api/v4PlayerEvents'
import type { PageResult } from '@/api/types/common'
import { useTeamStore } from '@/stores/team'
import { useAccountStore } from '@/stores/account'
import type { MyEventsRoleMode } from '@/api/types/myEvents'
import {
  mapV4DivisionToAvailableEvent,
  mapV4EventToParticipating,
  mapV4PersonalDivisionToAvailableEvent,
  mapV4PersonalRegistrationToApplication,
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
const { t } = useI18n()
const teamStore = useTeamStore()
const accountStore = useAccountStore()

const loading = ref(false)
const loadError = ref('')
const availableEvents = ref<AvailableEvent[]>([])
const myApplications = ref<MyApplication[]>([])
const participatingEvents = ref<ParticipatingEvent[]>([])

const availablePage = ref(1)
const availablePageSize = ref(10)
const availableTotal = ref(0)

const joinedPage = ref(1)
const joinedPageSize = ref(10)
const joinedTotal = ref(0)

const pageSizeOptions = [10, 20, 50]

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
  if (loading.value) return { text: t('accountEvents.page.loading'), type: 'info' as const }
  if (loadError.value) return { text: t('accountEvents.page.loadFailed'), type: 'danger' as const }
  if (!accountStore.isVerifiedPlayer) return { text: t('accountEvents.page.unverified'), type: 'warning' as const }
  if (roleMode.value === 'NO_TEAM') return { text: t('accountEvents.page.noTeam'), type: 'info' as const }
  if (roleMode.value === 'TEAM_MEMBER') return { text: t('accountEvents.page.teamMember'), type: 'info' as const }
  if (roleMode.value === 'TEAM_CAPTAIN') return { text: t('accountEvents.page.activeCaptain'), type: 'success' as const }
  return { text: t('accountEvents.page.player'), type: 'info' as const }
})

function setTab(tab: MyEventsTabKey) {
  router.replace({
    path: '/account/my-events',
    query: tab === visibleTabs.value[0]?.key ? {} : { tab },
  })
}

function unwrapPage<T>(data: PageResult<T> | T[] | undefined | null): PageResult<T> {
  if (Array.isArray(data)) {
    return { records: data, total: data.length, size: data.length || 10, current: 1, pages: 1 }
  }
  return data ?? { records: [], total: 0, size: 10, current: 1, pages: 0 }
}

async function loadAvailableEvents(page = availablePage.value, pageSize = availablePageSize.value) {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await getV4MyAvailableEventsPage({ currentPage: page, pageSize })
    const pageData = unwrapPage(res.data)
    const total = Number(pageData.total ?? 0)
    const size = Number(pageData.size || pageSize) || 10
    const totalPages = Math.max(0, Math.ceil(total / size))
    let current = Number(pageData.current || page) || 1
    if (total > 0 && current > totalPages) {
      loading.value = false
      return loadAvailableEvents(totalPages, size)
    }
    availableEvents.value = (pageData.records || []).map((row) => {
      if ((row.source || '').toUpperCase() === 'PERSONAL' && row.personalItem) {
        return mapV4PersonalDivisionToAvailableEvent(row.personalItem)
      }
      return mapV4DivisionToAvailableEvent(row.teamItem as any)
    })
    availablePage.value = current
    availablePageSize.value = size
    availableTotal.value = total
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountEvents.common.loadFailed')
    availableEvents.value = []
    availableTotal.value = 0
  } finally {
    loading.value = false
  }
}

async function loadApplications() {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    const [teamRes, personalRes] = await Promise.all([
      getV4MyRegistrations().catch(() => ({ data: [] as any[] })),
      getV4MyPersonalRegistrations().catch(() => ({ data: [] as any[] })),
    ])
    const teamApps = (teamRes.data || []).map(mapV4RegistrationToApplication)
    const personalApps = (personalRes.data || []).map(mapV4PersonalRegistrationToApplication)
    myApplications.value = [...teamApps, ...personalApps]
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountEvents.common.loadFailed')
    myApplications.value = []
  } finally {
    loading.value = false
  }
}

async function loadJoinedEvents(page = joinedPage.value, pageSize = joinedPageSize.value) {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await getV4MyEvents({ currentPage: page, pageSize })
    const pageData = unwrapPage(res.data as any)
    const total = Number(pageData.total ?? 0)
    const size = Number(pageData.size || pageSize) || 10
    const totalPages = Math.max(0, Math.ceil(total / size))
    let current = Number(pageData.current || page) || 1
    if (total > 0 && current > totalPages) {
      loading.value = false
      return loadJoinedEvents(totalPages, size)
    }
    participatingEvents.value = ((pageData.records || []) as any[]).map(mapV4EventToParticipating)
    joinedPage.value = current
    joinedPageSize.value = size
    joinedTotal.value = total
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountEvents.common.loadFailed')
    participatingEvents.value = []
    joinedTotal.value = 0
  } finally {
    loading.value = false
  }
}

async function loadActiveTab() {
  if (activeTab.value === 'available') {
    await loadAvailableEvents(1, availablePageSize.value)
    return
  }
  if (activeTab.value === 'applications') {
    await loadApplications()
    return
  }
  if (activeTab.value === 'schedule') {
    return
  }
  await loadJoinedEvents(1, joinedPageSize.value)
}

onMounted(async () => {
  try {
    await teamStore.bootstrap().catch(() => undefined)
    await loadActiveTab()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountEvents.common.loadFailed')
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
    if (event.registrationType === 'INDIVIDUAL') {
      router.push({
        name: 'AccountV4PersonalRegistration',
        params: { divisionId: event.divisionId },
      })
      return
    }
    router.push({
      name: 'AccountV4Registration',
      params: { divisionId: event.divisionId },
    })
    return
  }
  if (event.actionType === 'INDIVIDUAL_REGISTRATION') {
    router.push({
      name: 'AccountV4PersonalRegistration',
      params: { divisionId: event.divisionId },
    })
    return
  }
  if (event.actionType === 'VIEW_REGISTRATION') {
    if (event.registrationType === 'TEAM' && event.divisionId) {
      router.push({
        name: 'AccountV4Registration',
        params: { divisionId: String(event.divisionId) },
      })
      return
    }
    if (event.registrationType === 'INDIVIDUAL' && event.divisionId) {
      router.push({
        name: 'AccountV4PersonalRegistration',
        params: { divisionId: String(event.divisionId) },
      })
      return
    }
    setTab('applications')
    return
  }
  if (event.disabledReason) {
    ElMessage.warning(event.disabledReason)
    return
  }
  ElMessage.warning(t('accountEvents.common.actionUnavailable'))
}

function handleApplicationAction(application: MyApplication) {
  if (application.status === 'APPROVED') {
    setTab('joined')
    return
  }
  if (application.applicationType === 'INDIVIDUAL') {
    router.push({
      name: 'AccountV4PersonalRegistration',
      params: { divisionId: application.divisionId },
    })
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

function handleParticipatingRosterChange(event: ParticipatingEvent) {
  if (!event.divisionId) return
  router.push({
    name: 'AccountV4Registration',
    params: { divisionId: String(event.divisionId) },
    query: { rosterChange: '1' },
  })
}
</script>

<template>
  <div class="account-view account-view--my-events">
    <AccountPageHeader
      :zh-title="tabMeta.title"
      :en-title="tabMeta.subtitle"
      :breadcrumb-items="[
        { label: t('accountEvents.breadcrumb.home'), to: '/home' },
        { label: t('accountEvents.breadcrumb.account'), to: '/account/overview' },
        { label: t('accountEvents.breadcrumb.myEvents') },
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
    <p v-else-if="loading && activeTab !== 'schedule'" class="my-events-loading">{{ t('accountEvents.common.loading') }}</p>

    <template v-else-if="activeTab === 'available'">
      <AccountEmptyState
        v-if="!availableEvents.length"
        :message="emptyMessage"
        :subtitle="roleMode === 'NO_TEAM' ? t('accountEvents.empty.availableNoTeamSubtitle') : undefined"
        large
      />
      <template v-else>
        <AvailableEventTable
          :events="availableEvents"
          :role-mode="roleMode"
          @action="handleAvailableAction"
        />
        <MyEventsPagination
          :page="availablePage"
          :page-size="availablePageSize"
          :total="availableTotal"
          :page-size-options="pageSizeOptions"
          :meta-text="t('accountEvents.pagination.availableTotal', { total: availableTotal })"
          :disabled="loading"
          :aria-label="t('accountEvents.pagination.availableAria')"
          @update:page="(p) => loadAvailableEvents(p, availablePageSize)"
          @update:page-size="(s) => loadAvailableEvents(1, s)"
        />
      </template>
    </template>

    <template v-else-if="activeTab === 'applications'">
      <V4PersonalFormationPanel />
      <AccountEmptyState
        v-if="!myApplications.length"
        :message="emptyMessage"
        :subtitle="roleMode === 'TEAM_MEMBER' ? t('accountEvents.empty.applicationsMemberSubtitle') : t('accountEvents.empty.applicationsSubtitle')"
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
        :subtitle="t('accountEvents.empty.joinedSubtitle')"
        large
      />
      <template v-else>
        <ParticipatingEventList
          :events="participatingEvents"
          @action="handleParticipatingAction"
          @match-data-action="handleParticipatingMatchDataAction"
          @roster-change="handleParticipatingRosterChange"
        />
        <MyEventsPagination
          :page="joinedPage"
          :page-size="joinedPageSize"
          :total="joinedTotal"
          :page-size-options="pageSizeOptions"
          :meta-text="t('accountEvents.pagination.joinedTotal', { total: joinedTotal })"
          :disabled="loading"
          :aria-label="t('accountEvents.pagination.joinedAria')"
          @update:page="(p) => loadJoinedEvents(p, joinedPageSize)"
          @update:page-size="(s) => loadJoinedEvents(1, s)"
        />
      </template>
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
