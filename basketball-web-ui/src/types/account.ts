export type AccountRole = 'BASIC_USER' | 'PLAYER' | 'CAPTAIN'

export type PlayerVerificationStatus =
  | 'NOT_APPLIED'
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'CHANGE_PENDING'

/** 球员身份业务状态（与认证状态映射，含赛事资格受限） */
export type PlayerIdentityStatus =
  | 'NOT_SUBMITTED'
  | 'PENDING'
  | 'REJECTED'
  | 'APPROVED'
  | 'ELIGIBILITY_LIMITED'

export type IdentityPlatform = 'PC' | 'PS' | 'XBOX'

export type PlatformGameIdStatus = 'EFFECTIVE' | 'PENDING' | 'UNBOUND'

export type GamePlatform = 'PC' | 'PS5' | 'XBOX' | 'MOBILE'

export type GamePosition = 'PG' | 'SG' | 'SF' | 'PF' | 'C'

export interface CurrentAccount {
  userId: number
  userNo: string
  playerNo: string
  nickname: string
  email: string
  maskedEmail: string
  avatarUrl: string
  accountRole: AccountRole
  emailVerified: boolean
  playerVerificationStatus: PlayerVerificationStatus
  unreadNotificationCount: number
  createdAt: string
  lastLoginAt: string
}

export interface AccountProfile {
  userId: number
  nickname: string
  userNo: string
  email: string
  maskedEmail: string
  avatarUrl: string
  emailVerified: boolean
  createdAt: string
}

export interface PlayerVerificationSnapshot {
  primaryPlatform: GamePlatform
  gameId: string
  gameIdScreenshotUrl: string
  gamePosition: GamePosition
  commonPosition: GamePosition
  backupContact: string
}

export interface PlayerVerification extends PlayerVerificationSnapshot {
  id: string
  userId: number
  status: PlayerVerificationStatus
  submittedAt: string
  reviewedAt: string
  effectiveAt: string
  reviewerComment: string
  rejectedFields: string[]
  applicationNo: string
  currentEffectiveData?: PlayerVerificationSnapshot
  pendingChangeData?: PlayerVerificationSnapshot
}

export type NotificationType = 'VERIFICATION' | 'TEAM' | 'EVENT' | 'SYSTEM' | 'SECURITY'

export type NotificationActionType =
  | 'VIEW_VERIFICATION'
  | 'VIEW_PLAYER_IDENTITY'
  | 'VIEW_PLATFORMS'
  | 'VIEW_ELIGIBILITY'
  | 'VIEW_MY_TOURNAMENTS'
  | 'VIEW_OPEN_APPLICATION'
  | 'RESUBMIT_OPEN_APPLICATION'
  | 'RESUBMIT_VERIFICATION'
  | 'HANDLE_TEAM_INVITATION'
  | 'VIEW_EVENT'
  | 'NONE'

export interface PlayerIdentity {
  status: PlayerIdentityStatus
  playerNo: string
  playerName: string
  avatarUrl: string
  platform: IdentityPlatform | ''
  gameId: string
  position: GamePosition | ''
  proofImageUrl: string
  submitTime: string
  reviewTime: string
  effectiveTime: string
  reviewRemark: string
  permanent: boolean
  eligibilityLimited: boolean
}

export interface PlayerApplication {
  applicationNo: string
  status: 'PENDING' | 'REJECTED' | 'APPROVED'
  playerName: string
  platform: IdentityPlatform
  gameId: string
  position: GamePosition
  proofCount: number
  submitTime: string
  reviewTime: string
  reviewRemark: string
  rejectedFields?: string[]
}

export interface PlatformGameIdRecord {
  platform: IdentityPlatform
  gameId: string
  status: PlatformGameIdStatus
  statusText: string
  locked: boolean
  lockText: string
}

export interface IdentityChangeRecord {
  effectiveTime: string
  platform: IdentityPlatform
  oldGameId: string
  newGameId: string
}

