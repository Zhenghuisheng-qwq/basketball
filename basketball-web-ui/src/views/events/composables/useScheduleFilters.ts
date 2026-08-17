import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue'
import { getV4PublicDivisionSchedule, getV4PublicStanding, getV4PublicTeamList } from '@/api/v4Public'
import type { V4PublicEventTeamItem, V4PublicScheduleItem } from '@/api/types/v4Public'
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
import { teamIdToNumeric } from '../utils/teamLookup'
import { v4ScheduleTeamLabel } from '@/utils/v4Display'
import {
  buildScheduleTeamOptions,
  DEFAULT_SCHEDULE_PAGE_SIZE,
  filterScheduleByDate,
  filterScheduleByTeam,
  isAllTeamsSelected,
  paginateScheduleItems,
  resolveSelectedTeamId,
  schedulePageCount,
  type ScheduleTeamOption,
} from '../utils/scheduleTeamFilter'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

export function useScheduleFilters() {
  const ctx = inject<SeasonContext>('publicSeasonContext')!

  const selectedDate = ref('')
  const selectedTeamId = ref<string | null>(null)
  const currentWeek = ref(getWeekNumber(formatDateKey(new Date())))
  const isCalendarOpen = ref(false)
  const teamPanelExpanded = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(DEFAULT_SCHEDULE_PAGE_SIZE)

  const matches = ref<Match[]>([])
  const scheduleItems = ref<V4PublicScheduleItem[]>([])
  const standingRows = ref<PublicStandingItemVo[]>([])
  const eventTeams = ref<V4PublicEventTeamItem[]>([])
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

  function resolveTeamLogo(eventTeamId?: string | number | null, logos?: Map<string, string>): string {
    const id = String(eventTeamId || '').trim()
    if (!id) return ''
    return (logos ?? logoByEventTeamId.value).get(id) || ''
  }

  function scheduleItemToMatch(
    item: V4PublicScheduleItem,
    seasonName: string,
    logos: Map<string, string>,
  ): Match {
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
      homeTeamLogo: String(item.homeTeamLogoUrl || '').trim() || resolveTeamLogo(item.homeEventTeamId, logos),
      awayTeamLogo: String(item.awayTeamLogoUrl || '').trim() || resolveTeamLogo(item.awayEventTeamId, logos),
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

  const matchFilterItems = computed(() =>
    matches.value.map((match) => ({
      ...match,
      homeEventTeamId: match.homeTeamKey,
      awayEventTeamId: match.awayTeamKey,
      scheduleDate: match.date,
    })),
  )

  const dateMatches = computed(() => filterScheduleByDate(matchFilterItems.value, selectedDate.value))
  const contextMatches = computed(() => dateMatches.value)
  const dateScheduleItems = computed(() => filterScheduleByDate(scheduleItems.value, selectedDate.value))
  const contextScheduleItems = computed(() => dateScheduleItems.value)

  const teamOptions = computed<ScheduleTeamOption[]>(() => {
    const source = hasSeriesItems.value ? contextScheduleItems.value : contextMatches.value
    return buildScheduleTeamOptions(source).map((option) => ({
      ...option,
      logoUrl: option.logoUrl || logoByEventTeamId.value.get(option.eventTeamId) || '',
    }))
  })

  const visibleMatches = computed(() => filterScheduleByTeam(contextMatches.value, selectedTeamId.value))
  const visibleScheduleItems = computed(() =>
    filterScheduleByTeam(contextScheduleItems.value, selectedTeamId.value),
  )
  const pagedMatches = computed(() =>
    paginateScheduleItems(visibleMatches.value, currentPage.value, pageSize.value),
  )
  const pagedScheduleItems = computed(() =>
    paginateScheduleItems(visibleScheduleItems.value, currentPage.value, pageSize.value),
  )

  const total = computed(() =>
    hasSeriesItems.value ? visibleScheduleItems.value.length : visibleMatches.value.length,
  )
  const pageCount = computed(() => schedulePageCount(total.value, pageSize.value))
  const showPagination = computed(() => total.value > 0 && pageCount.value > 1)

  const filteredMatches = visibleMatches
  const filteredScheduleItems = visibleScheduleItems

  const scheduleByRound = computed(() => {
    const map = new Map<string, V4PublicScheduleItem[]>()
    for (const item of pagedScheduleItems.value) {
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
    if (!force) return
    selectedDate.value = ''
    currentWeek.value = getWeekNumber(resolveCalendarBaseDate())
  }

  function closePanels() {
    isCalendarOpen.value = false
    teamPanelExpanded.value = false
  }

  function toggleCalendar() {
    isCalendarOpen.value = !isCalendarOpen.value
    if (isCalendarOpen.value) teamPanelExpanded.value = false
  }

  function toggleTeamSelector() {
    teamPanelExpanded.value = !teamPanelExpanded.value
    if (teamPanelExpanded.value) isCalendarOpen.value = false
  }

  function selectDate(date: string) {
    if (!isValidDateKey(date)) return
    selectedDate.value = date
    currentWeek.value = getWeekNumber(date)
    closePanels()
  }

  function selectTeam(teamId: string | null) {
    selectedTeamId.value = isAllTeamsSelected(teamId) ? null : String(teamId)
    closePanels()
  }

  function clearTeam() {
    selectedTeamId.value = null
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
      const seasonName = ctx.currentSeason.value?.seasonName || ''
      scheduleItems.value = scheduleRes.data ?? []
      standingRows.value = (standingRes.data ?? []).map((row) =>
        v4StandingToPublic(row, ctx.seasonId.value),
      )
      eventTeams.value = teamRes.data ?? []
      matches.value = scheduleItems.value
        .filter((item) => item.unitType === 'MATCH')
        .map((item) => scheduleItemToMatch(item, seasonName, logoByEventTeamId.value))

      if (Boolean(options?.resetDate)) {
        selectedDate.value = ''
      }

      state.value = scheduleItems.value.length ? 'success' : 'empty'
    } catch (err) {
      if (current !== reqId) return
      matches.value = []
      scheduleItems.value = []
      standingRows.value = []
      eventTeams.value = []
      state.value = 'error'
      errorMessage.value = err instanceof Error ? err.message : '加载赛程失败'
      applyCalendarBaseDate(true)
    }
  }

  watch(
    () => [ctx.divisionId.value, ctx.overviewState.value] as const,
    ([did], prev) => {
      const prevDid = prev?.[0]
      const resetDate = prev === undefined || did !== prevDid
      void loadSchedule({ resetDate })
    },
    { immediate: true },
  )

  watch(
    () => [ctx.divisionId.value, ctx.seasonId.value] as const,
    ([did, sid], prev) => {
      if (prev && (did !== prev[0] || sid !== prev[1])) {
        selectedTeamId.value = null
        currentPage.value = 1
      }
    },
  )

  watch(teamOptions, (options) => {
    if (state.value === 'loading' || state.value === 'error') return
    selectedTeamId.value = resolveSelectedTeamId(selectedTeamId.value, options)
  })

  watch(
    () => [selectedDate.value, selectedTeamId.value] as const,
    () => {
      currentPage.value = 1
    },
  )

  watch(
    [pageCount, currentPage],
    ([count, page]) => {
      const next = Math.max(1, Math.min(page, count))
      if (next !== page) currentPage.value = next
    },
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
    isTeamSelectorOpen: teamPanelExpanded,
    teamPanelExpanded,
    currentPage,
    pageSize,
    total,
    pageCount,
    showPagination,
    dateMatches,
    contextMatches,
    visibleMatches,
    pagedMatches,
    dateScheduleItems,
    visibleScheduleItems,
    pagedScheduleItems,
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
    clearTeam,
    selectSeason,
    goPrevWeek,
    goNextWeek,
    reload: () => loadSchedule(),
  }
}
