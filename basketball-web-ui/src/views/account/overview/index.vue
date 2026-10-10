<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import CaptainGrantPanel from '@/components/account/team/CaptainGrantPanel.vue'
import { useAccountStore } from '@/stores/account'
import { useTeamStore } from '@/stores/team'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'

const router = useRouter()
const { t, te } = useI18n()
const accountStore = useAccountStore()
const teamStore = useTeamStore()
const notificationStore = useNotificationCenterStore()

onMounted(async () => {
  const tasks: Promise<unknown>[] = []
  if (!accountStore.currentAccount && !accountStore.overviewLoading) {
    tasks.push(accountStore.loadOverview().catch(() => undefined))
  }
  tasks.push(teamStore.loadQualification().catch(() => undefined))
  tasks.push(teamStore.loadMyTeam().catch(() => undefined))
  await Promise.all(tasks)
})

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const isVerified = computed(() => accountStore.isVerifiedPlayer)

const recentNotifications = computed(() => notificationStore.items.slice(0, 5))

function taskCopy(task: { taskType?: string; title?: string; description?: string }, field: 'title' | 'description') {
  const key = `accountShell.overview.tasks.${task.taskType}.${field}`
  if (!task.taskType || !te(key)) return task[field] || ''
  if (task.taskType === 'UNREAD_MESSAGES' && field === 'description') {
    const count = Number((task.description || '').match(/\d+/)?.[0] || 0)
    return t(key, { count })
  }
  return t(key)
}

const joinedCount = computed(
  () => accountStore.myTournaments.filter((item) => item.status === 'JOINED').length,
)

/** 有效队长授权（含未绑定球队的可创建球队队长） */
const hasActiveCaptainGrant = computed(() => Boolean(teamStore.captainQualification?.runtimeValid))

/** 当前可管理球队的队长 */
const isCurrentTeamCaptain = computed(() => teamStore.canManageCurrentTeam)

/** 历史队长：球队内曾为队长，但当前无有效管理权限 */
const isHistoricalCaptain = computed(() => teamStore.isHistoricalCaptain)

/** 普通球队成员 */
const isRegularMember = computed(() => teamStore.isRegularMember)

/** 展示完整队长权限卡片（有效授权 / 当前队长） */
const showActiveCaptainGrantPanel = computed(() => {
  if (isHistoricalCaptain.value) return false
  return isCurrentTeamCaptain.value || hasActiveCaptainGrant.value
})

/** 展示历史队长只读状态 */
const showHistoricalCaptainPanel = computed(() => isHistoricalCaptain.value)

const showCaptainGrantSection = computed(
  () => showActiveCaptainGrantPanel.value || showHistoricalCaptainPanel.value,
)

const bannerStatusText = computed(() => {
  if (isVerified.value) return t('accountShell.overview.verifiedPlayer')
  return t('accountShell.overview.playerUnverified')
})

const bannerStatusExtra = computed(() =>
  account.value?.emailVerified ? t('accountShell.common.emailVerified') : t('accountShell.common.emailUnverified'),
)

const identityStatusText = computed(() => {
  const status = verification.value?.status
  if (status === 'PENDING') return t('accountShell.overview.identityPending')
  if (status === 'REJECTED') return t('accountShell.overview.identityRejected')
  if (isVerified.value) return t('accountShell.overview.identityVerified')
  return t('accountShell.overview.identityNone')
})

const playerIdText = computed(() =>
  isVerified.value && account.value?.playerNo ? account.value.playerNo : t('accountShell.overview.playerIdEmpty'),
)

const accountStatusText = computed(() => t('accountShell.common.normal'))

const captainGrantSummary = computed(() => {
  const grant = teamStore.captainQualification
  if (!grant?.hasQualification) return t('accountShell.overview.noCaptain')
  if (grant.grantRevoked) return t('accountShell.overview.captainRevoked')
  if (grant.runtimeValid) {
    return grant.teamName
      ? t('accountShell.overview.grantActiveTeam', { name: grant.teamName })
      : t('accountShell.overview.grantActiveUnbound')
  }
  return grant.invalidReason || t('accountShell.overview.captainInvalid')
})

