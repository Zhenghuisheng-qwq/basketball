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
import { groupMatchItemsToRounds } from './playoffBracketDisplay'

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
    winRate: row.winRate != null ? toNumber(row.winRate, 0) : (played > 0 ? (wins / played) * 100 : 0),
    streakType: null,
    streakCount: null,
    disqualified: Boolean(row.disqualified),
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

export type BracketStageBucket = {
  stageId: string
  stageOrder?: number
  stageName?: string
  matches: V4PublicMatchItem[]
}

/**
 * 将淘汰赛 Match 映射为 PlayoffSeries。
 * 按 Match.roundNo 分组，禁止把单一淘汰 Stage 整表标成 final。
 */
export function v4BracketToPlayoffSeries(
  matchesOrBuckets: V4PublicMatchItem[] | BracketStageBucket[],
): import('../types/events').PlayoffSeries[] {
  const buckets: BracketStageBucket[] = Array.isArray(matchesOrBuckets) && matchesOrBuckets.length
    && typeof (matchesOrBuckets[0] as BracketStageBucket).matches !== 'undefined'
    ? [...(matchesOrBuckets as BracketStageBucket[])].sort(
        (a, b) => toNumber(a.stageOrder, 0) - toNumber(b.stageOrder, 0),
      )
    : [{ stageId: 'single', matches: matchesOrBuckets as V4PublicMatchItem[] }]

  const matches = buckets.flatMap((bucket) => bucket.matches ?? [])
  return groupMatchItemsToRounds(matches).flatMap((col) => col.series)
}
