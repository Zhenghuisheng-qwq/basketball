<script setup lang="ts">
import { computed, ref } from 'vue'
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
import { mapVerificationPlatform } from '@/mock/playerIdentityMock'
import { formatDateTime } from '@/utils/formatDate'

const router = useRouter()
const accountStore = useAccountStore()
const showWithdraw = ref(false)

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)

const steps = [
  { label: '提交资料', state: 'done' as const },
  { label: '管理员审核', state: 'current' as const },
  { label: '认证生效', state: 'upcoming' as const },
]

const platformLabel = computed(() => {
  const p = verification.value?.primaryPlatform
  if (!p) return '-'
  return IDENTITY_PLATFORM_LABELS[mapVerificationPlatform(p)] || GAME_PLATFORM_LABELS[p]
})

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showWithdraw.value = false
  ElMessage.success('已撤回申请，可修改资料后重新提交')
  router.push({ name: 'AccountPlayerIdentityApply' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员认证"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '审核中' },
      ]"
    />

    <AccountStatusBanner
      title="球员认证申请"
      description="认证资料已提交，当前由管理员审核"
      status-text="审核中"
      status-type="warning"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel title="本次认证资料">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge type="warning" text="审核中" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">游戏平台</span><span>{{ platformLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>已上传 1 份</span></div>
          <div class="account-info-row"><span class="account-info-row__label">提交时间</span><span>{{ formatDateTime(verification?.submittedAt) }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel title="当前状态">
        <div class="identity-review-result identity-review-result--pending">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/></svg>
          </div>
          <strong>球员认证审核中</strong>
          <AccountStatusBadge type="warning" text="审核中" />
          <p>管理员将对整份认证资料进行审核</p>
          <p>审核期间不能重复提交</p>
          <button type="button" class="account-btn-secondary" @click="showWithdraw = true">撤回修改</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">撤回后可以修改认证资料；再次提交将生成新的审核记录。</p>

    <AccountConfirmDialog
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