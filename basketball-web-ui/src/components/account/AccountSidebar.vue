<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useAccountStore } from '@/stores/account'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'
import { resolveMemberAvatarUrl } from '@/utils/memberAvatar'

const route = useRoute()
const router = useRouter()
const { logout } = useAuth()
const accountStore = useAccountStore()
const notificationStore = useNotificationCenterStore()

const drawerOpen = ref(false)
const imageError = ref(false)

const account = computed(() => accountStore.currentAccount)
const avatarUrl = computed(() => resolveMemberAvatarUrl(account.value?.avatarUrl))

const showAvatarImage = computed(() => !imageError.value)

const unreadBadge = computed(() => {
  const count = notificationStore.unreadCount
  if (count <= 0) return ''
  if (count > 99) return '99+'
  return String(count)
})

const isVerifiedPlayer = computed(() => accountStore.isVerifiedPlayer)
const isPendingVerification = computed(() => accountStore.verification?.status === 'PENDING')

const identityRoute = computed(() => resolvePlayerIdentityRoute(accountStore))

const displayName = computed(() => {
  if (isVerifiedPlayer.value && account.value?.playerNo) {
    return account.value.playerNo
  }
  return account.value?.nickname || ''
})

interface MenuItem {
  key: string
  label: string
  routeName?: string
  to?: ReturnType<typeof resolvePlayerIdentityRoute>
  badge?: boolean
}

const mainMenus = computed<MenuItem[]>(() => [
  { key: 'overview', label: '概览', routeName: 'AccountOverview' },
  { key: 'identity', label: '球员身份', to: identityRoute.value },
  { key: 'tournaments', label: '我的赛事', routeName: 'AccountMyEvents' },
  { key: 'notifications', label: '消息通知', routeName: 'AccountNotifications', badge: true },
  { key: 'settings', label: '账号设置', routeName: 'AccountSettings' },
])

function isActive(item: MenuItem) {
  if (item.key === 'identity') {
    return route.path.startsWith('/account/player-identity')
  }
  if (item.key === 'tournaments') {
    return (
      route.path.startsWith('/account/my-events') ||
      route.path.startsWith('/account/my-tournaments') ||
      route.path.startsWith('/account/events')
    )
  }
  if (item.key === 'team') {
    return route.path.startsWith('/account/team')
  }
  if (item.key === 'settings') {
    return (
      route.name === 'AccountSettings' ||
      route.name === 'AccountSecurity' ||
      route.path.startsWith('/account/settings') ||
      route.path.startsWith('/account/security') ||
      route.path.startsWith('/account/profile')
    )
  }
  if (item.key === 'notifications') {
    return route.path.startsWith('/account/notifications')
  }
  return route.name === item.routeName
}

function closeDrawer() {
  drawerOpen.value = false
}

async function handleLogout() {
  drawerOpen.value = false
  await logout()
  router.replace('/home')
}

function toggleDrawer() {
  drawerOpen.value = !drawerOpen.value
}

function handleAvatarError() {
  imageError.value = true
}
</script>

<template>
  <aside
    class="account-sidebar"
    :class="{ 'account-sidebar--drawer-open': drawerOpen }"
  >
    <button
      type="button"
      class="account-sidebar__mobile-toggle"
      :aria-expanded="drawerOpen"
      @click="toggleDrawer"
    >
      账号菜单
    </button>

    <div class="account-sidebar__content account-sidebar__inner">
      <div class="account-sidebar__profile">
        <p class="account-sidebar__eyebrow">CNPA PLAYER CENTER</p>

        <div class="account-sidebar__avatar">
          <img
            v-if="showAvatarImage"
            :src="avatarUrl"
            :alt="account?.nickname || '用户头像'"
            @error="handleAvatarError"
          />
          <svg
            v-else
            class="account-sidebar__avatar-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              fill="currentColor"
              d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
            />
          </svg>
        </div>

        <p class="account-sidebar__nickname">{{ displayName }}</p>
        <div class="account-sidebar__identity-tags">
          <span v-if="!isVerifiedPlayer" class="account-sidebar__identity">基础用户</span>
          <span
            v-if="isPendingVerification && !isVerifiedPlayer"
            class="account-sidebar__identity account-sidebar__identity--pending"
          >
            审核中
          </span>
          <span
            v-if="isVerifiedPlayer"
            class="account-sidebar__identity account-sidebar__identity--player"
          >
            认证球员
          </span>
        </div>
        <p class="account-sidebar__email">{{ account?.maskedEmail }}</p>
      </div>

      <nav class="account-sidebar__nav" aria-label="个人中心导航">
        <RouterLink
          v-for="item in mainMenus"
          :key="item.key"
          :to="item.to ?? { name: item.routeName }"
          class="account-sidebar__link account-side-menu-item account-nav-item"
          active-class=""
          exact-active-class=""
          :class="{
            'account-sidebar__link--active': isActive(item),
            'is-active': isActive(item),
          }"
          @click="closeDrawer"
        >
          <svg
            v-if="item.key === 'overview'"
            class="account-sidebar__link-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path fill="currentColor" d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z" />
          </svg>
          <svg
            v-else-if="item.key === 'identity'"
            class="account-sidebar__link-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z" />
          </svg>
          <svg
            v-else-if="item.key === 'team'"
            class="account-sidebar__link-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
          </svg>
          <svg
            v-else-if="item.key === 'tournaments'"
            class="account-sidebar__link-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path fill="currentColor" d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z" />
          </svg>
          <svg
            v-else-if="item.key === 'notifications'"
            class="account-sidebar__link-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path fill="currentColor" d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z" />
          </svg>
          <svg
            v-else
            class="account-sidebar__link-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path fill="currentColor" d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z" />
          </svg>
          <span>{{ item.label }}</span>
          <span v-if="item.badge && unreadBadge" class="account-sidebar__badge">{{ unreadBadge }}</span>
        </RouterLink>
      </nav>

      <div class="account-sidebar__logout-wrap">
        <button
          type="button"
          class="account-sidebar__logout account-logout-btn"
          @click="handleLogout"
        >
          <svg class="account-sidebar__logout-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z" />
          </svg>
          退出登录
        </button>
      </div>
    </div>

    <button
      v-if="drawerOpen"
      type="button"
      class="account-sidebar__backdrop"
      aria-label="关闭菜单"
      @click="drawerOpen = false"
    />
  </aside>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
