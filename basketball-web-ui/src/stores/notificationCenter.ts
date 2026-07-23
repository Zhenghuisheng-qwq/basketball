import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cloneInitialNotifications } from '@/mock/accountNotificationsMock'
import type { AccountNotificationItem } from '@/types/accountNotifications'

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const items = ref<AccountNotificationItem[]>(cloneInitialNotifications())

  const total = computed(() => items.value.length)
  const unreadCount = computed(() => items.value.filter((item) => item.isUnread).length)

  function resetMock() {
    items.value = cloneInitialNotifications()
  }

  function markAsRead(id: string) {
    const target = items.value.find((item) => item.id === id)
    if (!target) return
    target.isUnread = false
    target.status = 'READ'
  }

  function markAllAsRead() {
    items.value.forEach((item) => {
      item.isUnread = false
      item.status = 'READ'
    })
  }

  function getById(id: string) {
    return items.value.find((item) => item.id === id)
  }

  return {
    items,
    total,
    unreadCount,
    resetMock,
    markAsRead,
    markAllAsRead,
    getById,
  }
})
