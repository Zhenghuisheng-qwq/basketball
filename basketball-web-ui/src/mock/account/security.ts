import type { AccountSecurityData } from '@/types/account'

export const SECURITY_STORAGE_KEY_PREFIX = 'cnpa_mock_security_'

/** Mock Hash — 不能替代真实后端加密，仅用于前端演示 */
export async function hashPassword(password: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(password)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('')
}

export async function buildDefaultSecurity(
  userId: number,
  password = 'Cnpa2026',
): Promise<AccountSecurityData> {
  return {
    userId,
    passwordHash: await hashPassword(password),
    passwordUpdatedAt: '2025-10-12T08:30:00+08:00',
    lastLoginAt: new Date().toISOString(),
    accountStatus: 'NORMAL',
  }
}

const defaultSecurityCache: Record<number, AccountSecurityData> = {}

export async function getDefaultSecurity(userId: number): Promise<AccountSecurityData> {
  if (defaultSecurityCache[userId]) {
    return { ...defaultSecurityCache[userId] }
  }
  const passwords: Record<number, string> = {
    10001: 'Cnpa2026',
    10002: 'Captain2026',
    10003: 'Pending2026',
    10004: 'Reject2026',
    10005: 'Change2026',
  }
  const data = await buildDefaultSecurity(userId, passwords[userId] || 'Cnpa2026')
  defaultSecurityCache[userId] = data
  return { ...data }
}

export function securityStorageKey(userId: number): string {
  return `${SECURITY_STORAGE_KEY_PREFIX}${userId}`
}

export function validatePasswordRules(password: string): string {
  if (password.length < 8 || password.length > 20) {
    return '密码长度应为 8 至 20 位'
  }
  if (!/[a-zA-Z]/.test(password)) {
    return '密码至少包含一个字母'
  }
  if (!/\d/.test(password)) {
    return '密码至少包含一个数字'
  }
  return ''
}