export interface EligibilityLimit {
  playerNo: string
  status: 'LIMITED' | 'OK'
  statusText: string
  tournamentName: string
  permanentIdentityStatus: string
  resultTitle: string
  resultStatus: string
  reason: string
  notice: string
}

export type MyTournamentStatus =
  | 'NOT_JOINED'
  | 'INVITED'
  | 'JOINED'
  | 'WAITING_REGISTRATION'
  | 'LIMITED'
  | 'ENDED'

export type MyTournamentEligibility = 'NEED_PLAYER_ID' | 'OK' | 'LIMITED'

export interface MyTournament {
  id: string
  name: string
  type?: string
  period?: string
  status: MyTournamentStatus
  statusText: string
  teamName: string
  role: string
  eligibility: MyTournamentEligibility
  inviteTime?: string
  inviteExpires?: string
  invitationId?: string
  openRegistrationStatus?: OpenApplicationStatus
}

export type OpenTournamentStatus = 'REGISTRATION_OPEN' | 'REGISTRATION_CLOSED'

export type OpenApplicationStatus =
  | 'NOT_SUBMITTED'
  | 'PENDING'
  | 'REJECTED'
  | 'APPROVED'
  | 'CANCELLED'
  | 'CLOSED'

export interface OpenTournament {
  id: string
  name: string
  shortName: string
  seasonName: string
  type: 'OPEN'
  platform: string
  registrationType: 'INDIVIDUAL'
  registrationTypeText: string
  registrationDeadline: string
  quotaTotal: number
  quotaUsed: number
  quotaLeft: number
  status: OpenTournamentStatus
  statusText: string
}

export interface CertifiedPlayerForOpen {
  userNo: string
  playerNo: string
  nickname: string
  avatarUrl: string
  identityStatus: 'APPROVED'
  identityStatusText: string
  platform: string
  gameId: string
}

export interface OpenApplication {
  applicationNo: string
  tournamentId: string
  tournamentName: string
  applicationType: 'INDIVIDUAL'
  applicationTypeText: string
  status: OpenApplicationStatus
  statusText: string
  playerNo: string
  nickname: string
  platform: string
  gameId: string
  submitTime: string
  reviewTime: string
  reviewRemark: string
  participationStatus?: 'ACTIVE'
  participationStatusText?: string
  groupingStatusText?: string
}

export const OPEN_APPLICATION_STATUS_LABELS: Record<OpenApplicationStatus, string> = {
  NOT_SUBMITTED: '未提交',
  PENDING: '审核中',
  REJECTED: '未通过',
  APPROVED: '审核通过',
  CANCELLED: '已取消',
  CLOSED: '报名关闭',
}

export function openStatusQueryToEnum(query: string | null | undefined): OpenApplicationStatus | null {
  if (query === 'not-submitted') return 'NOT_SUBMITTED'
  if (query === 'pending') return 'PENDING'
  if (query === 'rejected') return 'REJECTED'
  if (query === 'approved') return 'APPROVED'
  if (query === 'cancelled') return 'CANCELLED'
  if (query === 'closed') return 'CLOSED'
  return null
}

export function getOpenStatusRouteName(status: OpenApplicationStatus): string {
  if (status === 'PENDING') return 'AccountOpenApplicationPending'
  if (status === 'REJECTED') return 'AccountOpenApplicationRejected'
  if (status === 'APPROVED') return 'AccountOpenApplicationApproved'
  return 'AccountOpenApplicationApply'
}

export interface AccountNotification {
  id: string
  userId: number
  type: NotificationType
  title: string
  summary: string
  content: string
  createdAt: string
  read: boolean
  pendingAction: boolean
  actionType: NotificationActionType
  actionTargetId: string
  metadata: Record<string, string>
}

export type TournamentLineupStatus = 'ACTIVE' | 'WAITING_TEAM_REGISTRATION' | 'ENDED'

