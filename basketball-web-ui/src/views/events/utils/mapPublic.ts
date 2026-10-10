import type { PublicMatchItemVo, PublicStandingItemVo, PublicTeamRankItemVo } from '@/api/types/public'
import { translate } from '@/i18n'
import {
  displayText,
  formatDateTime,
  mapMatchStatus,
  toNumber,
} from '@/utils/publicDisplay'
import type { Match, Team } from '../types/events'
import { getWeekNumber } from '../composables/useCalendar'
import { teamIdToNumeric } from './teamLookup'

export function toEventTeam(input: {
  teamId: string
  teamName?: string | null
  teamLogoUrl?: string | null
  wins?: number | null
  losses?: number | null
  disqualified?: boolean | null
}): Team {
  const name = displayText(input.teamName, translate('eventsPage.teams.unknown'))
  const teamId = String(input.teamId)
  return {
    id: teamIdToNumeric(teamId),
    teamId,
    code: name,
    name,
    shortName: name.slice(0, 3).toUpperCase(),
    logo: input.teamLogoUrl?.trim() || '',
    wins: toNumber(input.wins, 0),
    losses: toNumber(input.losses, 0),
    disqualified: Boolean(input.disqualified),
  }
}

export function standingToTeam(row: PublicStandingItemVo): Team {
  return toEventTeam({
    teamId: row.teamId,
    teamName: row.teamName,
    teamLogoUrl: row.teamLogoUrl,
    wins: row.wins,
    losses: row.losses,
  })
}

export function rankToTeam(row: PublicTeamRankItemVo): Team {
  return toEventTeam({
    teamId: row.teamId,
    teamName: row.teamName,
    teamLogoUrl: row.teamLogoUrl,
  })
}

export function toScheduleMatch(item: PublicMatchItemVo, seasonName = ''): Match {
  const dt = formatDateTime(item.scheduledStartTime)
  const status = mapMatchStatus(item.matchStatus)
  return {
    id: Number(item.matchId) || 0,
    matchId: String(item.matchId),
    season: seasonName,
    date: dt.date === '--' ? '' : dt.date,
    startTime: dt.time,
    homeTeamKey: String(item.homeTeamId || ''),
    awayTeamKey: String(item.awayTeamId || ''),
    homeTeamId: teamIdToNumeric(String(item.homeTeamId || '')),
    awayTeamId: teamIdToNumeric(String(item.awayTeamId || '')),
    homeTeamName: displayText(item.homeTeamName),
    awayTeamName: displayText(item.awayTeamName),
    homeTeamLogo: item.homeTeamLogoUrl?.trim() || '',
    awayTeamLogo: item.awayTeamLogoUrl?.trim() || '',
    homeScore: item.homeScore ?? undefined,
    awayScore: item.awayScore ?? undefined,
    venue: displayText(item.venueName, '--'),
    status: status === 'postponed' ? 'scheduled' : status,
    week: dt.date && dt.date !== '--' ? getWeekNumber(dt.date) : 0,
    matchStatus: item.matchStatus ?? undefined,
    stageName: item.stageName ?? undefined,
    resultType: (item as { resultType?: string }).resultType,
    resultReason: (item as { resultReason?: string }).resultReason,
    homeResultLabel: (item as { homeResultLabel?: string }).homeResultLabel,
    awayResultLabel: (item as { awayResultLabel?: string }).awayResultLabel,
    homeDisqualified: Boolean((item as { homeDisqualified?: boolean }).homeDisqualified),
    awayDisqualified: Boolean((item as { awayDisqualified?: boolean }).awayDisqualified),
  }
}

export function matchHomeTeam(match: Match): Team {
  return toEventTeam({
    teamId: match.homeTeamKey,
    teamName: match.homeTeamName,
    teamLogoUrl: match.homeTeamLogo,
  })
}

export function matchAwayTeam(match: Match): Team {
  return toEventTeam({
    teamId: match.awayTeamKey,
    teamName: match.awayTeamName,
    teamLogoUrl: match.awayTeamLogo,
  })
}
