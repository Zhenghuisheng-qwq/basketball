import type { OpenAvailableSeasonItemDto, OpenRegistrationDetailDto, OpenRegistrationItemDto } from '@/api/types/openRegistration'
import type { EventRosterMemberDto, EventTeamDetailDto, EventTeamSummaryDto, RosterChangeHistoryItemDto } from '@/api/types/eventParticipation'
import type { RosterChangeRequestItemDto } from '@/api/types/rosterChange'
import type { AvailableSeasonItemDto, TeamRegistrationItemDto } from '@/api/types/teamRegistration'
import type {
  OpenApplication,
  OpenApplicationStatus,
  OpenTournament,
  OpenTournamentStatus,
} from '@/types/account'
import type { AvailableEvent, MyApplication, ParticipatingEvent } from '@/types/accountEvents'
import type { EventTeam, EventTournament, RosterMember, TransferRecord } from '@/types/eventTeam'
import { resolveSeasonRosterLimits } from '@/constants/rosterRules'
import { OPEN_APPLICATION_STATUS_LABELS } from '@/types/account'

export function isRegistrationDeadlinePassed(deadline?: string): boolean {
  if (!deadline) return false
  const normalized = deadline.replace(' ', 'T')
  const deadlineMs = Date.parse(`${normalized.includes('T') ? normalized : `${normalized}T23:59:59`}+08:00`)
  if (Number.isNaN(deadlineMs)) return false
  return Date.now() > deadlineMs
}

export function mapOpenRegistrationStatus(status?: number): OpenApplicationStatus {
  switch (status) {
    case 0: return 'NOT_SUBMITTED'
    case 1:
    case 2: return 'PENDING'
    case 3: return 'APPROVED'
    case 4: return 'REJECTED'
    case 5: return 'NOT_SUBMITTED'
    case 6: return 'CANCELLED'
    default: return 'NOT_SUBMITTED'
  }
}

export function mapOpenSeasonToTournament(season: OpenAvailableSeasonItemDto): OpenTournament {
  const deadline = season.registrationEndTime || ''
  const closed = isRegistrationDeadlinePassed(deadline)
  const status: OpenTournamentStatus = closed ? 'REGISTRATION_CLOSED' : 'REGISTRATION_OPEN'
  return {
    id: season.tournamentId || season.seasonId,
    name: season.tournamentName || season.seasonName || 'Open 赛事',
    shortName: season.seasonName || 'Open',
    seasonName: season.seasonName || '',
    type: 'OPEN',
    platform: 'Xbox',
    registrationType: 'INDIVIDUAL',
    registrationTypeText: '个人报名',
    registrationDeadline: deadline,
    quotaTotal: 0,
    quotaUsed: 0,
    quotaLeft: 0,
    status,
    statusText: status === 'REGISTRATION_OPEN' ? '报名开放' : '报名关闭',
  }
}

export function mapOpenDetailToApplication(detail: OpenRegistrationDetailDto): OpenApplication {
  const status = mapOpenRegistrationStatus(detail.registrationStatus)
  return {
    applicationNo: detail.registrationNo || '',
    tournamentId: detail.seasonId || '',
    tournamentName: detail.tournamentName || detail.seasonName || '',
    applicationType: 'INDIVIDUAL',
    applicationTypeText: '个人报名',
    status,
    statusText: detail.registrationStatusText || OPEN_APPLICATION_STATUS_LABELS[status],
    playerNo: detail.playerNo || '',
    nickname: detail.playerNickname || '',
    platform: detail.platform || '',
    gameId: detail.displayGameId || '',
    submitTime: detail.submittedTime || '',
    reviewTime: '',
    reviewRemark: detail.remark || '',
    participationStatus: status === 'APPROVED' ? 'ACTIVE' : undefined,
    participationStatusText: status === 'APPROVED' ? '正式参赛' : undefined,
  }
}

export function mapOpenItemToApplication(item: OpenRegistrationItemDto): OpenApplication {
  const status = mapOpenRegistrationStatus(item.registrationStatus)
  return {
    applicationNo: item.registrationNo || '',
    tournamentId: item.seasonId || '',
    tournamentName: item.tournamentName || item.seasonName || '',
    applicationType: 'INDIVIDUAL',
    applicationTypeText: '个人报名',
    status,
    statusText: item.registrationStatusText || OPEN_APPLICATION_STATUS_LABELS[status],
    playerNo: '',
    nickname: '',
    platform: item.platform || '',
    gameId: item.displayGameId || '',
    submitTime: item.submittedTime || item.createTime || '',
    reviewTime: '',
    reviewRemark: '',
  }
}

