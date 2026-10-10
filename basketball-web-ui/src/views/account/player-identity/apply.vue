<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PlayerIdentityApplyForm from '../components/PlayerIdentityApplyForm.vue'
import type { PlayerCertFormPayload } from '../components/PlayerIdentityApplyForm.vue'
import { useAccountStore } from '@/stores/account'

const router = useRouter()
const accountStore = useAccountStore()
const { t, te } = useI18n()
const submitting = ref(false)

const profile = computed(() => accountStore.profile)
const account = computed(() => accountStore.currentAccount)
const cert = computed(() => accountStore.playerCert)

const draft = reactive({
  avatarUrl: '',
  platform: '' as '' | 'PC' | 'PS' | 'XBOX',
  gameAccount: '',
  position: '' as '' | 'PG' | 'SG' | 'SF' | 'PF' | 'C',
})

onMounted(async () => {
  try {
    if (!profile.value) {
      await accountStore.loadProfile()
    }
    draft.avatarUrl = profile.value?.avatarUrl || account.value?.avatarUrl || ''
    await accountStore.loadPlayerCertification()
    if (cert.value) {
      draft.gameAccount = cert.value.gameAccount || ''
      if (cert.value.positionCode) draft.position = cert.value.positionCode as typeof draft.position
      if (cert.value.gameTerminal) {
        const terminal = cert.value.gameTerminal.toUpperCase()
        if (terminal.includes('PS')) draft.platform = 'PS'
        else if (terminal.includes('XBOX')) draft.platform = 'XBOX'
        else draft.platform = 'PC'
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountIdentity.feedback.loadCertFailed'))
  }
})

const statusSummary = computed(() => {
  const platformKey = draft.platform ? `accountIdentity.platform.${draft.platform}` : ''
  return {
    nickname: profile.value?.nickname || account.value?.nickname || t('accountIdentity.status.notFilled'),
    email: profile.value?.maskedEmail || account.value?.maskedEmail || t('accountIdentity.status.notFilled'),
    avatar: draft.avatarUrl.trim() ? t('accountIdentity.status.avatarSet') : t('accountIdentity.status.notUploaded'),
    platform: platformKey && te(platformKey) ? t(platformKey) : t('accountIdentity.status.notSelected'),
    gameAccount: draft.gameAccount.trim() || t('accountIdentity.status.notFilled'),
    position: draft.position && te(`accountIdentity.position.${draft.position}`)
      ? t(`accountIdentity.position.${draft.position}`)
      : (draft.position || t('accountIdentity.status.notSelected')),
  }
})

async function handleSubmit(data: PlayerCertFormPayload) {
  if (submitting.value) return
  submitting.value = true
  try {
    await accountStore.submitPlayerVerificationFromForm({
      platform: data.platform,
      gameAccount: data.gameAccount,
      positionCode: data.position,
      avatarFile: data.avatarFile,
      gameScreenshotFile: data.gameScreenshotFile,
    })
    ElMessage.success(t('accountIdentity.feedback.submitSuccess'))
    router.push({ name: 'AccountPlayerIdentityStatusPending' })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountIdentity.feedback.submitFailed'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountIdentity.apply.pageTitle')"
      en-title="PLAYER VERIFICATION APPLICATION"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.title'), to: '/account/player-identity' },
        { label: t('accountIdentity.apply.crumb') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountIdentity.apply.bannerTitle')"
      :description="t('accountIdentity.apply.bannerDesc')"
      :status-text="cert?.applicationStatusText || t('accountIdentity.status.notSubmitted')"
      status-type="warning"
    />

    <div class="identity-apply-grid">
      <AccountPanel :title="t('accountIdentity.apply.materials')">
        <PlayerIdentityApplyForm
          :initial="{
            avatarUrl: draft.avatarUrl,
            platform: draft.platform || undefined,
            gameAccount: draft.gameAccount,
            position: draft.position || undefined,
          }"
          @submit="handleSubmit"
        >
          <template #actions="{ complete, submit }">
            <div class="identity-apply-footer">
              <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity')">
                {{ t('accountIdentity.actions.backToIdentity') }}
              </button>
              <button
                type="button"
                class="account-btn-primary"
                :disabled="!complete || submitting"
                @click="submit()"
              >
                {{ submitting ? t('accountIdentity.actions.submitting') : t('accountIdentity.actions.submitApplication') }}
              </button>
              <p v-if="!complete" class="identity-apply-footer__hint">{{ t('accountIdentity.apply.incomplete') }}</p>
            </div>
          </template>
        </PlayerIdentityApplyForm>
      </AccountPanel>

      <div class="identity-apply-side">
        <AccountPanel :title="t('accountIdentity.apply.notesTitle')">
          <ul class="identity-entry-rules">
            <li>{{ t('accountIdentity.apply.noteCurrentId') }}</li>
            <li>{{ t('accountIdentity.apply.notePermanentId') }}</li>
            <li>{{ t('accountIdentity.apply.noteUnique') }}</li>
            <li>{{ t('accountIdentity.apply.noteReview') }}</li>
            <li>{{ t('accountIdentity.apply.noteIndependent') }}</li>
          </ul>
        </AccountPanel>

        <AccountPanel :title="t('accountIdentity.apply.currentTitle')">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.nickname') }}</span><span>{{ statusSummary.nickname }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.email') }}</span><span>{{ statusSummary.email }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.avatar') }}</span><span>{{ statusSummary.avatar }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.platform') }}</span><span>{{ statusSummary.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gameId') }}</span><span>{{ statusSummary.gameAccount }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.position') }}</span><span>{{ statusSummary.position }}</span></div>
          </div>
        </AccountPanel>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
