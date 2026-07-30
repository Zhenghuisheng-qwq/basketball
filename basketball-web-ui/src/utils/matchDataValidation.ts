/** 比赛数据提交前置校验，镜像 Admin BasketMatchDataSubmissionValidationServiceImpl */

export interface PeriodScoreItem {
  periodNo?: number
  periodType?: string
  homeScore?: number
  awayScore?: number
}

export interface TeamStatItem {
  teamId?: string | number
  points?: number
  rebounds?: number
  offensiveRebounds?: number
  defensiveRebounds?: number
  assists?: number
  steals?: number
  blocks?: number
  turnovers?: number
  fouls?: number
  fieldGoalsMade?: number
  fieldGoalsAttempted?: number
  threePointsMade?: number
  threePointsAttempted?: number
  freeThrowsMade?: number
  freeThrowsAttempted?: number
}

export interface PlayerStatItem {
  teamId?: string | number
  playerId?: string | number
  points?: number
  rebounds?: number
  offensiveRebounds?: number
  defensiveRebounds?: number
  assists?: number
  steals?: number
  blocks?: number
  turnovers?: number
  fouls?: number
  fieldGoalsMade?: number
  fieldGoalsAttempted?: number
  threePointsMade?: number
  threePointsAttempted?: number
  freeThrowsMade?: number
  freeThrowsAttempted?: number
  minutesSeconds?: number
}

export interface MatchDataSubmitForm {
  homeScore?: number
  awayScore?: number
  periodScores?: PeriodScoreItem[]
  teamStats?: TeamStatItem[]
  playerStats?: PlayerStatItem[]
  materialFileIds?: (string | number)[]
}

export interface MatchDataValidationContext {
  homeTeamId?: string | number
  awayTeamId?: string | number
  homeTeamName?: string
  awayTeamName?: string
  rosterPlayerIds?: (string | number)[]
}

const AGGREGATE_FIELDS: (keyof TeamStatItem)[] = [
  'points',
  'rebounds',
  'offensiveRebounds',
  'defensiveRebounds',
  'assists',
  'steals',
  'blocks',
  'turnovers',
  'fouls',
  'fieldGoalsMade',
  'fieldGoalsAttempted',
  'threePointsMade',
  'threePointsAttempted',
  'freeThrowsMade',
  'freeThrowsAttempted',
]

const STAT_FIELD_LABELS: Record<string, string> = {
  points: '得分',
  rebounds: '篮板',
  offensiveRebounds: '进攻篮板',
  defensiveRebounds: '防守篮板',
  assists: '助攻',
  steals: '抢断',
  blocks: '盖帽',
  turnovers: '失误',
  fouls: '犯规',
  fieldGoalsMade: '投篮命中',
  fieldGoalsAttempted: '投篮出手',
  threePointsMade: '三分命中',
  threePointsAttempted: '三分出手',
  freeThrowsMade: '罚球命中',
  freeThrowsAttempted: '罚球出手',
}

function nvl(value?: number | null): number {
  return value != null && !Number.isNaN(Number(value)) ? Number(value) : 0
}

function teamLabel(teamId: string | number | undefined, ctx: MatchDataValidationContext): string {
  if (String(teamId) === String(ctx.homeTeamId)) return ctx.homeTeamName || '主队'
  if (String(teamId) === String(ctx.awayTeamId)) return ctx.awayTeamName || '客队'
  return String(teamId ?? '球队')
}

export function aggregateTeamPlayerStats(
  playerStats: PlayerStatItem[],
  teamId: string | number
): Record<string, number> {
  const agg: Record<string, number> = {}
  for (const field of AGGREGATE_FIELDS) {
    agg[field] = 0
  }
  for (const ps of playerStats) {
    if (String(ps.teamId) !== String(teamId)) continue
    for (const field of AGGREGATE_FIELDS) {
      agg[field] += nvl(ps[field] as number | undefined)
    }
  }
  return agg
}

export function validateFinalScores(form: MatchDataSubmitForm): string[] {
  const errors: string[] = []
  const homeScore = nvl(form.homeScore)
  const awayScore = nvl(form.awayScore)
  if (homeScore < 0 || awayScore < 0) {
    errors.push('比分不能为负数')
  }
  return errors
}