const teamSummaryText = computed(() => {
  if (teamStore.currentTeams.length > 1) {
    return t('accountShell.overview.teamCount', { count: teamStore.currentTeams.length })
  }
  if (teamStore.myTeamSummary?.teamName) return teamStore.myTeamSummary.teamName
  if (teamStore.hasTeam) return t('accountShell.overview.hasEventTeam')
  return t('accountShell.overview.noTeam')
})

/** 球队与赛事面板行：非队长大卡片场景下的摘要信息 */
const teamEventRows = computed(() => {
  const rows: Array<{ label: string; value: string }> = [
    { label: t('accountShell.overview.currentTeam'), value: teamSummaryText.value },
  ]

  if (isRegularMember.value) {
    rows.push({ label: t('accountShell.overview.myRole'), value: t('accountShell.overview.roleMember') })
  } else if (isHistoricalCaptain.value) {
    rows.push({ label: t('accountShell.overview.myRole'), value: t('accountShell.overview.roleFormerCaptain') })
  } else if (isCurrentTeamCaptain.value) {
    rows.push({ label: t('accountShell.overview.myRole'), value: t('accountShell.overview.roleCaptain') })
  } else if (!showCaptainGrantSection.value) {
    rows.push({ label: t('accountShell.overview.captainAccess'), value: captainGrantSummary.value })
  } else if (hasActiveCaptainGrant.value && !teamStore.hasTeam) {
    rows.push({ label: t('accountShell.overview.captainAccess'), value: captainGrantSummary.value })
  }

  rows.push({ label: t('accountShell.overview.eventsJoined'), value: String(joinedCount.value) })
  return rows
})

const emailStatusText = computed(() =>
  account.value?.emailVerified ? t('accountShell.common.verified') : t('accountShell.common.unverified'),
)

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter') },
])

const pendingTasks = computed(() => accountStore.pendingTasks)

function go(path: string) {
  router.push(path)
}

function goPlayerIdentity() {
  router.push(resolvePlayerIdentityRoute(accountStore))
}

