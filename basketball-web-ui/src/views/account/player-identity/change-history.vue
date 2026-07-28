<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const router = useRouter()
const accountStore = useAccountStore()

const loading = ref(true)
const loadError = ref('')

const account = computed(() => accountStore.currentAccount)
const history = computed(() => accountStore.identityChangeHistory)

onMounted(async () => {
  loading.value = true
  loadError.value = ''
  try {
    if (!accountStore.verification) {
      await accountStore.loadOverview()
    }
    await accountStore.loadIdentityChangeHistory()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载变更记录失败'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
})
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
      :title="`永久球员身份 ${account?.playerNo || ''}`"
      description="查看平台与游戏 ID 已生效的历史变更"
      status-text="只读记录"
      status-type="info"
    />

    <AccountPanel title="平台与游戏 ID 变更记录">
      <p v-if="loading" class="identity-loading">加载中…</p>
      <p v-else-if="loadError" class="identity-error">{{ loadError }}</p>
      <AccountEmptyState
        v-else-if="!history.length"
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
              <th>变更类型</th>
              <th>原游戏 ID</th>
              <th>新游戏 ID</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in history" :key="`${row.effectiveTime}-${row.platform}-${index}`">
              <td>{{ formatDateTime(row.effectiveTime) || row.effectiveTime }}</td>
              <td>{{ IDENTITY_PLATFORM_LABELS[row.platform] || row.platform }}</td>
              <td>{{ row.changeTypeName || '—' }}</td>
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

.identity-loading,
.identity-error {
  margin: 0 0 1rem;
  color: var(--account-text-muted, #5f6b7a);
}

.identity-error {
  color: #b45309;
}
</style>
