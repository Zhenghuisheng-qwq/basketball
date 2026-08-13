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

export type BracketStageBucket = {
  stageId: string
  stageOrder?: number
  stageName?: string
  matches: V4PublicMatchItem[]
}

/** 按 MatchSeries 去重：BO3/BO5/BO7 多场 Match 只生成一张 SeriesCard */
function dedupeMatchesToSeries(matches: V4PublicMatchItem[]): V4PublicMatchItem[] {
  const bySeries = new Map<string, V4PublicMatchItem>()
  const sorted = [...matches].sort((a, b) => {
    const ga = toNumber(a.seriesGameNo, 0) - toNumber(b.seriesGameNo, 0)
    if (ga !== 0) return ga
    return String(a.matchId).localeCompare(String(b.matchId))
  })
  for (const item of sorted) {
    const key = String(item.matchSeriesId || '').trim() || `match:${item.matchId}`
    const prev = bySeries.get(key)
    if (!prev) {
      bySeries.set(key, item)
      continue
    }
    // 优先保留已完结 / 有胜者的代表场次
    const prevDone = String(prev.seriesStatus || '').toUpperCase() === 'COMPLETED' || Boolean(prev.winnerEventTeamId)
    const nextDone = String(item.seriesStatus || '').toUpperCase() === 'COMPLETED' || Boolean(item.winnerEventTeamId)
    if (!prevDone && nextDone) bySeries.set(key, item)
  }
  return [...bySeries.values()]
}

/**
 * 将多 Stage 淘汰赛 Match 映射为历史 PlayoffSeries。
 * round 按 stageOrder：最后一阶段=决赛，倒数第二=半决赛，其余=四分之一决赛。
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

  const elimBuckets = buckets.filter((b) => (b.matches?.length || 0) > 0)
  if (!elimBuckets.length) return []

  const result: import('../types/events').PlayoffSeries[] = []
  elimBuckets.forEach((bucket, bucketIndex) => {
    const seriesList = dedupeMatchesToSeries(bucket.matches).sort((a, b) => {
      const ra = toNumber(a.roundNo, 0) - toNumber(b.roundNo, 0)
      if (ra !== 0) return ra
      return String(a.matchNo || a.matchSeriesId || a.matchId).localeCompare(
        String(b.matchNo || b.matchSeriesId || b.matchId),
      )
    })
    const fromEnd = elimBuckets.length - 1 - bucketIndex
    let round: 'quarterfinal' | 'semifinal' | 'final' = 'quarterfinal'
    if (fromEnd === 0 || (seriesList.length === 1 && elimBuckets.length === 1)) {
      round = 'final'
    } else if (fromEnd === 1 || (elimBuckets.length === 1 && seriesList.length <= 2)) {
      round = 'semifinal'
    }

    seriesList.forEach((item, index) => {
      let side: 'left' | 'right' | 'center' = index % 2 === 0 ? 'left' : 'right'
      if (round === 'final') side = 'center'

      const homeId = publicTeamId(item.homeEventTeamId)
      const awayId = publicTeamId(item.awayEventTeamId)
      // Series 局分优先；无系列分时才回落单场比分
      const topScore = item.seriesHomeWinCount != null
        ? toNumber(item.seriesHomeWinCount, 0)
        : toNumber(item.homeScore, 0)
      const bottomScore = item.seriesAwayWinCount != null
        ? toNumber(item.seriesAwayWinCount, 0)
        : toNumber(item.awayScore, 0)
      const winnerTeamId = publicTeamId(item.winnerEventTeamId)

      result.push({
        id: String(item.matchSeriesId || item.matchId),
        matchSeriesId: item.matchSeriesId,
        matchId: item.matchId,
        bestOf: item.bestOf,
        round,
        side,
        topTeamId: homeId,
        bottomTeamId: awayId,
        topTeamName: placeholderName(item.homeTeamName, item.homeSourceDisplay, item.homeSourceType, homeId),
        bottomTeamName: placeholderName(item.awayTeamName, item.awaySourceDisplay, item.awaySourceType, awayId),
        topTeamLogo: item.homeTeamLogoUrl || '',
        bottomTeamLogo: item.awayTeamLogoUrl || '',
        topScore,
        bottomScore,
        winnerTeamId,
        topSeed: '-',
        bottomSeed: '-',
      })
    })
  })
  return result
}

function publicTeamId(id?: string | number | null): string {
  if (id == null || id === '') return ''
  const numeric = Number(id)
  if (!Number.isFinite(numeric) || numeric <= 0) return ''
  return String(id)
}

function placeholderName(
  teamName?: string | null,
  sourceDisplay?: string | null,
  sourceType?: string | null,
  teamId?: string,
): string {
  if (teamName && teamName.trim()) {
    return sourceDisplay && !teamName.includes(sourceDisplay)
      ? `${sourceDisplay} · ${teamName}`
      : teamName
  }
  if (sourceType === 'BYE') return 'BYE'
  if (sourceDisplay && sourceDisplay.trim()) return sourceDisplay
  if (teamId) return ''
  return '待定'
}
