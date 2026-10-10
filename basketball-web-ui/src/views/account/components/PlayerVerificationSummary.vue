<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { PlayerVerificationSnapshot } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const props = defineProps<{
  nickname: string
  avatarUrl?: string
  data: PlayerVerificationSnapshot
  submittedAt?: string
  applicationNo?: string
  highlightFields?: string[]
  title?: string
}>()

const { t, te } = useI18n()

const initials = computed(() => props.nickname.slice(0, 2).toUpperCase())

function platformLabel(code?: string) {
  if (!code) return '-'
  const key = `accountIdentity.platform.${code}`
  return te(key) ? t(key) : code
}

function positionLabel(code?: string) {
  if (!code) return '-'
  const key = `accountIdentity.position.${code}`
  return te(key) ? t(key) : code
}
</script>

<template>
  <div class="verification-summary">
    <h4 v-if="title" class="verification-summary__title">{{ title }}</h4>
    <div class="verification-summary__head">
      <div class="verification-summary__avatar">
        <img v-if="avatarUrl" :src="avatarUrl" :alt="nickname" />
        <span v-else>{{ initials }}</span>
      </div>
      <strong>{{ nickname }}</strong>
    </div>
    <dl class="account-info-list">
      <div class="account-field-row">
        <dt>{{ t('accountIdentity.fields.primaryPlatform') }}</dt>
        <dd>{{ platformLabel(data.primaryPlatform) }}</dd>
      </div>
      <div class="account-field-row" :class="{ 'field-error': highlightFields?.includes('gameId') }">
        <dt>{{ t('accountIdentity.fields.gameId') }}</dt>
        <dd>{{ data.gameId }}</dd>
      </div>
      <div class="account-field-row" :class="{ 'field-error': highlightFields?.includes('gameIdScreenshotUrl') }">
        <dt>{{ t('accountIdentity.fields.screenshot') }}</dt>
        <dd>{{ data.gameIdScreenshotUrl ? t('accountIdentity.status.uploaded') : t('accountIdentity.status.notUploaded') }}</dd>
      </div>
      <div class="account-field-row">
        <dt>{{ t('accountIdentity.fields.gamePosition2k') }}</dt>
        <dd>{{ positionLabel(data.gamePosition) }}</dd>
      </div>
      <div class="account-field-row">
        <dt>{{ t('accountIdentity.fields.position') }}</dt>
        <dd>{{ positionLabel(data.commonPosition) }}</dd>
      </div>
      <div v-if="submittedAt" class="account-field-row">
        <dt>{{ t('accountIdentity.fields.submittedAt') }}</dt>
        <dd>{{ formatDateTime(submittedAt) }}</dd>
      </div>
      <div v-if="applicationNo" class="account-field-row">
        <dt>{{ t('accountIdentity.fields.applicationNo') }}</dt>
        <dd>{{ applicationNo }}</dd>
      </div>
    </dl>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.verification-summary__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 900;
}

.verification-summary__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.verification-summary__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: #ddd;
  font-weight: 800;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.field-error dd {
  color: var(--account-danger);
}
</style>
