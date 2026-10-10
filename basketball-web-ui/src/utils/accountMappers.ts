import type { AccountCenterOverview, AccountSummaryDto, PlayerIdentityDto, PendingTaskDto } from '@/api/types/accountCenter'
import type { AccountProfileDto } from '@/api/types/accountProfile'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'
import type { MessageItemDto, MessageTeamInviteInfoDto } from '@/api/types/accountMessage'
import type { PlayerCertCurrentDto } from '@/api/types/playerCertification'
import type {
  PlatformAccountItemDto,
  PlatformChangeHistoryItemDto,
  PlayerEligibilityStatusDto,
} from '@/api/types/playerPlatformAccount'
import type {
  AccountProfile,
  AccountRole,
  CurrentAccount,
  EligibilityLimit,
  GamePlatform,
  GamePosition,
  IdentityChangeRecord,
  IdentityPlatform,
  MyTournament,
  MyTournamentStatus,
  PlatformGameIdRecord,
  PlatformGameIdStatus,
  PlayerVerification,
  PlayerVerificationStatus,
} from '@/types/account'
import type {
  AccountNotificationItem,
  AccountNotificationResultStatus,
  AccountNotificationTeamInvite,
  AccountNotificationType,
} from '@/types/accountNotifications'
import { buildPlayerIdentity } from '@/utils/playerIdentity'

const PLAYER_CERT_APPROVED = 3
const PLAYER_CERT_DRAFT = 0

export function normalizePendingTaskPath(task: PendingTaskDto): string {
  const path = task.actionPath || ''
  if (!path.includes('/player/certification')) {
    return path
  }
  switch (task.taskType) {
    case 'CERT_REVIEWING':
      return '/account/player-identity/pending'
    case 'CERT_REJECTED':
      return '/account/player-identity/rejected'
    case 'CERT_DRAFT':
    case 'NO_CERT':
    default:
      return '/account/player-identity/apply'
  }
}

export function mapPlatformAccountItem(item: PlatformAccountItemDto): PlatformGameIdRecord {
  const platform = (item.platform || 'PC') as IdentityPlatform
  let status: PlatformGameIdStatus
  let statusText = item.accountStatusText || '未绑定'

  if (item.pendingChange) {
    status = 'PENDING'
    statusText = item.accountStatus == null ? '新增审核中' : '修改审核中'
  } else if (item.accountStatus == null) {
    status = 'UNBOUND'
    statusText = '未绑定'
  } else if (item.accountStatus === 1) {
    status = 'EFFECTIVE'
  } else if (item.accountStatus === 0) {
    status = 'PENDING'
  } else {
    status = 'UNBOUND'
  }

  return {
    platform,
    platformAccountId: item.platformAccountId,
    gameId: item.displayGameId || '',
    status,
    pendingKind: item.pendingChange ? (item.accountStatus == null ? 'ADD' : 'EDIT') : undefined,
    statusText,
    locked: false,
    lockText: '',
  }
}

export function mapPlatformChangeHistoryItem(item: PlatformChangeHistoryItemDto): IdentityChangeRecord {
  const platformCode = (item.platformCode || item.platform || 'PC').toUpperCase()
  const platform = (platformCode === 'PS5' || platformCode === 'PSN' ? 'PS' : platformCode) as IdentityPlatform
  const oldGameId = item.oldGameId ?? item.oldDisplayGameId
  const newGameId = item.newGameId ?? item.newDisplayGameId ?? '-'
  return {
    effectiveTime: toIsoDateTime(item.effectiveTime),
    platform,
    changeType: item.changeType,
    changeTypeName: item.changeTypeName || resolveChangeTypeName(item.changeType),
    oldGameId: oldGameId ? oldGameId : '—',
    newGameId,
  }
}

function resolveChangeTypeName(changeType?: string): string {
  if (changeType === 'INITIAL_BIND') return '首次绑定'
  if (changeType === 'UPDATE_GAME_ID' || changeType === 'CHANGE_GAME_ID') return '修改游戏 ID'
  if (changeType === 'ADD_PLATFORM') return '首次绑定'
  return changeType || '变更'
}

