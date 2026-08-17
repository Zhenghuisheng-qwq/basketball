import { matchPassesDateFilter } from '@/utils/scheduleDateKey'

export type ScheduleTeamOption = {
  eventTeamId: string
  name: string
  shortName?: string
  logoUrl?: string
}

export type ScheduleTeamSide = {
  eventTeamId?: string | null
  name?: string | null
  shortName?: string | null
  logoUrl?: string | null
}

export type ScheduleTeamMatchLike = {
  homeEventTeamId?: string | null
  awayEventTeamId?: string | null
  homeTeamKey?: string | null
  awayTeamKey?: string | null
  homeTeam?: string | null
  awayTeam?: string | null
  homeTeamName?: string | null
  awayTeamName?: string | null
  homeTeamShortName?: string | null
  awayTeamShortName?: string | null
  homeTeamLogo?: string | null
  awayTeamLogo?: string | null
  scheduleDate?: string | null
  date?: string | null
}

export const DEFAULT_SCHEDULE_PAGE_SIZE = 10

const TBD = '\u5f85\u5b9a'
const BYE_CN = '\u8f6e\u7a7a'
const UNKNOWN_TEAM = '\u672a\u77e5\u7403\u961f'

const BLOCKED_TEAM_IDS = new Set(['TBD', 'BYE', 'NULL', 'UNDEFINED', 'ALL', 'NONE'])
const BLOCKED_TEAM_NAMES = new Set(['TBD', 'BYE', TBD, BYE_CN, '--', 'ALL'])

export function normalizeEventTeamId(value: unknown): string {
  if (value == null) return ''
  return String(value).trim()
}

export function isAllTeamsSelected(selectedTeamId: string | null | undefined): boolean {
  return selectedTeamId == null || selectedTeamId === ''
}

export function isRealEventTeamId(value: unknown): boolean {
  const id = normalizeEventTeamId(value)
  if (!id) return false
  return !BLOCKED_TEAM_IDS.has(id.toUpperCase())
}

export function isBlockedTeamName(name?: string | null): boolean {
  const text = String(name || '').trim()
  if (!text) return false
  return BLOCKED_TEAM_NAMES.has(text) || BLOCKED_TEAM_NAMES.has(text.toUpperCase())
}

export function getHomeEventTeamId(match: ScheduleTeamMatchLike): string {
  return normalizeEventTeamId(match.homeEventTeamId ?? match.homeTeamKey)
}

export function getAwayEventTeamId(match: ScheduleTeamMatchLike): string {
  return normalizeEventTeamId(match.awayEventTeamId ?? match.awayTeamKey)
}

function sideFromMatch(match: ScheduleTeamMatchLike, side: 'home' | 'away'): ScheduleTeamSide {
  if (side === 'home') {
    return {
      eventTeamId: match.homeEventTeamId ?? match.homeTeamKey,
      name: match.homeTeam ?? match.homeTeamName,
      shortName: match.homeTeamShortName,
      logoUrl: match.homeTeamLogo,
    }
  }
  return {
    eventTeamId: match.awayEventTeamId ?? match.awayTeamKey,
    name: match.awayTeam ?? match.awayTeamName,
    shortName: match.awayTeamShortName,
    logoUrl: match.awayTeamLogo,
  }
}

export function extractScheduleTeamSides(match: ScheduleTeamMatchLike): ScheduleTeamSide[] {
  return [sideFromMatch(match, 'home'), sideFromMatch(match, 'away')]
}

export function toScheduleTeamOption(side: ScheduleTeamSide): ScheduleTeamOption | null {
  if (!isRealEventTeamId(side.eventTeamId)) return null
  if (isBlockedTeamName(side.name) || isBlockedTeamName(side.shortName)) return null
  const name = String(side.name || '').trim()
  const shortName = String(side.shortName || '').trim()
  const logoUrl = String(side.logoUrl || '').trim()
  return {
    eventTeamId: normalizeEventTeamId(side.eventTeamId),
    name: name || shortName || UNKNOWN_TEAM,
    shortName: shortName || undefined,
    logoUrl: logoUrl || undefined,
  }
}

function mergeTeamOption(current: ScheduleTeamOption, incoming: ScheduleTeamOption): ScheduleTeamOption {
  return {
    eventTeamId: current.eventTeamId,
    name: current.name && current.name !== UNKNOWN_TEAM ? current.name : incoming.name,
    shortName: current.shortName || incoming.shortName,
    logoUrl: current.logoUrl || incoming.logoUrl,
  }
}