export function validatePeriodTotals(form: MatchDataSubmitForm): string[] {
  const errors: string[] = []
  const periods = form.periodScores || []
  if (periods.length < 4) {
    errors.push('分节比分至少包含4节')
    return errors
  }

  const homeScore = nvl(form.homeScore)
  const awayScore = nvl(form.awayScore)
  let homeTotal = 0
  let awayTotal = 0
  let otCount = 0
  const regularPeriodNos = new Set<number>()

  for (const period of periods) {
    if (period.periodNo == null || !period.periodType) {
      errors.push('节次信息不完整')
      continue
    }
    const hs = nvl(period.homeScore)
    const as = nvl(period.awayScore)
    if (hs < 0 || as < 0) {
      errors.push('分节比分不能为负数')
    }
    homeTotal += hs
    awayTotal += as
    if (period.periodType.toUpperCase() === 'REGULAR') {
      regularPeriodNos.add(period.periodNo)
    } else if (period.periodType.toUpperCase() === 'OVERTIME') {
      otCount++
    }
  }

  for (let i = 1; i <= 4; i++) {
    if (!regularPeriodNos.has(i)) {
      errors.push('须录入四节常规比分')
      break
    }
  }

  if (homeTotal !== homeScore) {
    errors.push('主队四节比分合计不等于最终比分')
  }
  if (awayTotal !== awayScore) {
    errors.push('客队四节比分合计不等于最终比分')
  }
  if (homeScore === awayScore && otCount === 0) {
    errors.push('常规时间平局须通过加时赛分出胜负')
  }
  return errors
}

export function validateTeamPlayerPoints(
  form: MatchDataSubmitForm,
  ctx: MatchDataValidationContext
): string[] {
  const errors: string[] = []
  const homeScore = nvl(form.homeScore)
  const awayScore = nvl(form.awayScore)
  const playerStats = form.playerStats || []

  for (const teamId of [ctx.homeTeamId, ctx.awayTeamId]) {
    if (teamId == null) continue
    const agg = aggregateTeamPlayerStats(playerStats, teamId)
    const expected = String(teamId) === String(ctx.homeTeamId) ? homeScore : awayScore
    if (agg.points !== expected) {
      errors.push(
        `${teamLabel(teamId, ctx)} 球员得分合计 ${agg.points}，不等于球队得分 ${expected}`
      )
    }
  }

  const totalPlayerPoints = (form.playerStats || []).reduce((sum, ps) => sum + nvl(ps.points), 0)
  if (totalPlayerPoints !== homeScore + awayScore) {
    errors.push('球员总得分须等于比赛总得分')
  }
  return errors
}

export function validateTeamStatAggregates(
  form: MatchDataSubmitForm,
  ctx: MatchDataValidationContext
): string[] {
  const errors: string[] = []
  const teamStats = form.teamStats || []
  const playerStats = form.playerStats || []

  for (const ts of teamStats) {
    if (ts.teamId == null) continue
    const label = teamLabel(ts.teamId, ctx)
    const agg = aggregateTeamPlayerStats(playerStats, ts.teamId)
    for (const field of AGGREGATE_FIELDS) {
      const teamVal = nvl(ts[field] as number | undefined)
      const playerSum = agg[field] ?? 0
      if (teamVal !== playerSum) {
        const fieldLabel = STAT_FIELD_LABELS[field] || field
        errors.push(`${label} ${fieldLabel} ${teamVal} 与球员汇总 ${playerSum} 不一致`)
      }
    }
  }
  return errors
}