export function mapOpenSeasonToAvailableEvent(
  season: OpenAvailableSeasonItemDto,
  isVerified: boolean,
): AvailableEvent {
  const deadline = season.registrationEndTime || ''
  const closed = isRegistrationDeadlinePassed(deadline)
  let eligibilityStatus: AvailableEvent['eligibilityStatus'] = 'ELIGIBLE'
  let actionType: AvailableEvent['actionType'] = 'INDIVIDUAL_REGISTRATION'
  let actionText = '个人报名'

  if (!isVerified) {
    eligibilityStatus = 'NOT_VERIFIED'
    actionType = 'DISABLED'
    actionText = '需球员认证'
  } else if (closed) {
    eligibilityStatus = 'FULL'
    actionType = 'DISABLED'
    actionText = '已截止'
  } else if (season.hasRegistration) {
    eligibilityStatus = 'APPLIED'
    actionType = 'GO_APPLICATIONS'
    actionText = '查看报名'
  }

  return {
    tournamentId: season.tournamentId || season.seasonId,
    tournamentName: season.tournamentName || season.seasonName || 'Open 赛事',
    platform: '个人',
    registrationType: 'INDIVIDUAL',
    registrationTypeText: '个人报名',
    deadline,
    quotaUsed: 0,
    quotaTotal: 0,
    quotaFull: closed,
    eligibilityStatus,
    actionText,
    actionType,
  }
}

export function mapTeamSeasonToAvailableEvent(
  season: AvailableSeasonItemDto,
  isCaptain: boolean,
): AvailableEvent {
  const deadline = season.registrationEndTime || ''
  const closed = isRegistrationDeadlinePassed(deadline)
  let eligibilityStatus: AvailableEvent['eligibilityStatus'] = 'ELIGIBLE'
  let actionType: AvailableEvent['actionType'] = 'TEAM_REGISTRATION'
  let actionText = '球队报名'

  if (!isCaptain) {
    eligibilityStatus = 'NOT_VERIFIED'
    actionType = 'DISABLED'
    actionText = '需队长权限'
  } else if (closed) {
    eligibilityStatus = 'FULL'
    actionType = 'DISABLED'
    actionText = '已截止'
  } else if (season.hasRegistration) {
    eligibilityStatus = 'APPLIED'
    actionType = 'GO_APPLICATIONS'
    actionText = '查看报名'
  }

  return {
    tournamentId: season.seasonId || season.tournamentId || '',
    seasonId: season.seasonId,
    tournamentName: season.tournamentName || season.seasonName || '联赛赛事',
    platform: season.gameTerminal || season.competitionSeasonName || season.seasonName || '-',
    registrationType: 'TEAM',
    registrationTypeText: '球队报名',
    deadline,
    quotaUsed: 0,
    quotaTotal: resolveSeasonRosterLimits(season.minRosterCount, season.maxRosterCount).maxRosterCount,
    quotaFull: closed,
    eligibilityStatus,
    actionText,
    actionType,
  }
}

export function mapTeamRegistrationToApplication(
  item: TeamRegistrationItemDto,
  isCaptain = true,
): MyApplication {
  const status = String(item.registrationStatus ?? '')
  const isApproved = item.registrationStatus === 3
  const isPending = item.registrationStatus === 2 || item.registrationStatus === 1
  let actionType: MyApplication['actionType'] = 'VIEW_APPLICATION_STATUS'
  let actionText = '查看状态'
  if (item.canEdit) {
    actionType = 'VIEW_TEAM_APPLICATION'
    actionText = '继续报名'
  } else if (isApproved) {
    actionText = '查看状态'
  }
  return {
    applicationId: item.registrationId,
    tournamentId: item.seasonId || item.registrationId,
    tournamentName: item.tournamentName || item.seasonName || '球队报名',
    platform: item.platformName || item.platformCode || '-',
    applicationType: 'TEAM',
    applicationTypeText: '球队报名',
    status,
    statusText: item.registrationStatusText || mapTeamRegistrationStatusText(item.registrationStatus),
    statusHint: isPending
      ? '报名已提交，正在等待后台审核。'
      : isApproved
        ? '报名审核已通过，正式参赛关系请查看「已参加赛事」。'
        : undefined,
    submitTime: item.submittedTime || item.createTime,
    actionText,
    actionType,
    pending: isPending,
    showAction: isCaptain,
    platformGroupLabel: [
      item.tournamentName || item.seasonName,
      item.platformName || item.platformCode ? `${item.platformName || item.platformCode} 组` : null,
      item.registrationStatusText || mapTeamRegistrationStatusText(item.registrationStatus),
    ].filter(Boolean).join(' / '),
  }
}

