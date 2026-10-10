import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import { i18n } from '@/i18n'
import {
  getAccountMessageDetail,
  getAccountMessagePage,
  markAccountMessageRead,
  markAllAccountMessagesRead,
} from '@/api/accountMessage'
import { mapMessageItemToNotification } from '@/utils/accountMappers'
import type { MessageItemDto } from '@/api/types/accountMessage'
import type { NotificationTabKey } from '@/types/accountNotifications'

export const NOTIFICATION_PAGE_SIZE = 10

export const useNotificationCenterStore = defineStore('notificationCenter', () => {
  const records = ref<MessageItemDto[]>([])
  const localeTick = ref(0)
  watch(() => i18n.global.locale.value, () => {
    localeTick.value += 1
  })
  const items = computed(() => {
    void localeTick.value
    return records.value.map(mapMessageItemToNotification)
  })
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
      records.value = data.records || []
      const tabTotal = data.total || 0
      setTabPageTotal(tab, page, tabTotal)
      allCountValue.value = Number(data.allCount ?? allCountValue.value)
      unreadCountValue.value = Number(data.unreadCount ?? unreadCountValue.value)
      activeTab.value = tab

      if (records.value.length === 0 && tabTotal > 0 && page > 1) {
        const lastPage = Math.max(1, Math.ceil(tabTotal / size))
        await refresh(lastPage, size, resolveReadStatus(tab), tab)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : String(i18n.global.t('accountShell.notifications.loadFailed'))
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
    const existingIndex = records.value.findIndex((item) => item.receiverId === receiverId)
    if (existingIndex >= 0) {
      records.value[existingIndex] = res.data
    }
    return mapMessageItemToNotification(res.data)
  }

  async function markAsRead(id: string) {
    const target = records.value.find((item) => item.receiverId === id)
    const wasUnread = target ? target.readStatus !== 1 : true
    await markAccountMessageRead(id)
    if (target && target.readStatus !== 1) {
      target.readStatus = 1
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
    const record = records.value.find((item) => item.receiverId === id)
    return record ? mapMessageItemToNotification(record) : undefined
  }

  function resetState() {
    records.value = []
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
