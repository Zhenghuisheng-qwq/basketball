import type { V4PublicMatchItem } from '@/api/types/v4Public'
import { translate } from '@/i18n'
import { isDisqualificationResult } from '@/utils/eventContext'
import { v4MatchStatusToNumber } from '@/views/events/utils/v4PublicAdapters'
import { businessClock, businessKickoffLabel } from '@/utils/businessDateTime'
import { formatDateTime, mapMatchStatus, toNumber } from '@/utils/publicDisplay'
import { v4BestOfLabel, v4MatchSeriesStatusLabel } from '@/utils/v4Display'
import { isKnockoutStage } from './publicStageLabel'
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
  const seriesStatusLabel = item.seriesStatus ? v4MatchSeriesStatusLabel(item.seriesStatus) : ''
  const bestOf = toNumber(item.bestOf, 1)
  const independentTime = Boolean(String(item.matchTime || '').trim())
  const dt = independentTime ? formatDateTime(item.matchTime) : { date: '', time: '' }
  const seriesClock = businessKickoffLabel(item.seriesScheduleDate, item.seriesStartTime) || businessClock(item.seriesStartTime)
  const followsSeries = !independentTime && Boolean(item.matchSeriesId) && bestOf > 1
  const gameNo = toNumber(item.seriesGameNo, 0)
  const roundNo = toNumber(item.roundNo, 0)
  const knockout = isKnockoutStage(item.stageType, item.competitionFormat) || (Boolean(item.matchSeriesId) && bestOf > 1)
  const stageName = item.stageName?.trim()
  const seriesLine = knockout && bestOf > 1
    ? `${v4BestOfLabel(bestOf)}${gameNo > 0 ? ` G${gameNo}` : ''}`
    : ''
  const stageHeadline = stageName
    || (knockout
      ? (roundNo > 0 ? translate('publicPages.team.roundN', { round: roundNo }) : translate('publicPages.team.knockout'))
      : translate('publicPages.team.notStarted'))
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
    date: independentTime && dt.date !== '--' ? dt.date : '',
    time: independentTime ? dt.time : '',
    followsSeries,
    seriesStartTime: followsSeries ? seriesClock : undefined,
    teamScore: officialTeamScore ?? undefined,
    opponentScore: officialOpponentScore ?? undefined,
    result,
    status: finished ? 'finished' : 'scheduled',
    stage: knockout ? 'playoffs' : 'regular',
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

export function formatTeamMatchWhen(match: TeamMatchRecord): string {
  if (match.date) {
    const [, month, day] = match.date.split('-')
    return `${month}-${day} ${match.time || ''}`.trim()
  }
  if (match.followsSeries) {
    return translate('publicPages.team.followsSeries')
  }
  return '--'
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