export function mapEligibilityStatus(dto: PlayerEligibilityStatusDto): EligibilityLimit {
  const limited = dto.eligible === false
  return {
    playerNo: dto.playerNo || '',
    status: limited ? 'LIMITED' : 'OK',
    statusText: dto.playerStatusText || (limited ? '参赛资格受限' : '参赛资格正常'),
    tournamentName: '',
    permanentIdentityStatus: 'KEPT',
    resultTitle: limited ? '参赛资格受限' : '',
    resultStatus: dto.playerStatusText || '',
    reason: dto.notice || '',
    notice: dto.notice || '',
  }
}


const PLAYER_CERT_SUBMITTED = 1
const PLAYER_CERT_REVIEWING = 2
const PLAYER_CERT_REJECTED = 4
const PLAYER_CERT_CANCELLED = 5

export function toIsoDateTime(value?: string | Date | null): string {
  if (!value) return ''
  if (typeof value === 'string') return value
  return value.toISOString()
}

export function mapCertApplicationStatus(
  applicationStatus?: number | null,
  certificationStatus?: number | null,
): PlayerVerificationStatus {
  if (certificationStatus === PLAYER_CERT_APPROVED) return 'APPROVED'
  if (applicationStatus == null) return 'NOT_APPLIED'
  if (applicationStatus === PLAYER_CERT_DRAFT) {
    return 'NOT_APPLIED'
  }
  if (applicationStatus === PLAYER_CERT_CANCELLED) {
    return 'WITHDRAWN'
  }
  if (applicationStatus === PLAYER_CERT_SUBMITTED || applicationStatus === PLAYER_CERT_REVIEWING) {
    return 'PENDING'
  }
  if (applicationStatus === PLAYER_CERT_REJECTED) return 'REJECTED'
  if (applicationStatus === PLAYER_CERT_APPROVED) return 'APPROVED'
  return 'NOT_APPLIED'
}

export function mapGameTerminalToPlatform(terminal?: string): GamePlatform {
  const normalized = (terminal || '').toUpperCase()
  if (normalized.includes('PS')) return 'PS5'
  if (normalized.includes('XBOX')) return 'XBOX'
  if (normalized.includes('PC')) return 'PC'
  if (normalized.includes('MOBILE')) return 'MOBILE'
  return 'PC'
}

export function mapPlatformToGameTerminal(platform: string): string {
  const normalized = platform.toUpperCase()
  if (normalized === 'PS' || normalized === 'PS5') return 'PS5'
  if (normalized === 'XBOX') return 'XBOX'
  if (normalized === 'MOBILE') return 'MOBILE'
  return 'PC'
}

function mapAccountRole(certificationStatus?: number | null): AccountRole {
  if (certificationStatus === PLAYER_CERT_APPROVED) return 'PLAYER'
  return 'BASIC_USER'
}

export function mapSummaryToCurrentAccount(
  summary: AccountSummaryDto,
  playerIdentity?: PlayerIdentityDto | null,
  unreadCount = 0,
): CurrentAccount {
  const verificationStatus = mapCertApplicationStatus(
    playerIdentity?.applicationStatus,
    playerIdentity?.certificationStatus,
  )
  return {
    userId: Number(summary.accountId) || 0,
    userNo: summary.accountNo,
    playerNo: playerIdentity?.playerNo || '',
    nickname: summary.nickname,
    email: summary.emailMasked,
    maskedEmail: summary.emailMasked,
    avatarUrl: summary.avatarUrl || '',
    accountRole: mapAccountRole(playerIdentity?.certificationStatus),
    emailVerified: Boolean(summary.emailVerified),
    playerVerificationStatus: verificationStatus,
    unreadNotificationCount: unreadCount,
    createdAt: toIsoDateTime(summary.registerTime),
    lastLoginAt: toIsoDateTime(summary.lastLoginTime),
  }
}

