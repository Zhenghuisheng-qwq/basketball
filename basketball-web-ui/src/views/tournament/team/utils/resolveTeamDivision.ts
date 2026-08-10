import {
  getV4PublicSeasonList,
  getV4PublicStanding,
  getV4PublicTeamList,
} from '@/api/v4Public'

async function teamInDivision(divisionId: string, eventTeamId: string): Promise<boolean> {
  try {
    const teamRes = await getV4PublicTeamList(divisionId)
    if (teamRes.data?.some((row) => String(row.eventTeamId) === String(eventTeamId))) {
      return true
    }
  } catch {
    // fall through to standing
  }
  try {
    const standingRes = await getV4PublicStanding(divisionId)
    return Boolean(
      standingRes.data?.some((row) => String(row.eventTeamId) === String(eventTeamId)),
    )
  } catch {
    return false
  }
}

export async function resolveEventTeamDivisionId(
  eventTeamId: string,
  options?: {
    divisionId?: string | null
    seasonId?: string | null
  },
): Promise<string> {
  const id = String(eventTeamId || '').trim()
  if (!id) return ''

  const queryDivisionId = String(options?.divisionId || '').trim()
  if (queryDivisionId && /^\d+$/.test(queryDivisionId)) {
    if (await teamInDivision(queryDivisionId, id)) {
      return queryDivisionId
    }
  }

  const seasonsRes = await getV4PublicSeasonList()
  const seasons = seasonsRes.data ?? []
  const seasonId = String(options?.seasonId || '').trim()
  const scopedSeasons = seasonId
    ? seasons.filter((season) => String(season.seasonId) === seasonId)
    : seasons

  for (const season of scopedSeasons) {
    for (const division of season.divisions ?? []) {
      if (await teamInDivision(division.divisionId, id)) {
        return division.divisionId
      }
    }
  }

  return ''
}
