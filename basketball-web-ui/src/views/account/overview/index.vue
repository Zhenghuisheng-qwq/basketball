<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useAccountStore } from '@/stores/account'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'

const router = useRouter()
const accountStore = useAccountStore()
const notificationStore = useNotificationCenterStore()

onMounted(async () => {
  if (!accountStore.currentAccount && !accountStore.overviewLoading) {
    try {
      await accountStore.loadOverview()
    } catch {
      // error shown in template
    }
  }
})

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const isVerified = computed(() => accountStore.isVerifiedPlayer)

const recentNotifications = computed(() => notificationStore.items.slice(0, 5))

const joinedCount = computed(
  () => accountStore.myTournaments.filter((item) => item.status === 'JOINED').length,
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

const accountStatusText = computed(() => {
  const status = account.value?.accountRole
  if (status === 'CAPTAIN') return '队长账号'
  if (status === 'PLAYER') return '正常'
  return '正常'
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

        <AccountPanel title="当前赛事">
          <div class="account-info-list">
            <div class="account-info-row">
              <span class="account-info-row__label">已参加赛事</span>
              <span class="account-info-row__value">{{ joinedCount }}</span>
            </div>
          </div>
          <p class="account-view__desc">
            {{ joinedCount ? `已参加 ${joinedCount} 场赛事` : '暂无已参加赛事' }}
          </p>
          <p class="account-view__hint">
            完成球员认证后，可在“我的赛事”查看符合资格的赛事
          </p>
          <div class="account-panel__action">
            <button type="button" class="account-btn-secondary" @click="go('/account/my-tournaments')">
              查看我的赛事
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
</style>
