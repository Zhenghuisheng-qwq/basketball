import type { V4PublicScheduleItem } from '@/api/types/v4Public'
import type { Team } from '../types/events'
import {
  formatSeriesSetScore,
  isOfficialForfeitResult,
  v4BestOfLabel,
  v4MatchStatusLabel,
  v4PublicRoundLabel,
  v4ScheduleStatusLabel,
  v4ScheduleTeamLabel,
  v4StageLabel,
} from '@/utils/v4Display'
import { translate } from '@/i18n'
import { isValidDateKey } from '@/utils/scheduleDateKey'

function genericRoundLabel(): string {
  return translate('events.round.SCHEDULE')
}

export type ScheduleStatusTone = 'scheduled' | 'live' | 'finished' | 'forfeit' | 'cancelled'

export type ScheduleScoreView = {
  kind: 'vs' | 'score' | 'forfeit'
  left?: number
  right?: number
  leftLabel?: string
  rightLabel?: string
  caption?: string
}

function upper(value?: string | null): string {
  return String(value || '').trim().toUpperCase()
}

function forfeitSideLabel(winnerId?: string | number | null, sideId?: string | number | null): string | undefined {
  if (winnerId == null || sideId == null || String(winnerId) === '') return undefined
  return String(winnerId) === String(sideId)
    ? translate('eventsPage.match.forfeitWin')
    : translate('match.forfeitLoss')
}

export function isScheduleSeries(item: V4PublicScheduleItem): boolean {
  return item.unitType === 'MATCH_SERIES'
}

export function scheduleTeamName(item: V4PublicScheduleItem, side: 'home' | 'away'): string {
  return v4ScheduleTeamLabel(side === 'home' ? item.homeTeam : item.awayTeam, item.placeholderSource)
}

export function scheduleTeamCode(item: V4PublicScheduleItem, side: 'home' | 'away'): string {
  const name = scheduleTeamName(item, side)
  const code = side === 'home'
    ? String(item.homeTeamShortName || item.homeTeamCode || '').trim()
    : String(item.awayTeamShortName || item.awayTeamCode || '').trim()
  if (!code || code === name) return ''
  if (/^CNPA-T-\d+$/i.test(code)) return ''
  if (name.includes(code)) return ''
  return code
}

export function toScheduleTeam(item: V4PublicScheduleItem, side: 'home' | 'away'): Team {
  const eventTeamId = String(side === 'home' ? item.homeEventTeamId || '' : item.awayEventTeamId || '')
  const name = scheduleTeamName(item, side)
  const code = scheduleTeamCode(item, side)
  const logo = String(side === 'home' ? item.homeTeamLogoUrl || '' : item.awayTeamLogoUrl || '').trim()
  return {
    id: Number(eventTeamId) || 0,
    teamId: eventTeamId,
    code: code || name.slice(0, 2).toUpperCase(),
    name,
    shortName: code || name,
    logo,
    wins: 0,
    losses: 0,
  }
}

