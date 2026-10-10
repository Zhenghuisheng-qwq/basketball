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
import type { PlayerMatchTaskItem } from '@/api/matchData'
import type { AvailableEvent, MyApplication, ParticipatingEvent } from '@/types/accountEvents'
import type { EventTeam, EventTournament, RosterMember, TransferRecord } from '@/types/eventTeam'
import { translate } from '@/i18n'
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
    divisionId: season.tournamentId || season.seasonId,
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
    divisionId: season.seasonId || season.tournamentId || '',
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

export function formatPlatformList(platforms?: string[]) {
  if (!platforms?.length) return '-'
  return platforms.join(' / ')
}

export function formatDateTimeShort(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

export function formatEventStandardDisplay(season: {
  competitionSeasonName?: string
  seasonName?: string
  tournamentName?: string
  platformName?: string
  platformCode?: string
  gameTerminal?: string
}) {
  const seasonLabel = season.competitionSeasonName || season.seasonName || ''
  const platformLabel = season.platformName
    || formatPlatformDisplay(season.platformCode || season.gameTerminal)
  const tournamentLabel = season.tournamentName || ''
  return [seasonLabel, platformLabel ? `${platformLabel}端` : '', tournamentLabel]
    .filter(Boolean)
    .join(' / ')
}

export function mapMyEventsSeasonToAvailableEvent(season: AvailableSeasonItemDto): AvailableEvent {
  const actionCode = season.actionCode || season.operationType || 'DISABLED'
  const actionType = resolveMyEventsActionType(actionCode, season.canRegister)
  const closed = season.registrationStatus === 'CLOSED' || season.registrationStatus === 'ENDED'
  const platformDisplay = season.platformName
    || (season.gameTerminal ? formatPlatformDisplay(season.gameTerminal) : '')
    || formatPlatformList(season.openedPlatforms)
    || '-'
  const competitionSeasonName = season.competitionSeasonName || season.seasonName
  const tournamentName = season.tournamentName || season.eventTypeName || season.operationalSeasonName || '联赛赛事'
  return {
    divisionId: season.tournamentId || season.seasonId || '',
    seasonId: season.seasonId || season.operationalSeasonId,
    operationalSeasonId: season.operationalSeasonId || season.seasonId,
    tournamentName,
    seasonName: competitionSeasonName,
    competitionSeasonName,
    displayTitle: formatEventStandardDisplay({
      competitionSeasonName,
      seasonName: season.seasonName,
      tournamentName,
      platformName: season.platformName,
      platformCode: season.platformCode,
      gameTerminal: season.gameTerminal,
    }),
    platform: platformDisplay,
    platformCode: season.platformCode,
    openedPlatforms: season.openedPlatforms,
    registrationType: 'TEAM',
    registrationTypeText: '球队报名',
    signupStartTime: season.signupStartTime || season.registrationStartTime,
    deadline: season.signupEndTime || season.registrationEndTime || '',
    matchStartTime: season.matchStartTime,
    matchEndTime: season.matchEndTime,
    quotaUsed: season.registeredPlatforms?.length ?? 0,
    quotaTotal: season.openedPlatforms?.length ?? 0,
    quotaFull: closed,
    eligibilityStatus: season.canRegister ? 'ELIGIBLE' : 'NOT_VERIFIED',
    actionText: season.actionText || '暂不可报名',
    actionType,
    actionCode,
    operationType: season.operationType || actionCode,
    disabledReason: season.disabledReason || season.cannotRegisterReason,
    canRegister: season.canRegister,
    teamName: season.userTeamName || season.teamName,
    userMembershipStatusText: season.userMembershipStatusText,
    teamRegistrationStatusText: season.teamRegistrationStatusName || season.teamRegistrationStatusText,
    userRosterStatusText: season.myRosterStatusName || season.userRosterStatusText,
    captainGrantStatusText: season.captainGrantStatusText,
    registrationStatus: season.registrationStatus,
    eventStatus: season.eventStatusName || season.eventStatus,
    eventStatusCode: season.eventStatus,
    availableRegisterPlatforms: season.availableRegisterPlatforms,
    registrationId: season.registrationId,
    canRemindCaptain: season.canRemindCaptain,
    remindedRecently: season.remindedRecently,
    isCaptain: season.isCaptain,
    hasCaptainGrant: season.hasCaptainGrant ?? season.hasActiveCaptainGrantForEvent,
  }
}

function formatPlatformDisplay(code?: string) {
  if (!code) return ''
  const normalized = code.trim().toUpperCase()
  if (normalized === 'XBOX') return 'Xbox'
  if (normalized === 'PS' || normalized === 'PS5') return 'PS'
  return normalized
}

function resolveMyEventsActionType(actionCode: string, canRegister?: boolean): AvailableEvent['actionType'] {
  switch (actionCode) {
    case 'JOIN_TEAM':
      return 'JOIN_TEAM'
    case 'CONTACT_CAPTAIN':
      return 'CONTACT_CAPTAIN'
    case 'REMIND_CAPTAIN':
      return 'REMIND_CAPTAIN'
    case 'REGISTER':
      return canRegister ? 'TEAM_REGISTRATION' : 'DISABLED'
    case 'RE_REGISTER':
      return canRegister ? 'RE_REGISTER' : 'DISABLED'
    case 'REGISTERED':
    case 'VIEW_REGISTRATION':
    case 'PENDING_REVIEW':
      return 'VIEW_REGISTRATION'
    case 'MANAGE_REGISTRATION':
      return 'MANAGE_REGISTRATION'
    case 'VIEW_ROSTER':
      return 'VIEW_ROSTER'
    case 'ROSTER_NOT_SELECTED':
      return 'ROSTER_NOT_SELECTED'
    case 'GRANT_EXPIRED':
    case 'NO_GRANT':
    case 'NOT_STARTED':
    case 'CLOSED':
    default:
      return 'DISABLED'
  }
}

export function mapTeamEventRecordToApplication(item: import('@/api/types/myEvents').TeamEventRecordItemDto): MyApplication {
  const status = String(item.registrationStatus ?? '')
  const isApproved = item.registrationStatus === 3
  const isPending = item.registrationStatus === 2 || item.registrationStatus === 1
  return {
    applicationId: item.registrationId,
    divisionId: item.seasonId || item.registrationId,
    tournamentName: item.tournamentName || item.seasonName || '球队报名',
    platform: '-',
    applicationType: 'TEAM',
    applicationTypeText: '球队报名',
    status,
    statusText: item.registrationStatusText || mapTeamRegistrationStatusText(item.registrationStatus),
    statusHint: isPending
      ? '报名已提交，正在等待后台审核。'
      : isApproved
        ? '报名审核已通过，正式参赛关系请查看「我的阵容」。'
        : item.rejectReason,
    submitTime: item.submitTime,
    actionText: '查看状态',
    actionType: 'VIEW_APPLICATION_STATUS',
    pending: isPending,
    showAction: true,
    platformGroupLabel: [item.tournamentName || item.seasonName, item.rosterStatusText, item.registrationStatusText]
      .filter(Boolean)
      .join(' / '),
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
    divisionId: item.seasonId || item.registrationId,
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
      item.platformName || item.platformCode || null,
      item.registrationStatusText || mapTeamRegistrationStatusText(item.registrationStatus),
    ].filter(Boolean).join(' / '),
  }
}

