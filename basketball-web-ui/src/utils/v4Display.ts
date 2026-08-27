/** V4 赛事域前台展示辅助 */

const SERIES_CODE_LABELS: Record<string, string> = {
  LEAGUE: '联赛',
  ARENA: '竞技场',
  CUP: '杯赛',
  OPEN: '公开赛',
}

const SEASON_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '未开始',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名已截止',
  PLAYING: '进行中',
  SCHEDULED: '已排赛',
  FINISHED: '已结束',
  ARCHIVED: '已归档',
  REGISTRATION: '报名中',
  MATCHING: '进行中',
  DRAFT: '筹备中',
  ACTIVE: '进行中',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}

/** Division 运营状态（与 Season 文案对齐，供 Hero / 列表复用） */
const DIVISION_STATUS_LABELS: Record<string, string> = {
  ...SEASON_STATUS_LABELS,
  READY: '已就绪',
  RUNNING: '进行中',
  RESULT_PENDING_CONFIRM: '结果待确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

/**
 * Stage execution_status (StageExecutionStatusEnum).
 * Do not infer COMPLETED from Final winner — show the official lifecycle code.
 */
const STAGE_EXECUTION_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  READY: '已就绪',
  SCHEDULE_GENERATED: '赛程已生成',
  RUNNING: '进行中',
  RESULT_PENDING_CONFIRM: '结果待确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const PUBLISH_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下线',
}

function lookup(map: Record<string, string>, code?: string | null, fallback = '--'): string {
  if (!code) return fallback
  return map[code.toUpperCase()] || code
}

export function v4SeriesLabel(code: string | null | undefined): string {
  const upper = (code || '').toUpperCase()
  if (!upper) return '--'
  return upper.charAt(0) + upper.slice(1).toLowerCase()
}

export function v4SeriesDisplayLabel(seriesName?: string | null, seriesCode?: string | null): string {
  const code = (seriesCode || '').toUpperCase()
  const cn = SERIES_CODE_LABELS[code] || seriesName || code
  if (!code) return cn || '--'
  return `${cn} ${v4SeriesLabel(code)}`
}

export function v4SeriesAbbrev(code: string | null | undefined): string {
  switch ((code || '').toUpperCase()) {
    case 'LEAGUE':
      return 'LG'
    case 'ARENA':
      return 'AR'
    case 'CUP':
      return 'CP'
    case 'OPEN':
      return 'OP'
    default:
      return (code || 'CN').slice(0, 2).toUpperCase()
  }
}

export function v4PlatformLabel(code: string | null | undefined): string {
  switch ((code || '').toUpperCase()) {
    case 'PC':
      return 'PC'
    case 'PS_XBOX':
      return 'PS/Xbox'
    case 'PS':
      return 'PS（历史）'
    case 'XBOX':
      return 'Xbox（历史）'
    default:
      return code || '--'
  }
}

export function v4GameModeLabel(code: string | null | undefined): string {
  switch ((code || '').toUpperCase()) {
    case 'FIVE_V_FIVE':
      return '5V5'
    case 'THREE_V_THREE':
      return '3V3'
    default:
      if (!code) return '5V5'
      return '--'
  }
}

export function v4RosterRuleText(min?: number | null, max?: number | null, gameMode?: string | null): string {
  const mode = (gameMode || 'FIVE_V_FIVE').toUpperCase()
  const lo = min ?? (mode === 'THREE_V_THREE' ? 3 : 5)
  const hi = max ?? 8
  return `阵容 ${lo}～${hi} 人`
}

export function v4MatchStatusLabel(status: string | null | undefined): string {
  switch ((status || '').toUpperCase()) {
    case 'PLAYING':
      return '进行中'
    case 'FINISHED':
      return '已结束'
    case 'CANCELLED':
      return '取消'
    case 'FORFEITED':
    case 'FORFEIT':
      return '弃权'
    case 'SCHEDULED':
    case 'DRAFT':
      return '未开始'
    default:
      return '未开始'
  }
}