export function validateShootingStats(
  items: Array<TeamStatItem | PlayerStatItem>,
  ctx: MatchDataValidationContext,
  kind: 'team' | 'player'
): string[] {
  const errors: string[] = []
  for (const item of items) {
    const label =
      kind === 'team'
        ? teamLabel(item.teamId, ctx)
        : `${teamLabel(item.teamId, ctx)} 球员 ${(item as PlayerStatItem).playerId ?? ''}`.trim()

    if (nvl(item.fieldGoalsMade) > nvl(item.fieldGoalsAttempted)) {
      errors.push(`${label} 投篮命中数不能大于投篮出手数`)
    }
    if (nvl(item.threePointsMade) > nvl(item.threePointsAttempted)) {
      errors.push(`${label} 三分命中数不能大于三分出手数`)
    }
    if (nvl(item.freeThrowsMade) > nvl(item.freeThrowsAttempted)) {
      errors.push(`${label} 罚球命中数不能大于罚球出手数`)
    }
    if (nvl(item.threePointsMade) > nvl(item.fieldGoalsMade)) {
      errors.push(`${label} 三分命中数不能大于投篮命中数`)
    }

    const theoretical =
      2 * (nvl(item.fieldGoalsMade) - nvl(item.threePointsMade)) +
      3 * nvl(item.threePointsMade) +
      nvl(item.freeThrowsMade)
    if (nvl(item.points) !== theoretical) {
      errors.push(`${label} 得分与投篮数据不一致，理论得分=${theoretical}`)
    }
  }
  return errors
}

export function validateNonNegative(
  form: MatchDataSubmitForm
): string[] {
  const errors: string[] = []
  const numericFields: (keyof TeamStatItem)[] = [
    'points',
    'rebounds',
    'offensiveRebounds',
    'defensiveRebounds',
    'assists',
    'steals',
    'blocks',
    'turnovers',
    'fouls',
    'fieldGoalsMade',
    'fieldGoalsAttempted',
    'threePointsMade',
    'threePointsAttempted',
    'freeThrowsMade',
    'freeThrowsAttempted',
  ]

  for (const ts of form.teamStats || []) {
    for (const field of numericFields) {
      if (nvl(ts[field] as number | undefined) < 0) {
        errors.push('球队统计不能为负数')
        return errors
      }
    }
  }

  for (const ps of form.playerStats || []) {
    for (const field of numericFields) {
      if (nvl(ps[field] as number | undefined) < 0) {
        errors.push('球员统计不能为负数')
        return errors
      }
    }
    if (nvl(ps.minutesSeconds) < 0) {
      errors.push('球员统计不能为负数')
      return errors
    }
  }
  return errors
}

export function validateScreenshotMaterials(materialFileIds?: (string | number)[]): string[] {
  if (!materialFileIds?.length) {
    return ['请至少上传一张比赛结算截图']
  }
  return []
}

export function validateDuplicatePlayers(playerStats?: PlayerStatItem[]): string[] {
  const seen = new Set<string>()
  for (const ps of playerStats || []) {
    const id = String(ps.playerId ?? '')
    if (!id) continue
    if (seen.has(id)) {
      return ['同一球员不能重复录入']
    }
    seen.add(id)
  }
  return []
}

export function validateRosterPlayers(
  playerStats?: PlayerStatItem[],
  rosterPlayerIds?: (string | number)[]
): string[] {
  if (!playerStats?.length) {
    return ['球员技术统计不能为空']
  }
  if (!rosterPlayerIds?.length) {
    return []
  }
  const rosterSet = new Set(rosterPlayerIds.map(String))
  for (const ps of playerStats) {
    const playerId = String(ps.playerId ?? '')
    if (playerId && !rosterSet.has(playerId)) {
      return ['球员须来自本场比赛出场名单']
    }
  }
  return []
}

export function validateMatchDataSubmitForm(
  form: MatchDataSubmitForm,
  ctx: MatchDataValidationContext
): string[] {
  const errors: string[] = []

  if (!form.teamStats?.length || form.teamStats.length < 2) {
    errors.push('球队技术统计须包含主客队各一条')
  }

  errors.push(...validateFinalScores(form))
  errors.push(...validatePeriodTotals(form))
  errors.push(...validateNonNegative(form))
  errors.push(...validateScreenshotMaterials(form.materialFileIds))
  errors.push(...validateDuplicatePlayers(form.playerStats))
  errors.push(...validateRosterPlayers(form.playerStats, ctx.rosterPlayerIds))

  if (form.teamStats?.length) {
    errors.push(...validateShootingStats(form.teamStats, ctx, 'team'))
  }
  if (form.playerStats?.length) {
    errors.push(...validateShootingStats(form.playerStats, ctx, 'player'))
  }

  errors.push(...validateTeamPlayerPoints(form, ctx))
  errors.push(...validateTeamStatAggregates(form, ctx))

  return errors
}