export function formatScheduleClock(startTime?: string | null): string | null {
  const raw = String(startTime || '').trim()
  if (!raw) return null
  const match = raw.match(/^(\d{1,2}):(\d{2})/)
  if (!match) return null
  const hour = Number(match[1])
  const minute = Number(match[2])
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return null
  if (hour === 0 && minute === 0) return null
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

export function formatScheduleDateShort(scheduleDate?: string | null): string | null {
  const raw = String(scheduleDate || '').trim()
  if (!isValidDateKey(raw)) return null
  return `${raw.slice(5, 7)}-${raw.slice(8, 10)}`
}

export function formatRoundLine(item: V4PublicScheduleItem): string {
  const raw = String(item.roundName || '').trim()
  const labeled = v4PublicRoundLabel(item.roundName, item.roundNo)
  const generic = genericRoundLabel()
  if (labeled && labeled !== generic && labeled !== raw) {
    return labeled
  }
  if (/^R\d+$/i.test(raw)) {
    const roundNo = Number(raw.slice(1))
    if (roundNo > 0 && ![8, 16, 32].includes(roundNo)) {
      return translate('events.round.OTHER', { round: roundNo })
    }
  }
  return labeled && labeled !== generic ? labeled : ''
}

export function formatStageLine(item: V4PublicScheduleItem): string {
  return v4StageLabel(undefined, item.stageName)
}

export function formatStageRoundBlock(item: V4PublicScheduleItem): { stage: string; round: string } {
  return {
    stage: formatStageLine(item),
    round: formatRoundLine(item),
  }
}

export function formatRoundSectionTitle(item: V4PublicScheduleItem): string {
  const stage = formatStageLine(item)
  const round = formatRoundLine(item)
  if (stage && round && !round.includes(stage)) return `${stage} · ${round}`
  return stage || round || translate('events.schedule')
}

export function scheduleRoundGroupKey(item: V4PublicScheduleItem): string {
  const stage = formatStageLine(item)
  const round = String(item.roundName || (item.roundNo != null ? `R${item.roundNo}` : '')).trim()
  return [stage, round].filter(Boolean).join('|') || 'ROUND'
}

export function scheduleStatusCode(item: V4PublicScheduleItem): string {
  return upper(item.status)
}

export function isScheduleLive(item: V4PublicScheduleItem): boolean {
  return ['PLAYING', 'RUNNING', 'LIVE', 'IN_PROGRESS'].includes(scheduleStatusCode(item))
}

export function isScheduleFinished(item: V4PublicScheduleItem): boolean {
  const code = scheduleStatusCode(item)
  return ['FINISHED', 'COMPLETED', 'CLOSED', 'FORFEITED', 'FORFEIT'].includes(code)
    || isOfficialForfeitResult(item.status, item.resultType)
}

export function isScheduleCancelled(item: V4PublicScheduleItem): boolean {
  return scheduleStatusCode(item) === 'CANCELLED'
}

export function isScheduleScheduled(item: V4PublicScheduleItem): boolean {
  return ['READY', 'PENDING', 'SCHEDULED', 'NOT_STARTED', 'UPCOMING', 'DRAFT'].includes(scheduleStatusCode(item))
}

export function scheduleStatusLabel(item: V4PublicScheduleItem): string {
  if (isOfficialForfeitResult(item.status, item.resultType)) return translate('match.forfeit')
  if (isScheduleSeries(item)) {
    if (isScheduleScheduled(item)) return translate('match.notStarted')
    if (isScheduleLive(item)) return translate('match.live')
    if (isScheduleFinished(item)) return translate('match.finished')
    if (isScheduleCancelled(item)) return translate('match.cancelled')
    return v4ScheduleStatusLabel(item.status, item.statusName)
  }
  if (isScheduleScheduled(item)) return translate('match.notStarted')
  return v4MatchStatusLabel(item.status)
}

export function schedulePlatformBadge(item: V4PublicScheduleItem, side: 'home' | 'away'): string {
  const name = scheduleTeamName(item, side)
  const raw = String(
    side === 'home'
      ? (item.homeTeam || item.homeTeamCode || item.homeTeamShortName || '')
      : (item.awayTeam || item.awayTeamCode || item.awayTeamShortName || ''),
  ).trim()
  if (/E2/i.test(name) || /E2/i.test(raw)) return 'E2'
  const letters = raw.replace(/[^A-Za-z0-9\u4e00-\u9fff]/g, '')
  if (letters.length >= 2) return letters.slice(0, 2).toUpperCase()
  return 'E2'
}

export function scheduleStatusTone(item: V4PublicScheduleItem): ScheduleStatusTone {
  if (isOfficialForfeitResult(item.status, item.resultType)) return 'forfeit'
  if (isScheduleCancelled(item)) return 'cancelled'
  if (isScheduleLive(item)) return 'live'
  if (isScheduleFinished(item)) return 'finished'
  return 'scheduled'
}

export function scheduleBestOfLabel(item: V4PublicScheduleItem): string {
  return v4BestOfLabel(item.bestOf)
}

export function parseOfficialSeriesScore(item: V4PublicScheduleItem): { home: number; away: number } | null {
  const text = formatSeriesSetScore({
    homeWinCount: item.homeWinCount,
    awayWinCount: item.awayWinCount,
    bestOf: item.bestOf,
    seriesStatus: item.status,
    winnerEventTeamId: item.winnerEventTeamId,
    homeEventTeamId: item.homeEventTeamId,
    awayEventTeamId: item.awayEventTeamId,
    emptyText: '',
  })
  if (!text || text === '-- : --') return null
  const [home, away] = text.split(':').map((part) => Number(String(part).trim()))
  if (!Number.isFinite(home) || !Number.isFinite(away)) return null
  return { home, away }
}

export function scheduleScoreView(item: V4PublicScheduleItem): ScheduleScoreView {
  const forfeited = isOfficialForfeitResult(item.status, item.resultType)
  if (isScheduleSeries(item)) {
    const series = parseOfficialSeriesScore(item)
    if (series) {
      return {
        kind: forfeited ? 'forfeit' : 'score',
        left: series.away,
        right: series.home,
        caption: forfeited ? translate('match.forfeit') : (isScheduleLive(item) ? translate('match.live') : ''),
      }
    }
    return { kind: 'vs' }
  }

  if (forfeited) {
    const hasScore = item.homeScore != null && item.awayScore != null
    return {
      kind: 'forfeit',
      left: hasScore ? Number(item.awayScore) : undefined,
      right: hasScore ? Number(item.homeScore) : undefined,
      leftLabel: forfeitSideLabel(item.winnerEventTeamId, item.awayEventTeamId),
      rightLabel: forfeitSideLabel(item.winnerEventTeamId, item.homeEventTeamId),
      caption: translate('match.forfeit'),
    }
  }

  if (isScheduleFinished(item) || isScheduleLive(item)) {
    if (item.homeScore != null && item.awayScore != null) {
      return {
        kind: 'score',
        left: Number(item.awayScore),
        right: Number(item.homeScore),
        caption: isScheduleLive(item) ? translate('match.live') : '',
      }
    }
    return isScheduleLive(item) ? { kind: 'score', caption: translate('match.live') } : { kind: 'vs' }
  }

  return { kind: 'vs' }
}

export function isWinningSide(item: V4PublicScheduleItem, side: 'home' | 'away'): boolean {
  const winner = String(item.winnerEventTeamId || '').trim()
  if (!winner) return false
  const teamId = String(side === 'home' ? item.homeEventTeamId || '' : item.awayEventTeamId || '').trim()
  return Boolean(teamId) && winner === teamId
}

export function isSafeHttpUrl(value?: string | null): boolean {
  const raw = String(value || '').trim()
  if (!raw) return false
  try {
    const url = new URL(raw)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

export function scheduleReplayUrl(item: V4PublicScheduleItem): string {
  const raw = String(item.replayUrl || item.videoUrl || '').trim()
  return isSafeHttpUrl(raw) ? raw : ''
}

export function hasOfficialStats(item: V4PublicScheduleItem): boolean {
  return item.hasOfficialStats === true
}

export function seriesProgressDots(item: V4PublicScheduleItem): boolean[] {
  if (!isScheduleSeries(item)) return []
  const total = Number(item.bestOf)
  if (!Number.isFinite(total) || total <= 1) return []
  const played = item.gamesPlayed != null
    ? Number(item.gamesPlayed)
    : (Number(item.homeWinCount || 0) + Number(item.awayWinCount || 0))
  const filled = Math.max(0, Math.min(total, Number.isFinite(played) ? played : 0))
  return Array.from({ length: total }, (_, index) => index < filled)
}
