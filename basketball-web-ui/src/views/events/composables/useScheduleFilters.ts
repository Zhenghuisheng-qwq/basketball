import { computed, inject, ref, watch } from 'vue'
import { getV4PublicDivisionSchedule, getV4PublicStanding, getV4PublicTeamList } from '@/api/v4Public'
import type { V4PublicEventTeamItem, V4PublicScheduleItem } from '@/api/types/v4Public'
import { v4StandingToPublic } from '../utils/v4PublicAdapters'
import type { PublicStandingItemVo } from '@/api/types/public'
import { isValidDateKey, todayDateKey } from '@/utils/scheduleDateKey'
import type { usePublicSeasonContext } from './usePublicSeasonContext'
import {
  buildScheduleTeamOptions,
  filterScheduleByTeam,
  isAllTeamsSelected,
  isBlockedTeamName,
  isRealEventTeamId,
  resolveSelectedTeamId,
  type ScheduleTeamOption,
} from '../utils/scheduleTeamFilter'
import {
  countScheduleByDate,
  resolveInitWeekStart,
  scheduleItemDateKey,
  shiftMonthKey,
  shiftSundayWeek,
  sortScheduleItems,
  startOfMonthKey,
  startOfSundayWeek,
  weekNumberOf,
} from '../utils/scheduleCalendar'
import {
  browseScheduleDateGroups,
  DEFAULT_VISIBLE_DATE_GROUPS,
  nextVisibleDateGroupCount,
  resolveInitSelectedDate,
  uniqueSortedScheduleDates,
} from '../utils/scheduleDateBrowse'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