export function mapOpenRegistrationToApplication(item: OpenRegistrationItemDto): MyApplication {
  const status = mapOpenRegistrationStatus(item.registrationStatus)
  const isApproved = status === 'APPROVED'
  return {
    applicationId: item.registrationId,
    tournamentId: item.seasonId || item.registrationId,
    tournamentName: item.tournamentName || item.seasonName || 'Open 个人报名',
    platform: item.platform || '-',
    applicationType: 'INDIVIDUAL',
    applicationTypeText: '个人报名',
    status,
    statusText: item.registrationStatusText || OPEN_APPLICATION_STATUS_LABELS[status],
    statusHint: isApproved ? '报名审核已通过，正式参赛关系请查看「已参加赛事」。' : undefined,
    submitTime: item.submittedTime || item.createTime,
    actionText: status === 'REJECTED' ? '重新提交' : '查看状态',
    actionType: status === 'REJECTED' ? 'RESUBMIT' : 'VIEW_APPLICATION_STATUS',
    pending: status === 'PENDING',
  }
}

function mapTeamRegistrationStatusText(status?: number): string {
  switch (status) {
    case 0: return '草稿'
    case 1: return '审核中'
    case 2: return '审核中'
    case 3: return '已通过'
    case 4: return '已驳回'
    case 5: return '已撤回'
    case 6: return '已取消'
    default: return status == null ? '未知' : `状态 ${status}`
  }
}

export function formatParticipationIdentityText(rosterRole?: string | null, isCaptain?: boolean): string {
  if (isCaptain || rosterRole?.toUpperCase() === 'CAPTAIN') return '队长'
  if (rosterRole?.toUpperCase() === 'PLAYER') return '球员'
  return rosterRole || '球员'
}

/** 合并正式赛事球队与已通过报名，同一赛季只保留 eventTeam 记录 */
export function mergeParticipatingEvents(
  eventTeams: ParticipatingEvent[],
  approvedRegistrations: ParticipatingEvent[],
): ParticipatingEvent[] {
  const seasonKeys = new Set(eventTeams.map((item) => String(item.tournamentId)))
  const fallback = approvedRegistrations.filter(
    (item) => !seasonKeys.has(String(item.tournamentId)),
  )
  return [...eventTeams, ...fallback]
}

export function mapEventTeamSummaryToParticipating(item: EventTeamSummaryDto): ParticipatingEvent {
  const seasonKey = item.seasonId || item.eventTeamId
  const isCaptain = Boolean(item.isCaptain)
  const identityText = formatParticipationIdentityText(item.rosterRole, isCaptain)
  return {
    relationId: item.eventTeamId,
    tournamentId: seasonKey,
    tournamentName: item.tournamentName || item.seasonName || '赛事',
    platform: item.gameTerminal || '-',
    participationType: 'TEAM',
    participationTypeText: '球队参赛',
    identityType: identityText,
    identityText,
    status: String(item.participationStatus ?? ''),
    statusText: item.participationStatusText || item.rosterLockStatusText || '参赛中',
    actionText: isCaptain ? '管理本届球队' : '查看阵容',
    actionType: isCaptain ? 'MANAGE_EVENT_TEAM' : 'VIEW_EVENT_RELATION',
    actionRoute: isCaptain
      ? `/account/my-tournaments/${seasonKey}/team`
      : `/account/my-tournaments/${seasonKey}/team/roster`,
    primaryAction: isCaptain,
  }
}

export function mapEventTeamDetailToTournament(detail: EventTeamDetailDto): EventTournament {
  const transferOpen = Boolean(detail.transferEnabled)
  const rosterLimits = resolveSeasonRosterLimits(detail.minRosterCount, detail.maxRosterCount)
  return {
    tournamentId: detail.seasonId || detail.tournamentId || '',
    tournamentName: detail.tournamentName || detail.seasonName || '',
    phaseName: detail.participationStatusText || '进行中',
    platform: detail.gameTerminal || '-',
    rosterMin: rosterLimits.minRosterCount,
    rosterMax: rosterLimits.maxRosterCount,
    transferWindowOpen: transferOpen,
    transferWindowEndTime: detail.transferEndTime || '',
    freeAgentQuota: detail.freeAgentQuota ?? 0,
    freeAgentUsed: detail.freeAgentUsed ?? 0,
  }
}