export function buildScheduleTeamOptions(matches: ScheduleTeamMatchLike[]): ScheduleTeamOption[] {
  const map = new Map<string, ScheduleTeamOption>()
  for (const match of matches) {
    for (const side of extractScheduleTeamSides(match)) {
      const option = toScheduleTeamOption(side)
      if (!option) continue
      const existing = map.get(option.eventTeamId)
      map.set(option.eventTeamId, existing ? mergeTeamOption(existing, option) : option)
    }
  }
  return Array.from(map.values())
}

export function matchInvolvesTeam(
  match: ScheduleTeamMatchLike,
  selectedTeamId: string | null | undefined,
): boolean {
  if (isAllTeamsSelected(selectedTeamId)) return true
  const id = String(selectedTeamId)
  return getHomeEventTeamId(match) === id || getAwayEventTeamId(match) === id
}

export function filterMatchesByTeam<T extends ScheduleTeamMatchLike>(
  matches: T[],
  selectedTeamId: string | null | undefined,
): T[] {
  return matches.filter((match) => matchInvolvesTeam(match, selectedTeamId))
}

export const filterScheduleByTeam = filterMatchesByTeam

export function paginateScheduleItems<T>(items: T[], currentPage = 1, pageSize = DEFAULT_SCHEDULE_PAGE_SIZE): T[] {
  const size = Math.max(1, pageSize)
  const page = Math.min(Math.max(1, currentPage), schedulePageCount(items.length, size))
  const start = (page - 1) * size
  return items.slice(start, start + size)
}

export function schedulePageCount(total: number, pageSize = DEFAULT_SCHEDULE_PAGE_SIZE): number {
  const size = Math.max(1, pageSize)
  return Math.max(1, Math.ceil(Math.max(0, total) / size) || 1)
}

export function filterScheduleByDate<T extends ScheduleTeamMatchLike>(
  items: T[],
  selectedDate: string | null | undefined,
): T[] {
  return items.filter((item) => matchPassesDateFilter(item.scheduleDate ?? item.date, selectedDate))
}

export function resolveSelectedTeamId(
  current: string | null | undefined,
  options: ScheduleTeamOption[],
): string | null {
  if (isAllTeamsSelected(current)) return null
  const id = String(current)
  return options.some((option) => option.eventTeamId === id) ? id : null
}

export function enrichTeamOptions(
  options: ScheduleTeamOption[],
  extras: Map<string, { name?: string; shortName?: string; logoUrl?: string }>,
): ScheduleTeamOption[] {
  return options.map((option) => {
    const extra = extras.get(option.eventTeamId)
    if (!extra) return option
    return mergeTeamOption(option, {
      eventTeamId: option.eventTeamId,
      name: extra.name || option.name,
      shortName: extra.shortName,
      logoUrl: extra.logoUrl,
    })
  })
}

export function teamOptionLabel(option?: ScheduleTeamOption | null): string {
  if (!option) return ''
  return option.shortName || option.name
}

export function applyScheduleFilterPipeline<T extends ScheduleTeamMatchLike>(input: {
  allPublishedMatches: T[]
  selectedDate?: string | null
  selectedTeamId?: string | null
  currentPage?: number
  pageSize?: number
}) {
  const dateMatches = filterScheduleByDate(input.allPublishedMatches, input.selectedDate)
  const contextMatches = dateMatches
  const teamOptions = buildScheduleTeamOptions(contextMatches)
  const selectedTeamId = resolveSelectedTeamId(input.selectedTeamId, teamOptions)
  const visibleMatches = filterMatchesByTeam(contextMatches, selectedTeamId)
  const pageSize = Math.max(1, input.pageSize ?? DEFAULT_SCHEDULE_PAGE_SIZE)
  const total = visibleMatches.length
  const pageCount = schedulePageCount(total, pageSize)
  const currentPage = Math.min(Math.max(1, input.currentPage ?? 1), pageCount)
  return {
    dateMatches,
    contextMatches,
    teamOptions,
    selectedTeamId,
    visibleMatches,
    total,
    pageCount,
    currentPage,
    pageSize,
    pagedMatches: paginateScheduleItems(visibleMatches, currentPage, pageSize),
  }
}