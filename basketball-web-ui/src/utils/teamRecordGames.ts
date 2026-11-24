function finite(value: number): number {
  return Number.isFinite(value) ? value : 0
}

/** 战绩场次含弃权。出战场次不含弃权，场均只用出战场次。 */
export function teamRecordGames(
  matchCount?: number | null,
  winCount?: number | null,
  loseCount?: number | null,
): number {
  const played = finite(Number(matchCount ?? 0))
  const wins = finite(Number(winCount ?? 0))
  const losses = finite(Number(loseCount ?? 0))
  return Math.max(played, wins + losses)
}

export function boxScoreGames(
  matchCount?: number | null,
  winCount?: number | null,
  loseCount?: number | null,
): number {
  const played = finite(Number(matchCount ?? 0))
  return played > 0 ? played : teamRecordGames(matchCount, winCount, loseCount)
}