export function mapEventTeamDetailToEventTeam(detail: EventTeamDetailDto): EventTeam {
  const rosterLimits = resolveSeasonRosterLimits(detail.minRosterCount, detail.maxRosterCount)
  return {
    eventTeamId: detail.eventTeamId,
    teamId: detail.teamId || '',
    teamCode: detail.eventTeamNo || detail.shortName || detail.teamName || '',
    teamName: detail.teamName || '',
    editTeamName: detail.teamName || '',
    teamLogoUrl: '',
    permanentTeamNo: detail.teamId || '',
    tournamentId: detail.seasonId || detail.tournamentId || '',
    tournamentName: detail.tournamentName || detail.seasonName || '',
    platform: detail.gameTerminal || '-',
    captainPlayerId: detail.captainPlayerId || '',
    captainUserId: 0,
    captainName: detail.captainNickname || '',
    captainPlayerNo: '',
    rosterCount: detail.rosterCount ?? 0,
    rosterMax: rosterLimits.maxRosterCount,
    rosterMin: rosterLimits.minRosterCount,
    stageName: detail.participationStatusText || '',
    rosterStatus: detail.rosterLockStatus === 1 ? 'LOCKED' : 'ACTIVE',
    rosterStatusText: detail.rosterLockStatusText || '生效中',
    teamProfileStatus: 'APPROVED',
    teamProfileStatusText: '已生效',
    captainPermissionStatus: detail.currentUserIsCaptain ? 'ACTIVE' : 'ACTIVE',
    captainPermissionText: detail.currentUserIsCaptain ? '权限生效中' : '队员',
  }
}

export function mapEventRosterMember(item: EventRosterMemberDto): RosterMember {
  const isCaptain = item.isCaptain === 1 || item.rosterRole === 'CAPTAIN'
  return {
    playerId: item.playerId,
    playerNo: item.playerNo,
    nickname: item.nickname || item.playerNo || item.playerId,
    avatarUrl: '',
    platform: item.platform || '-',
    gameId: item.gameId || '-',
    position: item.position || '-',
    role: isCaptain ? 'CAPTAIN' : 'MEMBER',
    roleText: item.rosterRoleText || (isCaptain ? '赛事队长' : '队员'),
    joinTime: item.effectiveTime || '',
    status: item.rosterStatus === 1 ? 'LOCKED' : 'ACTIVE',
    statusText: item.rosterStatusText || '生效中',
    canRemove: false,
    eligibleForCaptain: !isCaptain,
  }
}

export function mapRosterChangeToTransfer(
  item: RosterChangeRequestItemDto,
  currentEventTeamId: string,
): TransferRecord {
  const isIncoming = item.targetEventTeamId === currentEventTeamId
  const statusMap: Record<number, TransferRecord['status']> = {
    0: 'DRAFT',
    1: 'WAIT_ADMIN_REVIEW',
    2: 'APPROVED',
    3: 'REJECTED',
    4: 'CANCELLED',
  }
  const status = statusMap[item.requestStatus ?? 1] || 'WAIT_ADMIN_REVIEW'
  return {
    transferId: item.requestId,
    type: isIncoming ? 'IN' : 'OUT',
    typeText: item.changeTypeText || (isIncoming ? '转入' : '转出'),
    playerId: item.playerId || '',
    playerName: item.playerId || '球员',
    avatarUrl: '',
    position: '-',
    platform: '-',
    gameId: '-',
    fromTeamCode: item.sourceEventTeamId || '-',
    toTeamCode: item.targetEventTeamId || '-',
    targetCaptainName: '-',
    status,
    statusText: item.requestStatusText || '处理中',
    applyTime: item.submitTime,
    steps: [
      { key: 'submit', text: '已提交', status: 'done', time: item.submitTime },
      { key: 'review', text: '管理员审核', status: status === 'APPROVED' ? 'done' : 'current' },
    ],
  }
}

export function mapRosterHistoryItem(item: RosterChangeHistoryItemDto) {
  return {
    recordId: item.recordId,
    changeType: item.changeType || '',
    changeTypeText: item.changeTypeText || item.changeType || '变更',
    playerId: item.playerId || '-',
    beforeSnapshot: item.beforeSnapshot || '-',
    afterSnapshot: item.afterSnapshot || '-',
    operateReason: item.operateReason || '',
    operatorName: item.operatorName || '-',
    effectiveTime: item.effectiveTime || item.createTime || '',
  }
}
