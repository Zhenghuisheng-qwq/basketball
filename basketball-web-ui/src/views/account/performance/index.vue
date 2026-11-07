<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import PublicPlayerDetail from '@/views/player/detail/index.vue'
import { useAccountStore } from '@/stores/account'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'
import { goToPlayerDetail } from '@/utils/playerNavigate'

const { t } = useI18n()
const router = useRouter()
const accountStore = useAccountStore()
const loading = ref(true)
const loadError = ref('')

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter'), to: '/account/overview' },
  { label: t('accountShell.sidebar.performance') },
])

const playerId = computed(() => {
  const cert = accountStore.playerCert
  if (!cert?.playerId) return ''
  if (cert.certificationStatus !== 3 && !accountStore.isVerifiedPlayer) return ''
  return String(cert.playerId).trim()
})

async function loadPage() {
  loading.value = true
  loadError.value = ''
  try {
    if (!accountStore.currentAccount) {
      await accountStore.loadOverview()
    }
    if (!accountStore.playerCert?.playerId) {
      await accountStore.loadPlayerCertification()
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountShell.overview.loadFailed')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadPage()
})

function openPublicPage() {
  if (!playerId.value) return
  goToPlayerDetail(router, playerId.value, 'overview')
}
</script>

<template>
  <div class="account-view account-performance">
    <AccountPageHeader
      :zh-title="t('accountShell.sidebar.performance')"
      en-title="MY PERFORMANCE"
      :breadcrumb-items="breadcrumbs"
    />

    <div v-if="loading" class="account-view__loading">{{ t('accountShell.common.loading') }}</div>

    <AccountEmptyState
      v-else-if="loadError"
      :message="t('accountShell.overview.loadFailed')"
      :subtitle="loadError"
    >
      <button type="button" class="account-btn-primary" @click="loadPage">{{ t('accountShell.common.retry') }}</button>
    </AccountEmptyState>

    <AccountEmptyState
      v-else-if="!playerId"
      :message="t('accountShell.performancePage.needVerification')"
    >
      <button type="button" class="account-btn-primary" @click="router.push(resolvePlayerIdentityRoute(accountStore))">
        {{ t('accountShell.performancePage.goVerify') }}
      </button>
    </AccountEmptyState>

    <template v-else>
      <div class="account-performance__public">
        <button type="button" class="account-btn-secondary" @click="openPublicPage">
          {{ t('accountShell.performancePage.openPublic') }}
        </button>
      </div>
      <PublicPlayerDetail :source-player-id="playerId" self-view />
    </template>
  </div>
</template>

<style scoped lang="scss">
.account-performance__public {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
</style>
