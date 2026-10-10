<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'
import type { StepItem } from '@/components/account/AccountStepper.vue'

const props = defineProps<{
  statusMode: 'pending' | 'rejected' | 'approved'
}>()

const router = useRouter()
const accountStore = useAccountStore()
const { t, te } = useI18n()
const showWithdraw = ref(false)

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const cert = computed(() => accountStore.playerCert)

onMounted(async () => {
  try {
    await accountStore.loadPlayerCertification()
  } catch {
    // continue with existing state
  }
  const currentStatus = verification.value?.status
  if (props.statusMode === 'pending' && currentStatus !== 'PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'rejected' && currentStatus !== 'REJECTED') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'approved' && currentStatus !== 'APPROVED' && currentStatus !== 'CHANGE_PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
})

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

const reviewRemark = computed(
  () => cert.value?.rejectReason || verification.value?.reviewerComment || t('accountIdentity.status.noRemark'),
)

const pageConfig = computed(() => {
  if (props.statusMode === 'pending') {
    return {
      zhTitle: t('accountIdentity.review.pageTitle'),
      enTitle: 'PLAYER VERIFICATION',
      crumb: t('accountIdentity.status.pending'),
      bannerTitle: t('accountIdentity.review.applicationTitle'),
      bannerDesc: t('accountIdentity.review.pendingDesc'),
      bannerStatus: t('accountIdentity.status.pending'),
      bannerType: 'warning' as const,
      steps: [
        { label: t('accountIdentity.steps.submit'), state: 'done' },
        { label: t('accountIdentity.steps.review'), state: 'current' },
        { label: t('accountIdentity.steps.active'), state: 'upcoming' },
      ] as StepItem[],
      leftTitle: t('accountIdentity.review.thisSubmission'),
      badgeType: 'warning' as const,
      badgeText: t('accountIdentity.status.pending'),
      footnote: t('accountIdentity.review.pendingFootnote'),
    }
  }
  if (props.statusMode === 'rejected') {
    return {
      zhTitle: t('accountIdentity.review.pageTitle'),
      enTitle: 'PLAYER VERIFICATION',
      crumb: t('accountIdentity.status.rejected'),
      bannerTitle: t('accountIdentity.review.rejectedTitle'),
      bannerDesc: t('accountIdentity.review.rejectedDesc'),
      bannerStatus: t('accountIdentity.status.rejected'),
      bannerType: 'danger' as const,
      steps: [
        { label: t('accountIdentity.steps.submit'), state: 'done' },
        { label: t('accountIdentity.steps.review'), state: 'failed', subLabel: t('accountIdentity.status.rejected') },
        { label: t('accountIdentity.steps.active'), state: 'upcoming' },
      ] as StepItem[],
      leftTitle: t('accountIdentity.review.originalSubmission'),
      badgeType: 'danger' as const,
      badgeText: t('accountIdentity.status.rejected'),
      footnote: t('accountIdentity.review.rejectedFootnote'),
    }
  }
  return {
    zhTitle: t('accountIdentity.review.pageTitle'),
    enTitle: 'PLAYER VERIFICATION',
    crumb: t('accountIdentity.status.passed'),
    bannerTitle: t('accountIdentity.review.approvedBannerTitle'),
    bannerDesc: t('accountIdentity.review.approvedDesc'),
    bannerStatus: t('accountIdentity.status.approved'),
    bannerType: 'success' as const,
    steps: [
      { label: t('accountIdentity.steps.submit'), state: 'done' },
      { label: t('accountIdentity.steps.review'), state: 'done' },
      { label: t('accountIdentity.steps.active'), state: 'done' },
    ] as StepItem[],
    leftTitle: t('accountIdentity.review.effectiveMaterials'),
    badgeType: 'success' as const,
    badgeText: t('accountIdentity.status.certifiedPlayer'),
    footnote: t('accountIdentity.review.approvedFootnote'),
  }
})

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showWithdraw.value = false
  ElMessage.success(t('accountIdentity.feedback.withdrawThenEdit'))
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goPlatforms() {
  router.push({ name: 'AccountPlatformGameId' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="pageConfig.zhTitle"
      :en-title="pageConfig.enTitle"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.title'), to: '/account/player-identity' },
        { label: pageConfig.crumb },
      ]"
    />

    <AccountStatusBanner
      :title="pageConfig.bannerTitle"
      :description="pageConfig.bannerDesc"
      :status-text="pageConfig.bannerStatus"
      :status-type="pageConfig.bannerType"
    />

    <AccountStepper :steps="pageConfig.steps" />

    <div class="identity-review-grid identity-card-grid">
      <AccountPanel :title="pageConfig.leftTitle">
        <div class="identity-review-profile">
          <div
            class="identity-review-profile__avatar"
            :class="{
              'identity-review-profile__avatar--success': statusMode === 'approved',
              'identity-review-profile__avatar--error': statusMode === 'rejected',
            }"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge :type="pageConfig.badgeType" :text="pageConfig.badgeText" />
          </div>
        </div>
        <div class="account-info-list">
          <template v-if="statusMode === 'approved'">
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.boundPlatform') }}</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gameId') }}</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.platformStatus') }}</span><span class="account-status-text--success">{{ t('accountIdentity.status.effective') }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.position') }}</span><span>{{ positionLabel(verification?.gamePosition) }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.certStatus') }}</span><span class="account-status-text--success">{{ t('accountIdentity.status.longTerm') }}</span></div>
          </template>
          <template v-else>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gamePlatform') }}</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gameId') }}</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.position') }}</span><span>{{ positionLabel(verification?.gamePosition) }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.proof') }}</span><span>{{ t('accountIdentity.review.proofCount', { count: 1 }) }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.submittedAt') }}</span><span>{{ formatDateTime(verification?.submittedAt) }}</span></div>
          </template>
        </div>
      </AccountPanel>

      <AccountPanel :title="t('accountIdentity.index.currentStatus')">
        <div
          v-if="statusMode === 'pending'"
          class="identity-review-result identity-review-result--pending"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/></svg>
          </div>
          <strong>{{ t('accountIdentity.review.pendingHeadline') }}</strong>
          <AccountStatusBadge type="warning" :text="t('accountIdentity.status.pending')" />
          <p>{{ t('accountIdentity.review.pendingReviewAll') }}</p>
          <p>{{ t('accountIdentity.review.pendingNoResubmit') }}</p>
          <button type="button" class="account-btn-secondary" @click="showWithdraw = true">{{ t('accountIdentity.actions.withdrawEdit') }}</button>
        </div>

        <div
          v-else-if="statusMode === 'rejected'"
          class="identity-review-result identity-review-result--failed"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          </div>
          <strong>{{ t('accountIdentity.review.rejectedTitle') }}</strong>
          <AccountStatusBadge type="danger" :text="t('accountIdentity.status.rejected')" />
          <p class="identity-review-result__remark-label">{{ t('accountIdentity.review.adminRemark') }}</p>
          <p class="identity-review-result__remark">{{ reviewRemark }}</p>
          <button type="button" class="account-btn-primary" @click="goApply">{{ t('accountIdentity.actions.resubmit') }}</button>
        </div>

        <div
          v-else
          class="identity-review-result identity-review-result--success"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
          <span class="identity-review-result__label">{{ t('accountIdentity.fields.permanentPlayerId') }}</span>
          <strong class="identity-review-result__player-no">{{ account?.playerNo }}</strong>
          <p>{{ t('accountIdentity.review.certStatusActive') }}</p>
          <p>{{ t('accountIdentity.review.permanentIdLocked') }}</p>
          <button type="button" class="account-btn-secondary" @click="goPlatforms">{{ t('accountIdentity.actions.requestEdit') }}</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ pageConfig.footnote }}</p>

    <div v-if="statusMode === 'approved'" class="identity-shortcuts">
      <button type="button" class="account-btn-secondary" @click="goPlatforms">{{ t('accountIdentity.actions.managePlatforms') }}</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/change-history')">{{ t('accountIdentity.actions.changeHistory') }}</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/my-tournaments')">{{ t('accountIdentity.actions.myEvents') }}</button>
    </div>

    <AccountConfirmDialog
      v-if="statusMode === 'pending'"
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