export function useScheduleFilters() {
  const ctx = inject<SeasonContext>('publicSeasonContext')!

  const selectedDate = ref('')
  const selectedTeamId = ref<string | null>(null)
  const calendarWeekStart = ref(startOfSundayWeek(todayDateKey()))
  const calendarMonthAnchor = ref(startOfMonthKey(todayDateKey()))
  const calendarExpanded = ref(false)
  const teamFilterExpanded = ref(false)
  const visibleDateGroupCount = ref(DEFAULT_VISIBLE_DATE_GROUPS)
  const dateTouched = ref(false)

  const scheduleItems = ref<V4PublicScheduleItem[]>([])
  const standingRows = ref<PublicStandingItemVo[]>([])
  const eventTeams = ref<V4PublicEventTeamItem[]>([])
  const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
  const errorMessage = ref('')
  let reqId = 0

  const logoByEventTeamId = computed(() => {
    const map = new Map<string, string>()
    for (const row of eventTeams.value) {
      const id = String(row.eventTeamId || '').trim()
      const logo = String(row.teamLogoUrl || '').trim()
      if (id && logo) map.set(id, logo)
    }
    for (const row of standingRows.value) {
      const id = String(row.teamId || '').trim()
      const logo = String(row.teamLogoUrl || '').trim()
      if (id && logo && !map.has(id)) map.set(id, logo)
    }
    return map
  })

  const teamOptions = computed<ScheduleTeamOption[]>(() => {
    const logos = logoByEventTeamId.value
    const fromEventTeams: ScheduleTeamOption[] = []
    for (const row of eventTeams.value) {
      if (!isRealEventTeamId(row.eventTeamId) || isBlockedTeamName(row.teamName)) continue
      const eventTeamId = String(row.eventTeamId)
      fromEventTeams.push({
        eventTeamId,
        name: row.teamName || row.teamShortName || '未知球队',
        shortName: row.teamShortName,
        code: row.eventTeamNo || row.teamShortName,
        logoUrl: String(row.teamLogoUrl || '').trim() || logos.get(eventTeamId) || '',
      })
    }
    if (fromEventTeams.length) return fromEventTeams
    return buildScheduleTeamOptions(scheduleItems.value).map((option) => ({
      ...option,
      code: option.code || option.shortName,
      logoUrl: option.logoUrl || logos.get(option.eventTeamId) || '',
    }))
  })

  const selectedTeam = computed(() =>
    teamOptions.value.find((item) => item.eventTeamId === selectedTeamId.value) || null,
  )

  const teamFilteredItems = computed(() =>
    filterScheduleByTeam(scheduleItems.value, selectedTeamId.value),
  )

  const filteredScheduleItems = computed(() => {
    const date = selectedDate.value
    if (!isValidDateKey(date)) return teamFilteredItems.value
    return teamFilteredItems.value.filter((item) => scheduleItemDateKey(item) === date)
  })

  const sortedFilteredItems = computed(() => sortScheduleItems(filteredScheduleItems.value))

  const total = computed(() => filteredScheduleItems.value.length)

  const dateBrowse = computed(() =>
    browseScheduleDateGroups(
      sortedFilteredItems.value,
      selectedDate.value,
      visibleDateGroupCount.value,
    ),
  )

  const groupedScheduleByDate = computed(() => dateBrowse.value.visibleGroups)
  const allDateGroupCount = computed(() => dateBrowse.value.allGroups.length)
  const remainingDateGroups = computed(() => dateBrowse.value.remainingGroups)
  const canLoadMoreDateGroups = computed(() => dateBrowse.value.canLoadMore)
  const visibleItemCount = computed(() =>
    dateBrowse.value.visibleGroups.reduce((sum, group) => sum + group.items.length, 0),
  )

  const dateCounts = computed(() => countScheduleByDate(teamFilteredItems.value))

  const firstWeekStart = computed(() => {
    const dates = scheduleItems.value
      .map((item) => scheduleItemDateKey(item))
      .filter((item) => isValidDateKey(item))
      .sort()
    return dates[0] ? startOfSundayWeek(dates[0]) : calendarWeekStart.value
  })

  const weekNumber = computed(() => weekNumberOf(calendarWeekStart.value, firstWeekStart.value))

  function collapsePanels() {
    calendarExpanded.value = false
    teamFilterExpanded.value = false
  }

  function resetVisibleDateGroups() {
    visibleDateGroupCount.value = DEFAULT_VISIBLE_DATE_GROUPS
  }

  function resetFilters() {
    selectedTeamId.value = null
    dateTouched.value = false
    resetVisibleDateGroups()
    collapsePanels()
  }

  function syncCalendarToSelectedDate() {
    const status = ctx.seasonDetail.value?.seasonStatus
    if (isValidDateKey(selectedDate.value)) {
      calendarWeekStart.value = startOfSundayWeek(selectedDate.value)
    } else {
      calendarWeekStart.value = resolveInitWeekStart(scheduleItems.value, status)
    }
    calendarMonthAnchor.value = startOfMonthKey(calendarWeekStart.value)
  }

  function applyInitSelectedDate(force = false) {
    const status = ctx.seasonDetail.value?.seasonStatus
    const nextDate = resolveInitSelectedDate(scheduleItems.value, status)
    const dates = uniqueSortedScheduleDates(scheduleItems.value)
    if (force) {
      selectedDate.value = nextDate
    } else if (selectedDate.value && !dates.includes(selectedDate.value)) {
      selectedDate.value = nextDate
    }
    syncCalendarToSelectedDate()
  }

  function selectDate(date: string) {
    if (!isValidDateKey(date)) return
    dateTouched.value = true
    if (selectedDate.value === date) {
      selectedDate.value = ''
      return
    }
    selectedDate.value = date
    calendarWeekStart.value = startOfSundayWeek(date)
    calendarMonthAnchor.value = startOfMonthKey(date)
    calendarExpanded.value = false
  }

  function clearDate() {
    dateTouched.value = true
    selectedDate.value = ''
  }

  function selectTeam(teamId: string | null) {
    selectedTeamId.value = isAllTeamsSelected(teamId) ? null : String(teamId)
    teamFilterExpanded.value = false
  }

  function clearTeam() {
    selectedTeamId.value = null
  }

  function toggleCalendar() {
    calendarExpanded.value = !calendarExpanded.value
    if (calendarExpanded.value) {
      teamFilterExpanded.value = false
      calendarMonthAnchor.value = startOfMonthKey(selectedDate.value || calendarWeekStart.value)
    }
  }

  function toggleTeamFilter() {
    teamFilterExpanded.value = !teamFilterExpanded.value
    if (teamFilterExpanded.value) {
      calendarExpanded.value = false
    }
  }

  function goPrevWeek() {
    dateTouched.value = true
    calendarWeekStart.value = shiftSundayWeek(calendarWeekStart.value, -1)
    selectedDate.value = ''
    resetVisibleDateGroups()
  }

  function goNextWeek() {
    dateTouched.value = true
    calendarWeekStart.value = shiftSundayWeek(calendarWeekStart.value, 1)
    selectedDate.value = ''
    resetVisibleDateGroups()
  }

  function goPrevMonth() {
    calendarMonthAnchor.value = shiftMonthKey(calendarMonthAnchor.value, -1)
  }

  function goNextMonth() {
    calendarMonthAnchor.value = shiftMonthKey(calendarMonthAnchor.value, 1)
  }

  function loadMoreDateGroups() {
    if (!canLoadMoreDateGroups.value) return
    visibleDateGroupCount.value = nextVisibleDateGroupCount(
      visibleDateGroupCount.value,
      dateBrowse.value.allGroups.length,
    )
  }

  async function loadSchedule(options?: { resetFilters?: boolean }) {
    const current = ++reqId
    const divisionId = ctx.divisionId.value
    if (!divisionId) {
      scheduleItems.value = []
      standingRows.value = []
      eventTeams.value = []
      const contextPending =
        ctx.overviewState.value === 'idle' || ctx.overviewState.value === 'loading'
      state.value = contextPending ? 'loading' : 'empty'
      return
    }
    state.value = 'loading'
    errorMessage.value = ''
    try {
      const [scheduleRes, standingRes, teamRes] = await Promise.all([
        getV4PublicDivisionSchedule(divisionId),
        getV4PublicStanding(divisionId).catch(() => ({ data: [] })),
        getV4PublicTeamList(divisionId).catch(() => ({ data: [] })),
      ])
      if (current !== reqId) return
      scheduleItems.value = scheduleRes.data ?? []
      standingRows.value = (standingRes.data ?? []).map((row) =>
        v4StandingToPublic(row, ctx.seasonId.value),
      )
      eventTeams.value = teamRes.data ?? []
      if (options?.resetFilters) {
        resetFilters()
        applyInitSelectedDate(true)
      } else if (!dateTouched.value) {
        applyInitSelectedDate(true)
      } else {
        applyInitSelectedDate(false)
      }
      state.value = scheduleItems.value.length ? 'success' : 'empty'
    } catch (err) {
      if (current !== reqId) return
      scheduleItems.value = []
      standingRows.value = []
      eventTeams.value = []
      state.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '赛程加载失败'
    }
  }

  watch(
    () => [ctx.divisionId.value, ctx.overviewState.value] as const,
    ([did], prev) => {
      const reset = prev === undefined || did !== prev[0]
      void loadSchedule({ resetFilters: reset })
    },
    { immediate: true },
  )

  watch(
    () => [ctx.divisionId.value, ctx.seasonId.value] as const,
    ([did, sid], prev) => {
      if (prev && (did !== prev[0] || sid !== prev[1])) {
        resetFilters()
      }
    },
  )

  watch(teamOptions, (options) => {
    if (state.value === 'loading' || state.value === 'error') return
    selectedTeamId.value = resolveSelectedTeamId(selectedTeamId.value, options)
  })

  watch(selectedTeamId, () => {
    resetVisibleDateGroups()
  })

  watch(selectedDate, (date) => {
    if (!isValidDateKey(date)) resetVisibleDateGroups()
  })

  return {
    selectedDate,
    selectedTeamId,
    selectedTeam,
    calendarWeekStart,
    weekNumber,
    calendarMonthAnchor,
    calendarExpanded,
    teamFilterExpanded,
    weekAnchorDate: calendarWeekStart,
    currentWeek: computed(() => calendarWeekStart.value),
    teamOptions,
    dateCounts,
    filteredScheduleItems,
    groupedScheduleByDate,
    allDateGroupCount,
    total,
    visibleItemCount,
    remainingDateGroups,
    canLoadMoreDateGroups,
    loadMoreDateGroups,
    state,
    errorMessage,
    selectDate,
    clearDate,
    selectTeam,
    clearTeam,
    toggleCalendar,
    toggleTeamFilter,
    goPrevWeek,
    goNextWeek,
    goPrevMonth,
    goNextMonth,
    reload: () => loadSchedule(),
  }
}
