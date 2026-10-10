<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const { t } = useI18n()

const identity = computed(() => accountStore.playerIdentity)
const status = computed(() => identity.value?.status || 'NOT_SUBMITTED')
const cert = computed(() => accountStore.playerCert)

onMounted(async () => {
  try {
    await accountStore.loadPlayerCertification()
  } catch {
    // shown via empty state
  }
})

watch(
  [status, () => route.name],
  ([nextStatus, routeName]) => {
    if (routeName !== 'AccountPlayerIdentity') return
    if (nextStatus === 'PENDING') router.replace({ name: 'AccountPlayerIdentityStatusPending' })
    else if (nextStatus === 'REJECTED') router.replace({ name: 'AccountPlayerIdentityStatusRejected' })
    else if (nextStatus === 'APPROVED' || nextStatus === 'ELIGIBILITY_LIMITED') {
      if (nextStatus === 'ELIGIBILITY_LIMITED') {
        router.replace({ name: 'AccountPlayerEligibility' })
        return
      }
      router.replace({ name: 'AccountPlayerIdentityStatusApproved' })
    }
  },
  { immediate: true },
)

const banner = computed(() => ({
  title: t('accountIdentity.index.bannerTitle'),
  description: t('accountIdentity.index.bannerDesc'),
  statusText: cert.value?.applicationStatusText || t('accountIdentity.status.notVerified'),
  statusType: 'warning' as const,
}))

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountIdentity.title')"
      en-title="PLAYER IDENTITY"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.title') },
      ]"
    />

    <AccountStatusBanner v-bind="banner" />

    <div v-if="accountStore.certLoading" class="identity-loading">{{ t('accountIdentity.feedback.loadingCert') }}</div>

    <div v-else class="account-grid-2 identity-entry-grid">
      <AccountPanel :title="t('accountIdentity.index.currentStatus')">
        <div class="identity-entry-state">
          <div class="identity-entry-state__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" />
            </svg>
          </div>
          <AccountStatusBadge type="warning" :text="cert?.applicationStatusText || t('accountIdentity.status.notVerified')" />
          <p class="identity-entry-state__title">
            {{ cert?.hasPlayerProfile ? t('accountIdentity.index.hasProfile') : t('accountIdentity.index.notApplied') }}
          </p>
          <p v-if="cert?.playerNo" class="identity-entry-hint">{{ t('accountIdentity.index.permanentId', { id: cert.playerNo }) }}</p>
        </div>
      </AccountPanel>

      <AccountPanel :title="t('accountIdentity.index.applyTitle')">
        <ul class="identity-entry-rules">
          <li>{{ t('accountIdentity.index.rulePermanentId') }}</li>
          <li>{{ t('accountIdentity.index.rulePlatform') }}</li>
          <li>{{ t('accountIdentity.index.ruleLongTerm') }}</li>
        </ul>
        <p class="identity-entry-hint">
          {{ t('accountIdentity.index.hintBlocked') }}
        </p>
        <div class="account-panel__action">
          <button
            type="button"
            class="account-btn-primary"
            :disabled="cert?.canEdit === false"
            @click="goApply"
          >
            {{ t('accountIdentity.actions.applyCert') }}
          </button>
        </div>
      </AccountPanel>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.identity-loading {
  padding: 32px 0;
  color: #696969;
}
</style>
