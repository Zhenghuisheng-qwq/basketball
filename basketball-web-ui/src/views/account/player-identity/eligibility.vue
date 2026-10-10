<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'

const router = useRouter()
const accountStore = useAccountStore()
const { t } = useI18n()

const account = computed(() => accountStore.currentAccount)
const limit = computed(() => accountStore.eligibility)

const statusLabel = computed(() => {
  if (limit.value?.status === 'LIMITED') return t('accountIdentity.eligibility.playLimited')
  if (limit.value?.status === 'OK') return t('accountIdentity.eligibility.playOk')
  return limit.value?.statusText || ''
})

const identityStatusLabel = computed(() => {
  const text = limit.value?.permanentIdentityStatus || ''
  if (!text || text === 'KEPT') return t('accountIdentity.eligibility.identityKept')
  return text
})

const resultTitleLabel = computed(() => {
  if (limit.value?.status === 'LIMITED') return t('accountIdentity.eligibility.playLimited')
  return limit.value?.resultTitle || ''
})

const resultStatusLabel = computed(() => {
  if (limit.value?.status === 'LIMITED') return t('accountIdentity.eligibility.playLimited')
  if (limit.value?.status === 'OK') return t('accountIdentity.eligibility.playOk')
  if (!limit.value?.resultStatus) return t('accountIdentity.status.effective')
  return limit.value.resultStatus
})

onMounted(async () => {
  try {
    await accountStore.loadPlayerPlatformState()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountIdentity.feedback.loadEligibilityFailed'))
  }
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountIdentity.eligibility.pageTitle')"
      en-title="PLAYER ELIGIBILITY"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.title'), to: '/account/player-identity' },
        { label: t('accountIdentity.eligibility.crumb') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountIdentity.eligibility.bannerTitle')"
      :description="t('accountIdentity.eligibility.bannerDesc')"
      :status-text="t('accountIdentity.eligibility.limited')"
      status-type="danger"
    />

    <div class="identity-review-grid">
      <AccountPanel :title="t('accountIdentity.eligibility.statusTitle')">
        <div class="identity-review-result identity-review-result--warning">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.permanentPlayerId') }}</span><span>{{ account?.playerNo || limit?.playerNo }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.currentStatus') }}</span><span>{{ statusLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.event') }}</span><span>{{ limit?.tournamentName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.permanentIdentity') }}</span><span class="account-status-text--success">{{ identityStatusLabel }}</span></div>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel :title="t('accountIdentity.eligibility.resultTitle')">
        <strong>{{ resultTitleLabel }}</strong>
        <AccountStatusBadge type="danger" :text="resultStatusLabel" />
        <p>{{ limit?.reason }}</p>
      </AccountPanel>
    </div>

    <AccountPanel :title="t('accountIdentity.eligibility.contactTitle')">
      <p>{{ t('accountIdentity.eligibility.contactBody') }}</p>
      <p class="identity-eligibility-notice">{{ limit?.notice }}</p>
      <div class="account-panel__action account-panel__action--right">
        <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity')">
          {{ t('accountIdentity.actions.backToIdentity') }}
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