export function mapProfileDtoToAccountProfile(dto: AccountProfileDto): AccountProfile {
  return {
    userId: Number(dto.accountId) || 0,
    nickname: dto.nickname,
    userNo: dto.accountNo,
    email: dto.emailMasked,
    maskedEmail: dto.emailMasked,
    avatarUrl: dto.avatarUrl || '',
    emailVerified: Boolean(dto.emailVerified),
    createdAt: toIsoDateTime(dto.registerTime),
    hasPendingChange: Boolean(dto.hasPendingChange),
    pendingChange: dto.pendingChange || null,
    latestRejectedChange: dto.latestRejectedChange || null,
  }
}

export function mapPlayerCertToVerification(
  cert: PlayerCertCurrentDto,
  userId: number,
): PlayerVerification {
  const screenshot = cert.materials?.find((item) =>
    item.materialType?.toUpperCase().includes('GAME_SCREENSHOT'),
  )
  const position = (cert.positionCode || 'PG') as GamePosition
  return {
    id: cert.currentApplicationId || '',
    userId,
    status: mapCertApplicationStatus(cert.applicationStatus, cert.certificationStatus),
    primaryPlatform: mapGameTerminalToPlatform(cert.gameTerminal),
    gameId: cert.gameAccount || '',
    gameIdScreenshotUrl: screenshot?.accessUrl || '',
    gamePosition: position,
    commonPosition: position,
    backupContact: '',
    submittedAt: toIsoDateTime(cert.submittedTime),
    reviewedAt: toIsoDateTime(cert.reviewedTime),
    effectiveAt: cert.certificationStatus === PLAYER_CERT_APPROVED ? toIsoDateTime(cert.reviewedTime) : '',
    reviewerComment: cert.rejectReason || '',
    rejectedFields: [],
    applicationNo: cert.applicationNo || '',
  }
}

export function mapPlayerIdentityFromOverview(
  playerIdentity: PlayerIdentityDto | null | undefined,
  userId: number,
  nickname: string,
  eligibilityLimited: boolean,
) {
  if (!playerIdentity) return null
  const verification: PlayerVerification = {
    id: '',
    userId,
    status: mapCertApplicationStatus(playerIdentity.applicationStatus, playerIdentity.certificationStatus),
    primaryPlatform: mapGameTerminalToPlatform(playerIdentity.gameTerminal),
    gameId: playerIdentity.gameAccount || '',
    gameIdScreenshotUrl: '',
    gamePosition: (playerIdentity.positionCode || 'PG') as GamePosition,
    commonPosition: (playerIdentity.positionCode || 'PG') as GamePosition,
    backupContact: '',
    submittedAt: toIsoDateTime(playerIdentity.submittedTime),
    reviewedAt: toIsoDateTime(playerIdentity.reviewedTime),
    effectiveAt: toIsoDateTime(playerIdentity.certifiedTime),
    reviewerComment: playerIdentity.reviewRemark || '',
    rejectedFields: [],
    applicationNo: playerIdentity.applicationNo || '',
  }
  return buildPlayerIdentity(verification, nickname, eligibilityLimited)
}

export type TeamRegistrationStatusCode =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'REVIEWING'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'CANCELLED'

export function mapTeamRegistrationStatus(status?: number | null): TeamRegistrationStatusCode {
  if (status == null) return 'DRAFT'
  if (status === 0) return 'DRAFT'
  if (status === 1) return 'SUBMITTED'
  if (status === 2) return 'REVIEWING'
  if (status === 3) return 'APPROVED'
  if (status === 4) return 'REJECTED'
  if (status === 5) return 'WITHDRAWN'
  if (status === 6) return 'CANCELLED'
  return 'DRAFT'
}

export function mapTeamRegistrationStatusText(status?: number | null): string {
  if (status == null) return '未知'
  if (status === 1) return '审核中'
  const mapped = mapTeamRegistrationStatus(status)
  const labels: Record<TeamRegistrationStatusCode, string> = {
    DRAFT: '待提交',
    SUBMITTED: '审核中',
    REVIEWING: '审核中',
    APPROVED: '已通过',
    REJECTED: '未通过',
    WITHDRAWN: '已撤回',
    CANCELLED: '已取消',
  }
  return labels[mapped]
}

