import type { AccountCenterOverview, AccountSummaryDto, PlayerIdentityDto } from '@/api/types/accountCenter'
import type { AccountProfileDto } from '@/api/types/accountProfile'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'
import type { MessageItemDto } from '@/api/types/accountMessage'
import type { PlayerCertCurrentDto } from '@/api/types/playerCertification'
import type {
  AccountProfile,
  AccountRole,
  CurrentAccount,
  GamePlatform,
  GamePosition,
  MyTournament,
  MyTournamentStatus,
  PlayerVerification,
  PlayerVerificationStatus,
} from '@/types/account'
import type { AccountNotificationItem, AccountNotificationType } from '@/types/accountNotifications'
import { buildPlayerIdentity } from '@/mock/playerIdentityMock'

const PLAYER_CERT_APPROVED = 3
const PLAYER_CERT_DRAFT = 0
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
  if (applicationStatus === PLAYER_CERT_DRAFT || applicationStatus === PLAYER_CERT_CANCELLED) {
    return 'NOT_APPLIED'
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
    backupContact: cert.contactMobile || '',
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
  const mapped = mapTeamRegistrationStatus(status)
  const labels: Record<TeamRegistrationStatusCode, string> = {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
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
  if (teamStatus === 'DRAFT' || teamStatus === 'SUBMITTED' || teamStatus === 'REVIEWING') {
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

function mapMessageType(messageType?: string): AccountNotificationType {
  const normalized = (messageType || '').toUpperCase()
  if (normalized.includes('TEAM')) return 'TEAM_INVITE'
  if (normalized.includes('CERT') || normalized.includes('VERIFY')) return 'PLAYER_VERIFY'
  if (normalized.includes('SECURITY')) return 'ACCOUNT_SECURITY'
  if (normalized.includes('REGIST') || normalized.includes('TOURNAMENT')) return 'REGISTRATION'
  if (normalized.includes('MATCH')) return 'MATCH_DATA'
  return 'SYSTEM'
}

function mapRedirectRoute(redirectType?: string, redirectValue?: string): string {
  if (!redirectType || !redirectValue) return '/account/notifications'
  const normalized = redirectType.toUpperCase()
  if (normalized.includes('PLAYER') || normalized.includes('CERT')) return '/account/player-identity'
  if (normalized.includes('TOURNAMENT')) return `/account/my-tournaments`
  if (redirectValue.startsWith('/')) return redirectValue
  return '/account/notifications'
}

export function mapMessageItemToNotification(item: MessageItemDto): AccountNotificationItem {
  const unread = item.readStatus !== 1
  const type = mapMessageType(item.messageType)
  const primaryRoute = mapRedirectRoute(item.redirectType, item.redirectValue)
  return {
    id: item.receiverId,
    type,
    typeText: item.messageType || '系统',
    title: item.title,
    summary: (item.content || '').slice(0, 80),
    status: unread ? 'UNREAD' : 'READ',
    isUnread: unread,
    createdAt: toIsoDateTime(item.sentTime),
    detailTitle: item.title,
    detailSubtitle: item.content || '',
    body: item.content ? item.content.split('\n').filter(Boolean) : [],
    meta: [
      ...(item.bizType ? [{ label: '业务类型', value: item.bizType }] : []),
      ...(item.bizId ? [{ label: '关联业务', value: item.bizId }] : []),
    ],
    resultText: unread ? '未读' : '已读',
    resultStatus: unread ? 'PENDING' : 'SUCCESS',
    primaryActionText: item.redirectType ? '查看详情' : '返回通知列表',
    primaryActionRoute: primaryRoute,
    secondaryActionText: '返回通知列表',
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
  const pendingTasks = overview.pendingTasks || []
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
