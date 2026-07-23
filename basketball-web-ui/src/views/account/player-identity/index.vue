<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { identityQueryToVerificationStatus } from '@/types/account'
import type { PlayerVerificationStatus } from '@/types/account'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const isDev = import.meta.env.DEV

const identity = computed(() => accountStore.playerIdentity)

const status = computed(() => identity.value?.status || 'NOT_SUBMITTED')

watch(
  () => route.query.status,
  async (queryStatus) => {
    if (!import.meta.env.DEV || typeof queryStatus !== 'string') return
    const mapped = identityQueryToVerificationStatus(queryStatus)
    if (!mapped) return
    await accountStore.loadAccountMock(mapped, queryStatus === 'limited')
  },
  { immediate: true },
)

watch(
  [status, () => route.name],
  ([nextStatus, routeName]) => {
    if (routeName !== 'AccountPlayerIdentity') return
    if (nextStatus === 'PENDING') router.replace({ name: 'AccountPlayerIdentityPending' })
    else if (nextStatus === 'REJECTED') router.replace({ name: 'AccountPlayerIdentityRejected' })
    else if (nextStatus === 'APPROVED' || nextStatus === 'ELIGIBILITY_LIMITED') {
      if (nextStatus === 'ELIGIBILITY_LIMITED' && route.query.status === 'limited') {
        router.replace({ name: 'AccountPlayerEligibility' })
        return
      }
      router.replace({ name: 'AccountPlayerIdentityApproved' })
    }
  },
  { immediate: true },
)

const banner = computed(() => ({
  title: 'CNPA 永久球员身份',
  description: '完成一次认证，建立长期有效的球员档案',
  statusText: '未认证',
  statusType: 'warning' as const,
}))

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}

async function switchDevStatus(next: PlayerVerificationStatus) {
  if (!import.meta.env.DEV) return
  await router.replace({ query: { status: next === 'NOT_APPLIED' ? 'not-submitted' : next.toLowerCase() } })
  await accountStore.loadAccountMock(next)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员身份"
      en-title="PLAYER IDENTITY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份' },
      ]"
    />

    <AccountStatusBanner v-bind="banner" />

    <div class="account-grid-2 identity-entry-grid">
      <AccountPanel title="当前状态">
        <div class="identity-entry-state">
          <div class="identity-entry-state__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" />
            </svg>
          </div>
          <AccountStatusBadge type="warning" text="未认证" />
          <p class="identity-entry-state__title">尚未申请球员认证</p>
        </div>
      </AccountPanel>

      <AccountPanel title="申请 CNPA 球员认证">
        <ul class="identity-entry-rules">
          <li>审核通过后将获得不可修改的永久球员 ID</li>
          <li>认证资料用于确认游戏平台、游戏 ID 与球员赛事资格</li>
          <li>认证长期有效，无需在每届赛事中重复申请</li>
        </ul>
        <p class="identity-entry-hint">
          未认证账号不能报名、接受球队邀请、进入阵容或参与转会
        </p>
        <div class="account-panel__action">
          <button type="button" class="account-btn-primary" @click="goApply">申请球员认证</button>
        </div>
      </AccountPanel>
    </div>

    <details v-if="isDev" class="verification-dev-switcher">
      <summary>DEV</summary>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('NOT_APPLIED')">未提交</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('PENDING')">审核中</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('REJECTED')">未通过</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('APPROVED')">已通过</button>
    </details>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
