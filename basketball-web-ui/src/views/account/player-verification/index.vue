<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import PlayerVerificationForm from '../components/PlayerVerificationForm.vue'
import PlayerVerificationSummary from '../components/PlayerVerificationSummary.vue'
import PlayerVerificationStatusCard from '../components/PlayerVerificationStatusCard.vue'
import { useAccountStore } from '@/stores/account'
import type { PlayerVerificationSnapshot } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const accountStore = useAccountStore()
const { t } = useI18n()

const showSubmitConfirm = ref(false)
const showWithdrawConfirm = ref(false)
const showChangeForm = ref(false)
const pendingSubmit = ref<PlayerVerificationSnapshot | null>(null)

const profile = computed(() => accountStore.profile)
const verification = computed(() => accountStore.verification)

const status = computed(() => verification.value?.status || 'NOT_APPLIED')

const effectiveData = computed(() => {
  if (verification.value?.currentEffectiveData) return verification.value.currentEffectiveData
  if (status.value === 'APPROVED' || status.value === 'CHANGE_PENDING') {
    return verification.value
  }
  return null
})

const pendingData = computed(() => verification.value?.pendingChangeData)

const bannerConfig = computed(() => {
  switch (status.value) {
    case 'NOT_APPLIED':
      return {
        title: t('accountIdentity.verification.notAppliedTitle'),
        description: t('accountIdentity.verification.notAppliedDesc'),
        statusText: t('accountIdentity.status.notVerified'),
        statusType: 'warning' as const,
      }
    case 'PENDING':
      return {
        title: t('accountIdentity.verification.pendingTitle'),
        description: t('accountIdentity.verification.pendingDesc'),
        statusText: t('accountIdentity.status.pending'),
        statusType: 'warning' as const,
      }
    case 'APPROVED':
      return {
        title: t('accountIdentity.verification.approvedTitle'),
        description: t('accountIdentity.verification.approvedDesc'),
        statusText: t('accountIdentity.status.certifiedPlayer'),
        statusType: 'success' as const,
      }
    case 'CHANGE_PENDING':
      return {
        title: t('accountIdentity.verification.changePendingTitle'),
        description: t('accountIdentity.verification.changePendingDesc'),
        statusText: t('accountIdentity.status.certifiedPlayer'),
        statusType: 'success' as const,
      }
    case 'REJECTED':
      return {
        title: t('accountIdentity.verification.rejectedTitle'),
        description: t('accountIdentity.verification.rejectedDesc'),
        statusText: t('accountIdentity.status.needsChange'),
        statusType: 'danger' as const,
      }
    default:
      return {
        title: t('accountIdentity.verification.fallbackTitle'),
        description: '',
        statusText: '',
        statusType: 'default' as const,
      }
  }
})

function requestSubmit(data: PlayerVerificationSnapshot) {
  pendingSubmit.value = data
  showSubmitConfirm.value = true
}

