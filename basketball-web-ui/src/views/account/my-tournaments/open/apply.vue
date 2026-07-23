<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import TournamentApplicationStepper from '@/components/account/TournamentApplicationStepper.vue'
import TournamentInfoCard from '@/components/account/TournamentInfoCard.vue'
import OpenPlayerProfileCard from '@/components/account/OpenPlayerProfileCard.vue'
import RegistrationConfirmCard from '@/components/account/RegistrationConfirmCard.vue'
import { useAccountStore } from '@/stores/account'
import type { StepItem } from '@/components/account/AccountStepper.vue'

const router = useRouter()
const accountStore = useAccountStore()

const tournament = computed(() => accountStore.openTournament)
const application = computed(() => accountStore.openApplication)
const player = computed(() => accountStore.certifiedPlayerForOpen)
const isVerified = computed(() => accountStore.isVerifiedPlayer)

const blockReason = computed(() => accountStore.getOpenRegistrationBlockReason())
const canSubmit = computed(() => accountStore.canSubmitOpenApplication())

const steps: StepItem[] = [
  { label: '提交报名', state: 'current' },
  { label: '管理员审核', state: 'upcoming' },
  { label: '正式参赛', state: 'upcoming' },
]

function redirectByStatus() {
  const status = application.value?.status
  if (status === 'PENDING') {
    router.replace({ name: 'AccountOpenApplicationPending' })
  } else if (status === 'REJECTED') {
    router.replace({ name: 'AccountOpenApplicationRejected' })
  } else if (status === 'APPROVED') {
    router.replace({ name: 'AccountOpenApplicationApproved' })
  }
}

onMounted(() => {
  if (!isVerified.value) {
    ElMessage.warning('请先完成球员身份认证')
    router.replace('/account/player-identity')
    return
  }
  if (application.value?.status === 'PENDING' || application.value?.status === 'APPROVED') {
    redirectByStatus()
  }
})

function handleSubmit() {
  if (!canSubmit.value) {
    ElMessage.warning(blockReason.value || '当前无法提交报名')
    return
  }
  accountStore.submitOpenApplication()
  ElMessage.success('Open 个人报名已提交')
  router.push({ name: 'AccountOpenApplicationPending' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="Open 个人报名"
      en-title="OPEN INDIVIDUAL REGISTRATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-tournaments' },
        { label: tournament.name },
      ]"
    />

    <AccountStatusBanner
      :title="`${tournament.name} · 个人报名`"
      description="认证球员直接提交个人赛事报名"
      :status-text="tournament.statusText"
      status-type="success"
    />

    <TournamentApplicationStepper :steps="steps" />

    <div class="open-apply-grid">
      <AccountPanel title="赛事信息">
        <TournamentInfoCard :tournament="tournament" />
      </AccountPanel>

      <AccountPanel title="个人报名资料">
        <OpenPlayerProfileCard v-if="player" :player="player" />
      </AccountPanel>

      <AccountPanel title="报名确认">
        <RegistrationConfirmCard
          :disabled="!canSubmit"
          @submit="handleSubmit"
        />
        <p v-if="blockReason && application?.status !== 'REJECTED'" class="open-apply-block-reason">{{ blockReason }}</p>
      </AccountPanel>
    </div>

    <div class="open-apply-footnotes">
      <p>不保存草稿</p>
      <p>同一赛事不能重复报名</p>
      <p>参加其他同期赛事不影响 Open 报名</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/account-center.scss';
</style>