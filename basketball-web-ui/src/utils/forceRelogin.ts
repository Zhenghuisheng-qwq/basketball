import { useAccountStore } from '@/stores/account'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { useTeamStore } from '@/stores/team'
import { useUserStore } from '@/stores/user'
import { clearAuthSession, removeToken } from '@/utils/auth'

export interface ForceReloginOptions {
  /** 登录页 query.reason，用于展示提示 */
  reason?: string
  /** 登录成功后回跳地址 */
  redirect?: string
  /** 是否跳过后端 logout 接口（改密后 token 已失效时使用） */
  skipLogoutApi?: boolean
}

/**
 * 清除全部前端登录态并跳转登录页（改密、401 等场景复用）。
 */
export async function forceRelogin(options: ForceReloginOptions = {}) {
  const userStore = useUserStore()
  const accountStore = useAccountStore()
  const teamStore = useTeamStore()
  const notificationStore = useNotificationCenterStore()

  accountStore.clearAccountLocalStorage()
  accountStore.clearAccountState()
  teamStore.resetState()
  notificationStore.resetState()

  if (options.skipLogoutApi) {
    userStore.resetLocalAuth()
  } else {
    try {
      await userStore.logout()
    } catch {
      userStore.resetLocalAuth()
    }
  }

  removeToken()
  clearAuthSession()

  const redirect = options.redirect || '/account'
  const query = new URLSearchParams({ redirect })
  if (options.reason) {
    query.set('reason', options.reason)
  }

  if (typeof window !== 'undefined') {
    window.location.href = `/login?${query.toString()}`
  }
}
