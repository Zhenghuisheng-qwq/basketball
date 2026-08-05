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
  REGISTRATION_CLOSED: '报名截止',
  PLAYING: '比赛中',
  FINISHED: '已结束',
  ARCHIVED: '已归档',
  REGISTRATION: '报名中',
  MATCHING: '进行中',
  DRAFT: '筹备中',
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
      return code
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
      return '比赛中'
    case 'FINISHED':
      return '已结束'
    case 'CANCELLED':
      return '已取消'
    case 'FORFEITED':
      return '弃权'
    case 'SCHEDULED':
      return '待比赛'
    case 'DRAFT':
      return '未发布'
    default:
      return '未开赛'
  }
}

export function v4SeasonStatusLabel(status: string | null | undefined): string {
  return lookup(SEASON_STATUS_LABELS, status)
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
      return '常规赛循环赛'
    case 'GROUP_KNOCKOUT':
      return '小组赛+淘汰赛'
    case 'KNOCKOUT':
      return '单败淘汰'
    case 'SINGLE_ELIMINATION':
      return '单败淘汰'
    case 'DOUBLE_ELIMINATION':
      return '双败淘汰'
    default:
      return type || '--'
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

export function formatScore(home?: number | null, away?: number | null, status?: string | null): string {
  if ((status || '').toUpperCase() === 'FINISHED' || home != null || away != null) {
    const h = home ?? 0
    const a = away ?? 0
    return `${h} - ${a}`
  }
  return 'VS'
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
