import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAccountStore } from '@/stores/account'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { useTeamStore } from '@/stores/team'

export interface AuthLoginResult {
  success: boolean
  message?: string
  errorCode?: 'INVALID_CREDENTIALS' | 'ACCOUNT_NOT_FOUND' | 'UNKNOWN'
}

function mapError(error: unknown): AuthLoginResult {
  const message = error instanceof Error ? error.message : '登录失败'
  let errorCode: AuthLoginResult['errorCode'] = 'UNKNOWN'
  if (message.includes('密码') || message.includes('邮箱或密码')) {
    errorCode = 'INVALID_CREDENTIALS'
  } else if (message.includes('不存在') || message.includes('尚未注册')) {
    errorCode = 'ACCOUNT_NOT_FOUND'
  }
  return { success: false, message, errorCode }
}

async function syncAccountAfterLogin(loggedIn: boolean) {
  const accountStore = useAccountStore()
  const notificationStore = useNotificationCenterStore()
  if (!loggedIn) {
    accountStore.clearAccountState()
    return
  }
  await Promise.all([
    accountStore.loadOverview().catch(() => undefined),
    notificationStore.refresh().catch(() => undefined),
  ])
}

export async function initAuthState() {
  const userStore = useUserStore()
  await userStore.ensureInitialized()
  await syncAccountAfterLogin(userStore.isAuthenticated)
}

export function useAuth() {
  const userStore = useUserStore()
  const accountStore = useAccountStore()
  const notificationStore = useNotificationCenterStore()

  const isAuthenticated = computed(() => userStore.isAuthenticated)
  const currentUser = computed(() => {
    const member = userStore.currentMember
    if (!member) return null
    return {
      loggedIn: true as const,
      userId: Number(member.memberId) || 0,
      memberId: member.memberId,
      email: member.emailMasked,
      nickname: member.nickname,
      avatarUrl: member.avatarUrl || '',
      accountRole: 'BASIC_USER' as const,
      emailVerified: Boolean(member.emailVerified),
      loginTime: Date.now(),
    }
  })
  const unreadNotificationCount = computed(() => notificationStore.unreadCount)

  async function login(email: string, password: string, remember = true): Promise<AuthLoginResult> {
    try {
      await userStore.loginByPassword(email, password, remember)
      await syncAccountAfterLogin(true)
      return { success: true }
    } catch (error) {
      return mapError(error)
    }
  }

  async function logout() {
    await userStore.logout()
    accountStore.clearAccountLocalStorage()
    accountStore.clearAccountState()
    const teamStore = useTeamStore()
    const notificationStore = useNotificationCenterStore()
    teamStore.resetState()
    notificationStore.resetState()
  }

  async function refreshAuth() {
    await userStore.restoreSession()
    await syncAccountAfterLogin(userStore.isAuthenticated)
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
