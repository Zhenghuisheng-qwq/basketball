import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getMemberSession,
  loginByEmailCode as apiLoginByEmailCode,
  loginByPassword as apiLoginByPassword,
  logoutMember,
  type MemberLoginResult,
  type MemberSessionResult,
} from '@/api/memberAuth'
import {
  clearAuthSession,
  getAuthSession,
  getToken,
  removeToken,
  setAuthSession,
  setToken,
  toBridgeUserId,
  type AuthSession,
} from '@/utils/auth'

export interface UserInfo {
  id?: number | string
  nickname?: string
  avatar?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const currentMember = ref<MemberSessionResult | null>(null)
  const userInfo = ref<UserInfo | null>(null)
  const initialized = ref(false)
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value && currentMember.value))

  function applyLoginResult(result: MemberLoginResult, remember: boolean) {
    setToken(result.token, remember)
    token.value = result.token
    currentMember.value = {
      ...result.member,
    }
    userInfo.value = {
      id: result.member.memberId,
      nickname: result.member.nickname,
      avatar: result.member.avatarUrl || undefined,
    }
    const session: AuthSession = {
      loggedIn: true,
      memberId: result.member.memberId,
      userId: toBridgeUserId(result.member.memberId),
      email: result.member.emailMasked,
      nickname: result.member.nickname,
      avatarUrl: result.member.avatarUrl || '',
      accountNo: result.member.accountNo,
      emailVerified: Boolean(result.member.emailVerified),
      mobileVerified: Boolean(result.member.mobileVerified),
      accountStatus: result.member.accountStatus,
      loginTime: Date.now(),
    }
    setAuthSession(session, remember)
  }

  function clearLocalAuth() {
    removeToken()
    clearAuthSession()
    token.value = ''
    currentMember.value = null
    userInfo.value = null
  }

  async function restoreSession() {
    if (loading.value) return
    loading.value = true
    try {
      const localToken = getToken()
      if (!localToken) {
        clearLocalAuth()
        return
      }
      token.value = localToken
      const res = await getMemberSession()
      currentMember.value = res.data
      userInfo.value = {
        id: res.data.memberId,
        nickname: res.data.nickname,
        avatar: res.data.avatarUrl || undefined,
      }
      const remember = Boolean(localStorage.getItem('cnpa_web_token'))
      setAuthSession({
        loggedIn: true,
        memberId: res.data.memberId,
        userId: toBridgeUserId(res.data.memberId),
        email: res.data.emailMasked,
        nickname: res.data.nickname,
        avatarUrl: res.data.avatarUrl || '',
        accountNo: res.data.accountNo,
        emailVerified: Boolean(res.data.emailVerified),
        mobileVerified: Boolean(res.data.mobileVerified),
        accountStatus: res.data.accountStatus,
        loginTime: Date.now(),
      }, remember)
    } catch {
      clearLocalAuth()
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  async function ensureInitialized() {
    if (initialized.value) return
    await restoreSession()
  }

  async function loginByPassword(account: string, password: string, remember = true) {
    const res = await apiLoginByPassword({ account, password, terminalType: 'WEB' })
    applyLoginResult(res.data, remember)
    initialized.value = true
    return res.data
  }

  async function loginByEmailCode(email: string, verifyBizNo: string, verifyCode: string, remember = true) {
    const res = await apiLoginByEmailCode({
      email,
      verifyBizNo,
      verifyCode,
      terminalType: 'WEB',
    })
    applyLoginResult(res.data, remember)
    initialized.value = true
    return res.data
  }

  async function logout() {
    try {
      if (getToken()) {
        await logoutMember()
      }
    } catch {
      // 本地清理优先
    }
    clearLocalAuth()
    initialized.value = true
  }

  function setUserInfo(info: UserInfo | null) {
    userInfo.value = info
  }

  function setTokenState(value: string) {
    token.value = value
  }

  function clearToken() {
    clearLocalAuth()
  }

  // 兼容旧调用
  function setTokenCompat(value: string) {
    setTokenState(value)
  }

  return {
    token,
    currentMember,
    userInfo,
    initialized,
    loading,
    isAuthenticated,
    getAuthSession,
    restoreSession,
    ensureInitialized,
    loginByPassword,
    loginByEmailCode,
    applyLoginResult,
    logout,
    setUserInfo,
    setToken: setTokenCompat,
    clearToken,
  }
})
