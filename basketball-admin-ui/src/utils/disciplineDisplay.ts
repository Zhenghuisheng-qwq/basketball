/** 处罚管理展示辅助（纯展示，不改业务 code） */

export const DISCIPLINE_TARGET_TYPE_OPTIONS = [
  { value: 'PLAYER', label: '球员' },
  { value: 'PERMANENT_TEAM', label: '历史球队' },
  { value: 'EVENT_TEAM', label: '赛事球队' }
]

export const DISCIPLINE_TYPE_OPTIONS = [
  { value: 'DISCIPLINE', label: '纪律处罚' }
]

/** 与 DisciplinePenaltyCodeEnum + Service.applyPenaltyCode 对齐的可执行措施 */
export const DISCIPLINE_PENALTY_OPTIONS = [
  { value: 'WARNING', label: '警告', requires: [] as string[] },
  { value: 'MATCH_SUSPENSION', label: '禁赛（按场次）', requires: ['matchSuspensionCount'] },
  { value: 'DATE_RANGE_SUSPENSION', label: '禁赛（按日期）', requires: ['effectiveRange'] },
  { value: 'POINT_DEDUCTION', label: '扣分', requires: ['pointDeduction'] },
  { value: 'REGISTRATION_BAN', label: '禁止报名', requires: [] as string[] },
  { value: 'TRANSFER_BAN', label: '禁止转会', requires: [] as string[] },
  { value: 'MATCH_ELIGIBILITY_CANCEL', label: '取消比赛资格', requires: ['matchSuspensionCount'] },
  { value: 'EVENT_DISQUALIFICATION', label: '取消赛事资格', requires: [] as string[] },
  { value: 'TEAM_SUSPENSION', label: '球队停赛', requires: [] as string[] },
  { value: 'PLAYER_QUALIFICATION_SUSPENSION', label: '球员资格停赛', requires: [] as string[] },
  { value: 'CAPTAIN_BAN', label: '禁止担任队长', requires: [] as string[] }
]

/** 与 DisciplinePenaltyTargetCompatibilityPolicy 对齐。ACCOUNT 本轮全部 CREATE DISABLED。 */
export const DISCIPLINE_PENALTY_TARGET_MATRIX: Record<string, string[]> = {
  WARNING: ['PLAYER', 'PERMANENT_TEAM', 'EVENT_TEAM'],
  MATCH_SUSPENSION: ['PLAYER'],
  DATE_RANGE_SUSPENSION: ['PLAYER'],
  POINT_DEDUCTION: ['EVENT_TEAM'],
  REGISTRATION_BAN: ['PLAYER'],
  TRANSFER_BAN: ['PLAYER'],
  MATCH_ELIGIBILITY_CANCEL: [],
  EVENT_DISQUALIFICATION: ['EVENT_TEAM'],
  TEAM_SUSPENSION: [],
  PLAYER_QUALIFICATION_SUSPENSION: [],
  CAPTAIN_BAN: ['PLAYER']
}

export function isPenaltyTargetCompatible(penaltyCode?: string, targetType?: string) {
  if (!penaltyCode || !targetType) return false
  return (DISCIPLINE_PENALTY_TARGET_MATRIX[penaltyCode] || []).includes(targetType)
}

export function getCreatablePenaltyOptions(targetType?: string) {
  if (!targetType) return []
  return DISCIPLINE_PENALTY_OPTIONS.filter(item => isPenaltyTargetCompatible(item.value, targetType))
}

export const DISCIPLINE_RECORD_STATUS_OPTIONS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'ACTIVE', label: '生效中' },
  { value: 'REVOKED', label: '已解除' },
  { value: 'EXPIRED', label: '已过期' }
]

export const DISCIPLINE_EXECUTION_STATUS_LABELS: Record<string, string> = {
  PENDING: '待执行',
  PROCESSING: '执行中',
  EFFECTIVE: '已生效',
  PARTIAL: '部分完成',
  FAILED: '执行失败',
  SKIPPED: '无需执行',
  REVOKED: '已撤销执行'
}

export function formatDisciplineExecutionStatus(value?: string) {
  if (!value) return '-'
  return DISCIPLINE_EXECUTION_STATUS_LABELS[value] || value
}

const TARGET_TYPE_LABELS = Object.fromEntries(DISCIPLINE_TARGET_TYPE_OPTIONS.map(i => [i.value, i.label]))
const DISCIPLINE_TYPE_LABELS = Object.fromEntries(DISCIPLINE_TYPE_OPTIONS.map(i => [i.value, i.label]))
const PENALTY_CODE_LABELS = Object.fromEntries(DISCIPLINE_PENALTY_OPTIONS.map(i => [i.value, i.label]))
const RECORD_STATUS_LABELS = Object.fromEntries(DISCIPLINE_RECORD_STATUS_OPTIONS.map(i => [i.value, i.label]))

const SEASON_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '未开始',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名截止',
  PLAYING: '进行中',
  FINISHED: '已结束',
  ARCHIVED: '已归档'
}

