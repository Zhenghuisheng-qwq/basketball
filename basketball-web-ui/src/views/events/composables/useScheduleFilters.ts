import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue'
import { getPublicSeasonSchedulePage } from '@/api/publicMatch'
import { getPublicSeasonStanding } from '@/api/publicStanding'
import type { PublicStandingItemVo } from '@/api/types/public'
import {
  formatDateKey,
  getWeekNumber,
  isValidDateKey,
  shiftWeek,
  toDateKey,
} from './useCalendar'
import type { Match } from '../types/events'
import type { usePublicSeasonContext } from './usePublicSeasonContext'
import { standingToTeam, toScheduleMatch } from '../utils/mapPublic'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

export function useScheduleFilters() {
  const ctx = inject<SeasonContext>('publicSeasonContext')!

  const selectedDate = ref(formatDateKey(new Date()))
  const selectedTeamId = ref<string | null>(null)
  const currentWeek = ref(getWeekNumber(selectedDate.value))
  const isCalendarOpen = ref(false)
  const isTeamSelectorOpen = ref(false)

  const matches = ref<Match[]>([])
  const teams = ref<PublicStandingItemVo[]>([])
  const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
  const errorMessage = ref('')
  let reqId = 0

  const seasonOptions = computed(() =>
    ctx.seasons.value.map((item) => ({
      id: item.seasonId,
      label: item.seasonName || item.seasonId,
    })),
  )

  const selectedSeason = computed(() => ctx.seasonId.value)

  const teamOptions = computed(() => teams.value.map(standingToTeam))

  const filteredMatches = computed<Match[]>(() => {
    return matches.value.filter((match) => {
      const dateMatch = !selectedDate.value || match.date === selectedDate.value
      const teamMatch =
        selectedTeamId.value === null ||
        match.homeTeamKey === selectedTeamId.value ||
        match.awayTeamKey === selectedTeamId.value
      return dateMatch && teamMatch
    })
  })

  function resolveCalendarBaseDate(): string {
    const candidates = [
      toDateKey(ctx.currentSeason.value?.seasonStartTime),
      matches.value.find((item) => item.date)?.date,
      formatDateKey(new Date()),
    ]

    for (const candidate of candidates) {
      if (candidate && isValidDateKey(candidate)) {
        return candidate
      }
    }

    return formatDateKey(new Date())
  }

  function applyCalendarBaseDate(force = false) {
    if (!force && isValidDateKey(selectedDate.value)) return
    selectedDate.value = resolveCalendarBaseDate()
    currentWeek.value = getWeekNumber(selectedDate.value)
  }

  function closePanels() {
    isCalendarOpen.value = false
    isTeamSelectorOpen.value = false
  }

  function toggleCalendar() {
    isCalendarOpen.value = !isCalendarOpen.value
    if (isCalendarOpen.value) isTeamSelectorOpen.value = false
  }

  function toggleTeamSelector() {
    isTeamSelectorOpen.value = !isTeamSelectorOpen.value
    if (isTeamSelectorOpen.value) isCalendarOpen.value = false
  }

  function selectDate(date: string) {
    if (!isValidDateKey(date)) return
    selectedDate.value = date
    currentWeek.value = getWeekNumber(date)
    closePanels()
  }

  function selectTeam(teamId: string | null) {
    selectedTeamId.value = teamId
    closePanels()
  }

  function selectSeason(seasonId: string) {
    if (!seasonId) return
    ctx.selectSeason(seasonId)
  }

  function goPrevWeek() {
    if (!isValidDateKey(selectedDate.value)) return
    selectedDate.value = shiftWeek(selectedDate.value, -1)
    currentWeek.value = getWeekNumber(selectedDate.value)
  }

  function goNextWeek() {
    if (!isValidDateKey(selectedDate.value)) return
    selectedDate.value = shiftWeek(selectedDate.value, 1)
    currentWeek.value = getWeekNumber(selectedDate.value)
  }

  function handleOutsideClick(event: MouseEvent) {
    const target = event.target as HTMLElement
    if (!target.closest('.schedule-filters') && !target.closest('.schedule-panel')) {
      closePanels()
    }
  }

  function handleEscape(event: KeyboardEvent) {
    if (event.key === 'Escape') closePanels()
  }

  async function loadSchedule(seasonId: string, options?: { resetDate?: boolean }) {
    const current = ++reqId
    if (!seasonId) {
      matches.value = []
      teams.value = []
      state.value = 'empty'
      applyCalendarBaseDate(true)
      return
    }
    state.value = 'loading'
    errorMessage.value = ''
    try {
      const [scheduleRes, standingRes] = await Promise.all([
        getPublicSeasonSchedulePage(seasonId, {
          currentPage: 1,
          pageSize: 100,
          teamId: selectedTeamId.value || undefined,
        }),
        getPublicSeasonStanding(seasonId),
      ])
      if (current !== reqId) return
      const seasonName = ctx.currentSeason.value?.seasonName || ''
      matches.value = (scheduleRes.data?.records ?? []).map((item) =>
        toScheduleMatch(item, seasonName),
      )
      teams.value = standingRes.data ?? []

      applyCalendarBaseDate(Boolean(options?.resetDate))

      state.value = matches.value.length ? 'success' : 'empty'
    } catch (err) {
      if (current !== reqId) return
      matches.value = []
      state.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '加载赛程失败'
      applyCalendarBaseDate(true)
    }
  }

  watch(
    () => [ctx.seasonId.value, selectedTeamId.value] as const,
    ([sid], prev) => {
      const prevSid = prev?.[0]
      const resetDate = prev === undefined || sid !== prevSid
      void loadSchedule(sid, { resetDate })
    },
    { immediate: true },
  )

  onMounted(() => {
    document.addEventListener('click', handleOutsideClick)
    document.addEventListener('keydown', handleEscape)
  })

  onUnmounted(() => {
    document.removeEventListener('click', handleOutsideClick)
    document.removeEventListener('keydown', handleEscape)
  })

  return {
    selectedSeason,
    seasonOptions,
    selectedDate,
    selectedTeamId,
    currentWeek,
    isCalendarOpen,
    isTeamSelectorOpen,
    filteredMatches,
    teamOptions,
    state,
    errorMessage,
    closePanels,
    toggleCalendar,
    toggleTeamSelector,
    selectDate,
    selectTeam,
    selectSeason,
    goPrevWeek,
    goNextWeek,
    reload: () => loadSchedule(ctx.seasonId.value),
  }
}