export function mapOpenRegistrationToApplication(item: OpenRegistrationItemDto): MyApplication {
  const status = mapOpenRegistrationStatus(item.registrationStatus)
  const isApproved = status === 'APPROVED'
  return {
    applicationId: item.registrationId,
    divisionId: item.seasonId || item.registrationId,
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
    case 0: return translate('accountEvents.common.awaitingSubmit')
    case 1: return translate('accountEvents.common.pendingReview')
    case 2: return translate('accountEvents.common.pendingReview')
    case 3: return translate('accountEvents.common.approved')
    case 4: return translate('accountEvents.common.rejected')
    case 5: return translate('accountEvents.common.withdrawn')
    case 6: return translate('accountEvents.common.cancelled')
    default: return status == null
      ? translate('accountEvents.block.unknownStatus')
      : translate('accountEvents.block.statusCode', { status })
  }
}

export function formatParticipationIdentityText(rosterRole?: string | null, isCaptain?: boolean): string {
  if (isCaptain || rosterRole?.toUpperCase() === 'CAPTAIN') return translate('accountEvents.common.captainRole')
  if (rosterRole?.toUpperCase() === 'PLAYER') return translate('accountEvents.common.memberRole')
  return rosterRole || translate('accountEvents.common.memberRole')
}

export const MATCH_STATUS_FINISHED = 3
export const MATCH_DATA_STATUS = {
  DRAFT: 0,
  SUBMITTED: 1,
  CONFIRMED: 2,
  REJECTED: 4,
} as const
export const SUBMISSION_STATUS_PENDING = 1
export const SUBMISSION_STATUS_REJECTED = 3

export function buildMatchDataListRoute(eventTeamId: string) {
  return `/account/my-events/${eventTeamId}/matches`
}

