import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getAccountMessageDetail,
  getAccountMessagePage,
  getAccountMessageUnreadCount,
  markAccountMessageRead,
  markAllAccountMessagesRead,
} from '@/api/accountMessage'
import { mapMessageItemToNotification } from '@/utils/accountMappers'
import type { AccountNotificationItem } from '@/types/accountNotifications'

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const items = ref<AccountNotificationItem[]>([])
  const totalCount = ref(0)
  const unreadCountValue = ref(0)
  const loading = ref(false)
  const error = ref('')

  const total = computed(() => totalCount.value || items.value.length)
  const unreadCount = computed(() => unreadCountValue.value)

  async function refresh(page = 1, pageSize = 20, readStatus?: number) {
    loading.value = true
    error.value = ''
    try {
      const [pageRes, countRes] = await Promise.all([
        getAccountMessagePage({ currentPage: page, pageSize, readStatus }),
        getAccountMessageUnreadCount(),
      ])
      items.value = (pageRes.data.records || []).map(mapMessageItemToNotification)
      totalCount.value = pageRes.data.total || items.value.length
      unreadCountValue.value = Number(countRes.data || 0)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function loadDetail(receiverId: string) {
    const res = await getAccountMessageDetail(receiverId)
    const mapped = mapMessageItemToNotification(res.data)
    const existingIndex = items.value.findIndex((item) => item.id === receiverId)
    if (existingIndex >= 0) {
      items.value[existingIndex] = mapped
    } else {
      items.value.unshift(mapped)
    }
    return mapped
  }

  async function markAsRead(id: string) {
    await markAccountMessageRead(id)
    const target = items.value.find((item) => item.id === id)
    if (target && target.isUnread) {
      target.isUnread = false
      target.status = 'READ'
      unreadCountValue.value = Math.max(0, unreadCountValue.value - 1)
    }
  }

  async function markAllAsRead() {
    await markAllAccountMessagesRead()
    items.value.forEach((item) => {
      item.isUnread = false
      item.status = 'READ'
    })
    unreadCountValue.value = 0
  }

  function getById(id: string) {
    return items.value.find((item) => item.id === id)
  }

  return {
    items,
    total,
    unreadCount,
    loading,
    error,
    refresh,
    loadDetail,
    markAsRead,
    markAllAsRead,
    getById,
  }
})
