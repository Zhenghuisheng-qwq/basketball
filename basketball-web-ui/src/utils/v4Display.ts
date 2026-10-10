/** V4 赛事域前台展示辅助。文案走 i18n，code 保持不变。 */

import { translate } from '@/i18n'
import { codeLabel } from '@/i18n/codeLabel'

export function v4SeriesLabel(code: string | null | undefined): string {
  const upper = (code || '').toUpperCase()
  if (!upper) return '--'
  return upper.charAt(0) + upper.slice(1).toLowerCase()
}

export function v4SeriesDisplayLabel(seriesName?: string | null, seriesCode?: string | null): string {
  const code = (seriesCode || '').toUpperCase()
  const localized = code ? codeLabel('events.series', code, '') : ''
  const englishCode = code ? v4SeriesLabel(code) : ''
  if (!code) return localized || seriesName || '--'
  if (!localized || localized === englishCode) return localized || seriesName || englishCode
  return `${localized} ${englishCode}`
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
  return codeLabel('events.platform', code)
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
  return translate('events.rosterRange', { min: lo, max: hi })
}

export function v4MatchStatusLabel(status: string | null | undefined): string {
  switch ((status || '').toUpperCase()) {
    case 'PLAYING':
      return translate('match.live')
    case 'FINISHED':
      return translate('match.finished')
    case 'CANCELLED':
      return translate('match.cancelled')
    case 'FORFEITED':
    case 'FORFEIT':
      return translate('match.forfeit')
    case 'SCHEDULED':
    case 'DRAFT':
      return translate('match.notStarted')
    default:
      return translate('match.notStarted')
  }
}

export function v4MatchSeriesStatusLabel(status: string | null | undefined): string {
  switch ((status || '').toUpperCase()) {
    case 'PENDING':
      return translate('match.pendingPrep')
    case 'READY':
      return translate('match.ready')
    case 'RUNNING':
      return translate('match.live')
    case 'COMPLETED':
      return translate('match.finished')
    case 'CANCELLED':
      return translate('match.cancelled')
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
      return translate('match.seriesNotStarted')
    case 'RUNNING':
      return translate('match.seriesLive')
    case 'COMPLETED':
      return translate('match.seriesFinished')
    case 'CANCELLED':
      return translate('match.seriesCancelled')
    default:
      return v4MatchSeriesStatusLabel(status)
  }
}

export function v4PublicRoundLabel(name?: string | null, roundNo?: number | null): string {
  const raw = String(name || '').trim()
  const upper = raw.toUpperCase()
  if (upper === 'R32' || upper === 'R16' || upper === 'R8' || upper === 'QF') {
    return codeLabel('events.round', upper)
  }
  if (upper === 'SF' || upper === 'SEMI') return codeLabel('events.round', 'SF')
  if (upper === 'FINAL') return codeLabel('events.round', 'FINAL')
  if (raw) return raw
  return roundNo != null ? translate('events.round.OTHER', { round: roundNo }) : translate('events.round.SCHEDULE')
}

export function v4ScheduleStatusLabel(status?: string | null, statusName?: string | null): string {
  const code = String(status || '').toUpperCase()
  if (code === 'READY' || code === 'PENDING' || code === 'SCHEDULED' || code === 'NOT_STARTED') {
    return translate('match.notStarted')
  }
  if (statusName) return statusName
  return v4MatchSeriesStatusLabel(status)
}

export function v4ScheduleTeamLabel(name?: string | null, placeholderSource?: string | null): string {
  const text = String(name || '').trim()
  if (text) return text
  const source = String(placeholderSource || '').toUpperCase()
  if (source === 'WINNER_OF_SERIES') return translate('match.winnerOfPrevious')
  if (source === 'GROUP_RANK') return translate('match.groupRankPending')
  return translate('match.tbd')
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
  return contestUnitType(unitType) === 'MATCH_SERIES'
    ? translate('match.seriesDetail')
    : translate('match.detail')
}

export function v4QualificationTypeLabel(type?: string | null): string {
  const code = (type || '').toUpperCase()
  if (!code) return '--'
  if (code === 'MATCH_WINNER' || code === 'SERIES_WINNER' || code === 'CHAMPION') {
    return codeLabel('events.qualification', 'MATCH_WINNER')
  }
  if (code === 'OVERALL_TOP_N' || code === 'GROUP_TOP_N') {
    return codeLabel('events.qualification', 'OVERALL_TOP_N')
  }
  return codeLabel('events.qualification', code)
}

export function v4SeriesResolutionLabel(type?: string | null): string {
  const code = (type || '').toUpperCase()
  if (code === 'FORFEIT' || code === 'MANUAL') return codeLabel('events.resolution', code)
  return ''
}

export function v4SeasonStatusLabel(status: string | null | undefined): string {
  return codeLabel('events.seasonStatus', status)
}

export function v4DivisionStatusLabel(status: string | null | undefined): string {
  return codeLabel('events.divisionStatus', status)
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
  const label = codeLabel('events.stageStatus', code, '')
  if (!label || label === code.toUpperCase()) {
    return String(fallbackName || '').trim() || code
  }
  return label
}

/** 公开页状态展示：优先 Division，其次 Season */
export function v4PublicLifecycleLabel(
  divisionStatus?: string | null,
  seasonStatus?: string | null,
  fallback = translate('events.regularSeason'),
): string {
  const raw = (divisionStatus || seasonStatus || '').trim()
  if (!raw) return fallback
  const label = v4DivisionStatusLabel(raw)
  if (label === raw.toUpperCase() || label === raw) {
    return fallback
  }
  return label
}

export function v4CompetitionFormatLabel(format?: string | null, stageName?: string | null): string {
  const name = (stageName || '').trim()
  if (name) return name
  if (!format) return '--'
  return v4FormatTypeLabel(format)
}

export function v4PublishStatusLabel(status: string | null | undefined): string {
  return codeLabel('events.publishStatus', status)
}

export function v4RegistrationModeLabel(mode: string | null | undefined): string {
  return codeLabel('events.registrationMode', mode)
}

export function v4FormatTypeLabel(type: string | null | undefined): string {
  const code = (type || '').toUpperCase()
  if (!code) return '--'
  if (code === 'KNOCKOUT') return codeLabel('events.format', 'SINGLE_ELIMINATION')
  const label = codeLabel('events.format', code, '')
  return label && label !== code ? label : '--'
}

export function v4RankTypeLabel(type: string | null | undefined): string {
  if (!type) return translate('events.rank.DEFAULT')
  const label = codeLabel('events.rank', type, '')
  return label && label !== type.toUpperCase() ? label : translate('events.rank.DEFAULT')
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
    return translate('match.forfeitLoss')
  }
  if (home != null && away != null) {
    return `${home} - ${away}`
  }
  if (String(status || '').toUpperCase() === 'FINISHED') {
    return translate('match.finished')
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

export function v4SeriesFilters(): Array<{ code: string; label: string }> {
  return [
    { code: '', label: translate('events.all') },
    { code: 'LEAGUE', label: 'League' },
    { code: 'ARENA', label: 'Arena' },
    { code: 'CUP', label: 'Cup' },
    { code: 'OPEN', label: 'Open' },
  ]
}
