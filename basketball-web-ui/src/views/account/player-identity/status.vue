<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS, GAME_PLATFORM_LABELS } from '@/types/account'
import { mapVerificationPlatform } from '@/utils/playerIdentity'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'
import type { StepItem } from '@/components/account/AccountStepper.vue'

const props = defineProps<{
  statusMode: 'pending' | 'rejected' | 'approved'
}>()

const router = useRouter()
const accountStore = useAccountStore()
const showWithdraw = ref(false)

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const cert = computed(() => accountStore.playerCert)

onMounted(async () => {
  try {
    await accountStore.loadPlayerCertification()
  } catch {
    // continue with existing state
  }
  const currentStatus = verification.value?.status
  if (props.statusMode === 'pending' && currentStatus !== 'PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'rejected' && currentStatus !== 'REJECTED') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'approved' && currentStatus !== 'APPROVED' && currentStatus !== 'CHANGE_PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
})

const platformLabel = computed(() => {
  const p = verification.value?.primaryPlatform
  if (!p) return '-'
  return IDENTITY_PLATFORM_LABELS[mapVerificationPlatform(p)] || GAME_PLATFORM_LABELS[p]
})

const reviewRemark = computed(
  () => cert.value?.rejectReason || verification.value?.reviewerComment || '暂无备注',
)

const pageConfig = computed(() => {
  if (props.statusMode === 'pending') {
    return {
      zhTitle: '球员认证',
      enTitle: 'PLAYER VERIFICATION',
      crumb: '审核中',
      bannerTitle: '球员认证申请',
      bannerDesc: '认证资料已提交，当前由管理员审核',
      bannerStatus: '审核中',
      bannerType: 'warning' as const,
      steps: [
        { label: '提交资料', state: 'done' },
        { label: '管理员审核', state: 'current' },
        { label: '认证生效', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '本次认证资料',
      badgeType: 'warning' as const,
      badgeText: '审核中',
      footnote: '撤回后可以修改认证资料；再次提交将生成新的审核记录。',
    }
  }
  if (props.statusMode === 'rejected') {
    return {
      zhTitle: '球员认证',
      enTitle: 'PLAYER VERIFICATION',
      crumb: '未通过',
      bannerTitle: '球员认证未通过',
      bannerDesc: '请查看管理员备注，修改资料后重新提交',
      bannerStatus: '未通过',
      bannerType: 'danger' as const,
      steps: [
        { label: '提交资料', state: 'done' },
        { label: '管理员审核', state: 'failed', subLabel: '未通过' },
        { label: '认证生效', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '原提交资料',
      badgeType: 'danger' as const,
      badgeText: '未通过',
      footnote: '认证未通过时不会生成永久球员 ID；重新提交将建立新的审核记录。',
    }
  }
  return {
    zhTitle: '球员认证',
    enTitle: 'PLAYER VERIFICATION',
    crumb: '已通过',
    bannerTitle: '永久球员身份',
    bannerDesc: '球员认证已通过，认证资料长期有效',
    bannerStatus: '已认证',
    bannerType: 'success' as const,
    steps: [
      { label: '提交资料', state: 'done' },
      { label: '管理员审核', state: 'done' },
      { label: '认证生效', state: 'done' },
    ] as StepItem[],
    leftTitle: '有效认证资料',
    badgeType: 'success' as const,
    badgeText: '认证球员',
    footnote: '球员认证长期有效，无需按赛事重复认证，永久球员 ID 不可修改。',
  }
})

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showWithdraw.value = false
  ElMessage.success('已撤回申请，可修改资料后重新提交')
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goPlatforms() {
  router.push({ name: 'AccountPlatformGameId' })
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
        { label: '球员身份', to: '/account/player-identity' },
        { label: pageConfig.crumb },
      ]"
    />

    <AccountStatusBanner
      :title="pageConfig.bannerTitle"
      :description="pageConfig.bannerDesc"
      :status-text="pageConfig.bannerStatus"
      :status-type="pageConfig.bannerType"
    />

    <AccountStepper :steps="pageConfig.steps" />

    <div class="identity-review-grid">
      <AccountPanel :title="pageConfig.leftTitle">
        <div class="identity-review-profile">
          <div
            class="identity-review-profile__avatar"
            :class="{
              'identity-review-profile__avatar--success': statusMode === 'approved',
              'identity-review-profile__avatar--error': statusMode === 'rejected',
            }"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge :type="pageConfig.badgeType" :text="pageConfig.badgeText" />
          </div>
        </div>
        <div class="account-info-list">
          <template v-if="statusMode === 'approved'">
            <div class="account-info-row"><span class="account-info-row__label">已绑定平台</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">平台状态</span><span class="account-status-text--success">已生效</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">认证状态</span><span class="account-status-text--success">长期有效</span></div>
          </template>
          <template v-else>
            <div class="account-info-row"><span class="account-info-row__label">游戏平台</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>已上传 1 份</span></div>
            <div class="account-info-row"><span class="account-info-row__label">提交时间</span><span>{{ formatDateTime(verification?.submittedAt) }}</span></div>
          </template>
        </div>
      </AccountPanel>

      <AccountPanel title="当前状态">
        <div
          v-if="statusMode === 'pending'"
          class="identity-review-result identity-review-result--pending"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/></svg>
          </div>
          <strong>球员认证审核中</strong>
          <AccountStatusBadge type="warning" text="审核中" />
          <p>管理员将对整份认证资料进行审核</p>
          <p>审核期间不能重复提交</p>
          <button type="button" class="account-btn-secondary" @click="showWithdraw = true">撤回修改</button>
        </div>

        <div
          v-else-if="statusMode === 'rejected'"
          class="identity-review-result identity-review-result--failed"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          </div>
          <strong>球员认证未通过</strong>
          <AccountStatusBadge type="danger" text="未通过" />
          <p class="identity-review-result__remark-label">管理员备注</p>
          <p class="identity-review-result__remark">{{ reviewRemark }}</p>
          <button type="button" class="account-btn-primary" @click="goApply">修改并重新提交</button>
        </div>

        <div
          v-else
          class="identity-review-result identity-review-result--success"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
          <span class="identity-review-result__label">永久球员 ID</span>
          <strong class="identity-review-result__player-no">{{ account?.playerNo }}</strong>
          <p>认证状态：已生效</p>
          <p>永久球员 ID 不可修改</p>
          <button type="button" class="account-btn-secondary" @click="goPlatforms">申请修改认证资料</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ pageConfig.footnote }}</p>

    <div v-if="statusMode === 'approved'" class="identity-shortcuts">
      <button type="button" class="account-btn-secondary" @click="goPlatforms">平台与游戏 ID 管理</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/change-history')">资料变更记录</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/my-tournaments')">我的赛事</button>
    </div>

    <AccountConfirmDialog
      v-if="statusMode === 'pending'"
      v-model="showWithdraw"
      title="撤回认证申请"
      message="确认撤回当前审核中的认证申请？撤回后可修改资料并重新提交。"
      confirm-text="确认撤回"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>