function mapRegistrationStatus(status?: number): MyTournamentStatus {
  if (status == null) return 'NOT_JOINED'
  const teamStatus = mapTeamRegistrationStatus(status)
  if (teamStatus === 'DRAFT' || teamStatus === 'REVIEWING' || teamStatus === 'SUBMITTED') {
    return 'WAITING_REGISTRATION'
  }
  if (teamStatus === 'APPROVED') return 'JOINED'
  if (teamStatus === 'REJECTED' || teamStatus === 'WITHDRAWN' || teamStatus === 'CANCELLED') {
    return 'NOT_JOINED'
  }
  return 'NOT_JOINED'
}

function mapRegistrationStatusText(status?: number): string {
  return mapTeamRegistrationStatusText(status)
}

export function mapTournamentItemToMyTournament(item: AccountTournamentItemDto): MyTournament {
  return {
    id: item.registrationId,
    name: item.tournamentName,
    type: item.seasonName || '',
    period: item.seasonName || '',
    status: mapRegistrationStatus(item.registrationStatus),
    statusText: mapRegistrationStatusText(item.registrationStatus),
    teamName: item.teamName || '',
    role: item.rosterRole || '',
    eligibility: item.validationStatus === 2 ? 'LIMITED' : 'OK',
  }
}

function mapMessageType(messageType?: string, bizType?: string): AccountNotificationType {
  const biz = (bizType || '').toUpperCase()
  if (biz === 'TEAM_INVITE') return 'TEAM_INVITE'
  const normalized = (messageType || '').toUpperCase()
  if (normalized.includes('TEAM')) return 'TEAM_INVITE'
  if (normalized.includes('CERT') || normalized.includes('VERIFY')) return 'PLAYER_VERIFY'
  if (normalized.includes('SECURITY')) return 'ACCOUNT_SECURITY'
  if (normalized.includes('REGIST') || normalized.includes('TOURNAMENT')) return 'REGISTRATION'
  if (normalized.includes('MATCH')) return 'MATCH_DATA'
  return 'SYSTEM'
}

function mapTeamInviteDto(invite: MessageTeamInviteInfoDto): AccountNotificationTeamInvite {
  return {
    inviteId: invite.inviteId,
    teamId: invite.teamId,
    teamName: invite.teamName,
    teamLogo: invite.teamLogo,
    inviteStatus: invite.inviteStatus,
    inviteStatusText: invite.inviteStatusText,
    canRespond: invite.canRespond,
    canAccept: invite.canAccept,
    cannotAcceptReason: invite.cannotAcceptReason,
    invitationMessage: invite.invitationMessage,
    inviterName: invite.inviterName,
    inviteTime: invite.inviteTime,
    expireTime: invite.expireTime,
  }
}

function mapTeamInvitePresentation(invite: AccountNotificationTeamInvite): {
  resultText: string
  resultStatus: AccountNotificationResultStatus
} {
  const teamName = invite.teamName || '该球队'
  if (invite.inviteStatus === 1) {
    return { resultText: `您已加入球队「${teamName}」`, resultStatus: 'SUCCESS' }
  }
  if (invite.inviteStatus === 2) {
    return { resultText: `您已拒绝球队「${teamName}」的邀请`, resultStatus: 'FAILED' }
  }
  if (invite.inviteStatus === 3) {
    return { resultText: '该邀请已过期', resultStatus: 'WARNING' }
  }
  if (invite.inviteStatus === 4) {
    return { resultText: '该邀请已被队长撤销', resultStatus: 'WARNING' }
  }
  if (invite.canRespond && invite.canAccept === false && invite.cannotAcceptReason) {
    return { resultText: invite.cannotAcceptReason, resultStatus: 'WARNING' }
  }
  if (invite.canRespond) {
    return { resultText: '待处理', resultStatus: 'PENDING' }
  }
  return { resultText: invite.inviteStatusText || '待处理', resultStatus: 'WARNING' }
}

