import type { V4PublicMatchItem } from '@/api/types/v4Public'
import { isDisqualificationResult } from '@/utils/eventContext'
import { v4MatchStatusToNumber } from '@/views/events/utils/v4PublicAdapters'
import { formatDateTime, mapMatchStatus, toNumber } from '@/utils/publicDisplay'
import { v4BestOfLabel } from '@/utils/v4Display'
import type { TeamBase, TeamMatchRecord } from '@/types/team'

export function mapV4MatchToTeamRecord(
  item: V4PublicMatchItem,
  eventTeamId: string,
): TeamMatchRecord {
  const isHome = String(item.homeEventTeamId) === String(eventTeamId)
  const opponentId = String(isHome ? item.awayEventTeamId : item.homeEventTeamId)
  const officialTeamScore = isHome ? item.homeScore : item.awayScore
  const officialOpponentScore = isHome ? item.awayScore : item.homeScore
  const originalTeamScore = isHome ? item.originalHomeScore : item.originalAwayScore
  const originalOpponentScore = isHome ? item.originalAwayScore : item.originalHomeScore
  const status = mapMatchStatus(v4MatchStatusToNumber(item.matchStatus))
  const forfeited = String(item.matchStatus || '').toUpperCase() === 'FORFEITED'
  const finished = status === 'finished' || status === 'live' || forfeited
  const dq = isDisqualificationResult(item.resultType)
  const sideLabel = isHome ? item.homeResultLabel : item.awayResultLabel
  let result: TeamMatchRecord['result']
  if (finished && String(item.winnerEventTeamId || '') === String(eventTeamId)) {
    result = 'win'
  } else if (finished && item.winnerEventTeamId) {
    result = 'loss'
  }
  const dt = formatDateTime(item.matchTime)
  const stageLabel = item.seriesStatusName || ''
  const isPlayoff = /淘汰|季后|playoff/i.test(stageLabel)
  const bestOf = toNumber(item.bestOf, 1)
  const gameNo = toNumber(item.seriesGameNo, 0)
  const seriesContext = bestOf > 1
    ? `${v4BestOfLabel(bestOf)}${gameNo > 0 ? ` G${gameNo}` : ''}`
    : ''

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
    teamScore: officialTeamScore ?? undefined,
    opponentScore: officialOpponentScore ?? undefined,
    result,
    status: finished ? 'finished' : 'scheduled',
    stage: isPlayoff ? 'playoffs' : 'regular',
    competition: [seriesContext, stageLabel || `第${item.roundNo ?? '--'}轮`].filter(Boolean).join(' · '),
    resultType: item.resultType || undefined,
    resultReason: item.resultReason || undefined,
    resultLabel: dq
      ? (sideLabel || (result === 'win' ? '取消资格判胜' : '取消资格判负'))
      : sideLabel || undefined,
    originalTeamScore: originalTeamScore ?? undefined,
    originalOpponentScore: originalOpponentScore ?? undefined,
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