function confirmSubmit() {
  if (!pendingSubmit.value) return
  if (status.value === 'REJECTED') {
    accountStore.resubmitPlayerVerification(pendingSubmit.value)
  } else if (showChangeForm.value) {
    accountStore.submitVerificationChange(pendingSubmit.value)
    showChangeForm.value = false
  } else {
    accountStore.submitPlayerVerification(pendingSubmit.value)
  }
  pendingSubmit.value = null
  ElMessage.success(t('accountIdentity.feedback.submitSuccess'))
}

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showChangeForm.value = false
  ElMessage.success(t('accountIdentity.feedback.withdrawDone'))
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountIdentity.verification.pageTitle')"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.verification.crumb') },
      ]"
    />

    <AccountStatusBanner v-bind="bannerConfig" />

    <!-- NOT_APPLIED -->
    <div v-if="status === 'NOT_APPLIED'" class="verification-layout">
      <AccountPanel :title="t('accountIdentity.verification.introTitle')">
        <p class="verification-intro">
          {{ t('accountIdentity.verification.intro') }}
        </p>
      </AccountPanel>
      <AccountPanel :title="t('accountIdentity.verification.formTitle')">
        <PlayerVerificationForm @submit="requestSubmit" />
      </AccountPanel>
    </div>

    <!-- PENDING -->
    <div v-else-if="status === 'PENDING'" class="verification-layout verification-layout--split">
      <AccountPanel :title="t('accountIdentity.verification.submittedTitle')">
        <PlayerVerificationSummary
          v-if="verification"
          :nickname="profile?.nickname || ''"
          :avatar-url="profile?.avatarUrl"
          :data="verification"
          :submitted-at="verification.submittedAt"
          :application-no="verification.applicationNo"
        />
      </AccountPanel>
      <PlayerVerificationStatusCard
        :title="t('accountIdentity.verification.reviewCardTitle')"
        :status-text="t('accountIdentity.status.pending')"
        status-type="warning"
        :description="t('accountIdentity.verification.reviewCardDesc')"
      >
        <div class="verification-rules">
          <p>{{ t('accountIdentity.verification.duringReview') }}</p>
          <ol>
            <li>{{ t('accountIdentity.verification.ruleInactive') }}</li>
            <li>{{ t('accountIdentity.verification.ruleWithdraw') }}</li>
          </ol>
        </div>
        <button type="button" class="account-btn-secondary" @click="showWithdrawConfirm = true">
          {{ t('accountIdentity.actions.withdrawAndEdit') }}
        </button>
      </PlayerVerificationStatusCard>
    </div>

    <!-- APPROVED -->
    <div v-else-if="status === 'APPROVED' && !showChangeForm" class="verification-layout verification-layout--split">
      <AccountPanel :title="t('accountIdentity.verification.effectiveTitle')">
        <PlayerVerificationSummary
          v-if="effectiveData"
          :nickname="profile?.nickname || ''"
          :avatar-url="profile?.avatarUrl"
          :data="effectiveData"
        />
        <button type="button" class="account-btn-primary verification-action" @click="showChangeForm = true">
          {{ t('accountIdentity.actions.applyChangeCert') }}
        </button>
      </AccountPanel>
      <div class="verification-side">
        <PlayerVerificationStatusCard
          :title="t('accountIdentity.verification.currentStatus')"
          :status-text="t('accountIdentity.status.reviewPassed')"
          status-type="success"
          :description="t('accountIdentity.verification.effectiveAt', { time: formatDateTime(verification?.effectiveAt) })"
        />
        <AccountPanel :title="t('accountIdentity.verification.rulesTitle')">
          <ol class="verification-rules">
            <li>{{ t('accountIdentity.verification.ruleAllowEdit') }}</li>
            <li>{{ t('accountIdentity.verification.ruleReaudit') }}</li>
            <li>{{ t('accountIdentity.verification.ruleKeepEffective') }}</li>
            <li>{{ t('accountIdentity.verification.ruleUnchanged') }}</li>
          </ol>
        </AccountPanel>
      </div>
    </div>

    <!-- CHANGE_PENDING -->
    <div v-else-if="status === 'CHANGE_PENDING'" class="verification-layout">
      <div class="verification-layout verification-layout--split">
        <AccountPanel :title="t('accountIdentity.verification.effectiveVersion')">
          <PlayerVerificationSummary
            v-if="effectiveData"
            :nickname="profile?.nickname || ''"
            :data="effectiveData"
            :title="t('accountIdentity.verification.effectiveSummary')"
          />
        </AccountPanel>
        <AccountPanel :title="t('accountIdentity.verification.pendingVersion')">
          <PlayerVerificationSummary
            v-if="pendingData"
            :nickname="profile?.nickname || ''"
            :data="pendingData"
            :submitted-at="verification?.submittedAt"
            :application-no="verification?.applicationNo"
            :title="t('accountIdentity.verification.newVersion')"
          />
        </AccountPanel>
      </div>
      <button type="button" class="account-btn-secondary" @click="showWithdrawConfirm = true">
        {{ t('accountIdentity.actions.withdrawChange') }}
      </button>
    </div>

    <!-- REJECTED -->
    <div v-else-if="status === 'REJECTED'" class="verification-layout verification-layout--split">
      <div class="verification-reject">
        <AccountPanel :title="t('accountIdentity.review.reviewResult')">
          <p class="verification-reject__title">{{ t('accountIdentity.verification.rejectedResult') }}</p>
          <p>{{ t('accountIdentity.verification.reviewedAt', { time: formatDateTime(verification?.reviewedAt) }) }}</p>
          <p class="verification-reject__hint">{{ t('accountIdentity.verification.notLocked') }}</p>
        </AccountPanel>
        <AccountPanel :title="t('accountIdentity.verification.adminComment')">
          <p>{{ verification?.reviewerComment }}</p>
        </AccountPanel>
        <AccountPanel :title="t('accountIdentity.verification.submittedTitle')">
          <PlayerVerificationSummary
            v-if="verification"
            :nickname="profile?.nickname || ''"
            :data="verification"
            :highlight-fields="verification.rejectedFields"
          />
        </AccountPanel>
      </div>
      <div class="verification-side">
        <AccountPanel :title="t('accountIdentity.verification.nextTitle')">
          <ol class="verification-rules">
            <li>{{ t('accountIdentity.verification.nextRead') }}</li>
            <li>{{ t('accountIdentity.verification.nextFix') }}</li>
            <li>{{ t('accountIdentity.verification.nextResubmit') }}</li>
          </ol>
          <p class="verification-reject__hint">{{ t('accountIdentity.verification.resubmitHint') }}</p>
        </AccountPanel>
        <AccountPanel :title="t('accountIdentity.verification.resubmitTitle')">
          <PlayerVerificationForm
            :initial="verification || undefined"
            :rejected-fields="verification?.rejectedFields"
            :submit-label="t('accountIdentity.actions.resubmit')"
            @submit="requestSubmit"
          />
        </AccountPanel>
      </div>
    </div>

    <!-- Change form from APPROVED -->
    <div v-if="showChangeForm && status === 'APPROVED'" class="verification-layout">
      <AccountPanel :title="t('accountIdentity.actions.applyChangeCert')">
        <PlayerVerificationForm
          :initial="effectiveData || undefined"
          :submit-label="t('accountIdentity.actions.submitChangeRequest')"
          @submit="requestSubmit"
          @cancel="showChangeForm = false"
        />
      </AccountPanel>
    </div>

    <AccountConfirmDialog
      v-model="showSubmitConfirm"
      :title="t('accountIdentity.verification.confirmSubmitTitle')"
      :message="t('accountIdentity.verification.confirmSubmitMessage')"
      :confirm-text="t('accountIdentity.actions.confirmSubmit')"
      :cancel-text="t('accountIdentity.actions.cancel')"
      @confirm="confirmSubmit"
    />

    <AccountConfirmDialog
      v-model="showWithdrawConfirm"
      :title="t('accountIdentity.verification.confirmWithdrawTitle')"
      :message="t('accountIdentity.verification.confirmWithdrawMessage')"
      :confirm-text="t('accountIdentity.actions.confirmWithdraw')"
      :cancel-text="t('accountIdentity.actions.cancel')"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view {
  width: 100%;
  min-width: 0;
}

.verification-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-layout--split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.verification-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-intro {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #696969;
}

.verification-rules {
  margin: 12px 0;
  padding-left: 18px;
  font-size: 13px;
  color: #696969;
  line-height: 1.7;
}

.verification-action {
  margin-top: 16px;
}

.verification-reject {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-reject__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 900;
  color: var(--account-danger);
}

.verification-reject__hint {
  font-size: 13px;
  color: #696969;
}

@media (max-width: 991px) {
  .verification-layout--split {
    grid-template-columns: 1fr;
  }
}
</style>
