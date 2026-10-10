<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import { useAccountStore } from '@/stores/account'
import { mapVerificationPlatform } from '@/utils/playerIdentity'
import { formatDateTime } from '@/utils/formatDate'

const router = useRouter()
const accountStore = useAccountStore()
const { t, te } = useI18n()
const showWithdraw = ref(false)

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)

const steps = computed(() => [
  { label: t('accountIdentity.steps.submit'), state: 'done' as const },
  { label: t('accountIdentity.steps.review'), state: 'current' as const },
  { label: t('accountIdentity.steps.active'), state: 'upcoming' as const },
])

const platformLabel = computed(() => {
  const p = verification.value?.primaryPlatform
  if (!p) return '-'
  const mapped = mapVerificationPlatform(p)
  const key = `accountIdentity.platform.${mapped}`
  return te(key) ? t(key) : p
})

function positionLabel(code?: string | null) {
  if (!code) return '-'
  const key = `accountIdentity.position.${code}`
  return te(key) ? t(key) : code
}

async function confirmWithdraw() {
  try {
    await accountStore.withdrawPlayerVerification()
    showWithdraw.value = false
    ElMessage.success(t('accountIdentity.feedback.withdrawThenEdit'))
    router.push({ name: 'AccountPlayerIdentityApply' })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountIdentity.feedback.withdrawFailed'))
  }
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
        { label: t('accountIdentity.status.pending') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountIdentity.review.applicationTitle')"
      :description="t('accountIdentity.review.pendingDesc')"
      :status-text="t('accountIdentity.status.pending')"
      status-type="warning"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel :title="t('accountIdentity.review.thisSubmission')">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge type="warning" :text="t('accountIdentity.status.pending')" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gamePlatform') }}</span><span>{{ platformLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gameId') }}</span><span>{{ verification?.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.position') }}</span><span>{{ positionLabel(verification?.gamePosition) }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.proof') }}</span><span>{{ t('accountIdentity.review.proofCount', { count: 1 }) }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.submittedAt') }}</span><span>{{ formatDateTime(verification?.submittedAt) }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel :title="t('accountIdentity.index.currentStatus')">
        <div class="identity-review-result identity-review-result--pending">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/></svg>
          </div>
          <strong>{{ t('accountIdentity.review.pendingHeadline') }}</strong>
          <AccountStatusBadge type="warning" :text="t('accountIdentity.status.pending')" />
          <p>{{ t('accountIdentity.review.pendingReviewAll') }}</p>
          <p>{{ t('accountIdentity.review.pendingNoResubmit') }}</p>
          <button type="button" class="account-btn-secondary" @click="showWithdraw = true">{{ t('accountIdentity.actions.withdrawEdit') }}</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ t('accountIdentity.review.pendingFootnote') }}</p>

    <AccountConfirmDialog
      v-model="showWithdraw"
      :title="t('accountIdentity.review.withdrawTitle')"
      :message="t('accountIdentity.review.withdrawMessage')"
      :confirm-text="t('accountIdentity.actions.confirmWithdraw')"
      :cancel-text="t('accountIdentity.actions.cancel')"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
