import { computed, ref } from 'vue'
import {
  getCurrentAuth,
  mockLogin as performMockLogin,
  mockLogout as performMockLogout,
  type MockAuthSession,
  type MockLoginResult,
} from '@/mock/auth'
import { useUserStore } from '@/stores/user'
import { useAccountStore } from '@/stores/account'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { PlayerVerificationStatus, OpenApplicationStatus } from '@/types/account'
import { identityQueryToVerificationStatus, isIdentityRestrictedQuery, openStatusQueryToEnum } from '@/types/account'

const authSession = ref<MockAuthSession | null>(getCurrentAuth())

function syncUserStore(session: MockAuthSession | null) {
  const userStore = useUserStore()
  if (!session) {
    userStore.clearToken()
    return
  }

  userStore.setToken(`mock-${session.userId}`)
  userStore.setUserInfo({
    id: session.userId,
    nickname: session.nickname,
    avatar: session.avatarUrl || undefined,
  })
}

function getDevVerificationOverride(): PlayerVerificationStatus | null {
  if (!import.meta.env.DEV || typeof window === 'undefined') return null
  const params = new URLSearchParams(window.location.search)
  const statusQuery = params.get('status')
  const identityStatus = identityQueryToVerificationStatus(statusQuery)
  if (identityStatus) return identityStatus

  const value = params.get('mockVerificationStatus')
  if (
    value === 'NOT_APPLIED'
    || value === 'PENDING'
    || value === 'APPROVED'
    || value === 'REJECTED'
    || value === 'CHANGE_PENDING'
  ) {
    return value
  }
  return null
}

function getDevEligibilityLimited(): boolean {
  if (!import.meta.env.DEV || typeof window === 'undefined') return false
  const params = new URLSearchParams(window.location.search)
  if (isIdentityRestrictedQuery(params.get('status'))) return true
  return params.get('status') === 'limited'
}

function getDevOpenStatusOverride(): OpenApplicationStatus | null {
  if (!import.meta.env.DEV || typeof window === 'undefined') return null
  const params = new URLSearchParams(window.location.search)
  const openStatus = params.get('openStatus')
  if (openStatus) return openStatusQueryToEnum(openStatus)
  if (window.location.pathname.includes('/my-tournaments/open')) {
    return openStatusQueryToEnum(params.get('status'))
  }
  return null
}

async function syncAccountStore() {
  const accountStore = useAccountStore()
  if (!authSession.value?.loggedIn) {
    accountStore.clearAccountState()
    return
  }
  await accountStore.loadAccountMock(
    getDevVerificationOverride(),
    getDevEligibilityLimited(),
    getDevOpenStatusOverride(),
  )
}

export async function initAuthState() {
  authSession.value = getCurrentAuth()
  syncUserStore(authSession.value)
  await syncAccountStore()
}

export function useAuth() {
  const accountStore = useAccountStore()
  const notificationStore = useNotificationCenterStore()
  const isAuthenticated = computed(() => Boolean(authSession.value?.loggedIn))
  const currentUser = computed(() => authSession.value)
  const unreadNotificationCount = computed(() => notificationStore.unreadCount)

  async function login(
    email: string,
    password: string,
    remember = false,
  ): Promise<MockLoginResult> {
    const result = await performMockLogin(email, password, remember)
    if (result.success && result.session) {
      authSession.value = result.session
      syncUserStore(result.session)
      await syncAccountStore()
    }
    return result
  }

  async function logout() {
    performMockLogout()
    authSession.value = null
    syncUserStore(null)
    accountStore.clearAccountState()
  }

  async function refreshAuth() {
    authSession.value = getCurrentAuth()
    syncUserStore(authSession.value)
    await syncAccountStore()
  }

  return {
    isAuthenticated,
    currentUser,
    unreadNotificationCount,
    login,
    logout,
    refreshAuth,
  }
}
