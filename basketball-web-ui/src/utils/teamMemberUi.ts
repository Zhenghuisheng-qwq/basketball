import { POSITION_LABELS } from '@/constants/rosterRules'
import type { TeamMemberItemDto } from '@/api/types/teamMember'
import { formatDate } from '@/utils/formatDate'

export function formatTeamMemberRole(roleType?: string): string {
  if (roleType === 'CAPTAIN') return '队长'
  if (roleType === 'MEMBER') return '队员'
  return roleType || '-'
}

export function formatTeamMemberRosterType(rosterType?: string): string {
  if (rosterType === 'OFFICIAL') return '正式'
  if (rosterType === 'TRIAL') return '试训'
  return rosterType || '-'
}

export function formatTeamMemberMembershipStatus(status?: number, rosterType?: string): string {
  if (status === 0) return '已离队'
  if (status === 2) return '已移除'
  if (status === 1) return formatTeamMemberRosterType(rosterType)
  return '-'
}

export function formatTeamMemberPosition(member: Pick<TeamMemberItemDto, 'positionCode' | 'positionName'>): string {
  if (!member.positionCode) return '未设置'
  const label = member.positionName || POSITION_LABELS[member.positionCode]
  return label ? `${member.positionCode} ${label}` : member.positionCode
}

export function formatTeamMemberPositionShort(code?: string): string {
  return code || '未设置'
}

export function formatTeamMemberJoinTime(value?: string): string {
  if (!value) return '-'
  return formatDate(value)
}

export function resolveTeamMemberDisplayName(member: TeamMemberItemDto): string {
  return member.nickname || member.gameNickname || member.realName || '未命名球员'
}

export function resolveTeamMemberAvatarFallback(member: TeamMemberItemDto): string {
  const name = resolveTeamMemberDisplayName(member)
  if (name && name !== '未命名球员') return name.slice(0, 1).toUpperCase()
  const tail = member.playerNo?.replace(/\D/g, '').slice(-2)
  return tail || '?'
}

export function isTeamCaptainMember(member: TeamMemberItemDto): boolean {
  return member.roleType === 'CAPTAIN'
}

export const TEAM_MEMBER_PLATFORM_COLUMNS = ['PS', 'PC', 'XBOX'] as const

export type TeamMemberPlatformCode = (typeof TEAM_MEMBER_PLATFORM_COLUMNS)[number]

function normalizePlatformCode(code?: string): string {
  return (code || '').trim().toUpperCase()
}

function matchesPlatformCode(itemCode: string, target: TeamMemberPlatformCode): boolean {
  if (target === 'PS') return itemCode === 'PS' || itemCode === 'PS5'
  if (target === 'XBOX') return itemCode === 'XBOX' || itemCode.startsWith('XBOX')
  return itemCode === target
}

export function resolveMemberPlatformGameId(
  member: Pick<TeamMemberItemDto, 'platformGameIds' | 'gameIdSummary'>,
  platformCode: TeamMemberPlatformCode,
): string {
  const matched = (member.platformGameIds || []).find((item) =>
    matchesPlatformCode(normalizePlatformCode(item.platformCode), platformCode),
  )
  if (matched?.gameId) return matched.gameId

  const summary = member.gameIdSummary?.trim()
  if (!summary || summary === '未绑定') return '-'

  for (const part of summary.split(/\s*\/\s*/)) {
    const match = part.trim().match(/^(PS5?|PC|XBOX(?:\sONE)?)\s*[:：]\s*(.+)$/i)
    if (!match) continue
    const code = normalizePlatformCode(match[1].replace(/\s+/g, ''))
    if (matchesPlatformCode(code, platformCode)) {
      return match[2].trim() || '-'
    }
  }

  return '-'
}
