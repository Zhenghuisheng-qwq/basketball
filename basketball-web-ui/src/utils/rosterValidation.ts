import {
  CNPA_MAX_ROSTER_COUNT,
  CNPA_MIN_BENCH_COUNT,
  CNPA_MIN_ROSTER_COUNT,
  CNPA_REQUIRED_STARTER_COUNT,
  STARTING_POSITION_CODES,
  formatPositionLabel,
} from '@/constants/rosterRules'

export interface RosterValidationMember {
  teamMemberId: string
  positionCode?: string | null
  certificationStatus?: number | null
  isStarting?: number | null
}

export interface RosterPositionStats {
  PG: number
  SG: number
  SF: number
  PF: number
  C: number
}

export interface RosterValidationStats {
  selectedCount: number
  starterCount: number
  benchCount: number
  starterPositions: RosterPositionStats
  minCount: number
  maxCount: number
  minBenchCount: number
}

export interface RosterValidationResult {
  valid: boolean
  errors: string[]
  stats: RosterValidationStats
}

const APPROVED_CERT_STATUS = 3

export function normalizePositionCode(code?: string | null): string {
  return code?.trim().toUpperCase() || ''
}

export function buildRosterStats(members: RosterValidationMember[]): RosterValidationStats {
  const starters = members.filter((item) => item.isStarting === 1)
  const starterPositions: RosterPositionStats = { PG: 0, SG: 0, SF: 0, PF: 0, C: 0 }
  for (const starter of starters) {
    const code = normalizePositionCode(starter.positionCode)
    if (code in starterPositions) {
      starterPositions[code as keyof RosterPositionStats] += 1
    }
  }
  return {
    selectedCount: members.length,
    starterCount: starters.length,
    benchCount: members.length - starters.length,
    starterPositions,
    minCount: CNPA_MIN_ROSTER_COUNT,
    maxCount: CNPA_MAX_ROSTER_COUNT,
    minBenchCount: CNPA_MIN_BENCH_COUNT,
  }
}

export function validateRegistrationRoster(members: RosterValidationMember[]): RosterValidationResult {
  const errors: string[] = []
  const stats = buildRosterStats(members)

  if (stats.selectedCount < CNPA_MIN_ROSTER_COUNT) {
    errors.push(`阵容人数不足，至少需要 ${CNPA_MIN_ROSTER_COUNT} 人`)
  }
  if (stats.selectedCount > CNPA_MAX_ROSTER_COUNT) {
    errors.push(`阵容人数超出上限，最多 ${CNPA_MAX_ROSTER_COUNT} 人`)
  }
  if (stats.starterCount !== CNPA_REQUIRED_STARTER_COUNT) {
    errors.push(`首发必须为 ${CNPA_REQUIRED_STARTER_COUNT} 人`)
  }
  if (stats.benchCount < CNPA_MIN_BENCH_COUNT) {
    errors.push(`替补至少需要 ${CNPA_MIN_BENCH_COUNT} 人`)
  }

  for (const member of members) {
    if (member.certificationStatus !== APPROVED_CERT_STATUS) {
      errors.push('阵容中存在未认证球员，无法提交')
      break
    }
  }

  for (const member of members) {
    if (!normalizePositionCode(member.positionCode)) {
      errors.push('阵容中存在未设置位置的球员，请先完善球员资料')
      break
    }
  }

  for (const code of STARTING_POSITION_CODES) {
    const count = stats.starterPositions[code]
    if (count > 1) {
      errors.push(`${formatPositionLabel(code)} 首发只能有 1 人`)
    }
    if (count === 0 && stats.starterCount > 0) {
      errors.push(`缺少首发位置：${formatPositionLabel(code)}`)
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    stats,
  }
}

export function getMemberEligibilityStatus(member: {
  certificationStatus?: number | null
  positionCode?: string | null
}): { text: string; type: 'success' | 'warning' | 'danger' | 'info' } {
  if (member.certificationStatus !== APPROVED_CERT_STATUS) {
    return { text: '未认证', type: 'danger' }
  }
  if (!normalizePositionCode(member.positionCode)) {
    return { text: '资料不完整', type: 'warning' }
  }
  return { text: '正常', type: 'success' }
}
