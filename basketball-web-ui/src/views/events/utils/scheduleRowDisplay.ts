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
} from '@/utils/v4Display'
import { isValidDateKey } from '@/utils/scheduleDateKey'

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

export function isScheduleSeries(item: V4PublicScheduleItem): boolean {
  return item.unitType === 'MATCH_SERIES'
}

export function scheduleTeamName(item: V4PublicScheduleItem, side: 'home' | 'away'): string {
  return v4ScheduleTeamLabel(side === 'home' ? item.homeTeam : item.awayTeam, item.placeholderSource)
}

export function toScheduleTeam(item: V4PublicScheduleItem, side: 'home' | 'away'): Team {
  const eventTeamId = String(side === 'home' ? item.homeEventTeamId || '' : item.awayEventTeamId || '')
  const name = scheduleTeamName(item, side)
  const logo = String(side === 'home' ? item.homeTeamLogoUrl || '' : item.awayTeamLogoUrl || '').trim()
  return {
    id: Number(eventTeamId) || 0,
    teamId: eventTeamId,
    code: name.slice(0, 2).toUpperCase(),
    name,
    shortName: name,
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

export function formatMatchNo(item: V4PublicScheduleItem): string {
  const code = String(item.seriesCode || '').trim()
  return code ? `#${code}` : ''
}

export function formatRoundLine(item: V4PublicScheduleItem): string {
  const raw = String(item.roundName || '').trim()
  const labeled = v4PublicRoundLabel(item.roundName, item.roundNo)
  if (labeled && labeled !== '赛程' && labeled !== raw) {
    return labeled
  }
  if (/^R\d+$/i.test(raw)) {
    const roundNo = Number(raw.slice(1))
    if (roundNo > 0 && ![8, 16, 32].includes(roundNo)) return `第${roundNo}轮`
  }
  return labeled && labeled !== '赛程' ? labeled : ''
}

export function formatStageLine(item: V4PublicScheduleItem): string {
  return String(item.stageName || '').trim()
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
  return stage || round || '赛程'
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
  if (isOfficialForfeitResult(item.status, item.resultType)) return '弃权'
  if (isScheduleSeries(item)) {
    if (isScheduleScheduled(item)) return '未开始'
    if (isScheduleLive(item)) return '进行中'
    if (isScheduleFinished(item)) return '已结束'
    if (isScheduleCancelled(item)) return '已取消'
    return v4ScheduleStatusLabel(item.status, item.statusName)
  }
  if (isScheduleScheduled(item)) return '未开始'
  return v4MatchStatusLabel(item.status)
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
        caption: forfeited ? (item.resultReason || '弃权') : (isScheduleLive(item) ? '进行中' : ''),
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
      leftLabel: item.awayResultLabel || undefined,
      rightLabel: item.homeResultLabel || undefined,
      caption: item.resultReason || '弃权',
    }
  }

  if (isScheduleFinished(item) || isScheduleLive(item)) {
    if (item.homeScore != null && item.awayScore != null) {
      return {
        kind: 'score',
        left: Number(item.awayScore),
        right: Number(item.homeScore),
        caption: isScheduleLive(item) ? '进行中' : '',
      }
    }
    return isScheduleLive(item) ? { kind: 'score', caption: '进行中' } : { kind: 'vs' }
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
