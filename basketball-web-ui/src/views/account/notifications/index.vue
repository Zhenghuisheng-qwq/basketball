<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useAccountStore } from '@/stores/account'
import type { AccountNotification, NotificationType } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const PAGE_SIZE = 5

type Category = 'ALL' | 'UNREAD' | NotificationType

const category = ref<Category>((route.query.category as Category) || 'ALL')
const page = ref(Number(route.query.page) || 1)
const selectedId = ref<string>((route.query.notificationId as string) || '')
const mobileShowDetail = ref(false)

const notifications = computed(() => accountStore.notifications)

const filtered = computed(() => {
  let list = [...notifications.value]
  if (category.value === 'UNREAD') {
    list = list.filter((item) => !item.read)
  } else if (category.value !== 'ALL') {
    list = list.filter((item) => item.type === category.value)
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)))

const paged = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filtered.value.slice(start, start + PAGE_SIZE)
})

const selected = computed(() =>
  notifications.value.find((item) => item.id === selectedId.value) || null,
)

const stats = computed(() => ({
  all: notifications.value.length,
  unread: notifications.value.filter((item) => !item.read).length,
  pending: notifications.value.filter((item) => item.pendingAction && !item.read).length,
}))

const categories: { key: Category; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'UNREAD', label: '未读' },
  { key: 'VERIFICATION', label: '认证通知' },
  { key: 'TEAM', label: '球队通知' },
  { key: 'EVENT', label: '赛事通知' },
  { key: 'SYSTEM', label: '系统通知' },
]

function syncQuery() {
  router.replace({
    query: {
      ...(category.value !== 'ALL' ? { category: category.value } : {}),
      ...(page.value > 1 ? { page: String(page.value) } : {}),
      ...(selectedId.value ? { notificationId: selectedId.value } : {}),
    },
  })
}

function setCategory(value: Category) {
  category.value = value
  page.value = 1
  syncQuery()
}

function selectNotification(item: AccountNotification) {
  selectedId.value = item.id
  accountStore.markNotificationRead(item.id)
  mobileShowDetail.value = true
  syncQuery()
}

function markAllRead() {
  accountStore.markAllNotificationsRead()
}

function goAction(item: AccountNotification) {
  if (item.actionType === 'VIEW_VERIFICATION') {
    router.push('/account/player-verification')
  } else if (item.actionType === 'HANDLE_TEAM_INVITATION') {
    router.push(`/account/team-relations?invitationId=${item.actionTargetId}`)
  } else if (item.actionType === 'VIEW_EVENT') {
    router.push('/tournament')
  }
}

