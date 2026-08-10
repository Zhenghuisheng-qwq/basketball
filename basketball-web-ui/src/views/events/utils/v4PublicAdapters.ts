import type {
  PublicPlayerRankItemVo,
  PublicStandingItemVo,
} from '@/api/types/public'
import type {
  V4PublicMatchItem,
  V4PublicRankItem,
  V4PublicStandingItem,
} from '@/api/types/v4Public'
import { toNumber } from '@/utils/publicDisplay'

export function v4StandingToPublic(row: V4PublicStandingItem, seasonId = ''): PublicStandingItemVo {
  const wins = toNumber(row.winCount, 0)
  const losses = toNumber(row.loseCount, 0)
  const played = wins + losses
  return {
    seasonId,
    stageId: row.stageId,
    groupId: row.groupId,
    teamId: row.eventTeamId,
    teamName: row.teamName,
    teamLogoUrl: row.teamLogoUrl,
    rankNo: row.rankNo,
    matchesPlayed: played,
    wins,
    losses,
    pointsFor: row.pointFor,
    pointsAgainst: row.pointAgainst,
    pointDiff: row.pointDiff,
    standingPoints: row.standingPoints,
    winRate: played > 0 ? wins / played : 0,
    streakType: null,
    streakCount: null,
  }
}

export function v4RankToPublicPlayer(row: V4PublicRankItem): PublicPlayerRankItemVo {
  return {
    // 公开球员详情路由必须用永久 Player ID
    playerId: String(row.sourcePlayerId || ''),
    playerName: row.playerName,
    teamId: row.eventTeamId,
    teamName: row.teamName,
    avatarUrl: '',
    rankNo: row.rankNo,
    statValue: row.rankValue,
  }
}

export function v4MatchStatusToNumber(status?: string | null): number {
  switch ((status || '').toUpperCase()) {
    case 'LIVE':
    case 'IN_PROGRESS':
    case 'PLAYING':
      return 2
    case 'FINISHED':
    case 'COMPLETED':
    case 'END':
      return 3
    case 'POSTPONED':
      return 4
    case 'CANCELLED':
    case 'CANCELED':
      return 5
    default:
      return 1
  }
}

export function v4MatchToLegacyShape(item: V4PublicMatchItem) {
  return {
    matchId: item.matchId,
    scheduledStartTime: item.matchTime,
    homeTeamId: item.homeEventTeamId,
    awayTeamId: item.awayEventTeamId,
    homeTeamName: item.homeTeamName,
    awayTeamName: item.awayTeamName,
    homeTeamLogoUrl: item.homeTeamLogoUrl,
    awayTeamLogoUrl: item.awayTeamLogoUrl,
    homeScore: item.homeScore,
    awayScore: item.awayScore,
    venueName: item.venueText,
    matchStatus: v4MatchStatusToNumber(item.matchStatus),
    stageName: item.seriesStatusName || undefined,
  }
}

export const V4_LEADER_RANK_TYPE: Record<string, string> = {
  AVG_POINTS: 'PLAYER_SCORE',
  AVG_ASSISTS: 'PLAYER_ASSIST',
  AVG_REBOUNDS: 'PLAYER_REBOUND',
}

/** 将 V4 淘汰赛 Match 列表映射为原型 PlayoffSeries 树所需序列 */
export function v4BracketToPlayoffSeries(matches: V4PublicMatchItem[]): import('../types/events').PlayoffSeries[] {
  const sorted = [...matches].sort((a, b) => {
    const ra = toNumber(a.roundNo, 0) - toNumber(b.roundNo, 0)
    if (ra !== 0) return ra
    return String(a.matchNo || a.matchId).localeCompare(String(b.matchNo || b.matchId))
  })

  const total = sorted.length
  if (!total) return []

  // 简单启发式：末场=决赛，倒数 2 场=半决赛，其余=首轮；左右交替
  const result: import('../types/events').PlayoffSeries[] = []
  sorted.forEach((item, index) => {
    const fromEnd = total - 1 - index
    let round: 'quarterfinal' | 'semifinal' | 'final' = 'quarterfinal'
    let side: 'left' | 'right' | 'center' = index % 2 === 0 ? 'left' : 'right'
    if (fromEnd === 0) {
      round = 'final'
      side = 'center'
    } else if (fromEnd <= 2 && total >= 3) {
      round = 'semifinal'
      side = fromEnd === 2 ? 'left' : 'right'
    }

    const homeScore = toNumber(item.seriesHomeWinCount ?? item.homeScore, 0)
    const awayScore = toNumber(item.seriesAwayWinCount ?? item.awayScore, 0)
    const homeId = String(item.homeEventTeamId || '')
    const awayId = String(item.awayEventTeamId || '')
    const winnerTeamId = String(item.winnerEventTeamId || '')

    result.push({
      id: item.matchId,
      round,
      side,
      topTeamId: homeId,
      bottomTeamId: awayId,
      topTeamName: item.homeTeamName || '',
      bottomTeamName: item.awayTeamName || '',
      topTeamLogo: item.homeTeamLogoUrl || '',
      bottomTeamLogo: item.awayTeamLogoUrl || '',
      topScore: homeScore,
      bottomScore: awayScore,
      winnerTeamId,
      topSeed: '-',
      bottomSeed: '-',
    })
  })
  return result
}
