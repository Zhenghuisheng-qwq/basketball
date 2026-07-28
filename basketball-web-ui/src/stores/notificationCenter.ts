import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getAccountMessageDetail,
  getAccountMessagePage,
  markAccountMessageRead,
  markAllAccountMessagesRead,
} from '@/api/accountMessage'
import { mapMessageItemToNotification } from '@/utils/accountMappers'
import type { AccountNotificationItem } from '@/types/accountNotifications'
import type { NotificationTabKey } from '@/types/accountNotifications'

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const items = ref<AccountNotificationItem[]>([])
  const pageTotal = ref(0)
  const allCountValue = ref(0)
  const unreadCountValue = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const activeTab = ref<NotificationTabKey>('ALL')
  const loading = ref(false)
  const error = ref('')

  const total = computed(() => pageTotal.value)
  const allCount = computed(() => allCountValue.value)
  const unreadCount = computed(() => unreadCountValue.value)

  function resolveReadStatus(tab: NotificationTabKey) {
    return tab === 'UNREAD' ? 0 : undefined
  }

  async function refresh(
    page = pageNum.value,
    size = pageSize.value,
    readStatus?: number,
    tab: NotificationTabKey = activeTab.value,
  ) {
    loading.value = true
    error.value = ''
    try {
      const pageRes = await getAccountMessagePage({
        currentPage: page,
        pageSize: size,
        readStatus,
      })
      const data = pageRes.data
      items.value = (data.records || []).map(mapMessageItemToNotification)
      pageTotal.value = data.total || 0
      allCountValue.value = Number(data.allCount ?? allCountValue.value)
      unreadCountValue.value = Number(data.unreadCount ?? unreadCountValue.value)
      pageNum.value = page
      pageSize.value = size
      activeTab.value = tab

      if (items.value.length === 0 && pageTotal.value > 0 && page > 1) {
        const lastPage = Math.max(1, Math.ceil(pageTotal.value / size))
        await refresh(lastPage, size, readStatus, tab)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function loadPage(tab: NotificationTabKey = activeTab.value) {
    pageNum.value = 1
    return refresh(1, pageSize.value, resolveReadStatus(tab), tab)
  }

  async function changePage(page: number) {
    return refresh(page, pageSize.value, resolveReadStatus(activeTab.value), activeTab.value)
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
    unreadCountValue.value = 0
    await refresh(pageNum.value, pageSize.value, resolveReadStatus(activeTab.value), activeTab.value)
  }

  function getById(id: string) {
    return items.value.find((item) => item.id === id)
  }

  function resetState() {
    items.value = []
    pageTotal.value = 0
    allCountValue.value = 0
    unreadCountValue.value = 0
    pageNum.value = 1
    pageSize.value = 10
    activeTab.value = 'ALL'
    loading.value = false
    error.value = ''
  }

  return {
    items,
    total,
    allCount,
    unreadCount,
    pageNum,
    pageSize,
    activeTab,
    loading,
    error,
    refresh,
    loadPage,
    changePage,
    loadDetail,
    markAsRead,
    markAllAsRead,
    getById,
    resetState,
  }
})
