import { translate } from '@/i18n'

export interface FullMatchPlayerRow {
  eventPlayerId?: string
  playerName?: string
  points?: number
  rebound?: number
  assist?: number
  steal?: number
  block?: number
  foul?: number
  turnover?: number
  fieldGoalMade?: number
  fieldGoalAttempt?: number
  threePointMade?: number
  threePointAttempt?: number
  freeThrowMade?: number
  freeThrowAttempt?: number
}

export interface FullMatchPeriodRow {
  periodNo: number
  homeScore?: number
  awayScore?: number
}

export function nvl(v?: number | null) {
  return v == null || Number.isNaN(Number(v)) ? 0 : Number(v)
}

/** 0 是合法得分；只有 null / undefined / NaN 表示未提供。 */
export function hasScoreValue(score?: number | null) {
  return score != null && Number.isFinite(Number(score))
}

/** 正式口径：fieldGoal* = 总运动战命中/出手（FGM/FGA），3PM 是 FGM 的子集。PTS = (FGM-3PM)×2 + 3PM×3 + FTM */
export function expectedPoints(fieldGoalMade: number, threePointMade: number, freeThrowMade: number) {
  const twoPointMade = fieldGoalMade - threePointMade
  return twoPointMade * 2 + threePointMade * 3 + freeThrowMade
}

export function requiresPeriodScores(gameMode?: string | null) {
  const code = String(gameMode || '').trim().toUpperCase()
  return code !== 'THREE_V_THREE' && code !== '3V3'
}

export function shootingPct(made?: number, attempt?: number) {
  const a = nvl(attempt)
  if (a <= 0) return '—'
  return `${((nvl(made) / a) * 100).toFixed(1)}%`
}

export function shootingLine(made?: number, attempt?: number) {
  const m = nvl(made)
  const a = nvl(attempt)
  if (a <= 0) return `${m}/0 · —`
  return `${m}/${a} · ${((m / a) * 100).toFixed(1)}%`
}

export function periodTotals(periods: FullMatchPeriodRow[]) {
  return {
    home: periods.reduce((sum, row) => sum + nvl(row.homeScore), 0),
    away: periods.reduce((sum, row) => sum + nvl(row.awayScore), 0),
  }
}

export function isBlankGame(opts: {
  homePlayers: FullMatchPlayerRow[]
  awayPlayers: FullMatchPlayerRow[]
  periods: FullMatchPeriodRow[]
  materialCount: number
  gameMode?: string | null
}) {
  const rows = [...usedRows(opts.homePlayers), ...usedRows(opts.awayPlayers)]
  const hasPlayer = rows.some((row) => String(row.eventPlayerId || '').trim())
  const hasStat = rows.some((row) => hasAnyStat(row))
  const periodSum = requiresPeriodScores(opts.gameMode) ? periodTotals(opts.periods) : { home: 0, away: 0 }
  return !hasPlayer && !hasStat && periodSum.home === 0 && periodSum.away === 0 && opts.materialCount <= 0
}

export function playerFieldErrors(row: FullMatchPlayerRow) {
  const flags: Record<string, boolean> = {}
  if (!String(row.eventPlayerId || '').trim()) {
    if (hasAnyStat(row)) flags.player = true
    return flags
  }
  const pts = nvl(row.points)
  const fgm = nvl(row.fieldGoalMade)
  const fga = nvl(row.fieldGoalAttempt)
  const tpm = nvl(row.threePointMade)
  const tpa = nvl(row.threePointAttempt)
  const ftm = nvl(row.freeThrowMade)
  const fta = nvl(row.freeThrowAttempt)
  if (fgm > fga || tpm > fgm || tpa > fga) flags.fieldGoal = true
  if (tpm > tpa || tpm > fgm || tpa > fga) flags.threePoint = true
  if (ftm > fta) flags.freeThrow = true
  if (pts !== expectedPoints(fgm, tpm, ftm)) flags.points = true
  return flags
}

export function hasAnyStat(row: FullMatchPlayerRow) {
  return ['points', 'rebound', 'assist', 'steal', 'block', 'foul', 'turnover', 'fieldGoalMade', 'fieldGoalAttempt', 'threePointMade', 'threePointAttempt', 'freeThrowMade', 'freeThrowAttempt']
    .some((key) => nvl((row as Record<string, number | undefined>)[key]) > 0)
}