export interface TournamentLineupRelation {
  id: string
  userId: number
  tournamentId: string
  tournamentName: string
  teamId: string
  teamName: string
  roleName: string
  status: TournamentLineupStatus
}

export type TeamInvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface TeamInvitation {
  id: string
  userId: number
  tournamentId: string
  tournamentName: string
  teamId: string
  teamName: string
  inviteRole: string
  status: TeamInvitationStatus
  createdAt: string
  expiresAt: string
}

export type TeamCreateRequestStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED'

export interface TeamRelationsData {
  userId: number
  lineupRelations: TournamentLineupRelation[]
  invitations: TeamInvitation[]
  managedTeamName: string
  hasCaptainPermission: boolean
  createRequestStatus: TeamCreateRequestStatus
  createRequestSubmittedAt: string
  createFeatureEnabled: boolean
}

export interface AccountSecurityData {
  userId: number
  passwordHash: string
  passwordUpdatedAt: string
  lastLoginAt: string
  accountStatus: 'NORMAL' | 'LOCKED'
}

export const ACCOUNT_ROLE_LABELS: Record<AccountRole, string> = {
  BASIC_USER: '基础用户',
  PLAYER: '认证球员',
  CAPTAIN: '认证队长',
}

export const VERIFICATION_STATUS_LABELS: Record<PlayerVerificationStatus, string> = {
  NOT_APPLIED: '未认证',
  PENDING: '审核中',
  APPROVED: '已生效',
  REJECTED: '需修改',
  WITHDRAWN: '已撤回',
  CHANGE_PENDING: '变更审核中',
}

export const PLAYER_IDENTITY_STATUS_LABELS: Record<PlayerIdentityStatus, string> = {
  NOT_SUBMITTED: '未认证',
  PENDING: '审核中',
  REJECTED: '未通过',
  APPROVED: '已认证',
  ELIGIBILITY_LIMITED: '资格受限',
}

export const IDENTITY_PLATFORM_LABELS: Record<IdentityPlatform, string> = {
  PC: 'PC',
  PS: 'PS',
  XBOX: 'Xbox',
}

export function verificationToIdentityStatus(
  status: PlayerVerificationStatus,
  eligibilityLimited = false,
): PlayerIdentityStatus {
  if (eligibilityLimited && (status === 'APPROVED' || status === 'CHANGE_PENDING')) {
    return 'ELIGIBILITY_LIMITED'
  }
  if (status === 'NOT_APPLIED' || status === 'WITHDRAWN') return 'NOT_SUBMITTED'
  if (status === 'CHANGE_PENDING') return 'APPROVED'
  return status
}

export function identityQueryToVerificationStatus(
  query: string | null | undefined,
): PlayerVerificationStatus | null {
  if (!query) return null
  const normalized = query.replace(/_/g, '-')
  if (normalized === 'not-submitted') return 'NOT_APPLIED'
  if (normalized === 'pending') return 'PENDING'
  if (normalized === 'rejected') return 'REJECTED'
  if (normalized === 'approved') return 'APPROVED'
  if (normalized === 'limited' || normalized === 'restricted') return 'APPROVED'
  return null
}

export function isIdentityRestrictedQuery(query: string | null | undefined): boolean {
  if (!query) return false
  const normalized = query.replace(/_/g, '-')
  return normalized === 'limited' || normalized === 'restricted'
}

export const GAME_PLATFORM_LABELS: Record<GamePlatform, string> = {
  PC: 'PC',
  PS5: 'PlayStation 5',
  XBOX: 'Xbox',
  MOBILE: '移动端',
}

export const GAME_POSITION_LABELS: Record<GamePosition, string> = {
  PG: '控球后卫 PG',
  SG: '得分后卫 SG',
  SF: '小前锋 SF',
  PF: '大前锋 PF',
  C: '中锋 C',
}
