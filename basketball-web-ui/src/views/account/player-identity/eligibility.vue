<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const limit = computed(() => accountStore.eligibility)
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员资格状态"
      en-title="PLAYER ELIGIBILITY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '资格状态' },
      ]"
    />

    <AccountStatusBanner
      title="赛事参赛资格受限"
      description="请查看赛事方处理结果与联系说明"
      status-text="资格受限"
      status-type="danger"
    />

    <div class="identity-review-grid">
      <AccountPanel title="资格状态">
        <div class="identity-review-result identity-review-result--warning">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">永久球员 ID</span><span>{{ account?.playerNo || limit?.playerNo }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">当前状态</span><span>{{ limit?.statusText }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">对应赛事</span><span>{{ limit?.tournamentName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">永久球员身份</span><span class="account-status-text--success">{{ limit?.permanentIdentityStatus }}</span></div>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="赛事方处理结果">
        <strong>{{ limit?.resultTitle }}</strong>
        <AccountStatusBadge type="danger" :text="limit?.resultStatus || '已生效'" />
        <p>{{ limit?.reason }}</p>
      </AccountPanel>
    </div>

    <AccountPanel title="联系赛事方说明">
      <p>如需了解处理依据或提交情况说明，请通过对应赛事公告公布的官方联系方式联系赛事方。</p>
      <p class="identity-eligibility-notice">{{ limit?.notice }}</p>
      <div class="account-panel__action account-panel__action--right">
        <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity')">
          返回球员身份
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>