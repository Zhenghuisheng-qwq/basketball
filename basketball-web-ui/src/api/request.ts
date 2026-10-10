import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { i18n } from '@/i18n'
import { clearAuthSession, getToken, removeToken } from '@/utils/auth'
import { resolveBusinessMessage } from '@/utils/apiError'

export interface ApiResult<T = unknown> {
  code: number
  msg?: string
  reasonCode?: string
  data: T
}

export class ApiBusinessError extends Error {
  reasonCode?: string

  constructor(message: string, reasonCode?: string) {
    super(message)
    this.name = 'ApiBusinessError'
    this.reasonCode = reasonCode
  }
}

function currentLocale() {
  return String(i18n.global.locale.value || 'zh-CN')
}

function messageFor(payload?: ApiResult, status?: number) {
  return resolveBusinessMessage({
    reasonCode: payload?.reasonCode,
    serverMessage: payload?.msg || (status ? `请求失败（${status}）` : ''),
    locale: currentLocale(),
    generic: String(i18n.global.t('errors.generic')),
    translate: (code) => String(i18n.global.t(`errors.${code}`)),
  })
}

let handling401 = false

async function redirectToLogin() {
  if (handling401 || typeof window === 'undefined') return
  handling401 = true
  try {
    const { forceRelogin } = await import('@/utils/forceRelogin')
    const path = window.location.pathname + window.location.search
    await forceRelogin({
      redirect: path.startsWith('/login') ? '/account' : path,
      skipLogoutApi: true,
    })
  } catch {
    removeToken()
    clearAuthSession()
    const path = window.location.pathname + window.location.search
    if (!path.startsWith('/login')) {
      const redirect = encodeURIComponent(path)
      window.location.href = `/login?redirect=${redirect}`
    }
  } finally {
    window.setTimeout(() => {
      handling401 = false
    }, 1500)
  }
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

const inflightGets = new Map<string, Promise<unknown>>()

function inflightGetKey(url: string, config?: { params?: unknown }) {
  return `${url}?${config?.params ? JSON.stringify(config.params) : ''}`
}

const originalGet = request.get.bind(request)
request.get = ((url: string, config?: Parameters<AxiosInstance['get']>[1]) => {
  const key = inflightGetKey(url, config)
  const pending = inflightGets.get(key)
  if (pending) {
    return pending as ReturnType<AxiosInstance['get']>
  }
  const next = originalGet(url, config).finally(() => {
    inflightGets.delete(key)
  })
  inflightGets.set(key, next)
  return next
}) as AxiosInstance['get']

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const locale = currentLocale()
  config.headers['X-CNPA-Locale'] = locale
  config.headers['Accept-Language'] = locale
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult
    if (payload && typeof payload.code === 'number' && payload.code !== 200) {
      if (payload.code === 401) {
        redirectToLogin()
      }
      return Promise.reject(new ApiBusinessError(messageFor(payload), payload.reasonCode))
    }
    return payload as unknown as typeof response
  },
  (error) => {
    const status = error?.response?.status
    const payload = error?.response?.data as ApiResult | undefined
    if (status === 401) {
      redirectToLogin()
    }
    const message = payload
      ? messageFor(payload, status)
      : String(i18n.global.t('errors.network'))
    return Promise.reject(new ApiBusinessError(message, payload?.reasonCode))
  },
)

export default request
