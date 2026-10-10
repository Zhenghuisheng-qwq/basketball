<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import { useAccountStore } from '@/stores/account'
import { formatDateTime } from '@/utils/formatDate'
import { v4PlatformLabel } from '@/utils/v4Display'
import type { IdentityChangeRecord } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()
const { t, te } = useI18n()

const loading = ref(true)
const loadError = ref('')
const pageSizeOptions = [10, 20, 50]

const account = computed(() => accountStore.currentAccount)
const history = computed(() => accountStore.identityChangeHistory)
const showPager = computed(() => accountStore.identityChangeTotal > accountStore.identityChangePageSize)

function platformLabel(platform?: string) {
  if (!platform) return '—'
  const code = platform.toUpperCase()
  const key = `accountIdentity.platform.${code}`
  if (te(key)) return t(key)
  return v4PlatformLabel(platform) || platform
}

function changeTypeLabel(row: IdentityChangeRecord) {
  const code = (row.changeType || '').toUpperCase()
  const key = code ? `accountIdentity.changeType.${code}` : ''
  if (key && te(key)) return t(key)
  return row.changeTypeName || t('accountIdentity.changeType.OTHER')
}

async function loadHistory(page = accountStore.identityChangePage, pageSize = accountStore.identityChangePageSize) {
  loading.value = true
  loadError.value = ''
  try {
    if (!accountStore.verification) {
      await accountStore.loadOverview()
    }
    await accountStore.loadIdentityChangeHistory({ currentPage: page, pageSize })
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountIdentity.feedback.loadHistoryFailed')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadHistory(1)
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountIdentity.history.pageTitle')"
      en-title="IDENTITY CHANGE HISTORY"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.title'), to: '/account/player-identity' },
        { label: t('accountIdentity.history.crumb') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountIdentity.history.bannerTitle', { id: account?.playerNo || '' })"
      :description="t('accountIdentity.history.bannerDesc')"
      :status-text="t('accountIdentity.status.readOnly')"
      status-type="info"
    />

    <AccountPanel :title="t('accountIdentity.history.panelTitle')">
      <p v-if="loading" class="identity-loading">{{ t('accountIdentity.feedback.loading') }}</p>
      <p v-else-if="loadError" class="identity-error">{{ loadError }}</p>
      <AccountEmptyState
        v-else-if="!history.length"
        large
        :message="t('accountIdentity.history.empty')"
        :subtitle="t('accountIdentity.history.emptyHint')"
      />
      <template v-else>
        <div class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>{{ t('accountIdentity.fields.effectiveTime') }}</th>
                <th>{{ t('accountIdentity.fields.platform') }}</th>
                <th>{{ t('accountIdentity.fields.changeType') }}</th>
                <th>{{ t('accountIdentity.fields.oldGameId') }}</th>
                <th>{{ t('accountIdentity.fields.newGameId') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in history" :key="`${row.effectiveTime}-${row.platform}-${index}`">
                <td>{{ formatDateTime(row.effectiveTime) || row.effectiveTime }}</td>
                <td>{{ platformLabel(row.platform) }}</td>
                <td>{{ changeTypeLabel(row) }}</td>
                <td>{{ row.oldGameId }}</td>
                <td>{{ row.newGameId }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <MyEventsPagination
          v-if="showPager"
          :page="accountStore.identityChangePage"
          :page-size="accountStore.identityChangePageSize"
          :total="accountStore.identityChangeTotal"
          :page-size-options="pageSizeOptions"
          :meta-text="t('accountIdentity.history.total', { count: accountStore.identityChangeTotal })"
          :disabled="loading"
          :aria-label="t('accountIdentity.history.pagination')"
          @update:page="(p) => loadHistory(p, accountStore.identityChangePageSize)"
          @update:page-size="(s) => loadHistory(1, s)"
        />
      </template>
      <p class="identity-footnote">{{ t('accountIdentity.history.footnote') }}</p>
      <div class="account-panel__action account-panel__action--right">
        <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/platforms')">
          {{ t('accountIdentity.actions.backToPlatforms') }}
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