export function buildMatchDetailRoute(eventTeamId: string, matchId: string) {
  return `/account/my-events/${eventTeamId}/matches/${matchId}`
}

export function buildLegacyMatchDataListRoute(seasonId: string) {
  return `/account/my-tournaments/${seasonId}/team/match-data`
}

export function buildMatchDataSubmitRoute(seasonId: string, matchId: string, resubmit = false) {
  const action = resubmit ? 'resubmit' : 'submit'
  return `/account/my-tournaments/${seasonId}/team/match-data/${matchId}/${action}`
}

export function buildMatchDataStatusRoute(seasonId: string, matchId: string) {
  return `/account/my-tournaments/${seasonId}/team/match-data/${matchId}/status`
}

export function buildMatchDataDisputeRoute(seasonId: string, matchId: string) {
  return `/account/my-tournaments/${seasonId}/team/match-data/${matchId}/dispute`
}

export function buildMatchDataExceptionRoute(seasonId: string, matchId: string) {
  return `/account/my-tournaments/${seasonId}/team/match-data/${matchId}/exception`
}

export function isMatchDataEntryEligible(rosterLocked: boolean, _matchTotal = 0): boolean {
  return rosterLocked
}

function countCaptainSubmitTasks(matches: PlayerMatchTaskItem[]) {
  return matches.filter((item) => item.canSubmitMatchData || item.canSubmit || item.canResubmit).length
}

function resolveCaptainSubmitRoute(eventTeamId: string, seasonId: string, matches: PlayerMatchTaskItem[]) {
  const submitTargets = matches.filter((item) => item.canSubmitMatchData || item.canSubmit || item.canResubmit)
  if (submitTargets.length !== 1 || !submitTargets[0].matchId) {
    return buildMatchDataListRoute(eventTeamId)
  }
  return buildMatchDataSubmitRoute(seasonId, submitTargets[0].matchId, Boolean(submitTargets[0].canResubmit))
}

export function deriveMatchDataEntry(
  eventTeamId: string,
  seasonId: string,
  matches: PlayerMatchTaskItem[],
  _isCaptain: boolean,
  rosterLocked: boolean,
): Pick<
  ParticipatingEvent,
  | 'showMatchDataEntry'
  | 'matchDataActionText'
  | 'matchDataActionRoute'
  | 'matchDataActionType'
  | 'matchDataPrimary'
  | 'pendingTaskCount'
  | 'pendingTaskText'
> {
  const matchTotal = matches.length
  if (!isMatchDataEntryEligible(rosterLocked, matchTotal)) {
    return { showMatchDataEntry: false }
  }

  const listRoute = buildMatchDataListRoute(eventTeamId)
  const submitCount = countCaptainSubmitTasks(matches)

  return {
    showMatchDataEntry: true,
    matchDataActionText: '我的比赛',
    matchDataActionRoute: submitCount === 1
      ? resolveCaptainSubmitRoute(eventTeamId, seasonId, matches)
      : listRoute,
    matchDataActionType: submitCount > 0 ? 'SUBMIT_MATCH_DATA' : 'VIEW_MATCH_DATA_LIST',
    matchDataPrimary: submitCount > 0,
    pendingTaskCount: submitCount > 0 ? submitCount : undefined,
    pendingTaskText: submitCount > 0 ? `${submitCount} 场待提交比赛数据` : undefined,
  }
}

/** 合并正式赛事球队与已通过报名，同一赛季只保留 eventTeam 记录 */
export function mergeParticipatingEvents(
  eventTeams: ParticipatingEvent[],
  approvedRegistrations: ParticipatingEvent[],
): ParticipatingEvent[] {
  const seasonKeys = new Set(eventTeams.map((item) => String(item.divisionId)))
  const fallback = approvedRegistrations.filter(
    (item) => !seasonKeys.has(String(item.divisionId)),
  )
  return [...eventTeams, ...fallback]
}

export function mapEventTeamSummaryToParticipating(item: EventTeamSummaryDto): ParticipatingEvent {
  const seasonKey = item.seasonId || item.eventTeamId
  const isCaptain = Boolean(item.isCaptain)
  const identityText = formatParticipationIdentityText(item.rosterRole, isCaptain)
  return {
    relationId: item.eventTeamId,
    eventTeamId: item.eventTeamId,
    divisionId: seasonKey,
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
    isCaptain,
    rosterLocked: item.rosterLockStatus === 1,
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
    avatarUrl: item.avatarUrl?.trim() || '',
    platform: item.platform || '-',
    gameId: item.gameId || '未绑定',
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