export function formatDisciplineTargetType(value?: string) {
  if (!value) return '-'
  return TARGET_TYPE_LABELS[value] || value
}

export function formatDisciplineType(value?: string) {
  if (!value) return '-'
  return DISCIPLINE_TYPE_LABELS[value] || value
}

export function formatDisciplinePenaltyCode(value?: string) {
  if (!value) return '-'
  return PENALTY_CODE_LABELS[value] || value
}

export function formatDisciplineRecordStatus(value?: string) {
  if (!value) return '-'
  return RECORD_STATUS_LABELS[value] || value
}

export function formatSeasonStatus(value?: string) {
  if (!value) return ''
  return SEASON_STATUS_LABELS[value] || value
}

export function formatDisciplineSeasonLabel(row: { seriesName?: string; seasonName?: string; seasonId?: number | string }) {
  if (row.seriesName && row.seasonName) return `${row.seriesName} · ${row.seasonName}`
  if (row.seasonName) return row.seasonName
  if (row.seasonId != null) return `赛季 #${row.seasonId}`
  return '-'
}

export function formatDisciplineDivisionLabel(row: { divisionName?: string; divisionId?: number | string }) {
  if (row.divisionName) return row.divisionName
  if (row.divisionId != null) return `Division #${row.divisionId}`
  return '-'
}

export function formatDisciplineBusinessStatus(row: { businessStatus?: string; recordStatus?: string; effectiveEndTime?: string }) {
  const status = row.businessStatus || row.recordStatus
  if (status === 'EXPIRED') return '已过期'
  return formatDisciplineRecordStatus(status)
}

export function formatAnnouncementStatus(row: { announcementStatusName?: string; announcementId?: number | string; announcementStatus?: number }) {
  if (row.announcementStatusName) return row.announcementStatusName
  if (row.announcementStatus == null) return '未生成'
  if (row.announcementStatus === 1) return '已发布'
  if (row.announcementStatus === 2) return '已下线'
  if (row.announcementStatus === 0) return '草稿'
  return '未生成'
}

export function formatEventImpactSummary(row: {
  executionMatchForfeitCount?: number
  executionSeriesForfeitCount?: number
  executionQualificationRemovedCount?: number
  executionWarningCount?: number
}) {
  const match = row.executionMatchForfeitCount ?? 0
  const series = row.executionSeriesForfeitCount ?? 0
  const qualification = row.executionQualificationRemovedCount ?? 0
  const warning = row.executionWarningCount ?? 0
  if (!match && !series && !qualification && !warning) return '-'
  return `比赛判负 ${match} / 系列赛判负 ${series} / 资格移除 ${qualification} / 警告 ${warning}`
}

export function formatReasonSummary(reason?: string) {
  const text = (reason || '').trim()
  if (!text) return '-'
  return text.length > 24 ? `${text.slice(0, 24)}…` : text
}

export function executionStatusTagType(status?: string) {
  if (status === 'EFFECTIVE') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PARTIAL') return 'warning'
  if (status === 'PROCESSING' || status === 'PENDING') return 'info'
  return ''
}

export function businessStatusTagType(status?: string) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'REVOKED' || status === 'EXPIRED') return 'info'
  if (status === 'DRAFT') return 'warning'
  return ''
}

export function formatDisciplineTargetName(row: {
  targetType?: string
  playerNo?: string
  playerName?: string
  playerId?: number | string
  teamName?: string
  teamId?: number | string
  eventTeamName?: string
  eventTeamId?: number | string
}) {
  if (row.targetType === 'PLAYER') {
    if (row.playerNo && row.playerName) return `${row.playerNo} · ${row.playerName}`
    if (row.playerNo) return row.playerNo
    if (row.playerName) return row.playerName
    return row.playerId != null ? `球员 #${row.playerId}` : '-'
  }
  if (row.targetType === 'PERMANENT_TEAM') {
    return row.teamName || (row.teamId != null ? `球队 #${row.teamId}` : '-')
  }
  if (row.targetType === 'EVENT_TEAM') {
    return row.eventTeamName || row.teamName || (row.eventTeamId != null ? `赛事球队 #${row.eventTeamId}` : '-')
  }
  return '-'
}

export function showsMatchSuspension(penaltyCode?: string) {
  return penaltyCode === 'MATCH_SUSPENSION' || penaltyCode === 'MATCH_ELIGIBILITY_CANCEL'
}

export function showsPointDeduction(penaltyCode?: string) {
  return penaltyCode === 'POINT_DEDUCTION'
}

export function showsDateRange(penaltyCode?: string) {
  return penaltyCode === 'DATE_RANGE_SUSPENSION'
}

export function formatGameModeLabel(gameMode?: string) {
  if (!gameMode) return ''
  if (gameMode === 'FIVE_V_FIVE') return '5V5'
  if (gameMode === 'THREE_V_THREE') return '3V3'
  return gameMode
}