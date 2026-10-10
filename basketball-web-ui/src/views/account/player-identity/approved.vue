<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import CaptainGrantPanel from '@/components/account/team/CaptainGrantPanel.vue'
import { useAccountStore } from '@/stores/account'
import { useTeamStore } from '@/stores/team'
import { mapVerificationPlatform } from '@/utils/playerIdentity'

const router = useRouter()
const accountStore = useAccountStore()
const teamStore = useTeamStore()
const { t, te } = useI18n()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const identity = computed(() => accountStore.playerIdentity)

const steps = computed(() => [
  { label: t('accountIdentity.steps.submit'), state: 'done' as const },
  { label: t('accountIdentity.steps.review'), state: 'done' as const },
  { label: t('accountIdentity.steps.active'), state: 'done' as const },
])

const platformLabel = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  if (!data?.primaryPlatform) return '-'
  const mapped = mapVerificationPlatform(data.primaryPlatform)
  const key = `accountIdentity.platform.${mapped}`
  return te(key) ? t(key) : mapped
})

const gameId = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  return data?.gameId || '-'
})

const position = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  const code = data?.gamePosition || data?.commonPosition || ''
  if (!code) return '-'
  const key = `accountIdentity.position.${code}`
  return te(key) ? t(key) : code
})

onMounted(() => {
  void teamStore.loadQualification()
})
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
        { label: t('accountIdentity.status.passed') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountIdentity.review.approvedBannerTitle')"
      :description="t('accountIdentity.review.approvedDesc')"
      :status-text="t('accountIdentity.status.approved')"
      status-type="success"
    />

    <AccountStepper :steps="steps" />

    <CaptainGrantPanel
      :qualification="teamStore.captainQualification"
      :loading="teamStore.qualificationLoading"
    />

    <div class="identity-review-grid">
      <AccountPanel :title="t('accountIdentity.review.effectiveMaterials')">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar identity-review-profile__avatar--success">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge type="success" :text="t('accountIdentity.status.certifiedPlayer')" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.boundPlatform') }}</span><span>{{ platformLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gameId') }}</span><span>{{ gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.platformStatus') }}</span><span class="account-status-text--success">{{ t('accountIdentity.status.effective') }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.position') }}</span><span>{{ position }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.certStatus') }}</span><span class="account-status-text--success">{{ t('accountIdentity.status.longTerm') }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel :title="t('accountIdentity.fields.permanentIdentity')">
        <div class="identity-review-result identity-review-result--success">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
          <span class="identity-review-result__label">{{ t('accountIdentity.fields.permanentPlayerId') }}</span>
          <strong class="identity-review-result__player-no">{{ account?.playerNo || identity?.playerNo }}</strong>
          <p>{{ t('accountIdentity.review.certStatusActive') }}</p>
          <p>{{ t('accountIdentity.review.permanentIdLocked') }}</p>
          <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/apply')">
            {{ t('accountIdentity.actions.requestEdit') }}
          </button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ t('accountIdentity.review.approvedFootnote') }}</p>

    <div class="identity-shortcuts">
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/platforms')">
        {{ t('accountIdentity.actions.managePlatforms') }}
      </button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/change-history')">
        {{ t('accountIdentity.actions.changeHistory') }}
      </button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/my-tournaments')">
        {{ t('accountIdentity.actions.myEvents') }}
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
