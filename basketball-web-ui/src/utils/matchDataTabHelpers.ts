import type {
  MatchDetailPlayerStat,
  PlayerMatchTaskItem,
  TeamPlayerStatsSubmitReq,
} from '@/api/matchData'
import {
  MATCH_DATA_STATUS,
  MATCH_STATUS_FINISHED,
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

export function isMatchDataEditable(match: PlayerMatchTaskItem | null | undefined): boolean {
  if (!match?.isCaptain) return false
  if (match.matchStatus !== MATCH_STATUS_FINISHED) return false
  if (match.canViewData) return false
  if (match.submissionStatus === SUBMISSION_STATUS_PENDING || match.dataStatus === MATCH_DATA_STATUS.SUBMITTED) {
    return false
  }
  return Boolean(match.canSubmit || match.canResubmit)
}

export function isMatchDataReadOnly(match: PlayerMatchTaskItem | null | undefined): boolean {
  if (!match) return true
  if (match.canViewData) return true
  if (match.submissionStatus === SUBMISSION_STATUS_PENDING || match.dataStatus === MATCH_DATA_STATUS.SUBMITTED) {
    return true
  }
  return false
}

export function resolveMatchDataTabMessage(match: PlayerMatchTaskItem | null | undefined): string {
  if (!match) return '-'

  const finished = match.matchStatus === MATCH_STATUS_FINISHED
  const isCaptain = Boolean(match.isCaptain)

  if (!finished) {
    return '比赛尚未开始，比赛结束后由赛事队长提交本队球员比赛数据。'
  }
  if (match.canViewData) {
    return '比赛数据已审核通过，当前数据已进入正式统计。'
  }
  if (match.submissionStatus === SUBMISSION_STATUS_PENDING || match.dataStatus === MATCH_DATA_STATUS.SUBMITTED) {
    return '比赛数据已提交，当前由管理员审核中，审核期间不能修改。'
  }
  if (match.submissionStatus === SUBMISSION_STATUS_REJECTED || match.dataStatus === MATCH_DATA_STATUS.REJECTED) {
    return isCaptain
      ? '比赛数据审核未通过，请根据管理员备注修改后重新提交。'
      : '比赛数据审核未通过，请等待赛事队长重新提交。'
  }
  if (finished && isCaptain) {
    return '比赛已结束，请赛事队长录入本队球员数据并提交审核。'
  }
  if (finished) {
    return '普通队员仅可查看比赛数据，数据由赛事队长提交。'
  }
  return '比赛数据尚未提交。'
}

export function resolveCaptainPermissionHint(match: PlayerMatchTaskItem | null | undefined): string {
  if (!match) return ''
  if (match.matchStatus !== MATCH_STATUS_FINISHED) {
    return '比赛结束后开放数据提交。'
  }
  if (match.isCaptain) {
    return '你是本队赛事队长，可在比赛结束后提交本队球员数据。'
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
