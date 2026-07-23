<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import MyEventsTabs from '@/components/account/events/MyEventsTabs.vue'
import AvailableEventTable from '@/components/account/events/AvailableEventTable.vue'
import MyApplicationList from '@/components/account/events/MyApplicationList.vue'
import ParticipatingEventList from '@/components/account/events/ParticipatingEventList.vue'
import { useAccountStore } from '@/stores/account'
import {
  availableEvents,
  getBannerCopy,
  getBannerStatus,
  getFooterNote,
  getTabMeta,
  myApplications,
  participatingEvents,
} from '@/mock/accountEventsMock'
import type { AvailableEvent, MyApplication, MyEventsTabKey, ParticipatingEvent } from '@/types/accountEvents'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const isVerified = computed(() => accountStore.isVerifiedPlayer)

const activeTab = computed<MyEventsTabKey>(() => {
  const tab = route.query.tab
  if (tab === 'applications' || tab === 'joined' || tab === 'available') return tab
  return 'available'
})

const tabMeta = computed(() => getTabMeta(activeTab.value))
const bannerCopy = computed(() => getBannerCopy(activeTab.value))
const bannerStatus = computed(() => getBannerStatus(activeTab.value))
const footerNote = computed(() => getFooterNote(activeTab.value))

function setTab(tab: MyEventsTabKey) {
  router.replace({
    path: '/account/my-events',
    query: tab === 'available' ? {} : { tab },
  })
}

watch(
  () => route.path,
  (path) => {
    if (path === '/account/my-events' && !route.query.tab) {
      return
    }
  },
  { immediate: true },
)

function handleAvailableAction(event: AvailableEvent) {
  if (event.actionType === 'DISABLED') return
  if (!isVerified.value) {
    ElMessage.warning('请先完成球员身份认证')
    router.push('/account/player-identity')
    return
  }
  if (event.actionType === 'INDIVIDUAL_REGISTRATION') {
    router.push('/account/my-events/2026-open/individual-registration')
    return
  }
  if (event.actionType === 'TEAM_REGISTRATION') {
    router.push(`/account/my-events/${event.tournamentId}/team-registration`)
  }
}

function handleApplicationAction(application: MyApplication) {
  if (application.actionType === 'CONFIRM_INVITE') {
    ElMessage.success('已确认邀请，等待管理员审核')
    return
  }
  if (application.actionType === 'VIEW_TEAM_APPLICATION') {
    router.push(`/account/my-events/${application.tournamentId}/team-application`)
    return
  }
  if (application.actionType === 'VIEW_APPLICATION_STATUS') {
    router.push('/account/my-events/2026-open/application-status')
  }
}

function handleParticipatingAction(event: ParticipatingEvent) {
  if (event.actionRoute) {
    router.push(event.actionRoute)
    return
  }
  if (event.actionType === 'MANAGE_EVENT_TEAM') {
    router.push('/account/my-tournaments/2026-league-s1/team')
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

    <AvailableEventTable
      v-if="activeTab === 'available'"
      :events="availableEvents"
      @action="handleAvailableAction"
    />

    <MyApplicationList
      v-else-if="activeTab === 'applications'"
      :applications="myApplications"
      @action="handleApplicationAction"
    />

    <ParticipatingEventList
      v-else
      :events="participatingEvents"
      @action="handleParticipatingAction"
    />

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
</style>
