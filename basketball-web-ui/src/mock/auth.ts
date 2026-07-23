import type { AccountRole } from '@/types/account'

export interface MockLoginAccount {
  userId: number
  email: string
  password: string
  nickname: string
  avatarUrl: string
  accountRole: AccountRole
  emailVerified: boolean
}

export interface MockAuthSession {
  loggedIn: true
  userId: number
  email: string
  nickname: string
  avatarUrl: string
  accountRole: AccountRole
  emailVerified: boolean
  loginTime: number
}

export type MockLoginErrorCode =
  | 'ACCOUNT_NOT_FOUND'
  | 'INVALID_CREDENTIALS'
  | 'EMAIL_NOT_VERIFIED'

export interface MockLoginResult {
  success: boolean
  session?: MockAuthSession
  errorCode?: MockLoginErrorCode
  message?: string
}

const AUTH_STORAGE_KEY = 'cnpa_mock_auth'
const AUTH_SESSION_KEY = 'cnpa_mock_auth'

const mockAccounts: MockLoginAccount[] = [
  {
    userId: 10001,
    email: 'zlz@example.com',
    password: 'Cnpa2026',
    nickname: 'ZLZyoung',
    avatarUrl: '',
    accountRole: 'BASIC_USER',
    emailVerified: true,
  },
  {
    userId: 10002,
    email: 'captain@cnpa.com',
    password: 'Captain2026',
    nickname: 'RNG_01',
    avatarUrl: '',
    accountRole: 'CAPTAIN',
    emailVerified: true,
  },
  {
    userId: 10003,
    email: 'player.pending@cnpa.com',
    password: 'Pending2026',
    nickname: 'PendingPlayer',
    avatarUrl: '',
    accountRole: 'BASIC_USER',
    emailVerified: true,
  },
  {
    userId: 10004,
    email: 'player.rejected@cnpa.com',
    password: 'Reject2026',
    nickname: 'RejectedPlayer',
    avatarUrl: '',
    accountRole: 'BASIC_USER',
    emailVerified: true,
  },
  {
    userId: 10005,
    email: 'player.change@cnpa.com',
    password: 'Change2026',
    nickname: 'ChangePending',
    avatarUrl: '',
    accountRole: 'PLAYER',
    emailVerified: true,
  },
]

function parseSession(raw: string | null): MockAuthSession | null {
  if (!raw) return null

  try {
    const data = JSON.parse(raw) as Partial<MockAuthSession> & { role?: string; playerVerified?: boolean }
    if (
      data.loggedIn !== true
      || typeof data.userId !== 'number'
      || typeof data.email !== 'string'
      || typeof data.nickname !== 'string'
    ) {
      return null
    }

    let accountRole: AccountRole = 'BASIC_USER'
    if (data.accountRole === 'CAPTAIN' || data.accountRole === 'PLAYER' || data.accountRole === 'BASIC_USER') {
      accountRole = data.accountRole
    } else if (data.role === 'CAPTAIN') {
      accountRole = 'CAPTAIN'
    } else if (data.playerVerified) {
      accountRole = 'PLAYER'
    }

    return {
      loggedIn: true,
      userId: data.userId,
      email: data.email,
      nickname: data.nickname,
      avatarUrl: typeof data.avatarUrl === 'string' ? data.avatarUrl : '',
      accountRole,
      emailVerified: Boolean(data.emailVerified ?? true),
      loginTime: typeof data.loginTime === 'number' ? data.loginTime : Date.now(),
    }
  } catch {
    return null
  }
}

function readPersistedSession(): MockAuthSession | null {
  const localRaw = localStorage.getItem(AUTH_STORAGE_KEY)
  const localSession = parseSession(localRaw)
  if (localSession) return localSession

  const sessionRaw = sessionStorage.getItem(AUTH_SESSION_KEY)
  return parseSession(sessionRaw)
}

function persistSession(session: MockAuthSession, remember: boolean) {
  const payload = JSON.stringify(session)
  localStorage.removeItem(AUTH_STORAGE_KEY)
  sessionStorage.removeItem(AUTH_SESSION_KEY)

  if (remember) {
    localStorage.setItem(AUTH_STORAGE_KEY, payload)
  } else {
    sessionStorage.setItem(AUTH_SESSION_KEY, payload)
  }
}

export function clearAuthStorage() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
  sessionStorage.removeItem(AUTH_SESSION_KEY)
}

export function getCurrentAuth(): MockAuthSession | null {
  return readPersistedSession()
}

export function isAuthenticated(): boolean {
  return Boolean(getCurrentAuth()?.loggedIn)
}

export function sanitizeRedirect(path: unknown): string {
  if (typeof path !== 'string' || !path.trim()) return '/account'
  const value = path.trim()
  if (!value.startsWith('/') || value.startsWith('//')) return '/account'
  return value
}

export function getRoleLabel(role: AccountRole): string {
  if (role === 'CAPTAIN') return '认证队长'
  if (role === 'PLAYER') return '认证球员'
  return '基础用户'
}

function deriveNickname(email: string): string {
  const local = email.split('@')[0]?.trim() || 'CNPA_USER'
  const normalized = local.replace(/[^a-zA-Z0-9_]/g, '_').toUpperCase()
  return normalized.slice(0, 20) || 'CNPA_USER'
}

function deriveUserId(email: string): number {
  let hash = 0
  for (let i = 0; i < email.length; i += 1) {
    hash = (hash * 31 + email.charCodeAt(i)) % 900000
  }
  return 10000 + hash
}

function buildDynamicSession(email: string): MockAuthSession {
  const normalizedEmail = email.trim().toLowerCase()
  return {
    loggedIn: true,
    userId: deriveUserId(normalizedEmail),
    email: normalizedEmail,
    nickname: deriveNickname(normalizedEmail),
    avatarUrl: '',
    accountRole: 'BASIC_USER',
    emailVerified: true,
    loginTime: Date.now(),
  }
}

function buildSession(account: MockLoginAccount): MockAuthSession {
  return {
    loggedIn: true,
    userId: account.userId,
    email: account.email,
    nickname: account.nickname,
    avatarUrl: account.avatarUrl,
    accountRole: account.accountRole,
    emailVerified: account.emailVerified,
    loginTime: Date.now(),
  }
}

export async function mockLogin(
  email: string,
  password: string,
  remember = false,
): Promise<MockLoginResult> {
  await new Promise((resolve) => window.setTimeout(resolve, 420))

  const normalizedEmail = email.trim().toLowerCase()
  const account = mockAccounts.find((item) => item.email.toLowerCase() === normalizedEmail)

  if (account && account.password !== password) {
    return {
      success: false,
      errorCode: 'INVALID_CREDENTIALS',
      message: '邮箱或密码错误',
    }
  }

  const session = account ? buildSession(account) : buildDynamicSession(normalizedEmail)
  persistSession(session, remember)

  return {
    success: true,
    session,
  }
}

export function mockLogout() {
  clearAuthStorage()
}

export function patchAuthSession(partial: Partial<Pick<MockAuthSession, 'nickname' | 'avatarUrl'>>) {
  const session = getCurrentAuth()
  if (!session) return
  const next = {
    ...session,
    nickname: partial.nickname ?? session.nickname,
    avatarUrl: partial.avatarUrl ?? session.avatarUrl,
  }
  const localRaw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (localRaw && parseSession(localRaw)) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(next))
    return
  }
  sessionStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(next))
}

export function getMockAccountsForDev(): Omit<MockLoginAccount, 'password'>[] {
  if (!import.meta.env.DEV) return []
  return mockAccounts.map(({ password: _password, ...rest }) => rest)
}
