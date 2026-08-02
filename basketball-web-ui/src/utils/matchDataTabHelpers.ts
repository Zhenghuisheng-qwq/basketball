import type {
  MatchDetailPlayerStat,
  PlayerMatchTaskItem,
  TeamPlayerStatsSubmitReq,
} from '@/api/matchData'
import {
  MATCH_DATA_STATUS,
  SUBMISSION_STATUS_PENDING,
  SUBMISSION_STATUS_REJECTED,
} from '@/utils/participationMappers'

export type MatchTeamSide = 'HOME' | 'AWAY'

export interface EditablePlayerStatRow extends MatchDetailPlayerStat {
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
}

const STAT_FIELDS = ['points', 'rebounds', 'assists', 'steals', 'blocks', 'turnovers', 'fouls'] as const

const MATCH_STATUS_CANCELLED = 5
const MATCH_STATUS_POSTPONED = 4

export function resolveScheduledStartTime(match: PlayerMatchTaskItem | null | undefined): Date | null {
  if (!match) return null
  if (match.scheduledStartTime) {
    const parsed = new Date(match.scheduledStartTime)
    if (!Number.isNaN(parsed.getTime())) return parsed
  }
  if (match.matchDate && match.matchTime) {
    const timePart = match.matchTime.length === 5 ? `${match.matchTime}:00` : match.matchTime
    const parsed = new Date(`${match.matchDate.replace(/-/g, '/')} ${timePart}`)
    if (!Number.isNaN(parsed.getTime())) return parsed
  }
  if (match.matchDate) {
    const parsed = new Date(`${match.matchDate.replace(/-/g, '/')} 00:00:00`)
    if (!Number.isNaN(parsed.getTime())) return parsed
  }
  return null
}

export function isMatchTimePassed(match: PlayerMatchTaskItem | null | undefined): boolean {
  if (!match) return false
  if (match.isMatchTimePassed != null) return match.isMatchTimePassed
  const scheduled = resolveScheduledStartTime(match)
  if (!scheduled) return false
  return Date.now() >= scheduled.getTime()
}

export function isMatchCancelled(match: PlayerMatchTaskItem | null | undefined): boolean {
  return match?.matchStatus === MATCH_STATUS_CANCELLED
}

export function isMatchPostponed(match: PlayerMatchTaskItem | null | undefined): boolean {
  return match?.matchStatus === MATCH_STATUS_POSTPONED
}

export function shouldDisplayMatchAsFinished(match: PlayerMatchTaskItem | null | undefined): boolean {
  if (!match || isMatchCancelled(match) || isMatchPostponed(match)) return false
  if (match.matchStatus === 3) return true
  return match.matchStatus === 1 && isMatchTimePassed(match)
}

export function resolveMatchStatusDisplayText(match: PlayerMatchTaskItem | null | undefined): string {
  if (!match) return '-'
  if (match.matchStatusText && shouldDisplayMatchAsFinished(match)) {
    return match.matchStatusText
  }
  if (shouldDisplayMatchAsFinished(match)) return '已结束'
  return match.matchStatusText || '-'
}

export function formatMatchScheduleText(match: PlayerMatchTaskItem | null | undefined): string {
  if (!match) return '-'
  const parts = [match.matchDate, match.weekday, match.matchTime].filter(Boolean)
  return parts.length ? parts.join(' ') : '-'
}

export function resolveMyTeamSide(match: PlayerMatchTaskItem | null | undefined): MatchTeamSide | null {
  if (!match?.myEventTeamId) return null
  if (match.myEventTeamId === match.homeEventTeamId) return 'HOME'
  if (match.myEventTeamId === match.awayEventTeamId) return 'AWAY'
  return null
}

export function resolveMyTeamId(match: PlayerMatchTaskItem | null | undefined): string | undefined {
  const side = resolveMyTeamSide(match)
  if (side === 'HOME') return match?.homeTeamId
  if (side === 'AWAY') return match?.awayTeamId
  return match?.myTeamId
}

export function canSubmitMatchData(match: PlayerMatchTaskItem | null | undefined): boolean {
  if (!match) return false
  if (match.canSubmitMatchData != null) return match.canSubmitMatchData
  if (!match.isCaptain) return false
  if (!isMatchTimePassed(match)) return false
  if (isMatchCancelled(match)) return false
  if (isMatchPostponed(match)) return false
  if (match.canViewData) return false
  if (match.submissionStatus === SUBMISSION_STATUS_PENDING) {
    return false
  }
  if (match.dataStatus === MATCH_DATA_STATUS.CONFIRMED) return false
  return Boolean(match.canSubmit || match.canResubmit)
}

export function isMatchDataEditable(match: PlayerMatchTaskItem | null | undefined): boolean {
  return canSubmitMatchData(match)
}

export function isMatchDataReadOnly(match: PlayerMatchTaskItem | null | undefined): boolean {
  if (!match) return true
  if (match.canViewData) return true
  if (match.submissionStatus === SUBMISSION_STATUS_PENDING) {
    return true
  }
  return !canSubmitMatchData(match)
}

