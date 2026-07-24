import type { AccountRole } from '@/types/account'
import {
  clearAuthSession,
  getAuthSession,
  isLoggedIn,
  patchAuthSession as patchRealAuthSession,
  sanitizeRedirect as sanitizeRealRedirect,
  type AuthSession,
} from '@/utils/auth'

/**
 * 仅作为个人中心等业务 Mock 的会话桥接，登录注册不再依赖本文件。
 */
export interface MockAuthSession {
  loggedIn: true
  userId: number
  memberId?: string
  email: string
  nickname: string
  avatarUrl: string
  accountRole: AccountRole
  emailVerified: boolean
  loginTime: number
}

function toMockSession(session: AuthSession): MockAuthSession {
  return {
    loggedIn: true,
    userId: session.userId,
    memberId: session.memberId,
    email: session.email,
    nickname: session.nickname,
    avatarUrl: session.avatarUrl,
    accountRole: 'BASIC_USER',
    emailVerified: session.emailVerified,
    loginTime: session.loginTime,
  }
}

export function clearAuthStorage() {
  clearAuthSession()
}

export function getCurrentAuth(): MockAuthSession | null {
  const session = getAuthSession()
  return session ? toMockSession(session) : null
}

export function isAuthenticated(): boolean {
  return isLoggedIn()
}

export function sanitizeRedirect(path: unknown): string {
  return sanitizeRealRedirect(path)
}

export function getRoleLabel(role: AccountRole): string {
  if (role === 'CAPTAIN') return '认证队长'
  if (role === 'PLAYER') return '认证球员'
  return '基础用户'
}

export function patchAuthSession(partial: Partial<Pick<MockAuthSession, 'nickname' | 'avatarUrl'>>) {
  patchRealAuthSession(partial)
}
