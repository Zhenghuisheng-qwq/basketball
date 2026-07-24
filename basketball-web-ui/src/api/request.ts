import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { getToken, removeToken } from '@/utils/auth'

export interface ApiResult<T = unknown> {
  code: number
  msg?: string
  data: T
}

let handling401 = false

function redirectToLogin() {
  if (handling401 || typeof window === 'undefined') return
  handling401 = true
  removeToken()
  const path = window.location.pathname + window.location.search
  if (!path.startsWith('/login')) {
    const redirect = encodeURIComponent(path)
    window.location.href = `/login?redirect=${redirect}`
  }
  window.setTimeout(() => {
    handling401 = false
  }, 1500)
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult
    if (payload && typeof payload.code === 'number' && payload.code !== 200) {
      if (payload.code === 401) {
        redirectToLogin()
      }
      return Promise.reject(new Error(payload.msg || '请求失败'))
    }
    return payload as unknown as typeof response
  },
  (error) => {
    const status = error?.response?.status
    const payload = error?.response?.data as ApiResult | undefined
    if (status === 401) {
      redirectToLogin()
    }
    const message = payload?.msg || (status ? `请求失败（${status}）` : '网络异常，请检查网络后重试')
    return Promise.reject(new Error(message))
  },
)

export default request
