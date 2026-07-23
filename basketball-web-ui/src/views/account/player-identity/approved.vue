<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'
import { mapVerificationPlatform } from '@/mock/playerIdentityMock'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const identity = computed(() => accountStore.playerIdentity)

const steps = [
  { label: '提交资料', state: 'done' as const },
  { label: '管理员审核', state: 'done' as const },
  { label: '认证生效', state: 'done' as const },
]

const platformLabel = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  if (!data?.primaryPlatform) return '-'
  return IDENTITY_PLATFORM_LABELS[mapVerificationPlatform(data.primaryPlatform)]
})

const gameId = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  return data?.gameId || '-'
})

const position = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  return data?.gamePosition || data?.commonPosition || '-'
})
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
        { label: '已通过' },
      ]"
    />

    <AccountStatusBanner
      title="永久球员身份"
      description="球员认证已通过，认证资料长期有效"
      status-text="已认证"
      status-type="success"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel title="有效认证资料">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar identity-review-profile__avatar--success">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge type="success" text="认证球员" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">已绑定平台</span><span>{{ platformLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">平台状态</span><span class="account-status-text--success">已生效</span></div>
          <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ position }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">认证状态</span><span class="account-status-text--success">长期有效</span></div>
        </div>
      </AccountPanel>

      <AccountPanel title="永久球员身份">
        <div class="identity-review-result identity-review-result--success">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
          <span class="identity-review-result__label">永久球员 ID</span>
          <strong class="identity-review-result__player-no">{{ account?.playerNo || identity?.playerNo }}</strong>
          <p>认证状态：已生效</p>
          <p>永久球员 ID 不可修改</p>
          <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/apply')">
            申请修改认证资料
          </button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">球员认证长期有效，无需按赛事重复认证，永久球员 ID 不可修改。</p>

    <div class="identity-shortcuts">
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/platforms')">
        平台与游戏 ID 管理
      </button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/change-history')">
        资料变更记录
      </button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/my-tournaments')">
        我的赛事
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>