export function v4MatchSeriesStatusLabel(status: string | null | undefined): string {
  switch ((status || '').toUpperCase()) {
    case 'PENDING':
      return '待准备'
    case 'READY':
      return '待比赛'
    case 'RUNNING':
      return '进行中'
    case 'COMPLETED':
      return '已结束'
    case 'CANCELLED':
      return '已取消'
    default:
      return '--'
  }
}

/** 系列赛详情 Hero 专用，不复用单场 Match 状态文案 */
export function v4SeriesHeroStatusLabel(status: string | null | undefined): string {
  switch ((status || '').toUpperCase()) {
    case 'PENDING':
    case 'READY':
    case 'SCHEDULED':
      return '系列赛未开始'
    case 'RUNNING':
      return '系列赛进行中'
    case 'COMPLETED':
      return '系列赛已结束'
    case 'CANCELLED':
      return '系列赛已取消'
    default:
      return v4MatchSeriesStatusLabel(status)
  }
}

export function v4PublicRoundLabel(name?: string | null, roundNo?: number | null): string {
  const raw = String(name || '').trim()
  const upper = raw.toUpperCase()
  if (upper === 'R32') return '32强'
  if (upper === 'R16') return '16强'
  if (upper === 'R8' || upper === 'QF') return '8强'
  if (upper === 'SF' || upper === 'SEMI' || raw === '半决赛') return '半决赛'
  if (upper === 'FINAL' || raw === '总决赛' || raw === 'Final') return '总决赛'
  if (raw) return raw
  return roundNo != null ? `R${roundNo}` : '赛程'
}

export function v4ScheduleStatusLabel(status?: string | null, statusName?: string | null): string {
  const code = String(status || '').toUpperCase()
  if (code === 'READY' || code === 'PENDING' || code === 'SCHEDULED' || code === 'NOT_STARTED') {
    return '未开始'
  }
  if (statusName) return statusName
  return v4MatchSeriesStatusLabel(status)
}

export function v4ScheduleTeamLabel(name?: string | null, placeholderSource?: string | null): string {
  const text = String(name || '').trim()
  if (text && text !== '待定' && text !== '--') return text
  const source = String(placeholderSource || '').toUpperCase()
  if (source === 'WINNER_OF_SERIES') return '上一轮胜者'
  if (source === 'GROUP_RANK') return '小组名次待定'
  return '待定'
}

export function v4BestOfLabel(bestOf?: number | null): string {
  if (bestOf == null || bestOf <= 1) return 'BO1'
  return `BO${bestOf}`
}

export function isSeriesBestOf(bestOf?: number | null): boolean {
  return Number(bestOf) > 1
}

export function hasMatchSeriesId(value?: string | null): boolean {
  return String(value || '').trim() !== ''
}

function contestUnitType(value?: string | null): 'MATCH' | 'MATCH_SERIES' | '' {
  const type = String(value || '').trim().toUpperCase()
  if (type === 'MATCH_SERIES') return 'MATCH_SERIES'
  if (type === 'MATCH') return 'MATCH'
  return ''
}

export function publicContestDetailPath(input: {
  unitType?: string | null
  matchId?: string | null
  matchSeriesId?: string | null
}): string {
  const type = contestUnitType(input.unitType)
  const matchId = String(input.matchId || '').trim()
  const matchSeriesId = String(input.matchSeriesId || '').trim()
  if (type === 'MATCH_SERIES') {
    return matchSeriesId ? `/events/match-series/${matchSeriesId}` : ''
  }
  if (type === 'MATCH') {
    return matchId ? `/events/match/${matchId}` : ''
  }
  if (matchId && !matchSeriesId) return `/events/match/${matchId}`
  if (matchSeriesId && !matchId) return `/events/match-series/${matchSeriesId}`
  return ''
}

export function publicContestDetailLabel(unitType?: string | null): string {
  return contestUnitType(unitType) === 'MATCH_SERIES' ? '系列赛详情' : '本场详情'
}

