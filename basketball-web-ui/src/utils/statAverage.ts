export function calculateAverage(total: unknown, gamesPlayed: unknown): number {
  const games = Number(gamesPlayed)
  const value = Number(total)
  if (!Number.isFinite(games) || games <= 0) return 0
  if (!Number.isFinite(value)) return 0
  return value / games
}

export function roundAverage(value: number, digits = 1): number {
  if (!Number.isFinite(value)) return 0
  const factor = 10 ** digits
  return Math.round(value * factor) / factor
}