async function retryLoad() {
  await accountStore.loadOverview()
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountShell.common.accountCenter')"
      en-title="ACCOUNT CENTER"
      :breadcrumb-items="breadcrumbs"
    />

    <div v-if="accountStore.overviewLoading" class="account-view__loading">{{ t('accountShell.common.loading') }}</div>

    <AccountEmptyState
      v-else-if="accountStore.overviewError"
      :message="t('accountShell.overview.loadFailed')"
      :subtitle="accountStore.overviewError"
    >
      <button type="button" class="account-btn-primary" @click="retryLoad">{{ t('accountShell.common.retry') }}</button>
    </AccountEmptyState>

    <template v-else-if="account">
      <AccountStatusBanner
        :title="t('accountShell.overview.welcome', { name: account.nickname || '' })"
        :description="t('accountShell.overview.welcomeDesc')"
        :status-text="`${bannerStatusText} · ${bannerStatusExtra}`"
        :status-type="isVerified ? 'success' : 'warning'"
      />

      <div class="account-overview-grid account-grid-2">
        <CaptainGrantPanel
          v-if="showActiveCaptainGrantPanel"
          :qualification="teamStore.captainQualification"
          :loading="teamStore.qualificationLoading"
        />

        <AccountPanel v-else-if="showHistoricalCaptainPanel" :title="t('accountShell.overview.captainTitle')">
          <p class="account-view__desc">{{ t('accountShell.overview.historicalCaptain') }}</p>
          <p class="account-view__hint">{{ t('accountShell.overview.historicalHint') }}</p>
          <div class="account-panel__action account-panel__action--split">
            <button type="button" class="account-btn-secondary" @click="go('/account/team')">
              {{ t('accountShell.common.myTeam') }}
            </button>
            <button type="button" class="account-btn-secondary" @click="go('/account/my-events')">
              {{ t('accountShell.common.myEvents') }}
            </button>
          </div>
        </AccountPanel>

        <AccountPanel :title="t('accountShell.overview.teamEvents')">
          <div class="account-info-list">
            <div
              v-for="row in teamEventRows"
              :key="row.label"
              class="account-info-row"
            >
              <span class="account-info-row__label">{{ row.label }}</span>
              <span class="account-info-row__value">{{ row.value }}</span>
            </div>
          </div>
          <div class="account-panel__action account-panel__action--split">
            <button type="button" class="account-btn-secondary" @click="go('/account/team')">
              {{ t('accountShell.common.myTeam') }}
            </button>
            <button type="button" class="account-btn-secondary" @click="go('/account/my-events')">
              {{ t('accountShell.common.myEvents') }}
            </button>
          </div>
        </AccountPanel>

        <AccountPanel :title="t('accountShell.overview.accountIdentity')">
          <div class="account-info-list">
            <div class="account-info-row">
              <span class="account-info-row__label">{{ t('accountShell.common.accountStatus') }}</span>
              <span class="account-info-row__value account-status-text--success">{{ accountStatusText }}</span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">{{ t('accountShell.common.emailStatus') }}</span>
              <span
                class="account-info-row__value"
                :class="account.emailVerified ? 'account-status-text--success' : ''"
              >
                {{ emailStatusText }}
              </span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">{{ t('accountShell.overview.playerIdentity') }}</span>
              <span class="account-info-row__value">{{ identityStatusText }}</span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">{{ t('accountShell.overview.playerId') }}</span>
              <span class="account-info-row__value">{{ playerIdText }}</span>
            </div>
          </div>
          <div class="account-panel__action">
            <button type="button" class="account-btn-secondary" @click="goPlayerIdentity">
              {{ t('accountShell.overview.viewIdentity') }}
            </button>
          </div>
        </AccountPanel>

        <AccountPanel :title="t('accountShell.overview.pendingTitle')">
          <template v-if="pendingTasks.length">
            <div v-for="task in pendingTasks" :key="task.taskType" class="account-pending-item">
              <p class="account-view__desc">{{ taskCopy(task, 'title') }}</p>
              <p class="account-view__hint">{{ taskCopy(task, 'description') }}</p>
              <div class="account-panel__action">
                <button type="button" class="account-btn-primary" @click="go(task.actionPath)">
                  {{ t('accountShell.overview.goHandle') }}
                </button>
              </div>
            </div>
          </template>
          <div v-else class="account-pending-empty">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
            </svg>
            <p>{{ t('accountShell.overview.noTasks') }}</p>
          </div>
        </AccountPanel>

        <AccountPanel :title="t('accountShell.overview.recentTitle')">
          <ul v-if="recentNotifications.length" class="account-notice-list">
            <li v-for="item in recentNotifications" :key="item.id">
              <span
                class="account-notice-list__dot"
                :class="{ 'account-notice-list__dot--unread': item.isUnread }"
              />
              <div>
                <strong>{{ item.title }}</strong>
                <time>{{ formatDateTime(item.createdAt) }}</time>
              </div>
            </li>
          </ul>
          <div v-else class="account-notification-empty">
            <p>{{ t('accountShell.overview.noNotifications') }}</p>
          </div>
          <div class="account-notification-footer">
            <button type="button" class="account-btn-secondary" @click="go('/account/notifications')">
              {{ t('accountShell.overview.viewAll') }}
            </button>
          </div>
        </AccountPanel>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view__loading {
  padding: 48px 0;
  text-align: center;
  color: #696969;
}

.account-view__desc {
  margin: 0 0 8px;
  font-size: clamp(18px, 1.6vw, 22px);
  font-weight: 800;
  color: var(--account-text);
}

.account-view__hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #696969;
  line-height: 1.6;
}

.account-pending-item + .account-pending-item {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.account-panel__action--split {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}
</style>