export function usedRows(rows: FullMatchPlayerRow[]) {
  return rows.filter((row) => String(row.eventPlayerId || '').trim() || hasAnyStat(row))
}

export function aggregateTeam(rows: FullMatchPlayerRow[]) {
  const t = {
    score: 0, rebound: 0, assist: 0, steal: 0, block: 0, foul: 0, turnover: 0,
    fieldGoalMade: 0, fieldGoalAttempt: 0, threePointMade: 0, threePointAttempt: 0, freeThrowMade: 0, freeThrowAttempt: 0,
  }
  for (const row of usedRows(rows)) {
    t.score += nvl(row.points)
    t.rebound += nvl(row.rebound)
    t.assist += nvl(row.assist)
    t.steal += nvl(row.steal)
    t.block += nvl(row.block)
    t.foul += nvl(row.foul)
    t.turnover += nvl(row.turnover)
    t.fieldGoalMade += nvl(row.fieldGoalMade)
    t.fieldGoalAttempt += nvl(row.fieldGoalAttempt)
    t.threePointMade += nvl(row.threePointMade)
    t.threePointAttempt += nvl(row.threePointAttempt)
    t.freeThrowMade += nvl(row.freeThrowMade)
    t.freeThrowAttempt += nvl(row.freeThrowAttempt)
  }
  return t
}

export function validatePlayerRow(prefix: string, row: FullMatchPlayerRow, requireComplete: boolean): string[] {
  const errors: string[] = []
  const valued = hasAnyStat(row)
  if (valued && !String(row.eventPlayerId || '').trim()) {
    errors.push(translate('accountEvents.validation.playerMissing', { prefix }))
    return errors
  }
  if (!valued && !String(row.eventPlayerId || '').trim()) return errors
  const pts = nvl(row.points)
  const fgm = nvl(row.fieldGoalMade)
  const fga = nvl(row.fieldGoalAttempt)
  const tpm = nvl(row.threePointMade)
  const tpa = nvl(row.threePointAttempt)
  const ftm = nvl(row.freeThrowMade)
  const fta = nvl(row.freeThrowAttempt)
  const ints = [pts, nvl(row.rebound), nvl(row.assist), nvl(row.steal), nvl(row.block), nvl(row.foul), nvl(row.turnover), fgm, fga, tpm, tpa, ftm, fta]
  if (ints.some((v) => v < 0 || !Number.isInteger(v))) errors.push(translate('accountEvents.validation.integers', { prefix }))
  if (!requireComplete && !valued) return errors
  if (fgm > fga) errors.push(translate('accountEvents.validation.statGreater', { prefix, left: 'FGM', leftValue: fgm, right: 'FGA', rightValue: fga }))
  if (tpm > tpa) errors.push(translate('accountEvents.validation.statGreater', { prefix, left: '3PM', leftValue: tpm, right: '3PA', rightValue: tpa }))
  if (ftm > fta) errors.push(translate('accountEvents.validation.statGreater', { prefix, left: 'FTM', leftValue: ftm, right: 'FTA', rightValue: fta }))
  if (tpm > fgm) errors.push(translate('accountEvents.validation.statGreater', { prefix, left: '3PM', leftValue: tpm, right: 'FGM', rightValue: fgm }))
  if (tpa > fga) errors.push(translate('accountEvents.validation.statGreater', { prefix, left: '3PA', leftValue: tpa, right: 'FGA', rightValue: fga }))
  const expected = expectedPoints(fgm, tpm, ftm)
  if (pts !== expected) errors.push(translate('accountEvents.validation.pointsFormula', { prefix, points: pts, expected }))
  return errors
}

