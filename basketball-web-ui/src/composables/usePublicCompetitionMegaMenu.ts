import { computed, ref, watch, type ComputedRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  competitionMenuGroups,
  DATA_TAB_VALUES,
  type MegaMenuType,
  type MenuEntryHighlight,
  type PublicMenuEntry,
} from '@/constants/publicCompetitionMenu'

export type OpenMenu = MegaMenuType | null

const CLOSE_DELAY_MS = 200

export function usePublicCompetitionMegaMenu() {
  const route = useRoute()
  const router = useRouter()

  const openMenu = ref<OpenMenu>(null)

  let closeTimer: ReturnType<typeof setTimeout> | null = null

  const isOpen = computed(() => openMenu.value !== null)

  const isTournamentRoute = computed(
    () =>
      route.path === '/tournament'
      || route.path === '/events'
      || route.path.startsWith('/events/'),
  )

  const isDataRoute = computed(() => route.path === '/data')

  const isEventsNavActive = computed(() => isTournamentRoute.value)

  const isDataNavActive = computed(() => isDataRoute.value)

  function clearCloseTimer() {
    if (closeTimer) {
      clearTimeout(closeTimer)
      closeTimer = null
    }
  }

  function setOpenMenu(menu: OpenMenu) {
    clearCloseTimer()
    openMenu.value = menu
  }

  function closeMenu() {
    clearCloseTimer()
    openMenu.value = null
  }

  function scheduleClose() {
    clearCloseTimer()
    closeTimer = setTimeout(() => {
      openMenu.value = null
    }, CLOSE_DELAY_MS)
  }

  function handleMouseEnter(type: MegaMenuType) {
    setOpenMenu(type)
  }

  function handleMouseLeave() {
    scheduleClose()
  }

  function toggleMenu(type: MegaMenuType) {
    if (openMenu.value === type) {
      closeMenu()
      return
    }
    setOpenMenu(type)
  }

  function navigate(entry: PublicMenuEntry) {
    router.push(entry.to)
    closeMenu()
  }

  function normalizeSeries(): string {
    const raw = route.query.series ?? route.query.seriesCode
    return typeof raw === 'string' ? raw.trim().toUpperCase() : ''
  }

  function normalizeDataTab(): string {
    const raw = route.query.tab
    if (typeof raw === 'string' && (DATA_TAB_VALUES as readonly string[]).includes(raw)) {
      return raw
    }
    return 'standing'
  }

  function isEventsListRoute(): boolean {
    return route.path === '/tournament' || route.path === '/events'
  }

  function getEntryHighlight(entry: PublicMenuEntry): MenuEntryHighlight | null {
    if (entry.id.startsWith('events-')) {
      if (!isEventsListRoute()) return null

      if (entry.id.startsWith('events-series-')) {
        const code = entry.id.slice('events-series-'.length)
        if (normalizeSeries() === code) return 'primary'
        return null
      }
    }

    if (entry.id.startsWith('data-') && isDataRoute.value) {
      if (entry.id.startsWith('data-series-')) {
        const code = entry.id.slice('data-series-'.length)
        if (normalizeSeries() === code) return 'secondary'
        return null
      }

      const tabByEntry: Record<string, string> = {
        'data-standing': 'standing',
        'data-team': 'team',
        'data-player': 'player',
        'data-rank': 'rank',
      }
      const tab = tabByEntry[entry.id]
      if (tab && normalizeDataTab() === tab) return 'primary'
    }

    return null
  }

  const currentGroups = computed(() => {
    if (!openMenu.value) return []
    return competitionMenuGroups[openMenu.value]
  })

  watch(
    () => route.fullPath,
    () => {
      closeMenu()
    },
  )

  return {
    isOpen,
    openMenu,
    currentGroups,
    competitionMenuGroups,
    isTournamentRoute,
    isDataRoute,
    isEventsNavActive,
    isDataNavActive,
    setOpenMenu,
    closeMenu,
    scheduleClose,
    clearCloseTimer,
    handleMouseEnter,
    handleMouseLeave,
    toggleMenu,
    navigate,
    getEntryHighlight,
  }
}

export type PublicCompetitionMegaMenuContext = ReturnType<typeof usePublicCompetitionMegaMenu>

export function useEntryHighlight(
  entry: PublicMenuEntry,
  getEntryHighlight: (entry: PublicMenuEntry) => MenuEntryHighlight | null,
): ComputedRef<MenuEntryHighlight | null> {
  return computed(() => getEntryHighlight(entry))
}
