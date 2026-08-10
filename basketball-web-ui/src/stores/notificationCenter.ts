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

export const NOTIFICATION_PAGE_SIZE = 10

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const items = ref<AccountNotificationItem[]>([])
  const allCountValue = ref(0)
  const unreadCountValue = ref(0)
  const allPageNum = ref(1)
  const unreadPageNum = ref(1)
  const allPageTotal = ref(0)
  const unreadPageTotal = ref(0)
  const activeTab = ref<NotificationTabKey>('ALL')
  const loading = ref(false)
  const error = ref('')

  const pageSize = computed(() => NOTIFICATION_PAGE_SIZE)
  const pageNum = computed(() => (activeTab.value === 'UNREAD' ? unreadPageNum.value : allPageNum.value))
  const total = computed(() => (activeTab.value === 'UNREAD' ? unreadPageTotal.value : allPageTotal.value))
  const allCount = computed(() => allCountValue.value)
  const unreadCount = computed(() => unreadCountValue.value)

  function resolveReadStatus(tab: NotificationTabKey) {
    return tab === 'UNREAD' ? 0 : undefined
  }

  function resolveTabPage(tab: NotificationTabKey) {
    return tab === 'UNREAD' ? unreadPageNum.value : allPageNum.value
  }

  function setTabPageTotal(tab: NotificationTabKey, page: number, tabTotal: number) {
    if (tab === 'UNREAD') {
      unreadPageNum.value = page
      unreadPageTotal.value = tabTotal
      return
    }
    allPageNum.value = page
    allPageTotal.value = tabTotal
  }

  async function refresh(
    page = resolveTabPage(activeTab.value),
    size = NOTIFICATION_PAGE_SIZE,
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
      const tabTotal = data.total || 0
      setTabPageTotal(tab, page, tabTotal)
      allCountValue.value = Number(data.allCount ?? allCountValue.value)
      unreadCountValue.value = Number(data.unreadCount ?? unreadCountValue.value)
      activeTab.value = tab

      if (items.value.length === 0 && tabTotal > 0 && page > 1) {
        const lastPage = Math.max(1, Math.ceil(tabTotal / size))
        await refresh(lastPage, size, resolveReadStatus(tab), tab)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function loadPage(tab: NotificationTabKey = activeTab.value) {
    if (tab === 'UNREAD') {
      unreadPageNum.value = 1
    } else {
      allPageNum.value = 1
    }
    return refresh(1, NOTIFICATION_PAGE_SIZE, resolveReadStatus(tab), tab)
  }

  async function changePage(page: number) {
    return refresh(page, NOTIFICATION_PAGE_SIZE, resolveReadStatus(activeTab.value), activeTab.value)
  }

  async function loadDetail(receiverId: string) {
    const res = await getAccountMessageDetail(receiverId)
    const mapped = mapMessageItemToNotification(res.data)
    const existingIndex = items.value.findIndex((item) => item.id === receiverId)
    if (existingIndex >= 0) {
      items.value[existingIndex] = mapped
    }
    return mapped
  }

  async function markAsRead(id: string) {
    const target = items.value.find((item) => item.id === id)
    const wasUnread = target?.isUnread ?? true
    await markAccountMessageRead(id)
    if (target && target.isUnread) {
      target.isUnread = false
      target.status = 'READ'
    }
    if (wasUnread) {
      unreadCountValue.value = Math.max(0, unreadCountValue.value - 1)
      await refresh(
        resolveTabPage(activeTab.value),
        NOTIFICATION_PAGE_SIZE,
        resolveReadStatus(activeTab.value),
        activeTab.value,
      )
    }
  }

  async function markAllAsRead() {
    await markAllAccountMessagesRead()
    unreadCountValue.value = 0
    if (activeTab.value === 'UNREAD') {
      unreadPageNum.value = 1
    }
    await refresh(
      activeTab.value === 'UNREAD' ? 1 : resolveTabPage(activeTab.value),
      NOTIFICATION_PAGE_SIZE,
      resolveReadStatus(activeTab.value),
      activeTab.value,
    )
  }

  function getById(id: string) {
    return items.value.find((item) => item.id === id)
  }

  function resetState() {
    items.value = []
    allPageTotal.value = 0
    unreadPageTotal.value = 0
    allCountValue.value = 0
    unreadCountValue.value = 0
    allPageNum.value = 1
    unreadPageNum.value = 1
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
    allPageNum,
    unreadPageNum,
    allPageTotal,
    unreadPageTotal,
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
