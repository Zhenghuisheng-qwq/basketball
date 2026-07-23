<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { useAccountStore } from '@/stores/account'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const isVerified = computed(() => accountStore.isVerifiedPlayer)

const recentNotifications = computed(() =>
  accountStore.notifications
    .filter((item) =>
      ['欢迎加入 CNPA', '邮箱验证成功', '完成球员认证后即可参与赛事'].includes(item.title),
    )
    .slice(0, 3),
)

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

const pendingTasks = computed(() => {
  if (isVerified.value) {
    const invited = accountStore.myTournaments.filter((item) => item.status === 'INVITED').length
    if (invited > 0) {
      return {
        title: `收到 ${invited} 条球队邀请`,
        action: '前往处理',
        path: '/account/my-events?tab=applications',
      }
    }
    return null
  }
  return {
    title: '完成球员认证',
    status: '待处理',
    desc: '认证后可报名赛事、接受球队邀请并进入正式阵容',
    action: '申请球员认证',
    path: '/account/player-identity/apply',
  }
})

function go(path: string) {
  router.push(path)
}

function goPlayerIdentity() {
  router.push(resolvePlayerIdentityRoute(accountStore))
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

    <AccountStatusBanner
      :title="`欢迎回来，${account?.nickname}`"
      description="管理账号资料、认证状态与当前待办"
      :status-text="`${bannerStatusText} · ${bannerStatusExtra}`"
      :status-type="isVerified ? 'success' : 'warning'"
    />

    <div class="account-overview-grid">
      <AccountPanel title="账号与身份">
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">账号状态</span>
            <span class="account-info-row__value account-status-text--success">正常</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">邮箱状态</span>
            <span class="account-info-row__value account-status-text--success">已验证</span>
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
            <span class="account-info-row__value">{{ joinedCount }} / 3</span>
          </div>
        </div>
        <p class="account-view__desc">
          {{ joinedCount ? `已参加 ${joinedCount} 场赛事` : '暂无已参加赛事' }}
        </p>
        <p class="account-view__hint">
          完成球员认证后，可在“我的赛事”查看符合资格的赛事
        </p>
        <div v-if="isVerified" class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="go('/account/my-events?tab=joined')">
            查看我的赛事
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="待处理事项">
        <template v-if="pendingTasks">
          <p class="account-view__desc">{{ pendingTasks.title }}</p>
          <p v-if="pendingTasks.status" class="account-pending-tag">{{ pendingTasks.status }}</p>
          <p v-if="pendingTasks.desc" class="account-view__hint">{{ pendingTasks.desc }}</p>
          <div class="account-panel__action">
            <button type="button" class="account-btn-primary" @click="go(pendingTasks.path)">
              {{ pendingTasks.action }}
            </button>
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
              :class="{ 'account-notice-list__dot--unread': !item.read }"
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
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

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

.account-pending-tag {
  display: inline-block;
  margin: 0 0 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(196, 30, 58, 0.12);
  color: var(--account-danger);
  font-size: 12px;
  font-weight: 800;
}
</style>
