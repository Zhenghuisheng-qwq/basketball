import type { LocationQueryRaw } from 'vue-router'

export function eventContextQuery(input?: {
  divisionId?: string | null
  seasonId?: string | null
  eventTeamId?: string | null
  eventPlayerId?: string | null
}): Record<string, string> {
  const query: Record<string, string> = {}
  const divisionId = String(input?.divisionId || '').trim()
  const seasonId = String(input?.seasonId || '').trim()
  const eventTeamId = String(input?.eventTeamId || '').trim()
  const eventPlayerId = String(input?.eventPlayerId || '').trim()
  if (divisionId) query.divisionId = divisionId
  if (seasonId) query.seasonId = seasonId
  if (eventTeamId) query.eventTeamId = eventTeamId
  if (eventPlayerId) query.eventPlayerId = eventPlayerId
  return query
}

export function teamDetailLocation(
  eventTeamId: string | number,
  extra?: { divisionId?: string | null; seasonId?: string | null; tab?: string },
) {
  const tab = extra?.tab || 'overview'
  return {
    path: `/tournament/team/${eventTeamId}/${tab}`,
    query: eventContextQuery(extra) as LocationQueryRaw,
  }
}

export function isDisqualificationResult(resultType?: string | null): boolean {
  return String(resultType || '').toUpperCase() === 'EVENT_DISQUALIFICATION'
}
