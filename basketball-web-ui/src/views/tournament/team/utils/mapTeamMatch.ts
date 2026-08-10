import type { V4PublicMatchItem } from '@/api/types/v4Public'
import { v4MatchStatusToNumber } from '@/views/events/utils/v4PublicAdapters'
import { formatDateTime, mapMatchStatus, toNumber } from '@/utils/publicDisplay'
import type { TeamBase, TeamMatchRecord } from '@/types/team'

export function mapV4MatchToTeamRecord(
  item: V4PublicMatchItem,
  eventTeamId: string,
): TeamMatchRecord {
  const isHome = String(item.homeEventTeamId) === String(eventTeamId)
  const opponentId = String(isHome ? item.awayEventTeamId : item.homeEventTeamId)
  const teamScore = isHome ? item.homeScore : item.awayScore
  const opponentScore = isHome ? item.awayScore : item.homeScore
  const status = mapMatchStatus(v4MatchStatusToNumber(item.matchStatus))
  const finished = status === 'finished' || status === 'live'
  let result: TeamMatchRecord['result']
  if (finished && teamScore != null && opponentScore != null) {
    result = toNumber(teamScore) > toNumber(opponentScore) ? 'win' : 'loss'
    if (toNumber(teamScore) === toNumber(opponentScore)) result = undefined
  }
  const dt = formatDateTime(item.matchTime)
  const stageLabel = item.seriesStatusName || ''
  const isPlayoff = /淘汰|季后|playoff/i.test(stageLabel)

  return {
    id: String(item.matchId),
    teamId: String(eventTeamId),
    opponentId,
    opponentName: isHome
      ? item.awayTeamName || '对手'
      : item.homeTeamName || '对手',
    opponentLogoUrl: isHome
      ? item.awayTeamLogoUrl || ''
      : item.homeTeamLogoUrl || '',
    date: dt.date === '--' ? '' : dt.date,
    time: dt.time,
    teamScore: teamScore ?? undefined,
    opponentScore: opponentScore ?? undefined,
    result,
    status: status === 'finished' || status === 'live' ? 'finished' : 'scheduled',
    stage: isPlayoff ? 'playoffs' : 'regular',
    competition: stageLabel || `第${item.roundNo ?? '--'}轮`,
  } as TeamMatchRecord & { opponentName: string; opponentLogoUrl: string }
}

/** @deprecated use mapV4MatchToTeamRecord */
export function mapPublicMatchToTeamRecord(
  item: Parameters<typeof mapV4MatchToTeamRecord>[0],
  teamId: string,
): TeamMatchRecord {
  return mapV4MatchToTeamRecord(item, teamId)
}

export function opponentAsTeam(match: TeamMatchRecord & {
  opponentName?: string
  opponentLogoUrl?: string
}): TeamBase {
  const name = match.opponentName || match.opponentId
  return {
    id: match.opponentId,
    code: name,
    name,
    shortName: name.slice(0, 3).toUpperCase(),
    logoUrl: match.opponentLogoUrl || '',
    largeLogoUrl: match.opponentLogoUrl || '',
    captainName: '--',
    platform: '--',
    currentStage: 'regular',
    rosterCount: 0,
    verified: false,
    seasonName: match.competition,
    seasonYear: new Date().getFullYear(),
    description: '',
    wins: 0,
    losses: 0,
    ranking: 0,
    themeColor: '#333',
    secondaryColor: '#111',
  }
}
