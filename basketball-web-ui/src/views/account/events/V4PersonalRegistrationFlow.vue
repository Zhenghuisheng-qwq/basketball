<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import {
  getV4MyPersonalRegistrations,
  getV4PersonalAvailableDivisions,
  saveV4PersonalRegistration,
  submitV4PersonalRegistration,
  withdrawV4PersonalRegistration,
} from '@/api/v4PlayerEvents'
import type { V4PersonalAvailableDivision } from '@/api/types/v4PlayerEvents'
import { v4GameModeLabel, v4PlatformLabel, v4RosterRuleText } from '@/utils/v4Display'
import { v4PersonalRegistrationStatusLabel } from '@/utils/v4AccountEventsMappers'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const pageLoading = ref(true)
const loadError = ref('')
const saving = ref(false)
const submitting = ref(false)
const division = ref<V4PersonalAvailableDivision | null>(null)
const preferredPosition = ref('')
const personalRegistrationId = ref('')

const divisionId = computed(() => String(route.params.divisionId || ''))
const rosterRuleText = computed(() =>
  v4RosterRuleText(division.value?.lineupMinCount, division.value?.lineupMaxCount, division.value?.gameMode)
)
const isReadonly = computed(() => {
  const status = (division.value?.myRegistrationStatus || '').toUpperCase()
  return ['PENDING_REVIEW', 'APPROVED', 'ASSIGNED', 'CONVERTED'].includes(status)
})

const bannerStatus = computed(() => {
  const status = division.value?.myRegistrationStatus
  return {
    text: v4PersonalRegistrationStatusLabel(status),
    type: status === 'APPROVED' || status === 'CONVERTED' ? 'success' as const
      : status === 'REJECTED' ? 'danger' as const : 'info' as const,
  }
})

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  try {
    const [divisionsRes, registrationsRes] = await Promise.all([
      getV4PersonalAvailableDivisions(),
      getV4MyPersonalRegistrations(),
    ])
    division.value = (divisionsRes.data || []).find((item) => item.divisionId === divisionId.value) || null
    if (!division.value) {
      loadError.value = t('accountEvents.personal.missingDivision')
      return
    }
    const existing = (registrationsRes.data || []).find((item) => item.divisionId === divisionId.value)
    personalRegistrationId.value = existing?.personalRegistrationId || ''
    preferredPosition.value = existing?.preferredPosition || ''
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountEvents.common.loadFailed')
  } finally {
    pageLoading.value = false
  }
}

async function handleSave() {
  if (!division.value) return
  saving.value = true
  try {
    const res = await saveV4PersonalRegistration({
      divisionId: divisionId.value,
      preferredPosition: preferredPosition.value || undefined,
    })
    personalRegistrationId.value = res.data || personalRegistrationId.value
    ElMessage.success(t('accountEvents.common.draftSaved'))
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountEvents.common.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!personalRegistrationId.value) {
    await handleSave()
  }
  if (!personalRegistrationId.value) return
  submitting.value = true
  try {
    await submitV4PersonalRegistration({ personalRegistrationId: personalRegistrationId.value })
    ElMessage.success(t('accountEvents.personal.submitSuccess'))
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountEvents.common.submitFailed'))
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  if (!personalRegistrationId.value) return
  submitting.value = true
  try {
    await withdrawV4PersonalRegistration({ personalRegistrationId: personalRegistrationId.value })
    ElMessage.success(t('accountEvents.common.withdrawn'))
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountEvents.common.withdrawFailed'))
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push({ name: 'AccountMyEvents', query: { tab: 'available' } })
}

onMounted(() => {
  void bootstrap()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountEvents.personal.pageTitle')"
      en-title="Personal Registration"
      :breadcrumb-items="[
        { label: t('accountEvents.breadcrumb.home'), to: '/home' },
        { label: t('accountEvents.breadcrumb.account'), to: '/account/overview' },
        { label: t('accountEvents.breadcrumb.myEvents'), to: '/account/my-events' },
        { label: t('accountEvents.breadcrumb.personalRegistration') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountEvents.personal.bannerTitle')"
      :description="t('accountEvents.personal.bannerDesc')"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <p v-if="pageLoading" class="page-loading">{{ t('accountEvents.common.loading') }}</p>
    <p v-else-if="loadError" class="page-error">{{ loadError }}</p>

    <AccountPanel v-else-if="division" :title="t('accountEvents.personal.info')">
      <div class="info-grid">
        <div><span>{{ t('accountEvents.personal.event') }}</span><strong>{{ division.seasonTitle }}</strong></div>
        <div><span>{{ t('accountEvents.personal.division') }}</span><strong>{{ division.divisionName }}</strong></div>
        <div><span>{{ t('accountEvents.personal.platform') }}</span><strong>{{ v4PlatformLabel(division.platformCode) }}</strong></div>
        <div><span>{{ t('accountEvents.personal.mode') }}</span><strong>{{ v4GameModeLabel(division.gameMode) }}</strong></div>
        <div><span>{{ t('accountEvents.personal.rosterRule') }}</span><strong>{{ rosterRuleText }}</strong></div>
      </div>

      <div class="form-block">
        <label for="preferredPosition">{{ t('accountEvents.personal.positionLabel') }}</label>
        <input
          id="preferredPosition"
          v-model="preferredPosition"
          class="account-input"
          :disabled="isReadonly"
          :placeholder="t('accountEvents.personal.positionPlaceholder')"
        />
      </div>

      <div class="actions">
        <button type="button" class="account-btn-secondary" @click="goBack">{{ t('accountEvents.common.back') }}</button>
        <template v-if="!isReadonly">
          <button type="button" class="account-btn-secondary" :disabled="saving" @click="handleSave">{{ t('accountEvents.common.saveDraft') }}</button>
          <button type="button" class="account-btn-primary" :disabled="submitting" @click="handleSubmit">{{ t('accountEvents.common.submitReview') }}</button>
        </template>
        <button
          v-else-if="division.myRegistrationStatus === 'PENDING_REVIEW'"
          type="button"
          class="account-btn-secondary"
          :disabled="submitting"
          @click="handleWithdraw"
        >
          {{ t('accountEvents.common.withdraw') }}
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
.page-loading,
.page-error {
  padding: 24px 0;
  color: rgba(0, 0, 0, 0.65);
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  margin-bottom: 20px;
  span {
    display: block;
    font-size: 13px;
    color: rgba(0, 0, 0, 0.55);
  }
  strong {
    font-size: 15px;
  }
}
.form-block {
  margin-bottom: 20px;
  label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
  }
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