export function v4QualificationTypeLabel(type?: string | null): string {
  switch ((type || '').toUpperCase()) {
    case 'MATCH_WINNER':
    case 'SERIES_WINNER':
    case 'CHAMPION':
      return '胜者晋级'
    case 'BYE_ADVANCE':
      return '轮空晋级'
    case 'MANUAL':
      return '直接晋级'
    case 'OVERALL_TOP_N':
    case 'GROUP_TOP_N':
      return '排名晋级'
    default:
      return type || '--'
  }
}

export function v4SeriesResolutionLabel(type?: string | null): string {
  switch ((type || '').toUpperCase()) {
    case 'FORFEIT':
      return '系列赛弃赛'
    case 'MANUAL':
      return '人工裁定'
    default:
      return ''
  }
}

export function v4SeasonStatusLabel(status: string | null | undefined): string {
  return lookup(SEASON_STATUS_LABELS, status)
}

export function v4DivisionStatusLabel(status: string | null | undefined): string {
  return lookup(DIVISION_STATUS_LABELS, status)
}

export function v4StageExecutionStatusLabel(
  status?: string | null,
  fallbackName?: string | null,
): string {
  const code = String(status || '').trim()
  if (!code) {
    const named = String(fallbackName || '').trim()
    return named || '--'
  }
  return lookup(STAGE_EXECUTION_STATUS_LABELS, code, String(fallbackName || '').trim() || '--')
}

/** 公开页状态展示：优先 Division，其次 Season */
export function v4PublicLifecycleLabel(
  divisionStatus?: string | null,
  seasonStatus?: string | null,
  fallback = '常规赛',
): string {
  const raw = (divisionStatus || seasonStatus || '').trim()
  if (!raw) return fallback
  const label = v4DivisionStatusLabel(raw)
  // lookup 未命中时会回落原 code，避免把裸 enum 直接给老板看
  if (label === raw.toUpperCase() || label === raw) {
    return fallback
  }
  return label
}

export function v4CompetitionFormatLabel(format?: string | null, stageName?: string | null): string {
  const name = (stageName || '').trim()
  if (name) return name
  switch ((format || '').toUpperCase()) {
    case 'ROUND_ROBIN':
      return '单循环'
    case 'GROUP_STAGE':
      return '小组赛'
    case 'SINGLE_ELIMINATION':
      return '单败淘汰'
    case 'DOUBLE_ELIMINATION':
      return '双败淘汰'
    default:
      return format ? v4FormatTypeLabel(format) : '--'
  }
}

export function v4PublishStatusLabel(status: string | null | undefined): string {
  return lookup(PUBLISH_STATUS_LABELS, status)
}

export function v4RegistrationModeLabel(mode: string | null | undefined): string {
  switch ((mode || '').toUpperCase()) {
    case 'TEAM':
      return '球队报名'
    case 'PERSONAL':
      return '个人报名'
    default:
      return mode || '--'
  }
}

export function v4FormatTypeLabel(type: string | null | undefined): string {
  switch ((type || '').toUpperCase()) {
    case 'REGULAR_ROUND_ROBIN':
    case 'ROUND_ROBIN':
      return '单循环'
    case 'GROUP_STAGE':
      return '小组赛'
    case 'GROUP_KNOCKOUT':
      return '小组赛+淘汰赛'
    case 'KNOCKOUT':
    case 'SINGLE_ELIMINATION':
      return '单败淘汰'
    case 'DOUBLE_ELIMINATION':
      return '双败淘汰'
    default:
      return type ? '--' : '--'
  }
}