export function validateTeamPlayers(gameLabel: string, teamName: string, rows: FullMatchPlayerRow[], requireComplete: boolean) {
  const errors: string[] = []
  const used = usedRows(rows)
  const seen = new Set<string>()
  used.forEach((row, index) => {
    const name = row.playerName || translate('accountEvents.validation.row', { index: index + 1 })
    const prefix = `${gameLabel} · ${teamName} · ${name}`
    const id = String(row.eventPlayerId || '')
    if (id && seen.has(id)) errors.push(translate('accountEvents.validation.duplicatePlayer', { prefix }))
    if (id) seen.add(id)
    errors.push(...validatePlayerRow(prefix, row, requireComplete))
  })
  if (requireComplete && used.length === 0) errors.push(translate('accountEvents.validation.playerRequired', { game: gameLabel, team: teamName }))
  return errors
}

export function validateGame(opts: {
  gameLabel: string
  homeName: string
  awayName: string
  homePlayers: FullMatchPlayerRow[]
  awayPlayers: FullMatchPlayerRow[]
  periods: FullMatchPeriodRow[]
  materialCount: number
  requireComplete: boolean
  gameMode?: string | null
}) {
  const errors: string[] = []
  const needPeriod = requiresPeriodScores(opts.gameMode)
  const empty = isBlankGame(opts)
  errors.push(...validateTeamPlayers(opts.gameLabel, opts.homeName, opts.homePlayers, false))
  errors.push(...validateTeamPlayers(opts.gameLabel, opts.awayName, opts.awayPlayers, false))
  const homeScore = aggregateTeam(opts.homePlayers).score
  const awayScore = aggregateTeam(opts.awayPlayers).score
  const quarters = periodTotals(opts.periods)
  const homeQ = quarters.home
  const awayQ = quarters.away
  if (needPeriod && !empty && (homeScore > 0 || awayScore > 0 || homeQ > 0 || awayQ > 0)) {
    if (homeQ !== homeScore) errors.push(translate('accountEvents.validation.quarterMismatch', { game: opts.gameLabel, team: opts.homeName, period: homeQ, score: homeScore }))
    if (awayQ !== awayScore) errors.push(translate('accountEvents.validation.quarterMismatch', { game: opts.gameLabel, team: opts.awayName, period: awayQ, score: awayScore }))
  }
  const homeReady = usedRows(opts.homePlayers).some((row) => String(row.eventPlayerId || '').trim())
  const awayReady = usedRows(opts.awayPlayers).some((row) => String(row.eventPlayerId || '').trim())
  if (opts.requireComplete && !empty) {
    if (!homeReady) errors.push(translate('accountEvents.validation.playerRequired', { game: opts.gameLabel, team: opts.homeName }))
    if (!awayReady) errors.push(translate('accountEvents.validation.playerRequired', { game: opts.gameLabel, team: opts.awayName }))
    if (!hasScoreValue(homeScore) || !hasScoreValue(awayScore) || homeScore < 0 || awayScore < 0) {
      errors.push(translate('accountEvents.validation.scoreRequired', { game: opts.gameLabel }))
    }
    if (homeScore === awayScore) errors.push(translate('accountEvents.validation.noTie', { game: opts.gameLabel }))
    if (opts.materialCount <= 0) errors.push(translate('accountEvents.validation.proofRequired', { game: opts.gameLabel }))
  }
  const periodOk = !needPeriod || (homeQ === homeScore && awayQ === awayScore)
  const pass = !empty
    && errors.length === 0
    && homeReady
    && awayReady
    && hasScoreValue(homeScore)
    && hasScoreValue(awayScore)
    && homeScore >= 0
    && awayScore >= 0
    && homeScore !== awayScore
    && periodOk
    && opts.materialCount > 0
  const status: 'empty' | 'error' | 'pass' = empty ? 'empty' : (errors.length || !pass ? (errors.length ? 'error' : 'empty') : 'pass')
  return { errors, homeScore, awayScore, homePeriod: homeQ, awayPeriod: awayQ, empty, status, pass }
}

export function seriesOptions(bestOf: number) {
  const need = Math.floor(bestOf / 2) + 1
  const options: Array<{ value: string; home: number; away: number; label: string }> = []
  for (let lose = 0; lose < need; lose++) {
    options.push({ value: `${need}-${lose}`, home: need, away: lose, label: `${need} : ${lose}` })
    options.push({ value: `${lose}-${need}`, home: lose, away: need, label: `${lose} : ${need}` })
  }
  return options
}
