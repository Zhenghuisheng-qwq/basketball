import type { V4MatchSubmissionPlayerStat } from '@/api/types/v4MatchSubmission'
import type { V4StatsScreenshotPlayer, V4StatsScreenshotResult } from '@/api/types/v4MatchSubmission'
import type { V4MatchSubmissionPeriodScore } from '@/api/types/v4SeriesSubmission'
import { requiresPeriodScores } from '@/utils/fullMatchSubmissionRules'

function emptyPlayer(): V4MatchSubmissionPlayerStat {
  return {
    eventPlayerId: '',
    points: 0,
    rebound: 0,
    assist: 0,
    steal: 0,
    block: 0,
    foul: 0,
    turnover: 0,
    fieldGoalMade: 0,
    fieldGoalAttempt: 0,
    threePointMade: 0,
    threePointAttempt: 0,
    freeThrowMade: 0,
    freeThrowAttempt: 0,
  }
}

export function rowsFromRecognition(players?: V4StatsScreenshotPlayer[]): V4MatchSubmissionPlayerStat[] {
  const rows = (players || []).map((player) => ({
    ...emptyPlayer(),
    eventPlayerId: player.eventPlayerId == null || player.eventPlayerId === '' ? '' : String(player.eventPlayerId),
    recognizedName: player.matched ? '' : (player.recognizedName || '').trim(),
    points: player.points ?? 0,
    rebound: player.rebound ?? 0,
    assist: player.assist ?? 0,
    steal: player.steal ?? 0,
    block: player.block ?? 0,
    turnover: player.turnover ?? 0,
    foul: player.foul ?? 0,
    fieldGoalMade: player.fieldGoalMade ?? 0,
    fieldGoalAttempt: player.fieldGoalAttempt ?? 0,
    threePointMade: player.threePointMade ?? 0,
    threePointAttempt: player.threePointAttempt ?? 0,
    freeThrowMade: player.freeThrowMade ?? 0,
    freeThrowAttempt: player.freeThrowAttempt ?? 0,
  }))
  return rows.length ? rows : [emptyPlayer()]
}

export function periodsFromRecognition(
  current: V4MatchSubmissionPeriodScore[],
  result: V4StatsScreenshotResult | null | undefined,
  gameMode?: string | null,
): V4MatchSubmissionPeriodScore[] {
  if (!requiresPeriodScores(gameMode) || !result?.periods?.length) {
    return current
  }
  const incoming = new Map(result.periods.map((row) => [row.periodNo, row]))
  const max = Math.max(4, ...result.periods.map((row) => row.periodNo || 0))
  return Array.from({ length: Math.min(max, 10) }, (_, index) => {
    const periodNo = index + 1
    const row = incoming.get(periodNo)
    return {
      periodNo,
      homeScore: row?.homeScore ?? 0,
      awayScore: row?.awayScore ?? 0,
    }
  })
}
