import type { PublicMatchItemVo } from '@/api/types/public'
import { formatDateTime, mapMatchStatus, toNumber } from '@/utils/publicDisplay'
import type { TeamBase, TeamMatchRecord } from '@/types/team'

export function mapPublicMatchToTeamRecord(
  item: PublicMatchItemVo,
  teamId: string,
): TeamMatchRecord {
  const isHome = String(item.homeTeamId) === String(teamId)
  const opponentId = String(isHome ? item.awayTeamId : item.homeTeamId)
  const teamScore = isHome ? item.homeScore : item.awayScore
  const opponentScore = isHome ? item.awayScore : item.homeScore
  const status = mapMatchStatus(item.matchStatus)
  const finished = status === 'finished' || status === 'live'
  let result: TeamMatchRecord['result']
  if (finished && teamScore != null && opponentScore != null) {
    result = toNumber(teamScore) > toNumber(opponentScore) ? 'win' : 'loss'
    if (toNumber(teamScore) === toNumber(opponentScore)) result = undefined
  }
  const dt = formatDateTime(item.scheduledStartTime)
  return {
    id: String(item.matchId),
    teamId: String(teamId),
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
    stage: 'regular',
    competition: item.stageName || '公开赛事',
  } as TeamMatchRecord & { opponentName: string; opponentLogoUrl: string }
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
