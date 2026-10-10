<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import type { CaptainQualificationDto } from '@/api/types/teamCaptain'
import { formatDateTime } from '@/utils/formatDate'

const { t } = useI18n()

const props = defineProps<{
  qualification: CaptainQualificationDto | null
  loading?: boolean
}>()

const hasGrant = computed(() => Boolean(props.qualification?.hasQualification))
const isActive = computed(() => Boolean(props.qualification?.runtimeValid))
const isRevoked = computed(() => Boolean(props.qualification?.grantRevoked))

type StatusTone = 'success' | 'warning' | 'danger' | 'default'

const statusLabel = computed(() => {
  if (isActive.value) return t('accountTeam.grant.active')
  if (isRevoked.value) return t('accountTeam.grant.revoked')
  if (hasGrant.value) return t('accountTeam.grant.invalid')
  return t('accountTeam.grant.inactive')
})

const statusTone = computed<StatusTone>(() => {
  if (isActive.value) return 'success'
  if (isRevoked.value) return 'warning'
  if (hasGrant.value) return 'danger'
  return 'default'
})

const subtitle = computed(() => {
  if (isActive.value) {
    return t('accountTeam.grant.activeHint')
  }
  if (isRevoked.value) {
    return t('accountTeam.grant.revokedHint')
  }
  if (hasGrant.value) {
    return props.qualification?.invalidReason || t('accountTeam.grant.invalidFallback')
  }
  return t('accountTeam.grant.inactiveHint')
})

const validityLabel = computed(() => {
  const from = formatDateTime(props.qualification?.validFrom)
  const to = formatDateTime(props.qualification?.validTo)
  if (from === '-' && to === '-') return '-'
  if (from === '-') return to
  if (to === '-') return from
  return `${from} - ${to}`
})

const showSummaryGrid = computed(() => hasGrant.value || isActive.value)
</script>

<template>
  <AccountPanel :title="t('accountTeam.grant.title')" class="captain-grant-panel">
    <p v-if="loading" class="captain-grant__loading">{{ t('accountTeam.grant.loading') }}</p>

    <template v-else>
      <p class="captain-grant__subtitle">{{ subtitle }}</p>

      <div v-if="showSummaryGrid" class="captain-grant__grid">
        <div class="captain-grant__cell">
          <span class="captain-grant__cell-label">{{ t('accountTeam.grant.status') }}</span>
          <div class="captain-grant__cell-value">
            <AccountStatusBadge :text="statusLabel" :type="statusTone" />
          </div>
        </div>

        <div class="captain-grant__cell captain-grant__cell--validity">
          <span class="captain-grant__cell-label">{{ t('accountTeam.grant.validity') }}</span>
          <span class="captain-grant__cell-value captain-grant__cell-value--validity">
            {{ validityLabel }}
          </span>
        </div>
      </div>
    </template>
  </AccountPanel>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.captain-grant-panel {
  :deep(.account-panel__body) {
    padding-top: 0;
  }
}

.captain-grant__loading {
  margin: 0;
  font-size: 13px;
  color: var(--account-muted, #565656);
}

.captain-grant__subtitle {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--account-muted, #565656);
}

.captain-grant__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.captain-grant__cell {
  position: relative;
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--account-border, #d6d0c6);
  border-radius: 3px;
  background: #fff;
  box-shadow: inset 3px 0 0 var(--account-red, #d90000);
}

.captain-grant__cell-label {
  display: block;
  margin-bottom: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--account-muted, #565656);
}

.captain-grant__cell-value {
  display: block;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.4;
  color: var(--account-text, #111111);
  word-break: break-word;
}

.captain-grant__cell-value--muted {
  font-weight: 600;
  color: var(--account-muted, #565656);
}

.captain-grant__cell-value--validity {
  font-size: 12px;
  font-weight: 700;
  line-height: 1.5;
}

.captain-grant__cell--validity {
  box-shadow: inset 3px 0 0 var(--account-black, #050505);
}

@media (max-width: 960px) {
  .captain-grant__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .captain-grant__grid {
    grid-template-columns: 1fr;
  }
}
</style>
