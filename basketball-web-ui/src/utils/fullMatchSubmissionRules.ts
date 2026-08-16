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

export function expectedPoints(fgm: number, tpm: number, ftm: number) {
  return 2 * fgm + tpm + ftm
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
}) {
  const rows = [...usedRows(opts.homePlayers), ...usedRows(opts.awayPlayers)]
  const hasPlayer = rows.some((row) => String(row.eventPlayerId || '').trim())
  const hasStat = rows.some((row) => hasAnyStat(row))
  const periodSum = periodTotals(opts.periods)
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
  if (fgm > fga) flags.fieldGoal = true
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
    errors.push(`${prefix}尚未选择球员`)
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
  if (ints.some((v) => v < 0 || !Number.isInteger(v))) errors.push(`${prefix}所有数值必须为大于等于 0 的整数`)
  if (!requireComplete && !valued) return errors
  if (fgm > fga) errors.push(`${prefix}：FGM ${fgm} 大于 FGA ${fga}`)
  if (tpm > tpa) errors.push(`${prefix}：3PM ${tpm} 大于 3PA ${tpa}`)
  if (tpm > fgm) errors.push(`${prefix}：3PM ${tpm} 大于 FGM ${fgm}`)
  if (tpa > fga) errors.push(`${prefix}：3PA ${tpa} 大于 FGA ${fga}`)
  if (ftm > fta) errors.push(`${prefix}：FTM ${ftm} 大于 FTA ${fta}`)
  const expected = expectedPoints(fgm, tpm, ftm)
  if (pts !== expected) errors.push(`${prefix}：PTS ${pts} 与公式 2×FGM+3PM+FTM=${expected} 不一致`)
  return errors
}

export function validateTeamPlayers(gameLabel: string, teamName: string, rows: FullMatchPlayerRow[], requireComplete: boolean) {
  const errors: string[] = []
  const used = usedRows(rows)
  const seen = new Set<string>()
  used.forEach((row, index) => {
    const name = row.playerName || `第 ${index + 1} 行`
    const prefix = `${gameLabel} · ${teamName} · ${name}`
    const id = String(row.eventPlayerId || '')
    if (id && seen.has(id)) errors.push(`${prefix}：同一球队同一场不能重复选择球员`)
    if (id) seen.add(id)
    errors.push(...validatePlayerRow(prefix, row, requireComplete))
  })
  if (requireComplete && used.length === 0) errors.push(`${gameLabel} · ${teamName}至少需要一名球员数据`)
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
}) {
  const errors: string[] = []
  const empty = isBlankGame(opts)
  errors.push(...validateTeamPlayers(opts.gameLabel, opts.homeName, opts.homePlayers, false))
  errors.push(...validateTeamPlayers(opts.gameLabel, opts.awayName, opts.awayPlayers, false))
  const homeScore = aggregateTeam(opts.homePlayers).score
  const awayScore = aggregateTeam(opts.awayPlayers).score
  const quarters = periodTotals(opts.periods)
  const homeQ = quarters.home
  const awayQ = quarters.away
  if (!empty && (homeScore > 0 || awayScore > 0 || homeQ > 0 || awayQ > 0)) {
    if (homeQ !== homeScore) errors.push(`${opts.gameLabel} · ${opts.homeName}：四节得分合计 ${homeQ}，与最终比分 ${homeScore} 不一致`)
    if (awayQ !== awayScore) errors.push(`${opts.gameLabel} · ${opts.awayName}：四节得分合计 ${awayQ}，与最终比分 ${awayScore} 不一致`)
  }
  const homeReady = usedRows(opts.homePlayers).some((row) => String(row.eventPlayerId || '').trim())
  const awayReady = usedRows(opts.awayPlayers).some((row) => String(row.eventPlayerId || '').trim())
  if (opts.requireComplete && !empty) {
    if (!homeReady) errors.push(`${opts.gameLabel} · ${opts.homeName}至少需要一名球员数据`)
    if (!awayReady) errors.push(`${opts.gameLabel} · ${opts.awayName}至少需要一名球员数据`)
    if (homeScore <= 0 || awayScore <= 0) errors.push(`${opts.gameLabel}：双方比分必须大于 0`)
    if (homeScore === awayScore) errors.push(`${opts.gameLabel}：比赛不允许平局`)
    if (opts.materialCount <= 0) errors.push(`${opts.gameLabel}：请上传本场证明材料`)
  }
  const pass = !empty
    && errors.length === 0
    && homeReady
    && awayReady
    && homeScore > 0
    && awayScore > 0
    && homeScore !== awayScore
    && homeQ === homeScore
    && awayQ === awayScore
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
