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

export const DISCIPLINE_RECORD_STATUS_OPTIONS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'ACTIVE', label: '生效中' },
  { value: 'REVOKED', label: '已解除' }
]

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