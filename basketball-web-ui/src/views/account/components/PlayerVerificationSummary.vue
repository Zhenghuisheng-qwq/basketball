<script setup lang="ts">
import { computed } from 'vue'
import type { PlayerVerificationSnapshot } from '@/types/account'
import { GAME_PLATFORM_LABELS, GAME_POSITION_LABELS } from '@/types/account'
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

const initials = computed(() => props.nickname.slice(0, 2).toUpperCase())
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
        <dt>主要游戏平台</dt>
        <dd>{{ GAME_PLATFORM_LABELS[data.primaryPlatform] }}</dd>
      </div>
      <div class="account-field-row" :class="{ 'field-error': highlightFields?.includes('gameId') }">
        <dt>游戏 ID</dt>
        <dd>{{ data.gameId }}</dd>
      </div>
      <div class="account-field-row" :class="{ 'field-error': highlightFields?.includes('gameIdScreenshotUrl') }">
        <dt>游戏 ID 截图</dt>
        <dd>{{ data.gameIdScreenshotUrl ? '已上传' : '未上传' }}</dd>
      </div>
      <div class="account-field-row">
        <dt>2K 角色位置</dt>
        <dd>{{ GAME_POSITION_LABELS[data.gamePosition] }}</dd>
      </div>
      <div class="account-field-row">
        <dt>常用位置</dt>
        <dd>{{ GAME_POSITION_LABELS[data.commonPosition] }}</dd>
      </div>
      <div v-if="submittedAt" class="account-field-row">
        <dt>提交时间</dt>
        <dd>{{ formatDateTime(submittedAt) }}</dd>
      </div>
      <div v-if="applicationNo" class="account-field-row">
        <dt>申请编号</dt>
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