export const TOKEN_KEY = 'cnpa_web_token'
export const AUTH_SESSION_KEY = 'cnpa_web_session'

export interface AuthSession {
  loggedIn: true
  memberId: string
  /** 供个人中心 Mock 桥接的稳定数字 ID */
  userId: number
  email: string
  nickname: string
  avatarUrl: string
  accountNo: string
  emailVerified: boolean
  mobileVerified: boolean
  accountStatus: number
  loginTime: number
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string, remember = true): void {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  if (remember) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
  }
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

export function isLoggedIn(): boolean {
  return Boolean(getToken())
}

export function sanitizeRedirect(path: unknown): string {
  if (typeof path !== 'string' || !path.trim()) return '/account'
  const value = path.trim()
  if (!value.startsWith('/') || value.startsWith('//')) return '/account'
  return value
}

export function toBridgeUserId(memberId: string): number {
  let hash = 0
  for (let i = 0; i < memberId.length; i += 1) {
    hash = (Math.imul(31, hash) + memberId.charCodeAt(i)) | 0
  }
  return 2_000_000_000 + Math.abs(hash % 1_000_000_000)
}

export function getAuthSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_SESSION_KEY) || sessionStorage.getItem(AUTH_SESSION_KEY)
  if (!raw) return null
  try {
    const data = JSON.parse(raw) as Partial<AuthSession>
    if (data.loggedIn !== true || typeof data.memberId !== 'string') {
      return null
    }
    return {
      loggedIn: true,
      memberId: data.memberId,
      userId: typeof data.userId === 'number' ? data.userId : toBridgeUserId(data.memberId),
      email: typeof data.email === 'string' ? data.email : '',
      nickname: typeof data.nickname === 'string' ? data.nickname : 'CNPA会员',
      avatarUrl: typeof data.avatarUrl === 'string' ? data.avatarUrl : '',
      accountNo: typeof data.accountNo === 'string' ? data.accountNo : '',
      emailVerified: Boolean(data.emailVerified),
      mobileVerified: Boolean(data.mobileVerified),
      accountStatus: typeof data.accountStatus === 'number' ? data.accountStatus : 1,
      loginTime: typeof data.loginTime === 'number' ? data.loginTime : Date.now(),
    }
  } catch {
    return null
  }
}

export function setAuthSession(session: AuthSession, remember = true): void {
  const payload = JSON.stringify(session)
  localStorage.removeItem(AUTH_SESSION_KEY)
  sessionStorage.removeItem(AUTH_SESSION_KEY)
  if (remember) {
    localStorage.setItem(AUTH_SESSION_KEY, payload)
  } else {
    sessionStorage.setItem(AUTH_SESSION_KEY, payload)
  }
}

export function clearAuthSession(): void {
  localStorage.removeItem(AUTH_SESSION_KEY)
  sessionStorage.removeItem(AUTH_SESSION_KEY)
}

export function patchAuthSession(partial: Partial<Pick<AuthSession, 'nickname' | 'avatarUrl'>>): void {
  const current = getAuthSession()
  if (!current) return
  const next: AuthSession = {
    ...current,
    nickname: partial.nickname ?? current.nickname,
    avatarUrl: partial.avatarUrl ?? current.avatarUrl,
  }
  const inLocal = Boolean(localStorage.getItem(AUTH_SESSION_KEY))
  setAuthSession(next, inLocal)
}
