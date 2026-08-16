import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue'
import { getV4PublicDivisionSchedule, getV4PublicStanding } from '@/api/v4Public'
import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import { v4StandingToPublic } from '../utils/v4PublicAdapters'
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
import { standingToTeam } from '../utils/mapPublic'
import { teamIdToNumeric } from '../utils/teamLookup'
import { v4ScheduleTeamLabel } from '@/utils/v4Display'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

export function useScheduleFilters() {
  const ctx = inject<SeasonContext>('publicSeasonContext')!

  const selectedDate = ref('')
  const selectedTeamId = ref<number | null>(null)
  const currentWeek = ref(getWeekNumber(formatDateKey(new Date())))
  const isCalendarOpen = ref(false)
  const isTeamSelectorOpen = ref(false)

  const matches = ref<Match[]>([])
  const scheduleItems = ref<V4PublicScheduleItem[]>([])
  const teams = ref<PublicStandingItemVo[]>([])
  const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
  const errorMessage = ref('')
  let reqId = 0

  const hasSeriesItems = computed(() =>
    scheduleItems.value.some((item) => item.unitType === 'MATCH_SERIES'),
  )

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

  function scheduleItemToMatch(item: V4PublicScheduleItem, seasonName: string): Match {
    const date = item.scheduleDate || ''
    const startTime = item.startTime ? String(item.startTime).slice(0, 5) : ''
    return {
      id: Number(item.id) || 0,
      matchId: String(item.id),
      season: seasonName,
      date,
      startTime,
      homeTeamKey: String(item.homeEventTeamId || ''),
      awayTeamKey: String(item.awayEventTeamId || ''),
      homeTeamId: teamIdToNumeric(String(item.homeEventTeamId || '')),
      awayTeamId: teamIdToNumeric(String(item.awayEventTeamId || '')),
      homeTeamName: v4ScheduleTeamLabel(item.homeTeam, item.placeholderSource),
      awayTeamName: v4ScheduleTeamLabel(item.awayTeam, item.placeholderSource),
      homeTeamLogo: '',
      awayTeamLogo: '',
      venue: '--',
      homeScore: item.homeScore ?? undefined,
      awayScore: item.awayScore ?? undefined,
      status: item.resultType === 'EVENT_DISQUALIFICATION' || item.resultType === 'FORFEIT'
        || String(item.status || '').toUpperCase() === 'FORFEITED'
        || String(item.status || '').toUpperCase() === 'FINISHED'
        ? 'finished'
        : 'scheduled',
      week: date && isValidDateKey(date) ? getWeekNumber(date) : 0,
      stageName: item.stageName || item.roundName,
      resultType: item.resultType,
      resultReason: item.resultReason,
      homeDisqualified: Boolean(item.homeDisqualified),
      awayDisqualified: Boolean(item.awayDisqualified),
      homeResultLabel: item.homeResultLabel || (item.homeDisqualified ? '判负' : undefined),
      awayResultLabel: item.awayResultLabel || (item.awayDisqualified ? '判负' : undefined),
    }
  }

  const filteredMatches = computed<Match[]>(() => {
    return matches.value.filter((match) => {
      const dateMatch = !selectedDate.value || !match.date || match.date === selectedDate.value
      const teamMatch =
        selectedTeamId.value === null ||
        teamIdToNumeric(match.homeTeamKey) === selectedTeamId.value ||
        teamIdToNumeric(match.awayTeamKey) === selectedTeamId.value
      return dateMatch && teamMatch
    })
  })

  const filteredScheduleItems = computed(() => {
    return scheduleItems.value.filter((item) => {
      // 日期只做辅助筛选；无日期的已发布项必须保留
      const itemDate = item.scheduleDate || ''
      const dateMatch = !selectedDate.value || !itemDate || itemDate === selectedDate.value
      const teamMatch =
        selectedTeamId.value === null ||
        teamIdToNumeric(String(item.homeEventTeamId || '')) === selectedTeamId.value ||
        teamIdToNumeric(String(item.awayEventTeamId || '')) === selectedTeamId.value
      return dateMatch && teamMatch
    })
  })

  const scheduleByRound = computed(() => {
    const map = new Map<string, V4PublicScheduleItem[]>()
    for (const item of filteredScheduleItems.value) {
      const key = item.roundName || (item.roundNo != null ? `R${item.roundNo}` : 'ROUND')
      const list = map.get(key) ?? []
      list.push(item)
      map.set(key, list)
    }
    return Array.from(map.entries())
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
    // 禁止默认用赛季开赛日过滤，否则 Arena / 无日期项会整页空白
    if (!force) return
    selectedDate.value = ''
    currentWeek.value = getWeekNumber(resolveCalendarBaseDate())
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
      scheduleItems.value = []
      teams.value = []
      state.value = 'empty'
      return
    }
    state.value = 'loading'
    errorMessage.value = ''
    try {
      const [scheduleRes, standingRes] = await Promise.all([
        getV4PublicDivisionSchedule(divisionId),
        getV4PublicStanding(divisionId),
      ])
      if (current !== reqId) return
      const seasonName = ctx.currentSeason.value?.seasonName || ''
      scheduleItems.value = scheduleRes.data ?? []
      matches.value = scheduleItems.value
        .filter((item) => item.unitType === 'MATCH')
        .map((item) => scheduleItemToMatch(item, seasonName))
      teams.value = (standingRes.data ?? []).map((row) =>
        v4StandingToPublic(row, ctx.seasonId.value),
      )

      if (Boolean(options?.resetDate)) {
        selectedDate.value = ''
      }

      state.value = scheduleItems.value.length ? 'success' : 'empty'
    } catch (err) {
      if (current !== reqId) return
      matches.value = []
      scheduleItems.value = []
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
    filteredScheduleItems,
    scheduleByRound,
    hasSeriesItems,
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
