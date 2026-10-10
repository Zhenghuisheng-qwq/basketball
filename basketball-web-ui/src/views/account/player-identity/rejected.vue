<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { mapVerificationPlatform } from '@/utils/playerIdentity'
import { formatDateTime } from '@/utils/formatDate'

const router = useRouter()
const accountStore = useAccountStore()
const { t, te } = useI18n()

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
    reviewRemark: v?.reviewerComment || t('accountIdentity.review.defaultRemark'),
    submitTime: v?.submittedAt ? formatDateTime(v.submittedAt) : '',
  }
})

const steps = computed(() => [
  { label: t('accountIdentity.steps.submit'), state: 'done' as const },
  { label: t('accountIdentity.steps.review'), state: 'failed' as const, subLabel: t('accountIdentity.status.rejected') },
  { label: t('accountIdentity.steps.active'), state: 'upcoming' as const },
])

const platformLabel = computed(() => {
  const key = `accountIdentity.platform.${rejected.value.platform}`
  return te(key) ? t(key) : rejected.value.platform
})

const positionLabel = computed(() => {
  const code = rejected.value.position
  if (!code) return '-'
  const key = `accountIdentity.position.${code}`
  return te(key) ? t(key) : code
})

function goResubmit() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountIdentity.review.pageTitle')"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.title'), to: '/account/player-identity' },
        { label: t('accountIdentity.status.rejected') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountIdentity.review.rejectedTitle')"
      :description="t('accountIdentity.review.rejectedDesc')"
      :status-text="t('accountIdentity.status.rejected')"
      status-type="danger"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel :title="t('accountIdentity.review.originalSubmission')">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar identity-review-profile__avatar--error">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ rejected.playerName }}</strong>
            <AccountStatusBadge type="danger" :text="t('accountIdentity.status.rejected')" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gamePlatform') }}</span><span>{{ platformLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gameId') }}</span><span>{{ rejected.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.position') }}</span><span>{{ positionLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.proof') }}</span><span>{{ t('accountIdentity.review.proofCount', { count: rejected.proofCount }) }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.submittedAt') }}</span><span>{{ rejected.submitTime }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel :title="t('accountIdentity.review.reviewResult')">
        <div class="identity-review-result identity-review-result--failed">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          </div>
          <strong>{{ t('accountIdentity.review.rejectedTitle') }}</strong>
          <AccountStatusBadge type="danger" :text="t('accountIdentity.status.rejected')" />
          <p class="identity-review-result__remark-label">{{ t('accountIdentity.review.adminRemark') }}</p>
          <p class="identity-review-result__remark">{{ rejected.reviewRemark }}</p>
          <button type="button" class="account-btn-primary" @click="goResubmit">{{ t('accountIdentity.actions.resubmit') }}</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ t('accountIdentity.review.rejectedFootnote') }}</p>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