export function v4RankTypeLabel(type: string | null | undefined): string {
  switch ((type || '').toUpperCase()) {
    case 'PLAYER_SCORE':
    case 'POINTS':
      return '得分榜'
    case 'PLAYER_REBOUND':
    case 'REBOUND':
      return '篮板榜'
    case 'PLAYER_ASSIST':
    case 'ASSIST':
      return '助攻榜'
    case 'PLAYER_STEAL':
    case 'STEAL':
      return '抢断榜'
    case 'PLAYER_BLOCK':
    case 'BLOCK':
      return '盖帽榜'
    case 'TEAM_STANDING':
      return '球队积分榜'
    default:
      return type || '排行榜'
  }
}

export function v4JoinLabels(
  codes: string[] | null | undefined,
  mapper: (code: string) => string,
  fallback = '--',
): string {
  if (!codes?.length) return fallback
  const labels = [...new Set(codes.map((code) => mapper(code)).filter(Boolean))]
  return labels.length ? labels.join(' / ') : fallback
}

export function requiredWins(bestOf?: number | null): number {
  const bo = Number(bestOf)
  const legal = bo === 1 || bo === 3 || bo === 5 || bo === 7 ? bo : 1
  return Math.floor(legal / 2) + 1
}

export function isOfficialForfeitResult(status?: string | null, resultType?: string | null): boolean {
  const normalized = String(status || '').toUpperCase()
  const type = String(resultType || '').toUpperCase()
  return normalized === 'FORFEITED' || normalized === 'FORFEIT'
    || type === 'FORFEIT' || type === 'EVENT_DISQUALIFICATION'
}

export function formatScore(
  home?: number | null,
  away?: number | null,
  status?: string | null,
  opts?: { homeLabel?: string | null; awayLabel?: string | null; resultType?: string | null },
): string {
  const forfeited = isOfficialForfeitResult(status, opts?.resultType)
  if (forfeited) {
    if (home != null && away != null && (Number(home) > 0 || Number(away) > 0)) {
      return `${home} - ${away}`
    }
    const homeLabel = String(opts?.homeLabel || '').trim()
    const awayLabel = String(opts?.awayLabel || '').trim()
    if (homeLabel || awayLabel) {
      return `${homeLabel || '--'} - ${awayLabel || '--'}`
    }
    return '判负'
  }
  if (home != null && away != null) {
    return `${home} - ${away}`
  }
  if (String(status || '').toUpperCase() === 'FINISHED') {
    return '已结束'
  }
  return 'VS'
}

export function formatSeriesSetScore(opts: {
  homeWinCount?: number | null
  awayWinCount?: number | null
  bestOf?: number | null
  seriesStatus?: string | null
  resolutionType?: string | null
  winnerEventTeamId?: string | number | null
  homeEventTeamId?: string | number | null
  awayEventTeamId?: string | number | null
  emptyText?: string
}): string {
  const required = requiredWins(opts.bestOf)
  let home = opts.homeWinCount
  let away = opts.awayWinCount
  const status = String(opts.seriesStatus || '').toUpperCase()
  const resolution = String(opts.resolutionType || '').toUpperCase()
  const finished = status === 'COMPLETED' || resolution === 'FORFEIT'
  if (finished && (home == null || home === 0) && (away == null || away === 0) && opts.winnerEventTeamId) {
    const winner = String(opts.winnerEventTeamId)
    if (winner && winner === String(opts.homeEventTeamId || '')) {
      home = required
      away = 0
    } else if (winner && winner === String(opts.awayEventTeamId || '')) {
      home = 0
      away = required
    }
  }
  if (!finished && (home == null || home === 0) && (away == null || away === 0)) {
    return opts.emptyText ?? '-- : --'
  }
  return `${home ?? 0} : ${away ?? 0}`
}

export function isSeasonRegistrationOpen(status?: string | null): boolean {
  return (status || '').toUpperCase() === 'REGISTRATION_OPEN'
}

export const V4_SERIES_FILTERS = [
  { code: '', label: '全部' },
  { code: 'LEAGUE', label: 'League' },
  { code: 'ARENA', label: 'Arena' },
  { code: 'CUP', label: 'Cup' },
  { code: 'OPEN', label: 'Open' },
] as const