export function resolveSubmitDisabledReason(match: PlayerMatchTaskItem | null | undefined): string {
  if (!match) return '-'
  if (match.submitDisabledReason) return match.submitDisabledReason
  if (!match.isCaptain) {
    return '普通队员仅可查看比赛数据，比赛数据由赛事队长提交。'
  }
  if (!isMatchTimePassed(match)) {
    return '比赛开始后开放数据提交。'
  }
  if (isMatchCancelled(match)) {
    return '比赛已取消，不能提交比赛数据。'
  }
  if (isMatchPostponed(match)) {
    return '比赛已延期，按新的比赛时间开放提交。'
  }
  if (match.submissionStatus === SUBMISSION_STATUS_PENDING) {
    return '比赛数据已提交，正在等待管理员审核。'
  }
  if (match.canViewData || match.dataStatus === MATCH_DATA_STATUS.CONFIRMED) {
    return '比赛数据已审核通过，不能再次提交。'
  }
  if (match.submissionStatus === SUBMISSION_STATUS_REJECTED || match.dataStatus === MATCH_DATA_STATUS.REJECTED) {
    return match.canResubmit
      ? ''
      : '仅驳回后可重新提交。'
  }
  return '当前暂不能提交比赛数据。'
}

export function resolveMatchDataTabMessage(match: PlayerMatchTaskItem | null | undefined): string {
  if (!match) return '-'

  const timePassed = isMatchTimePassed(match)
  const isCaptain = Boolean(match.isCaptain)

  if (isMatchCancelled(match)) {
    return '比赛已取消，不能提交比赛数据。'
  }
  if (isMatchPostponed(match)) {
    return '比赛已延期，按新的比赛时间开放提交。'
  }
  if (match.canViewData) {
    return '比赛数据已审核通过，当前数据已进入正式统计。'
  }
  if (match.submissionStatus === SUBMISSION_STATUS_PENDING) {
    return '比赛数据已提交，当前由管理员审核中，审核期间不能修改。'
  }
  if (match.submissionStatus === SUBMISSION_STATUS_REJECTED || match.dataStatus === MATCH_DATA_STATUS.REJECTED) {
    return isCaptain
      ? '比赛数据审核未通过，请根据管理员备注修改后重新提交。'
      : '比赛数据审核未通过，请等待赛事队长重新提交。'
  }
  if (timePassed && isCaptain) {
    return '比赛时间已过，赛事队长可提交本队比赛数据。'
  }
  if (timePassed) {
    return '普通队员仅可查看比赛数据，数据由赛事队长提交。'
  }
  if (isCaptain) {
    return '比赛开始后开放数据提交。'
  }
  return '普通队员仅可查看比赛数据，数据由赛事队长提交。'
}

export function resolveCaptainPermissionHint(match: PlayerMatchTaskItem | null | undefined): string {
  if (!match) return ''
  if (!isMatchTimePassed(match)) {
    return '比赛开始后开放数据提交。'
  }
  if (match.isCaptain) {
    return '比赛时间已过，赛事队长可提交本队比赛数据。'
  }
  return '普通队员仅可查看比赛数据，数据由赛事队长提交。'
}

export function cloneEditableStats(rows?: MatchDetailPlayerStat[]): EditablePlayerStatRow[] {
  return (rows || []).map((row) => ({
    ...row,
    points: normalizeStatValue(row.points),
    rebounds: normalizeStatValue(row.rebounds),
    assists: normalizeStatValue(row.assists),
    steals: normalizeStatValue(row.steals),
    blocks: normalizeStatValue(row.blocks),
    turnovers: normalizeStatValue(row.turnovers),
    fouls: normalizeStatValue(row.fouls),
  }))
}

export function normalizeStatValue(value?: number | null): number {
  if (value == null || Number.isNaN(value)) return 0
  return Math.max(0, Math.floor(value))
}

export function sanitizeStatInput(raw: string | number, max = 999): number {
  const text = String(raw ?? '').trim()
  if (!text) return 0
  const parsed = Number.parseInt(text, 10)
  if (Number.isNaN(parsed) || parsed < 0) return 0
  return Math.min(parsed, max)
}

export function validateEditableStats(rows: EditablePlayerStatRow[]): string | null {
  if (!rows.length) return '本队暂无出场球员'
  for (const row of rows) {
    if (!row.playerId) {
      return `${row.playerName || row.playerNo || '球员'} 缺少球员标识，无法提交`
    }
    for (const field of STAT_FIELDS) {
      const value = row[field]
      if (!Number.isInteger(value) || value < 0) {
        return `${row.playerName || row.playerNo || '球员'} 的 ${field} 数据无效`
      }
    }
    if (row.fouls > 6) {
      return `${row.playerName || row.playerNo || '球员'} 的犯规数不能超过 6`
    }
  }
  return null
}

export function buildTeamPlayerStatsPayload(
  match: PlayerMatchTaskItem,
  rows: EditablePlayerStatRow[],
): TeamPlayerStatsSubmitReq {
  const submitterTeamId = resolveMyTeamId(match)
  if (!submitterTeamId) {
    throw new Error('无法识别本队球队')
  }
  const playerStats = rows.map((row) => {
    if (!row.playerId) {
      throw new Error(`${row.playerName || row.playerNo || '球员'} 缺少球员标识，无法提交`)
    }
    return {
      playerId: row.playerId,
      rosterId: row.rosterId,
      points: row.points,
      rebounds: row.rebounds,
      assists: row.assists,
      steals: row.steals,
      blocks: row.blocks,
      turnovers: row.turnovers,
      fouls: row.fouls,
    }
  })
  return { submitterTeamId, playerStats }
}
