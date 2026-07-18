import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface UserInfo {
  id?: number | string
  nickname?: string
  avatar?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  function setToken(value: string) {
    token.value = value
  }

  function setUserInfo(info: UserInfo | null) {
    userInfo.value = info
  }

  function clearToken() {
    token.value = ''
    userInfo.value = null
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    clearToken,
  }
})
