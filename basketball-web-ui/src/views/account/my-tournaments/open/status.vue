<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import TournamentApplicationStepper from '@/components/account/TournamentApplicationStepper.vue'
import OpenApplicationStatusCard from '@/components/account/OpenApplicationStatusCard.vue'
import OpenPlayerProfileCard from '@/components/account/OpenPlayerProfileCard.vue'
import { useAccountStore } from '@/stores/account'
import { isRegistrationDeadlinePassed } from '@/utils/participationMappers'
import type { StepItem } from '@/components/account/AccountStepper.vue'

const props = defineProps<{
  statusMode: 'pending' | 'rejected' | 'approved'
}>()

const router = useRouter()
const accountStore = useAccountStore()

const tournament = computed(() => accountStore.openTournament)
const application = computed(() => accountStore.openApplication)

const playerSnapshot = computed(() => {
  if (!application.value) return null
  return {
    userNo: accountStore.currentAccount?.userNo || '',
    playerNo: application.value.playerNo,
    nickname: application.value.nickname,
    avatarUrl: accountStore.currentAccount?.avatarUrl || '',
    identityStatus: 'APPROVED' as const,
    identityStatusText: '认证球员',
    platform: application.value.platform,
    gameId: application.value.gameId,
  }
})

onMounted(async () => {
  await accountStore.loadOpenRegistration().catch(() => undefined)
  if (application.value?.status === 'NOT_SUBMITTED') {
    router.replace({ name: 'AccountOpenApplicationApply' })
  }
})

const pageConfig = computed(() => {
  if (props.statusMode === 'pending') {
    return {
      zhTitle: 'Open 报名状态',
      enTitle: 'OPEN APPLICATION STATUS',
      bannerDesc: '报名资料已提交，当前由管理员审核',
      bannerStatus: '审核中',
      bannerType: 'warning' as const,
      steps: [
        { label: '已提交', state: 'done' },
        { label: '管理员审核', state: 'current' },
        { label: '正式参赛', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '提交资料',
      footnote: '正式提交后不能主动撤销，不能重复提交。',
    }
  }
  if (props.statusMode === 'rejected') {
    return {
      zhTitle: 'Open 报名状态',
      enTitle: 'OPEN APPLICATION STATUS',
      bannerDesc: '报名审核未通过，请在报名期内修改后重新提交',
      bannerStatus: '未通过',
      bannerType: 'danger' as const,
      steps: [
        { label: '已提交', state: 'done' },
        { label: '管理员审核', state: 'failed', subLabel: '未通过' },
        { label: '正式参赛', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '原报名资料',
      footnote: `重新提交使用新的提交时间；报名截止 ${tournament.value.registrationDeadline}，截止后不能重交。`,
    }
  }
  return {
    zhTitle: 'Open 报名状态',
    enTitle: 'OPEN APPLICATION STATUS',
    bannerDesc: '个人报名审核已通过，正式参赛资格已生效',
    bannerStatus: '审核通过',
    bannerType: 'success' as const,
    steps: [
      { label: '已提交', state: 'done' },
      { label: '管理员审核', state: 'done' },
      { label: '正式参赛', state: 'done' },
    ] as StepItem[],
    leftTitle: '正式个人参赛关系',
    footnote: '报名通过后已进入「已参加赛事」；个人不能主动撤销，只能由管理员取消。',
  }
})

const deadlinePassed = computed(() =>
  isRegistrationDeadlinePassed(tournament.value.registrationDeadline),
)

function goMyTournaments() {
  router.push('/account/my-tournaments')
}

function goResubmit() {
  if (deadlinePassed.value) return
  router.push({ name: 'AccountOpenApplicationApply' })
}

function goEvent() {
  router.push('/tournament')
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="pageConfig.zhTitle"
      :en-title="pageConfig.enTitle"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-tournaments' },
        { label: tournament.name },
      ]"
    />

    <AccountStatusBanner
      :title="`${tournament.name} · 个人报名`"
      :description="pageConfig.bannerDesc"
      :status-text="pageConfig.bannerStatus"
      :status-type="pageConfig.bannerType"
    />

    <TournamentApplicationStepper :steps="pageConfig.steps" />

    <div class="identity-review-grid">
      <AccountPanel :title="pageConfig.leftTitle">
        <div v-if="playerSnapshot" class="open-status-profile">
          <OpenPlayerProfileCard :player="playerSnapshot" readonly />
          <div class="account-info-list">
            <div v-if="statusMode === 'approved'" class="account-info-row">
              <span class="account-info-row__label">赛事</span>
              <span>{{ application?.tournamentName }}</span>
            </div>
            <div v-if="statusMode === 'approved'" class="account-info-row">
              <span class="account-info-row__label">参赛方式</span>
              <span>个人参赛</span>
            </div>
            <div v-if="statusMode === 'approved'" class="account-info-row">
              <span class="account-info-row__label">当前状态</span>
              <span class="account-status-text--success">已生效</span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">{{ statusMode === 'rejected' ? '原提交时间' : '提交时间' }}</span>
              <span>{{ application?.submitTime }}</span>
            </div>
            <div v-if="application?.applicationNo" class="account-info-row">
              <span class="account-info-row__label">报名编号</span>
              <span>{{ application.applicationNo }}</span>
            </div>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="当前状态">
        <OpenApplicationStatusCard
          v-if="statusMode === 'pending'"
          variant="pending"
          title="个人报名审核中"
          description="管理员正在核对球员认证与赛事资格；审核完成后状态将自动更新"
          badge-text="审核中"
          @action="goMyTournaments"
        />
        <OpenApplicationStatusCard
          v-else-if="statusMode === 'rejected'"
          variant="rejected"
          title="个人报名审核未通过"
          description="当前报名资格未通过，请核对报名资料后重新提交。"
          :review-remark="application?.reviewRemark"
          badge-text="未通过"
          @action="goResubmit"
        />
        <div v-if="statusMode === 'rejected' && deadlinePassed" class="open-apply-block-reason">
          报名已截止，无法重新提交
        </div>
        <OpenApplicationStatusCard
          v-else-if="statusMode === 'approved'"
          variant="approved"
          title="Open 个人报名审核通过"
          badge-text="正式参赛"
          :review-time="application?.reviewTime"
          :grouping-status-text="application?.groupingStatusText"
          description="参赛资格：已确认"
          @action="goEvent"
        />
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ pageConfig.footnote }}</p>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/account-center.scss';
</style>