function buildTeamInviteMeta(invite: AccountNotificationTeamInvite) {
  return [
    { label: '邀请球队', value: invite.teamName || '-' },
    { label: '邀请说明', value: invite.invitationMessage || '邀请加入球队' },
    { label: '邀请人', value: invite.inviterName || '-' },
    { label: '邀请时间', value: toIsoDateTime(invite.inviteTime) || '-' },
    { label: '过期时间', value: toIsoDateTime(invite.expireTime) || '-' },
    { label: '邀请状态', value: invite.inviteStatusText || '-' },
  ]
}

export function mapMessageItemToNotification(item: MessageItemDto): AccountNotificationItem {
  const unread = item.readStatus !== 1
  const type = mapMessageType(item.messageType, item.bizType)
  const teamInvite = item.teamInvite ? mapTeamInviteDto(item.teamInvite) : undefined
  const invitePresentation = teamInvite ? mapTeamInvitePresentation(teamInvite) : null

  return {
    id: item.receiverId,
    type,
    typeText: type === 'TEAM_INVITE' ? '球队邀请' : (item.messageType || '系统'),
    title: item.title,
    summary: (item.content || '').slice(0, 80),
    status: unread ? 'UNREAD' : 'READ',
    isUnread: unread,
    createdAt: toIsoDateTime(item.sentTime),
    detailTitle: item.title,
    detailSubtitle: teamInvite?.teamName ? `来自球队「${teamInvite.teamName}」的邀请` : (item.content || ''),
    body: item.content ? item.content.split('\n').filter(Boolean) : [],
    meta: teamInvite
      ? buildTeamInviteMeta(teamInvite)
      : [
          ...(item.bizType ? [{ label: '业务类型', value: item.bizType }] : []),
          ...(item.bizId ? [{ label: '关联业务', value: item.bizId }] : []),
        ],
    resultText: invitePresentation?.resultText ?? (unread ? '未读' : '已读'),
    resultStatus: invitePresentation?.resultStatus ?? (unread ? 'PENDING' : 'SUCCESS'),
    teamInvite,
  }
}

export function applyOverviewToStoreState(
  overview: AccountCenterOverview,
  eligibilityLimited: boolean,
) {
  const unreadCount = Number(overview.unreadMessageCount || 0)
  const currentAccount = mapSummaryToCurrentAccount(
    overview.accountSummary,
    overview.playerIdentity,
    unreadCount,
  )
  const profile = mapProfileDtoToAccountProfile({
    ...overview.accountSummary,
    emailMasked: overview.accountSummary.emailMasked,
  })
  const verificationStatus = mapCertApplicationStatus(
    overview.playerIdentity?.applicationStatus,
    overview.playerIdentity?.certificationStatus,
  )
  const verification: PlayerVerification | null = overview.playerIdentity
    ? {
        id: '',
        userId: currentAccount.userId,
        status: verificationStatus,
        primaryPlatform: mapGameTerminalToPlatform(overview.playerIdentity.gameTerminal),
        gameId: overview.playerIdentity.gameAccount || '',
        gameIdScreenshotUrl: '',
        gamePosition: (overview.playerIdentity.positionCode || 'PG') as GamePosition,
        commonPosition: (overview.playerIdentity.positionCode || 'PG') as GamePosition,
        backupContact: '',
        submittedAt: toIsoDateTime(overview.playerIdentity.submittedTime),
        reviewedAt: toIsoDateTime(overview.playerIdentity.reviewedTime),
        effectiveAt: toIsoDateTime(overview.playerIdentity.certifiedTime),
        reviewerComment: overview.playerIdentity.reviewRemark || '',
        rejectedFields: [],
        applicationNo: overview.playerIdentity.applicationNo || '',
      }
    : null

  const myTournaments = (overview.currentTournaments || []).map(mapTournamentItemToMyTournament)
  const pendingTasks = (overview.pendingTasks || []).map((task) => ({
    ...task,
    actionPath: normalizePendingTaskPath(task),
  }))
  const recentMessages = (overview.recentMessages || []).map(mapMessageItemToNotification)

  return {
    currentAccount,
    profile,
    verification,
    myTournaments,
    pendingTasks,
    recentMessages,
    unreadCount,
    playerIdentity: verification
      ? buildPlayerIdentity(verification, currentAccount.nickname, eligibilityLimited)
      : null,
  }
}