watch([category, page], syncQuery)
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="消息通知"
      en-title="NOTIFICATIONS"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '消息通知' },
      ]"
    />

    <AccountStatusBanner
      title="系统消息"
      description="认证、球队、赛事与账号通知统一送达"
      :status-text="`未读消息 ${stats.unread}`"
      status-type="info"
    />

    <div class="notification-stats">
      <div class="notification-stats__item">
        <strong>{{ stats.all }}</strong>
        <span>全部消息</span>
      </div>
      <div class="notification-stats__item">
        <strong>{{ stats.unread }}</strong>
        <span>未读消息</span>
      </div>
      <div class="notification-stats__item">
        <strong>{{ stats.pending }}</strong>
        <span>待处理事项</span>
      </div>
      <button type="button" class="account-btn-secondary" @click="markAllRead">全部标为已读</button>
    </div>

    <div class="notification-tabs">
      <button
        v-for="item in categories"
        :key="item.key"
        type="button"
        class="notification-tabs__btn"
        :class="{ 'notification-tabs__btn--active': category === item.key }"
        @click="setCategory(item.key)"
      >
        {{ item.label }}
      </button>
    </div>

    <div class="account-grid-2 notification-layout">
      <section class="notification-list" :class="{ 'notification-list--hidden-mobile': mobileShowDetail }">
        <AccountEmptyState v-if="!paged.length" message="暂无消息" />
        <button
          v-for="item in paged"
          :key="item.id"
          type="button"
          class="notification-item"
          :class="{ 'notification-item--active': selectedId === item.id, 'notification-item--unread': !item.read }"
          @click="selectNotification(item)"
        >
          <span class="notification-item__dot" aria-hidden="true" />
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.summary }}</p>
            <time>{{ formatDateTime(item.createdAt) }}</time>
          </div>
        </button>

        <div v-if="totalPages > 1" class="notification-pagination">
          <button
            type="button"
            class="account-btn-secondary"
            :disabled="page <= 1"
            @click="page -= 1; syncQuery()"
          >
            上一页
          </button>
          <span>{{ page }} / {{ totalPages }}</span>
          <button
            type="button"
            class="account-btn-secondary"
            :disabled="page >= totalPages"
            @click="page += 1; syncQuery()"
          >
            下一页
          </button>
        </div>
      </section>

      <section class="notification-detail" :class="{ 'notification-detail--visible-mobile': mobileShowDetail }">
        <button
          v-if="mobileShowDetail"
          type="button"
          class="notification-detail__back account-btn-secondary"
          @click="mobileShowDetail = false"
        >
          返回列表
        </button>

        <AccountEmptyState v-if="!selected" message="请选择一条消息" />

        <article v-else class="notification-detail__content">
          <h2>{{ selected.title }}</h2>
          <p class="notification-detail__meta">
            {{ formatDateTime(selected.createdAt) }} · {{ selected.metadata.source || 'CNPA 系统' }}
          </p>
          <p class="notification-detail__body">{{ selected.content }}</p>
          <div class="notification-detail__actions">
            <button
              v-if="selected.actionType === 'VIEW_VERIFICATION'"
              type="button"
              class="account-btn-primary"
              @click="goAction(selected)"
            >
              查看球员认证
            </button>
            <button
              v-if="selected.actionType === 'HANDLE_TEAM_INVITATION'"
              type="button"
              class="account-btn-primary"
              @click="goAction(selected)"
            >
              处理球队邀请
            </button>
            <button
              v-if="selected.actionType === 'VIEW_EVENT'"
              type="button"
              class="account-btn-primary"
              @click="goAction(selected)"
            >
              查看赛事详情
            </button>
            <button
              v-if="!selected.read"
              type="button"
              class="account-btn-secondary"
              @click="accountStore.markNotificationRead(selected.id)"
            >
              标记为已读
            </button>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view {
  padding: clamp(24px, 3vw, 40px) clamp(20px, 3vw, 48px) 48px;
}

.notification-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px 20px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.12);
}

.notification-stats__item {
  display: flex;
  flex-direction: column;
  min-width: 80px;

  strong {
    font-size: 22px;
    color: var(--account-red);
  }

  span {
    font-size: 12px;
    color: #696969;
  }
}

.notification-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.notification-tabs__btn {
  padding: 8px 14px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;

  &--active {
    background: var(--account-red);
    border-color: var(--account-red);
    color: #fff;
  }
}

.notification-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--account-card-gap);
  width: 100%;
  min-height: 360px;
}

.notification-list,
.notification-detail {
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.12);
  padding: 12px;
  overflow-y: auto;
  max-height: 520px;
}

.notification-item {
  display: flex;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 0;
  border-bottom: 1px solid rgba(20, 20, 20, 0.08);
  background: transparent;
  text-align: left;
  font-family: inherit;
  cursor: pointer;

  &--active {
    background: rgba(213, 8, 13, 0.08);
  }

  strong {
    display: block;
    margin-bottom: 4px;
    font-size: 14px;
  }

  p {
    margin: 0 0 4px;
    font-size: 12px;
    color: #696969;
  }

  time {
    font-size: 11px;
    color: #aaa;
  }
}

.notification-item__dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  border: 1px solid rgba(20, 20, 20, 0.2);
  flex-shrink: 0;
}

.notification-item--unread .notification-item__dot {
  background: var(--account-red);
  border-color: var(--account-red);
}

.notification-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding-top: 12px;
}

.notification-detail__content h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.notification-detail__meta {
  margin: 0 0 16px;
  font-size: 12px;
  color: #696969;
}

.notification-detail__body {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.notification-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.notification-detail__back {
  display: none;
  margin-bottom: 12px;
}

@media (max-width: 991px) {
  .notification-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .notification-list--hidden-mobile {
    display: none;
  }

  .notification-detail {
    display: none;
  }

  .notification-detail--visible-mobile {
    display: block;
  }

  .notification-detail__back {
    display: inline-flex;
  }
}
</style>