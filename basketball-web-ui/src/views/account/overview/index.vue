<script setup lang="ts">
import { computed, onMounted } from 'vue'
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
  if (isVerified.value) return '认证球员'
  return '球员未认证'
})

const bannerStatusExtra = computed(() =>
  account.value?.emailVerified ? '邮箱已验证' : '邮箱未验证',
)

const identityStatusText = computed(() => {
  const status = verification.value?.status
  if (status === 'PENDING') return '审核中'
  if (status === 'REJECTED') return '未通过'
  if (isVerified.value) return '已认证'
  return '未认证'
})

const playerIdText = computed(() =>
  isVerified.value && account.value?.playerNo ? account.value.playerNo : '尚未生成',
)

const accountStatusText = computed(() => '正常')

const captainGrantSummary = computed(() => {
  const grant = teamStore.captainQualification
  if (!grant?.hasQualification) return '暂无队长权限'
  if (grant.grantRevoked) return '队长权限已收回'
  if (grant.runtimeValid) return grant.teamName ? `有效 · ${grant.teamName}` : '有效 · 未绑定球队'
  return grant.invalidReason || '队长权限无效'
})

const teamSummaryText = computed(() => {
  if (teamStore.myTeamSummary?.teamName) return teamStore.myTeamSummary.teamName
  if (teamStore.hasTeam) return '已加入球队'
  return '未加入球队'
})

/** 球队与赛事面板行：非队长大卡片场景下的摘要信息 */
const teamEventRows = computed(() => {
  const rows: Array<{ label: string; value: string }> = [
    { label: '当前球队', value: teamSummaryText.value },
  ]

  if (isRegularMember.value) {
    rows.push({ label: '我的角色', value: '成员' })
  } else if (isHistoricalCaptain.value) {
    rows.push({ label: '我的角色', value: '历史队长' })
  } else if (isCurrentTeamCaptain.value) {
    rows.push({ label: '我的角色', value: '队长' })
  } else if (!showCaptainGrantSection.value) {
    rows.push({ label: '队长权限', value: captainGrantSummary.value })
  } else if (hasActiveCaptainGrant.value && !teamStore.hasTeam) {
    rows.push({ label: '队长权限', value: captainGrantSummary.value })
  }

  rows.push({ label: '已参加赛事', value: String(joinedCount.value) })
  return rows
})

const emailStatusText = computed(() =>
  account.value?.emailVerified ? '已验证' : '未验证',
)

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
      zh-title="个人中心"
      en-title="ACCOUNT CENTER"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心' },
      ]"
    />

    <div v-if="accountStore.overviewLoading" class="account-view__loading">加载中...</div>

    <AccountEmptyState
      v-else-if="accountStore.overviewError"
      message="加载个人中心失败"
      :subtitle="accountStore.overviewError"
    >
      <button type="button" class="account-btn-primary" @click="retryLoad">重试</button>
    </AccountEmptyState>

    <template v-else-if="account">
      <AccountStatusBanner
        :title="`欢迎回来，${account.nickname}`"
        description="管理账号资料、认证状态与当前待办"
        :status-text="`${bannerStatusText} · ${bannerStatusExtra}`"
        :status-type="isVerified ? 'success' : 'warning'"
      />

      <div class="account-overview-grid account-grid-2">
        <CaptainGrantPanel
          v-if="showActiveCaptainGrantPanel"
          :qualification="teamStore.captainQualification"
          :loading="teamStore.qualificationLoading"
        />

        <AccountPanel v-else-if="showHistoricalCaptainPanel" title="队长权限">
          <p class="account-view__desc">历史队长</p>
          <p class="account-view__hint">当前暂无有效队长权限。历史球队和赛事记录仍可查看。</p>
          <div class="account-panel__action account-panel__action--split">
            <button type="button" class="account-btn-secondary" @click="go('/account/team')">
              我的球队
            </button>
            <button type="button" class="account-btn-secondary" @click="go('/account/my-events')">
              我的赛事
            </button>
          </div>
        </AccountPanel>

        <AccountPanel title="球队与赛事">
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
              我的球队
            </button>
            <button type="button" class="account-btn-secondary" @click="go('/account/my-events')">
              我的赛事
            </button>
          </div>
        </AccountPanel>

        <AccountPanel title="账号与身份">
          <div class="account-info-list">
            <div class="account-info-row">
              <span class="account-info-row__label">账号状态</span>
              <span class="account-info-row__value account-status-text--success">{{ accountStatusText }}</span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">邮箱状态</span>
              <span
                class="account-info-row__value"
                :class="account.emailVerified ? 'account-status-text--success' : ''"
              >
                {{ emailStatusText }}
              </span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">球员身份</span>
              <span class="account-info-row__value">{{ identityStatusText }}</span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">永久球员 ID</span>
              <span class="account-info-row__value">{{ playerIdText }}</span>
            </div>
          </div>
          <div class="account-panel__action">
            <button type="button" class="account-btn-secondary" @click="goPlayerIdentity">
              查看球员身份
            </button>
          </div>
        </AccountPanel>

        <AccountPanel title="待处理事项">
          <template v-if="pendingTasks.length">
            <div v-for="task in pendingTasks" :key="task.taskType" class="account-pending-item">
              <p class="account-view__desc">{{ task.title }}</p>
              <p class="account-view__hint">{{ task.description }}</p>
              <div class="account-panel__action">
                <button type="button" class="account-btn-primary" @click="go(task.actionPath)">
                  前往处理
                </button>
              </div>
            </div>
          </template>
          <div v-else class="account-pending-empty">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
            </svg>
            <p>暂无待办事项</p>
          </div>
        </AccountPanel>

        <AccountPanel title="最近通知">
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
            <p>暂无通知</p>
          </div>
          <div class="account-notification-footer">
            <button type="button" class="account-btn-secondary" @click="go('/account/notifications')">
              查看全部通知
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
