<script setup lang="ts">
import { computed } from 'vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import type { CaptainQualificationDto } from '@/api/types/teamCaptain'
import { formatDateTime } from '@/utils/formatDate'

const props = defineProps<{
  qualification: CaptainQualificationDto | null
  loading?: boolean
}>()

const hasGrant = computed(() => Boolean(props.qualification?.hasQualification))
const isActive = computed(() => Boolean(props.qualification?.runtimeValid))
const isRevoked = computed(() => Boolean(props.qualification?.grantRevoked))
const boundTeamLabel = computed(() => props.qualification?.teamName || '未绑定')
</script>

<template>
  <AccountPanel title="队长权限">
    <p v-if="loading" class="captain-grant__hint">加载中…</p>

    <template v-else-if="isActive">
      <div class="account-info-list">
        <div class="account-info-row">
          <span class="account-info-row__label">当前队长权限</span>
          <span class="account-info-row__value account-status-text--success">已开通</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">授权赛事</span>
          <span class="account-info-row__value">{{ qualification?.seasonName || '未关联赛事' }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">关联球队</span>
          <span class="account-info-row__value">{{ boundTeamLabel }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">权限状态</span>
          <span class="account-info-row__value account-status-text--success">有效</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">有效期</span>
          <span class="account-info-row__value">
            {{ formatDateTime(qualification?.validFrom) }} 至 {{ formatDateTime(qualification?.validTo) }}
          </span>
        </div>
      </div>
    </template>

    <template v-else-if="isRevoked">
      <p class="captain-grant__message">当前赛事周期已结束，队长权限已收回。</p>
      <p class="captain-grant__hint">历史球队和赛事记录仍可查看。</p>
    </template>

    <template v-else-if="hasGrant">
      <p class="captain-grant__message">{{ qualification?.invalidReason || '当前队长权限无效' }}</p>
    </template>

    <template v-else>
      <p class="captain-grant__message">当前暂无队长权限。</p>
      <p class="captain-grant__hint">队长权限由平台根据赛事缴费结果后台开通。</p>
    </template>
  </AccountPanel>
</template>

<style scoped>
.captain-grant__message {
  margin: 0 0 0.5rem;
  color: var(--account-text, #1f2937);
}

.captain-grant__hint {
  margin: 0;
  color: var(--account-text-muted, #5f6b7a);
  font-size: 0.92rem;
}
</style>
