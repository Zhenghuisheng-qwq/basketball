<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const history = computed(() => accountStore.identityChangeHistory)
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="资料变更记录"
      en-title="IDENTITY CHANGE HISTORY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '资料变更记录' },
      ]"
    />

    <AccountStatusBanner
      :title="`永久球员身份 ${account?.playerNo}`"
      description="查看平台与游戏 ID 已生效的历史变更"
      status-text="只读记录"
      status-type="info"
    />

    <AccountPanel title="平台与游戏 ID 变更记录">
      <AccountEmptyState
        v-if="!history.length"
        large
        message="暂无变更记录"
        subtitle="已生效的平台与游戏 ID 变更将在此展示"
      />
      <div v-else class="platform-table-wrap">
        <table class="platform-table">
          <thead>
            <tr>
              <th>生效时间</th>
              <th>平台</th>
              <th>原游戏 ID</th>
              <th>新游戏 ID</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in history" :key="`${row.effectiveTime}-${index}`">
              <td>{{ row.effectiveTime }}</td>
              <td>{{ IDENTITY_PLATFORM_LABELS[row.platform] }}</td>
              <td>{{ row.oldGameId }}</td>
              <td>{{ row.newGameId }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="identity-footnote">历史赛事继续显示当届平台与游戏 ID 快照；审核中的修改不会作为已生效记录展示。</p>
      <div class="account-panel__action account-panel__action--right">
        <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/platforms')">
          返回平台与游戏 ID
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>