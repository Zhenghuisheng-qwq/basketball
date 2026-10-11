import type { V4PublicMatchItem } from '@/api/types/v4Public'
import { translate } from '@/i18n'
import { isDisqualificationResult } from '@/utils/eventContext'
import { v4MatchStatusToNumber } from '@/views/events/utils/v4PublicAdapters'
import { formatDateTime, mapMatchStatus, toNumber } from '@/utils/publicDisplay'
import { v4BestOfLabel, v4MatchSeriesStatusLabel } from '@/utils/v4Display'
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
  let result: TeamMatchRecord['result']
  if (finished && String(item.winnerEventTeamId || '') === String(eventTeamId)) {
    result = 'win'
  } else if (finished && item.winnerEventTeamId) {
    result = 'loss'
  }
  const dt = formatDateTime(item.matchTime)
  const seriesStatusLabel = item.seriesStatus ? v4MatchSeriesStatusLabel(item.seriesStatus) : ''
  const bestOf = toNumber(item.bestOf, 1)
  const gameNo = toNumber(item.seriesGameNo, 0)
  const roundNo = toNumber(item.roundNo, 0)
  const isPlayoff = Boolean(item.matchSeriesId) && bestOf > 1
  const seriesLine = isPlayoff
    ? `${v4BestOfLabel(bestOf)}${gameNo > 0 ? ` G${gameNo}` : ''}`
    : ''
  const stageHeadline = isPlayoff
    ? (roundNo > 0 ? translate('publicPages.team.roundN', { round: roundNo }) : translate('publicPages.team.playoffs'))
    : translate('publicPages.team.regular')
  const seriesContext = bestOf > 1
    ? `${v4BestOfLabel(bestOf)}${gameNo > 0 ? ` G${gameNo}` : ''}`
    : ''

  return {
    id: String(item.matchId),
    teamId: String(eventTeamId),
    opponentId,
    opponentName: isHome
      ? item.awayTeamName || translate('publicPages.team.opponent')
      : item.homeTeamName || translate('publicPages.team.opponent'),
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
    competition: [seriesContext, seriesStatusLabel || translate('publicPages.team.roundN', { round: item.roundNo ?? '--' })].filter(Boolean).join(' · '),
    stageHeadline,
    stageMeta: seriesLine || undefined,
    playoffGame: seriesLine || undefined,
    resultType: item.resultType || undefined,
    resultReason: item.resultReason || undefined,
    resultLabel: dq
      ? (result === 'win' ? translate('publicPages.team.dqWin') : translate('publicPages.team.dqLoss'))
      : forfeited
        ? (result === 'win' ? translate('eventsPage.match.forfeitWin') : translate('match.forfeitLoss'))
        : undefined,
    originalTeamScore: originalTeamScore ?? undefined,
    originalOpponentScore: originalOpponentScore ?? undefined,
    matchSeriesId: item.matchSeriesId ? String(item.matchSeriesId) : undefined,
    bestOf: bestOf > 1 ? bestOf : undefined,
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
