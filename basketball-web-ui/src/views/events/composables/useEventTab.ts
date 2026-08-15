import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EventTab } from '../types/events'

const VALID_TABS: EventTab[] = ['overview', 'schedule', 'stage']

export function useEventTab() {
  const route = useRoute()
  const router = useRouter()

  const activeTab = computed<EventTab>(() => {
    const tab = route.query.tab
    if (typeof tab === 'string' && VALID_TABS.includes(tab as EventTab)) {
      return tab as EventTab
    }
    return 'overview'
  })

  function setTab(tab: EventTab) {
    router.push({
      path: route.path,
      query: {
        ...route.query,
        tab,
      },
    })
  }

  return {
    activeTab,
    setTab,
  }
}
