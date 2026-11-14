import { computed, onMounted, ref, watch, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getV4PublicSeasonList } from '@/api/v4Public'
import type { V4PublicSeasonItem } from '@/api/types/v4Public'
import {
  buildCurrentEventMenuEntries,
  competitionMenuGroups,
  type MegaMenuType,
  type MenuEntryHighlight,
  type PublicMenuEntry,
} from '@/constants/publicCompetitionMenu'
import { v4SeasonStatusLabel, v4SeriesDisplayLabel } from '@/utils/v4Display'

export type OpenMenu = MegaMenuType | null

const CLOSE_DELAY_MS = 200

export function usePublicCompetitionMegaMenu() {
  const { t, locale } = useI18n()
  const route = useRoute()
  const router = useRouter()

  const openMenu = ref<OpenMenu>(null)
  const seasons = ref<V4PublicSeasonItem[]>([])
  const seasonsReady = ref(false)

  const eventEntries = computed(() => {
    if (!seasonsReady.value) return competitionMenuGroups.event[0].entries
    return buildCurrentEventMenuEntries(seasons.value, {
      locale: locale.value,
      seriesLabel: (code) => v4SeriesDisplayLabel(undefined, code),
      statusLabel: (status) => v4SeasonStatusLabel(status),
      emptyHint: t('home.noEvent'),
    })
  })

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

  function isEventsListRoute(): boolean {
    return route.path === '/tournament' || route.path === '/events'
  }

  function getEntryHighlight(entry: PublicMenuEntry): MenuEntryHighlight | null {
    const param = route.params.seasonId
    const seasonParam = typeof param === 'string' ? param : (Array.isArray(param) ? param[0] : '')
    if (entry.seasonId && seasonParam && entry.seasonId === seasonParam) return 'primary'
    if (!entry.seriesCode || !isEventsListRoute()) return null
    return normalizeSeries() === entry.seriesCode ? 'primary' : null
  }

  const currentGroups = computed(() => {
    if (!openMenu.value) return []
    return [{
      title: t('home.currentEvents'),
      entries: eventEntries.value,
    }]
  })

  onMounted(() => {
    getV4PublicSeasonList()
      .then((res) => {
        seasons.value = res.data ?? []
        seasonsReady.value = true
      })
      .catch(() => {
        seasonsReady.value = false
      })
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
    eventEntries,
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
