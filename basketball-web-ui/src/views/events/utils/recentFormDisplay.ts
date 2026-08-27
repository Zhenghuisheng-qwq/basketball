import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import {
  isScheduleCancelled,
  isScheduleFinished,
  isScheduleLive,
  isScheduleScheduled,
} from './scheduleRowDisplay'

export type RecentFormResult = 'W' | 'L'

export type RecentFormOptions = {
  stageId?: string | null
  limit?: number
}

function teamKey(value?: string | number | null): string {
  return String(value ?? '').trim()
}

function hasOfficialWinner(item: V4PublicScheduleItem): boolean {
  const winner = teamKey(item.winnerEventTeamId)
  return Boolean(winner) && winner !== '0'
}

function scoreWinnerId(item: V4PublicScheduleItem): string {
  const home = Number(item.homeScore)
  const away = Number(item.awayScore)
  if (!Number.isFinite(home) || !Number.isFinite(away) || home === away) return ''
  return home > away ? teamKey(item.homeEventTeamId) : teamKey(item.awayEventTeamId)
}

export function resolveOfficialWinnerId(item: V4PublicScheduleItem): string {
  if (hasOfficialWinner(item)) return teamKey(item.winnerEventTeamId)
  return scoreWinnerId(item)
}

export function isCountableRegularFormMatch(
  item: V4PublicScheduleItem,
  stageId?: string | null,
): boolean {
  if (String(item.unitType || '').toUpperCase() === 'MATCH_SERIES') return false
  if (stageId && teamKey(item.stageId) !== teamKey(stageId)) return false
  if (isScheduleCancelled(item) || isScheduleLive(item) || isScheduleScheduled(item)) return false
  if (!isScheduleFinished(item)) return false
  return Boolean(resolveOfficialWinnerId(item))
}

export function compareFormMatchDesc(a: V4PublicScheduleItem, b: V4PublicScheduleItem): number {
  const date = String(b.scheduleDate || '').localeCompare(String(a.scheduleDate || ''))
  if (date) return date
  const time = String(b.startTime || '').localeCompare(String(a.startTime || ''))
  if (time) return time
  return (Number(b.id) || 0) - (Number(a.id) || 0)
}

export function formForTeam(
  items: V4PublicScheduleItem[],
  teamId: string,
  limit = 5,
): RecentFormResult[] {
  const id = teamKey(teamId)
  if (!id) return []
  const results: RecentFormResult[] = []
  for (const item of items) {
    const home = teamKey(item.homeEventTeamId)
    const away = teamKey(item.awayEventTeamId)
    if (home !== id && away !== id) continue
    const winner = resolveOfficialWinnerId(item)
    if (!winner) continue
    results.push(winner === id ? 'W' : 'L')
    if (results.length >= limit) break
  }
  return results
}

export function buildTeamRecentFormMap(
  items: V4PublicScheduleItem[] | null | undefined,
  options: RecentFormOptions = {},
): Record<string, RecentFormResult[]> {
  const limit = options.limit && options.limit > 0 ? options.limit : 5
  const countable = (items ?? [])
    .filter((item) => isCountableRegularFormMatch(item, options.stageId))
    .sort(compareFormMatchDesc)
  const map: Record<string, RecentFormResult[]> = {}
  const teamIds = new Set<string>()
  for (const item of countable) {
    const home = teamKey(item.homeEventTeamId)
    const away = teamKey(item.awayEventTeamId)
    if (home) teamIds.add(home)
    if (away) teamIds.add(away)
  }
  for (const teamId of teamIds) {
    map[teamId] = formForTeam(countable, teamId, limit)
  }
  return map
}
