import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue'
import { getV4PublicMatchList, getV4PublicStanding } from '@/api/v4Public'
import { v4MatchToLegacyShape, v4StandingToPublic } from '../utils/v4PublicAdapters'
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
import { teamIdToNumeric } from '../utils/teamLookup'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

export function useScheduleFilters() {
  const ctx = inject<SeasonContext>('publicSeasonContext')!

  const selectedDate = ref(formatDateKey(new Date()))
  const selectedTeamId = ref<number | null>(null)
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
      id: String(item.seasonId),
      label:
        item.competitionSeasonName && item.seasonName && item.competitionSeasonName !== item.seasonName
          ? `${item.competitionSeasonName} · ${item.seasonName}`
          : item.seasonName || item.competitionSeasonName || String(item.seasonId),
    })),
  )

  const selectedSeason = computed(() => ctx.seasonId.value)

  const teamOptions = computed(() => teams.value.map(standingToTeam))

  function resolveEventTeamId(numericId: number | null): string | undefined {
    if (numericId === null) return undefined
    return teamOptions.value.find((team) => team.id === numericId)?.teamId
  }

  const filteredMatches = computed<Match[]>(() => {
    return matches.value.filter((match) => {
      const dateMatch = !selectedDate.value || match.date === selectedDate.value
      const teamMatch =
        selectedTeamId.value === null ||
        teamIdToNumeric(match.homeTeamKey) === selectedTeamId.value ||
        teamIdToNumeric(match.awayTeamKey) === selectedTeamId.value
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

  function selectTeam(teamId: number | null) {
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

  async function loadSchedule(options?: { resetDate?: boolean }) {
    const current = ++reqId
    const divisionId = ctx.divisionId.value
    if (!divisionId) {
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
        getV4PublicMatchList({
          divisionId,
          eventTeamId: resolveEventTeamId(selectedTeamId.value),
        }),
        getV4PublicStanding(divisionId),
      ])
      if (current !== reqId) return
      const seasonName = ctx.currentSeason.value?.seasonName || ''
      matches.value = (scheduleRes.data ?? []).map((item) =>
        toScheduleMatch(v4MatchToLegacyShape(item) as any, seasonName),
      )
      teams.value = (standingRes.data ?? []).map((row) =>
        v4StandingToPublic(row, ctx.seasonId.value),
      )

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
    () => [ctx.divisionId.value, selectedTeamId.value] as const,
    ([did], prev) => {
      const prevDid = prev?.[0]
      const resetDate = prev === undefined || did !== prevDid
      void loadSchedule({ resetDate })
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
    reload: () => loadSchedule(),
  }
}
