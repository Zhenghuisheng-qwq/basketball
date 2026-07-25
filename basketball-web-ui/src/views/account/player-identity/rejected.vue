<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { mapVerificationPlatform } from '@/utils/playerIdentity'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)

const rejected = computed(() => {
  const v = verification.value
  return {
    playerName: account.value?.nickname || '',
    platform: v?.primaryPlatform ? mapVerificationPlatform(v.primaryPlatform) : 'PC',
    gameId: v?.gameId || '',
    position: v?.gamePosition || v?.commonPosition || '',
    proofCount: v?.gameIdScreenshotUrl ? 1 : 0,
    reviewRemark: v?.reviewerComment || '请根据管理员备注修改资料后重新提交。',
    submitTime: v?.submittedAt ? formatDateTime(v.submittedAt) : '',
  }
})

const steps = [
  { label: '提交资料', state: 'done' as const },
  { label: '管理员审核', state: 'failed' as const, subLabel: '未通过' },
  { label: '认证生效', state: 'upcoming' as const },
]

function goResubmit() {
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
        { label: '未通过' },
      ]"
    />

    <AccountStatusBanner
      title="球员认证未通过"
      description="请查看管理员备注，修改资料后重新提交"
      status-text="未通过"
      status-type="danger"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel title="原提交资料">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar identity-review-profile__avatar--error">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ rejected.playerName }}</strong>
            <AccountStatusBadge type="danger" text="未通过" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">游戏平台</span><span>{{ IDENTITY_PLATFORM_LABELS[rejected.platform] }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ rejected.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ rejected.position }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>已上传 {{ rejected.proofCount }} 份</span></div>
          <div class="account-info-row"><span class="account-info-row__label">提交时间</span><span>{{ rejected.submitTime }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel title="审核结果">
        <div class="identity-review-result identity-review-result--failed">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          </div>
          <strong>球员认证未通过</strong>
          <AccountStatusBadge type="danger" text="未通过" />
          <p class="identity-review-result__remark-label">管理员备注</p>
          <p class="identity-review-result__remark">{{ rejected.reviewRemark }}</p>
          <button type="button" class="account-btn-primary" @click="goResubmit">修改并重新提交</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">认证未通过时不会生成永久球员 ID；重新提交将建立新的审核记录。</p>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>