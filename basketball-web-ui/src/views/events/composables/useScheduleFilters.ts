import { computed, onMounted, onUnmounted, ref } from 'vue'
import { matches as allMatches, defaultScheduleDate, defaultScheduleWeek } from '../mock'
import { getWeekNumber, shiftWeek } from './useCalendar'
import type { Match } from '../types/events'

export function useScheduleFilters() {
  const selectedSeason = ref('2026 赛季')
  const selectedDate = ref(defaultScheduleDate)
  const selectedTeamId = ref<number | null>(null)
  const currentWeek = ref(defaultScheduleWeek)
  const isCalendarOpen = ref(false)
  const isTeamSelectorOpen = ref(false)

  const filteredMatches = computed<Match[]>(() => {
    return allMatches.filter((match) => {
      const seasonMatch = match.season === selectedSeason.value
      const dateMatch = match.date === selectedDate.value
      const teamMatch =
        selectedTeamId.value === null ||
        match.homeTeamId === selectedTeamId.value ||
        match.awayTeamId === selectedTeamId.value
      return seasonMatch && dateMatch && teamMatch
    })
  })

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
    selectedDate.value = date
    currentWeek.value = getWeekNumber(date)
    closePanels()
  }

  function selectTeam(teamId: number | null) {
    selectedTeamId.value = teamId
    closePanels()
  }

  function selectSeason(season: string) {
    selectedSeason.value = season
  }

  function goPrevWeek() {
    selectedDate.value = shiftWeek(selectedDate.value, -1)
    currentWeek.value = getWeekNumber(selectedDate.value)
  }

  function goNextWeek() {
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
    selectedDate,
    selectedTeamId,
    currentWeek,
    isCalendarOpen,
    isTeamSelectorOpen,
    filteredMatches,
    closePanels,
    toggleCalendar,
    toggleTeamSelector,
    selectDate,
    selectTeam,
    selectSeason,
    goPrevWeek,
    goNextWeek,
  }
}
