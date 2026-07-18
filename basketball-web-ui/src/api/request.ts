import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { getToken } from '@/utils/auth'

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

request.interceptors.response.use((response) => response.data)

export default request
