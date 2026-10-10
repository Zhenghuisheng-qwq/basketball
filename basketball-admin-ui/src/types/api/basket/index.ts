import type { AjaxResult, BaseEntity, PageDomain } from '../common'

/** CNPA 后台审核统一筛选项（待审核/已通过/已驳回/已取消） */
export const CNPA_AUDIT_STATUS_FILTER_OPTIONS = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'CANCELLED', label: '已取消' }
]

export interface BatchAuditOperationReq {
  ids: number[]
  auditRemark?: string
  rejectReason?: string
  positionCode?: string
  decidePayload?: Record<string, any>
}

export interface BatchAuditFailure {
  id: number
  message: string
}

export interface BatchAuditResult {
  successCount: number
  failCount: number
  failures: BatchAuditFailure[]
}

/** 球员认证状态（列表展示映射） */
export const PLAYER_CERT_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '待审核' },
  { value: 2, label: '待审核' },
  { value: 3, label: '已通过' },
  { value: 4, label: '已驳回' },
  { value: 5, label: '已取消' }
]

/** 用户认证阶段（用户管理列表展示） */
export const MEMBER_CERT_STAGE_OPTIONS = [
  { value: 'NOT_CERTIFIED', label: '未认证' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已认证' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'NO_PROFILE', label: '未建档' }
]

/** 官网用户账号状态 */
export const MEMBER_ACCOUNT_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 0, label: '禁用' }
]

/** 是否选项 */
export const YES_NO_OPTIONS = [
  { value: 1, label: '是' },
  { value: 0, label: '否' }
]

/** 队长授权状态 */
export const CAPTAIN_QUALIFICATION_STATUS_OPTIONS = [
  { value: 0, label: '已收回' },
  { value: 1, label: '有效' },
  { value: 2, label: '已过期' }
]

/** 球队审核状态（列表展示） */
export const TEAM_AUDIT_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' },
  { value: 3, label: '待审核' }
]

/** 球队招募状态 */
export const TEAM_RECRUIT_STATUS_OPTIONS = [
  { value: 0, label: '不公开招募' },
  { value: 1, label: '公开招募' }
]

/** 赛事报名窗口状态（展示辅助） */
export const TOURNAMENT_REGISTRATION_WINDOW_STATUS_OPTIONS = [
  { value: 'UNCONFIGURED', label: '未配置' },
  { value: 'NOT_STARTED', label: '未开始' },
  { value: 'OPEN', label: '报名中' },
  { value: 'CLOSED', label: '已截止' }
]

/** 赛事状态 */
export const TOURNAMENT_STATUS_OPTIONS = [
  { value: 0, label: '已禁用' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已结束' },
  { value: 3, label: '草稿' }
]

/** 赛事游戏端/参赛端 */
export const GAME_TERMINAL_OPTIONS = [
  { value: 'PC', label: 'PC' },
  { value: 'PS', label: 'PS' },
  { value: 'XBOX', label: 'Xbox' }
]

/** 赛事类型 */
export const TOURNAMENT_TYPE_OPTIONS = [
  { value: 'REGULAR_SEASON', label: '常规赛' },
  { value: 'PLAYOFFS', label: '季后赛' },
  { value: 'ALL_STAR', label: '全明星赛' }
]

/** 后台赛事下拉展示：赛季 / 游戏端 / 赛事名称 */
export function formatEventOptionLabel(item: {
  id?: number
  competitionSeasonName?: string
  competitionSeasonYear?: number
  gameTerminal?: string
  tournamentName?: string
}): string {
  const seasonLabel = item.competitionSeasonYear
    ? `${item.competitionSeasonYear}赛季`
    : (item.competitionSeasonName || '')
  const terminal = item.gameTerminal || ''
  const name = item.tournamentName || ''
  return [seasonLabel, terminal, name].filter(Boolean).join(' / ') || String(item.id ?? '')
}

/** 顶层赛季状态 */
export const COMPETITION_SEASON_STATUS_OPTIONS = [
  { value: 0, label: '筹备中' },
  { value: 1, label: '进行中' },
  { value: 2, label: '已结束' },
  { value: 3, label: '已归档' }
]

/** 赛季状态 */
export const SEASON_STATUS_OPTIONS = [
  { value: 0, label: '筹备中' },
  { value: 1, label: '报名中' },
  { value: 2, label: '进行中' },
  { value: 3, label: '已结束' },
  { value: 4, label: '报名截止' },
  { value: 5, label: '已关闭' },
  { value: 6, label: '已取消' }
]

/** 报名状态（全量，含草稿） */
export const REGISTRATION_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '审核中' },
  { value: 3, label: '已通过' },
  { value: 4, label: '已驳回' },
  { value: 5, label: '已撤回' },
  { value: 6, label: '已取消' }
]

/** 报名审核列表展示状态 */
export const REGISTRATION_AUDIT_STATUS_OPTIONS = [
  { value: 1, label: '待审核' },
  { value: 2, label: '待审核' },
  { value: 3, label: '已通过' },
  { value: 4, label: '已驳回' },
  { value: 5, label: '已取消' },
  { value: 6, label: '已取消' }
]

/** 阵容校验状态 */
export const REGISTRATION_VALIDATION_STATUS_OPTIONS = [
  { value: 0, label: '待校验' },
  { value: 1, label: '通过' },
  { value: 2, label: '未通过' }
]

export interface PlayerCertQuery extends PageDomain, BaseEntity {
  applicationNo?: string
  nickname?: string
  email?: string
  gameAccount?: string
  applicationStatus?: number
  auditStage?: string
  memberId?: number
}

export interface PlayerCertListItem {
  id: number
  applicationNo?: string
  memberId?: number
  nickname?: string
  email?: string
  avatarUrl?: string
  gameTerminal?: string
  gameAccount?: string
  positionCode?: string
  applicationStatus?: number
  applicationStatusName?: string
  submittedTime?: string
  reviewedTime?: string
}

export interface PlayerCertDetail extends PlayerCertListItem {
  gameNickname?: string
  reviewRemark?: string
  currentAuditorId?: number
  materials?: PlayerCertMaterial[]
}

export interface PlayerCertMaterial {
  materialId?: number
  materialType?: string
  fileId?: number
  materialName?: string
  fileName?: string
  mimeType?: string
  fileSize?: number
  accessUrl?: string
  sortNo?: number
  uploadTime?: string
}

export interface PlayerCertAuditRecord {
  id?: number
  auditAction?: string
  beforeStatus?: number
  afterStatus?: number
  auditorName?: string
  auditRemark?: string
  auditTime?: string
}

export interface PlayerCertApproveReq {
  auditRemark?: string
  positionCode?: string
}

export interface PlayerCertRejectReq {
  rejectReason?: string
  auditRemark?: string
}

export interface CaptainQualificationQuery extends PageDomain, BaseEntity {
  qualificationNo?: string
  memberId?: number
  playerId?: number
  nickname?: string
  seasonId?: number
  qualificationStatus?: number
  paymentReference?: string
  grantedTimeBegin?: string
  grantedTimeEnd?: string
}

export interface CaptainQualificationListItem {
  id: number
  qualificationNo?: string
  memberId?: number
  userId?: number
  playerId?: number
  username?: string
  email?: string
  nickname?: string
  playerNo?: string
  gameAccount?: string
  positionCode?: string
  certificationStatusName?: string
  seasonId?: number
  seasonName?: string
  tournamentName?: string
  teamId?: number
  teamName?: string
  qualificationStatus?: number
  isCurrent?: number
  paymentConfirmed?: number
  paymentReference?: string
  grantedTime?: string
  validFrom?: string
  validTo?: string
  grantedBy?: number
  grantedByName?: string
  runtimeValid?: number
  runtimeInvalidReason?: string
}

export interface CaptainQualificationDetail extends CaptainQualificationListItem {
  grantReason?: string
  revokedTime?: string
  revokedBy?: number
  revokedByName?: string
  revokeReason?: string
}

export interface CaptainGrantLogItem {
  id?: number
  grantId?: number
  actionType?: string
  actionTypeText?: string
  beforeStatus?: number
  beforeStatusText?: string
  afterStatus?: number
  afterStatusText?: string
  seasonId?: number
  seasonName?: string
  tournamentName?: string
  eventName?: string
  teamId?: number
  teamName?: string
  operatorId?: number
  operatorName?: string
  operatorType?: string
  reason?: string
  remark?: string
  createTime?: string
}

export interface CaptainGrantSeasonOption {
  seasonId: number
  seasonName?: string
  seasonStatus?: number
  seasonStatusName?: string
  tournamentId?: number
  tournamentName?: string
  seasonEndTime?: string
  displayName?: string
}

export interface PlatformGameIdItem {
  platformCode?: string
  gameId?: string
}

export interface EligibleCaptain {
  /** 雪花 ID，接口以字符串返回，避免 JS 精度丢失 */
  playerId: string | number
  memberId?: string | number
  userId?: string | number
  playerNo?: string
  nickname?: string
  gameNickname?: string
  certificationStatus?: number
  platformGameIds?: PlatformGameIdItem[]
  gameIdSummary?: string
}

export interface CaptainQualificationGrantReq {
  seasonId: number | string
  playerId: number | string
  teamId?: number | string
  paymentConfirmed?: number
  paymentReference?: string
  grantRemark?: string
}

export interface CaptainQualificationUpdateReq {
  paymentReference?: string
  grantRemark?: string
}

export interface CaptainQualificationBindTeamReq {
  teamId: number
}

export interface CaptainQualificationRevokeReq {
  revokeReason: string
}

export interface TeamAuditQuery extends PageDomain, BaseEntity {
  teamNo?: string
  teamName?: string
  auditStatus?: number
  auditStage?: string
}

export interface TeamAuditListItem {
  id: number
  teamNo?: string
  teamName?: string
  shortName?: string
  logoFileId?: number
  logoUrl?: string
  gameTerminal?: string
  publicStatus?: number
  publicStatusName?: string
  auditStatus?: number
  auditStatusName?: string
  currentMemberCount?: number
  createTime?: string
}

export interface TeamAuditMemberItem {
  memberId?: number
  playerId?: number
  nickname?: string
  playerNo?: string
  roleType?: string
  roleTypeName?: string
}

export interface TeamAuditDetail extends TeamAuditListItem {
  regionName?: string
  description?: string
  teamStatus?: number
  maxMemberCount?: number
  updateTime?: string
  captainMemberId?: number
  captainPlayerId?: number
  captainNickname?: string
  captainEmail?: string
  captainPlayerNo?: string
  captainCertStatus?: number
  captainCertStatusName?: string
  captainQualificationValid?: boolean
  submitTime?: string
  auditTime?: string
  auditUserName?: string
  rejectReason?: string
  auditRemark?: string
  captainAssigned?: boolean
  captainCertified?: boolean
  teamActive?: boolean
  members?: TeamAuditMemberItem[]
}

export interface TeamAuditApproveReq {
  auditRemark?: string
}

export interface TeamAuditRejectReq {
  rejectReason: string
  auditRemark?: string
}

export interface TeamAuditRecord {
  id?: number
  auditAction?: string
  beforeStatus?: number
  afterStatus?: number
  auditorName?: string
  auditRemark?: string
  auditTime?: string
}

/** 游戏平台 */
export const GAME_PLATFORM_OPTIONS = [
  { value: 'PC', label: 'PC' },
  { value: 'PS5', label: 'PS5' },
  { value: 'XBOX', label: 'XBOX' },
  { value: 'MOBILE', label: '移动端' }
]

/** 平台账号状态 */
export const PLATFORM_ACCOUNT_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '生效' },
  { value: 2, label: '失效' },
  { value: 3, label: '驳回' }
]

/** 平台变更类型 */
export const PLATFORM_CHANGE_TYPE_OPTIONS = [
  { value: 'ADD_PLATFORM', label: '新增平台' },
  { value: 'CHANGE_GAME_ID', label: '变更游戏ID' }
]

/** 平台变更状态 */
export const PLATFORM_CHANGE_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' },
  { value: 3, label: '已取消' }
]

export interface AccountPlayerQuery extends PageDomain, BaseEntity {
  memberId?: string | number
  email?: string
  nickname?: string
  playerNo?: string
  gameId?: string
  gameTerminal?: string
  accountStatus?: number
  certStage?: string
  hasTeam?: number
  currentTeamName?: string
  hasCaptainQual?: number
  certificationStatus?: number
  playerStatus?: number
}

export interface AccountPlayerListItem {
  memberId: string | number
  userId?: string | number
  accountNo?: string
  username?: string
  email?: string
  nickname?: string
  avatarUrl?: string
  emailVerified?: number
  playerId?: string | number
  playerNo?: string
  certificationStatus?: number
  certificationStatusName?: string
  latestApplicationStatus?: number
  certDisplayStatus?: string
  certDisplayStatusName?: string
  accountStatus?: number
  accountStatusName?: string
  currentTeamId?: string | number
  currentTeamName?: string
  hasCaptainQual?: number
  activeCaptainGrantEventName?: string
  platformGameIds?: PlatformGameIdItem[]
  gameIdSummary?: string
  gameIdCompactSummary?: string
  lastLoginTime?: string
  createTime?: string
}

export interface AccountPlayerAccountSection {
  memberId?: number
  userId?: number
  accountNo?: string
  username?: string
  email?: string
  nickname?: string
  avatarUrl?: string
  emailVerified?: number
  accountStatus?: number
  accountStatusName?: string
  lastLoginTime?: string
  lastLoginIp?: string
  createTime?: string
  updateTime?: string
}

export interface AccountPlayerProfileSection {
  playerId?: number
  memberId?: number
  nickname?: string
  avatarUrl?: string
  gender?: number
  profileStatus?: number
  playerNo?: string
  gameTerminal?: string
  gameAccount?: string
  gameNickname?: string
  positionCode?: string
  certificationStatus?: number
  certificationStatusName?: string
  playerStatus?: number
  certifiedTime?: string
  currentTeamId?: number
  currentTeamName?: string
}

export interface AccountPlayerPlatformAccount {
  id?: number
  platform?: string
  displayGameId?: string
  normalizedGameId?: string
  accountStatus?: number
  accountStatusName?: string
  isPrimary?: number
  approvedTime?: string
  effectiveTime?: string
  invalidTime?: string
  updateTime?: string
}

export interface AccountPlayerCertHistory {
  applicationId?: number
  applicationNo?: string
  gameTerminal?: string
  gameAccount?: string
  applicationStatus?: number
  applicationStatusName?: string
  submittedTime?: string
  reviewedTime?: string
  reviewRemark?: string
}

export interface AccountPlayerCaptainQual {
  qualificationId?: number
  qualificationNo?: string
  qualificationStatus?: number
  qualificationStatusName?: string
  isCurrent?: number
  grantedTime?: string
  validFrom?: string
  validTo?: string
  grantReason?: string
  runtimeValid?: number
  runtimeInvalidReason?: string
}

export interface AccountPlayerTeamSummary {
  teamId?: number
  teamNo?: string
  teamName?: string
  roleType?: string
  membershipStatus?: number
  membershipStatusName?: string
  isCurrent?: number
  joinTime?: string
  auditStatus?: number
  auditStatusName?: string
}

export interface AccountPlayerCaptainGrant {
  qualificationId?: number
  qualificationNo?: string
  qualificationStatus?: number
  qualificationStatusName?: string
  seasonName?: string
  tournamentName?: string
  eventName?: string
  teamId?: number
  teamName?: string
  paymentConfirmed?: number
  paymentReference?: string
  grantedTime?: string
  validTo?: string
  grantedBy?: number
  grantedByName?: string
  revokedTime?: string
  revokeReason?: string
}

export interface AccountPlayerRegistration {
  registrationId?: number
  registrationNo?: string
  tournamentName?: string
  seasonName?: string
  eventName?: string
  platformCode?: string
  teamName?: string
  registrationStatus?: number
  registrationStatusName?: string
  auditStatusName?: string
  submittedTime?: string
}

export interface AccountPlayerNotification {
  receiverId?: number
  title?: string
  messageType?: string
  bizType?: string
  readStatus?: number
  readStatusName?: string
  createTime?: string
}

export interface AccountPlayerOperationLog {
  id?: number
  eventType?: string
  eventTitle?: string
  operatorType?: string
  operatorId?: number
  operatorName?: string
  eventTime?: string
  remark?: string
  clientIp?: string
}

export interface AccountPlayerFullDetail {
  account?: AccountPlayerAccountSection
  profile?: AccountPlayerProfileSection
  platformAccounts?: AccountPlayerPlatformAccount[]
  certHistory?: AccountPlayerCertHistory[]
  captainQual?: AccountPlayerCaptainQual
  captainGrants?: AccountPlayerCaptainGrant[]
  teamSummary?: AccountPlayerTeamSummary[]
  registrations?: AccountPlayerRegistration[]
  notifications?: AccountPlayerNotification[]
  operationLogs?: AccountPlayerOperationLog[]
}

export interface PlayerPlatformChangeQuery extends PageDomain, BaseEntity {
  requestNo?: string
  nickname?: string
  email?: string
  playerNo?: string
  platform?: string
  changeType?: string
  requestStatus?: number
  auditStage?: string
  memberId?: number
}

export interface PlayerPlatformChangeListItem {
  id: number
  requestNo?: string
  playerId?: number
  memberId?: number
  nickname?: string
  email?: string
  playerNo?: string
  changeType?: string
  changeTypeName?: string
  platform?: string
  oldDisplayGameId?: string
  newDisplayGameId?: string
  requestStatus?: number
  requestStatusName?: string
  submitTime?: string
  reviewedTime?: string
}

export interface PlayerPlatformChangeDetail extends PlayerPlatformChangeListItem {
  platformAccountId?: number
  newNormalizedGameId?: string
  proofFileId?: number
  reviewerId?: number
  reviewRemark?: string
  resultPlatformAccountId?: number
}

export interface PlayerPlatformChangeApproveReq {
  auditRemark?: string
}

export interface PlayerPlatformChangeRejectReq {
  rejectReason: string
  auditRemark?: string
}

export interface TeamProfileChangeQuery extends PageDomain {
  changeNo?: string
  teamName?: string
  submitUser?: string
  auditStatus?: number
  auditStage?: string
  beginTime?: string
  endTime?: string
}

export interface TeamProfileChangeListItem {
  id: number
  changeNo?: string
  teamId?: number
  teamName?: string
  submitUserId?: number
  submitUserName?: string
  submitUserEmail?: string
  changeSummary?: string
  auditStatus?: number
  auditStatusName?: string
  submitTime?: string
  auditTime?: string
}

export interface TeamProfileChangeDetail extends TeamProfileChangeListItem {
  oldTeamName?: string
  newTeamName?: string
  oldTeamShortName?: string
  newTeamShortName?: string
  oldLogoUrl?: string
  newLogoUrl?: string
  oldDescription?: string
  newDescription?: string
  oldPublicStatus?: number
  newPublicStatus?: number
  changeReason?: string
  submitPlayerNo?: string
  auditUserId?: number
  auditRemark?: string
}

export interface TeamProfileChangeApproveReq {
  auditRemark?: string
}

export interface TeamProfileChangeRejectReq {
  auditRemark: string
}

export interface MemberProfileChangeQuery extends PageDomain {
  changeNo?: string
  nickname?: string
  submitUser?: string
  auditStatus?: number
  auditStage?: string
  beginTime?: string
  endTime?: string
}

export interface MemberProfileChangeListItem {
  id: number
  changeNo?: string
  memberId?: number
  accountNo?: string
  playerId?: number
  playerNo?: string
  submitUserName?: string
  submitUserEmail?: string
  oldNickname?: string
  newNickname?: string
  oldAvatarUrl?: string
  newAvatarUrl?: string
  changeSummary?: string
  auditStatus?: number
  auditStatusName?: string
  submitTime?: string
  auditTime?: string
}

export interface MemberProfileChangeDetail extends MemberProfileChangeListItem {
  oldAvatarFileId?: number
  newAvatarFileId?: number
  auditUserId?: number
  auditRemark?: string
}

export interface MemberProfileChangeApproveReq {
  auditRemark?: string
}

export interface MemberProfileChangeRejectReq {
  auditRemark: string
}

export interface TournamentQuery extends PageDomain, BaseEntity {
  tournamentNo?: string
  tournamentName?: string
  tournamentStatus?: number
  competitionSeasonId?: number
  registrationWindowStatus?: string
  registrationTimeBegin?: string
  registrationTimeEnd?: string
  competitionTimeBegin?: string
  competitionTimeEnd?: string
}

export interface TournamentListItem {
  id: number
  tournamentNo?: string
  competitionSeasonId?: number
  competitionSeasonName?: string
  competitionSeasonYear?: number
  tournamentName?: string
  tournamentType?: string
  tournamentTypeName?: string
  tournamentStatus?: number
  tournamentStatusName?: string
  gameTerminal?: string
  registrationStartTime?: string
  registrationEndTime?: string
  registrationWindowStatus?: string
  registrationWindowStatusName?: string
  competitionStartTime?: string
  competitionEndTime?: string
  operationalSeasonId?: number
  displayOrder?: number
  createTime?: string
  updateTime?: string
}

export interface TournamentDetail extends TournamentListItem {
  shortName?: string
  englishName?: string
  logoFileId?: number
  coverFileId?: number
  tournamentType?: string
  formatCode?: string
  organizerName?: string
  description?: string
  publicStatus?: number
}

export interface TournamentCreateReq {
  competitionSeasonId: number
  tournamentName: string
  shortName?: string
  englishName?: string
  logoFileId?: number
  coverFileId?: number
  tournamentType?: string
  gameTerminal?: string
  formatCode?: string
  organizerName?: string
  description?: string
  registrationStartTime?: string
  registrationEndTime?: string
  competitionStartTime?: string
  competitionEndTime?: string
  publicStatus?: number
  displayOrder?: number
}

export interface TournamentUpdateReq extends TournamentCreateReq {}

export interface CompetitionSeasonQuery extends PageDomain, BaseEntity {
  seasonName?: string
  seasonYear?: number
  seasonStatus?: number
}

export interface CompetitionSeasonListItem {
  id: number
  seasonNo?: string
  seasonName?: string
  seasonYear?: number
  startTime?: string
  endTime?: string
  seasonStatus?: number
  seasonStatusName?: string
  publicStatus?: number
  tournamentCount?: number
  createTime?: string
}

export interface CompetitionSeasonDetail extends CompetitionSeasonListItem {
  description?: string
}

export interface CompetitionSeasonCreateReq {
  seasonName: string
  seasonYear?: number
  startTime?: string
  endTime?: string
  seasonStatus?: number
  publicStatus?: number
  description?: string
}

export interface CompetitionSeasonUpdateReq extends CompetitionSeasonCreateReq {}

export interface SeasonListItem {
  id: number
  seasonNo?: string
  seasonName?: string
  seasonYear?: number
  seasonStatus?: number
  seasonStatusName?: string
  registrationStartTime?: string
  registrationEndTime?: string
}

export interface SeasonDetail extends SeasonListItem {
  tournamentId?: number
  seasonStartTime?: string
  seasonEndTime?: string
  venueId?: number
  maxTeamCount?: number
  minRosterCount?: number
  maxRosterCount?: number
  description?: string
}

export interface SeasonCreateReq {
  seasonName: string
  seasonYear?: number
  registrationStartTime?: string
  registrationEndTime?: string
  seasonStartTime?: string
  seasonEndTime?: string
  venueId?: number
  maxTeamCount?: number
  minRosterCount?: number
  maxRosterCount?: number
  publicStatus?: number
  description?: string
}

export interface SeasonUpdateReq extends SeasonCreateReq {}


export interface RegistrationAuditQuery extends PageDomain, BaseEntity {
  registrationNo?: string
  seasonId?: number
  teamId?: number
  registrationStatus?: number
  auditStage?: string
}

export interface RegistrationAuditListItem {
  id: number
  registrationNo?: string
  seasonId?: number
  teamId?: number
  tournamentName?: string
  teamName?: string
  captainName?: string
  registrationStatus?: number
  registrationStatusName?: string
  lineupCount?: number
  submittedTime?: string
}

export interface RegistrationAuditDetail extends RegistrationAuditListItem {
  captainMemberId?: number
  contactName?: string
  contactMobile?: string
  remark?: string
  rejectReason?: string
  auditTime?: string
  players?: RegistrationPlayer[]
}

export interface RegistrationPlayer {
  id?: number
  playerId?: number
  memberId?: number
  userId?: number
  playerNickname?: string
  playerNo?: string
  rosterRole?: string
  positionCode?: string
  positionName?: string
  positionText?: string
  jerseyNo?: string
  isCaptain?: number
  isStarting?: number
  validationStatus?: number
  validationStatusName?: string
  rejectReason?: string
}

export interface RegistrationAuditRecord {
  id?: number
  auditAction?: string
  beforeStatus?: number
  afterStatus?: number
  auditorName?: string
  auditRemark?: string
  auditTime?: string
}

export interface RegistrationAuditApproveReq {
  auditRemark?: string
}

export interface RegistrationAuditRejectReq {
  rejectReason: string
  auditRemark?: string
}

/** 赛事阶段状态 */
export const STAGE_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '进行中' },
  { value: 2, label: '已结束' },
  { value: 3, label: '已取消' }
]

/** 轮次状态 */
export const ROUND_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已编排' },
  { value: 2, label: '进行中' },
  { value: 3, label: '已结束' },
  { value: 4, label: '已取消' }
]

/** 分组状态 */
export const GROUP_STATUS_OPTIONS = [
  { value: 0, label: '已禁用' },
  { value: 1, label: '正常' }
]

/** 比赛状态 */
export const MATCH_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已编排' },
  { value: 2, label: '进行中' },
  { value: 3, label: '已结束' },
  { value: 4, label: '已延期' },
  { value: 5, label: '已取消' }
]

/** 比赛发布状态 */
export const MATCH_PUBLISH_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已下架' }
]

/** 比赛数据状态 */
export const MATCH_DATA_STATUS_OPTIONS = [
  { value: 0, label: '未提交' },
  { value: 1, label: '审核中' },
  { value: 2, label: '已通过' },
  { value: 3, label: '已重开' },
  { value: 4, label: '已驳回' }
]


export interface MatchQuery extends PageDomain, BaseEntity {
  matchNo?: string
  seasonId?: number
  stageId?: number
  roundId?: number
  groupId?: number
  venueId?: number
  teamId?: number
  teamKeyword?: string
  matchStatus?: number
  dataStatus?: number
  publishStatus?: number
}

export interface MatchListItem {
  id: number
  matchNo?: string
  seasonId?: number
  seasonName?: string
  stageId?: number
  stageName?: string
  roundId?: number
  roundName?: string
  groupId?: number
  groupName?: string
  venueId?: number
  venueName?: string
  homeTeamId?: number
  homeTeamName?: string
  awayTeamId?: number
  awayTeamName?: string
  scheduledStartTime?: string
  scheduledEndTime?: string
  matchStatus?: number
  matchStatusName?: string
  publishStatus?: number
  publishStatusName?: string
  operationalStatusName?: string
  homeScore?: number
  awayScore?: number
  dataStatus?: number
  dataStatusName?: string
  sortNo?: number
  canEdit?: boolean
  canSchedule?: boolean
  canStart?: boolean
  canPostpone?: boolean
  canCancel?: boolean
  canDelete?: boolean
  canEnterLineup?: boolean
  canEnterScore?: boolean
  canEnterStat?: boolean
}

export interface MatchDetail extends MatchListItem {
  actualStartTime?: string
  actualEndTime?: string
  winnerTeamId?: number
  winnerTeamName?: string
  overtimeCount?: number
  walkoverTeamId?: number
  cancelReason?: string
  publicStatus?: number
  canConfirmResult?: boolean
  canReopenResult?: boolean
}

export interface VenueSimple {
  id: number
  venueNo?: string
  venueName?: string
  address?: string
}

export interface MatchCreateReq {
  seasonId: number
  stageId?: number
  roundId?: number
  groupId?: number
  venueId?: number
  homeTeamId: number
  awayTeamId: number
  scheduledStartTime?: string
  scheduledEndTime?: string
  sortNo?: number
  publicStatus?: number
}

export interface MatchUpdateReq {
  roundId?: number
  groupId?: number
  venueId?: number
  homeTeamId?: number
  awayTeamId?: number
  scheduledStartTime?: string
  scheduledEndTime?: string
  sortNo?: number
  publicStatus?: number
}

export interface MatchScheduleReq {
  venueId?: number
  scheduledStartTime?: string
  scheduledEndTime?: string
}

export interface MatchPostponeReq {
  reason?: string
  scheduledStartTime?: string
  scheduledEndTime?: string
  venueId?: number
}

export interface MatchCancelReq {
  reason?: string
}

export interface MatchReopenReq {
  reason: string
}

export interface MatchForfeitReq {
  walkoverTeamId: number
  reason?: string
}

export interface MatchLineupPlayer {
  id?: number
  registrationPlayerId?: number
  playerId?: number
  playerName?: string
  playerNo?: string
  jerseyNo?: string
  rosterRole?: string
  isStarting?: number
  captainFlag?: number
  positionCode?: string
  playStatus?: number
  sortNo?: number
}

export interface MatchLineup {
  matchId?: number
  teamId?: number
  teamName?: string
  players?: MatchLineupPlayer[]
}

export interface AvailableMatchPlayer {
  registrationPlayerId?: number
  playerId?: number
  memberId?: number
  playerName?: string
  playerNo?: string
  jerseyNo?: string
  rosterRole?: string
  isStarting?: number
  isCaptain?: number
}

export interface MatchLineupPlayerItem {
  registrationPlayerId?: number
  jerseyNo?: string
  isStarting?: number
  captainFlag?: number
  rosterRole?: string
  positionCode?: string
  sortNo?: number
}

export interface MatchLineupSaveReq {
  players: MatchLineupPlayerItem[]
}

export interface MatchPeriodScore {
  id?: number
  periodNo?: number
  periodType?: string
  periodTypeName?: string
  homeScore?: number
  awayScore?: number
}

export interface MatchScore {
  matchId?: number
  homeScore?: number
  awayScore?: number
  winnerTeamId?: number
  overtimeCount?: number
  matchStatus?: number
  matchStatusName?: string
  dataStatus?: number
  dataStatusName?: string
  periods?: MatchPeriodScore[]
}

export interface MatchPeriodScoreItem {
  periodNo?: number
  periodType?: string
  homeScore?: number
  awayScore?: number
}

export interface MatchPeriodScoreSaveReq {
  periods: MatchPeriodScoreItem[]
}

export interface MatchTeamStat {
  id?: number
  matchId?: number
  teamId?: number
  teamName?: string
  opponentTeamId?: number
  opponentTeamName?: string
  points?: number
  rebounds?: number
  offensiveRebounds?: number
  defensiveRebounds?: number
  assists?: number
  steals?: number
  blocks?: number
  turnovers?: number
  fouls?: number
  fieldGoalsMade?: number
  fieldGoalsAttempted?: number
  fieldGoalPct?: number
  threePointsMade?: number
  threePointsAttempted?: number
  threePointPct?: number
  freeThrowsMade?: number
  freeThrowsAttempted?: number
  freeThrowPct?: number
  possessions?: number
  efficiency?: number
  dataStatus?: number
  dataStatusName?: string
  confirmedBy?: number
  confirmedTime?: string
}

export interface MatchPlayerStat {
  id?: number
  matchId?: number
  teamId?: number
  teamName?: string
  playerId?: number
  playerName?: string
  playerNo?: string
  rosterId?: number
  startedFlag?: number
  minutesSeconds?: number
  points?: number
  rebounds?: number
  offensiveRebounds?: number
  defensiveRebounds?: number
  assists?: number
  steals?: number
  blocks?: number
  turnovers?: number
  fouls?: number
  fieldGoalsMade?: number
  fieldGoalsAttempted?: number
  fieldGoalPct?: number
  threePointsMade?: number
  threePointsAttempted?: number
  threePointPct?: number
  freeThrowsMade?: number
  freeThrowsAttempted?: number
  freeThrowPct?: number
  plusMinus?: number
  efficiency?: number
  dataStatus?: number
  dataStatusName?: string
  confirmedBy?: number
  confirmedTime?: string
}

export interface MatchTeamStatItem {
  teamId?: number
  points?: number
  rebounds?: number
  offensiveRebounds?: number
  defensiveRebounds?: number
  assists?: number
  steals?: number
  blocks?: number
  turnovers?: number
  fouls?: number
  fieldGoalsMade?: number
  fieldGoalsAttempted?: number
  threePointsMade?: number
  threePointsAttempted?: number
  freeThrowsMade?: number
  freeThrowsAttempted?: number
  possessions?: number
  efficiency?: number
}

export interface MatchTeamStatSaveReq {
  stats: MatchTeamStatItem[]
}

export interface MatchPlayerStatItem {
  teamId?: number
  playerId?: number
  rosterId?: number
  startedFlag?: number
  minutesSeconds?: number
  points?: number
  rebounds?: number
  offensiveRebounds?: number
  defensiveRebounds?: number
  assists?: number
  steals?: number
  blocks?: number
  turnovers?: number
  fouls?: number
  fieldGoalsMade?: number
  fieldGoalsAttempted?: number
  threePointsMade?: number
  threePointsAttempted?: number
  freeThrowsMade?: number
  freeThrowsAttempted?: number
  plusMinus?: number
  efficiency?: number
}

export interface MatchPlayerStatSaveReq {
  stats: MatchPlayerStatItem[]
}

export interface MatchStatusLog {
  id?: number
  beforeStatus?: number
  beforeStatusName?: string
  afterStatus?: number
  afterStatusName?: string
  changeType?: string
  changeTypeName?: string
  changeReason?: string
  operatorId?: number
  operatorName?: string
  changeTime?: string
}

export interface StatChangeRecord {
  id?: number
  bizType?: string
  bizId?: number
  fieldName?: string
  beforeValue?: string
  afterValue?: string
  changeReason?: string
  operatorId?: number
  operatorName?: string
  createTime?: string
}

export interface MatchAuditLog {
  statusLogs?: MatchStatusLog[]
  statChanges?: StatChangeRecord[]
}

export type VenueListResult = AjaxResult<VenueSimple[]>
export type MatchLineupResult = AjaxResult<MatchLineup>
export type AvailableMatchPlayerListResult = AjaxResult<AvailableMatchPlayer[]>
export type MatchScoreResult = AjaxResult<MatchScore>
export type MatchTeamStatListResult = AjaxResult<MatchTeamStat[]>
export type MatchPlayerStatListResult = AjaxResult<MatchPlayerStat[]>
export type MatchAuditLogResult = AjaxResult<MatchAuditLog>

export type SeasonListResult = AjaxResult<SeasonListItem[]>
export type EligibleCaptainListResult = AjaxResult<EligibleCaptain[]>
export type RegistrationPlayerListResult = AjaxResult<RegistrationPlayer[]>

export interface HonorGenerationPreview {
  seasonId?: number
  seasonName?: string
  tournamentId?: number
  tournamentName?: string
  currentBatch?: HonorGenerationBatch
  items?: HonorGenerationPreviewItem[]
}

/** 荣誉展示状态 */
export const HONOR_DISPLAY_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已下线' },
  { value: 3, label: '已撤销' }
]

/** 赛季名次状态 */
export const SEASON_RESULT_STATUS_OPTIONS = [
  { value: 'DRAFT', label: '未确认' },
  { value: 'CONFIRMED', label: '已确认未发布' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'REVOKED', label: '已撤销' }
]

/** 球队荣誉编码 */
export const TEAM_HONOR_CODE_OPTIONS = [
  { value: 'TEAM_HONOR', label: '球队荣誉' },
  { value: 'CHAMPION', label: '冠军' },
  { value: 'RUNNER_UP', label: '亚军' },
  { value: 'THIRD_PLACE', label: '季军' }
]

/** 球员荣誉编码 / 候选奖项 */
export const PLAYER_HONOR_CODE_OPTIONS = [
  { value: 'PLAYER_HONOR', label: '球员荣誉' },
  { value: 'SCORING_CHAMPION', label: '得分王' },
  { value: 'REBOUND_CHAMPION', label: '篮板王' },
  { value: 'ASSIST_CHAMPION', label: '助攻王' },
  { value: 'STEAL_CHAMPION', label: '抢断王' },
  { value: 'BLOCK_CHAMPION', label: '盖帽王' }
]

export const PLAYER_HONOR_AWARD_TYPE_OPTIONS = [
  { value: 'SCORING', label: '得分王' },
  { value: 'REBOUND', label: '篮板王' },
  { value: 'ASSIST', label: '助攻王' },
  { value: 'STEAL', label: '抢断王' },
  { value: 'BLOCK', label: '盖帽王' }
]

/** 公告状态 */
export const ANNOUNCEMENT_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已下线' }
]

export const ANNOUNCEMENT_CATEGORY_OPTIONS = [
  { value: 'NOTICE', label: '通知公告' },
  { value: 'RULE', label: '规则说明' },
  { value: 'EVENT', label: '赛事资讯' },
  { value: 'OTHER', label: '其他' }
]

/** 首页横幅状态 */
export const HOME_BANNER_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已下线' }
]

export const HOME_BANNER_TARGET_TYPE_OPTIONS = [
  { value: 'NONE', label: '无跳转' },
  { value: 'TOURNAMENT', label: '赛事' },
  { value: 'ANNOUNCEMENT', label: '公告' },
  { value: 'TEAM', label: '球队' },
  { value: 'PLAYER', label: '球员' },
  { value: 'EXTERNAL', label: '外链' }
]

export const HOME_BANNER_POSITION_OPTIONS = [
  { value: 'HOME_TOP', label: '首页顶部' }
]

export const SOCIAL_LINK_STATUS_OPTIONS = [
  { value: 0, label: '停用' },
  { value: 1, label: '启用' }
]

export const SOCIAL_PLATFORM_LABELS: Record<string, string> = {
  KOOK: 'KOOK',
  WECHAT: '微信',
  BILIBILI: 'B站',
  DOUYIN: '抖音'
}

export interface SiteSocialLinkItem {
  id: number
  platformCode: string
  platformName?: string
  title?: string
  titleEn?: string
  subtitle?: string
  subtitleEn?: string
  buttonText?: string
  buttonTextEn?: string
  jumpUrl?: string
  sortOrder?: number
  status?: number
  statusName?: string
  createTime?: string
  updateTime?: string
}

export interface SiteSocialLinkEditReq {
  title: string
  titleEn?: string
  subtitle?: string
  subtitleEn?: string
  buttonText: string
  buttonTextEn?: string
  jumpUrl?: string
  sortOrder: number
  status: number
}

/** 合作伙伴状态 */
export const PARTNER_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '展示中' },
  { value: 2, label: '不展示' }
]

export const PARTNER_TYPE_OPTIONS = [
  { value: 'SPONSOR', label: '赞助商' },
  { value: 'MEDIA', label: '媒体' },
  { value: 'TECH', label: '技术合作' },
  { value: 'OTHER', label: '其他' }
]

export interface SeasonResultListItem {
  seasonId: number
  seasonNo?: string
  seasonName?: string
  seasonYear?: number
  tournamentId?: number
  seasonStatus?: number
  seasonStatusName?: string
  championTeamId?: number
  championTeamName?: string
  runnerUpTeamId?: number
  runnerUpTeamName?: string
  resultStatus?: string
  resultStatusName?: string
}

export interface SeasonResultDetail {
  seasonId: number
  seasonNo?: string
  seasonName?: string
  seasonYear?: number
  tournamentId?: number
  tournamentName?: string
  seasonStatus?: number
  seasonStatusName?: string
  championTeamId?: number
  championTeamName?: string
  runnerUpTeamId?: number
  runnerUpTeamName?: string
  thirdPlaceTeamId?: number
  thirdPlaceTeamName?: string
  resultStatus?: string
  resultStatusName?: string
  confirmedMatchCount?: number
  unfinishedMatchCount?: number
  reopenedMatchCount?: number
  finishedUnconfirmedCount?: number
  teamSeasonStatCount?: number
  honors?: HonorRecordListItem[]
  championLineup?: HonorLineupItem[]
  updateTime?: string
}

export interface SeasonEndCheck {
  seasonId?: number
  canConfirm?: boolean
  confirmedMatchCount?: number
  unfinishedMatchCount?: number
  reopenedMatchCount?: number
  finishedUnconfirmedCount?: number
  teamSeasonStatCount?: number
  requireConfirmRemark?: boolean
  blockers?: string[]
  warnings?: string[]
}

export interface SeasonCandidateTeam {
  teamId: number
  teamName?: string
  logoUrl?: string
}

export interface SeasonResultConfirmReq {
  championTeamId?: number
  runnerUpTeamId?: number
  thirdPlaceTeamId?: number
  confirmRemark?: string
}

export interface SeasonResultRevokeReq {
  revokeReason?: string
}

export interface HonorRecordQuery extends PageDomain {
  tournamentId?: number
  seasonId?: number
  honorCategoryId?: number
  honorCode?: string
  honorScope?: string
  recipientType?: string
  teamId?: number
  playerId?: number
  displayStatus?: number
  keyword?: string
}

export interface HonorRecordListItem {
  id: number
  tournamentId?: number
  seasonId?: number
  seasonName?: string
  honorCategoryId?: number
  honorCode?: string
  honorName?: string
  honorScope?: string
  recipientType?: string
  teamId?: number
  teamName?: string
  playerId?: number
  playerName?: string
  matchId?: number
  honorTitle?: string
  honorTitleEn?: string
  honorSubtitle?: string
  honorSubtitleEn?: string
  honorValue?: string
  awardDate?: string
  coverFileId?: number
  coverUrl?: string
  sourceType?: string
  sourceId?: number
  generationBatchId?: number
  displayStatus?: number
  displayStatusName?: string
  displayOrder?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface HonorLineupItem {
  id?: number
  honorRecordId?: number
  teamId?: number
  playerId?: number
  memberId?: number
  playerName?: string
  roleType?: string
  jerseyNo?: string
  positionCode?: string
  sortNo?: number
  avatarUrl?: string
}

export interface HonorRecordDetail extends HonorRecordListItem {
  lineup?: HonorLineupItem[]
}

export interface HonorRecordAddReq {
  tournamentId?: number
  seasonId?: number
  honorCategoryId?: number
  honorCode?: string
  teamId?: number
  playerId?: number
  matchId?: number
  honorTitle?: string
  honorTitleEn?: string
  honorSubtitle?: string
  honorSubtitleEn?: string
  honorValue?: string
  awardDate?: string
  coverFileId?: number
  displayOrder?: number
  remark?: string
}

export interface HonorRecordEditReq {
  honorCategoryId?: number
  honorCode?: string
  teamId?: number
  playerId?: number
  matchId?: number
  honorTitle?: string
  honorTitleEn?: string
  honorSubtitle?: string
  honorSubtitleEn?: string
  honorValue?: string
  awardDate?: string
  coverFileId?: number
  displayOrder?: number
  remark?: string
}

export interface HonorRecordRevokeReq {
  reason?: string
  revokeReason?: string
}

export interface PlayerHonorCandidateQuery {
  seasonId?: number
  stageId?: number
  awardType?: string
}

export interface PlayerHonorCandidate {
  playerId: number
  playerNo?: string
  displayName?: string
  avatarUrl?: string
  teamId?: number
  teamName?: string
  rankNo?: number
  statValue?: number
  games?: number
  rankType?: string
  awardType?: string
}

export interface AnnouncementQuery extends PageDomain {
  categoryCode?: string
  announcementStatus?: number
  isTop?: number
  isImportant?: number
  keyword?: string
}

export interface AnnouncementListItem {
  id: number
  announcementNo?: string
  categoryCode?: string
  categoryName?: string
  tournamentId?: number
  tournamentName?: string
  seasonId?: number
  seasonName?: string
  title?: string
  titleEn?: string
  summary?: string
  summaryEn?: string
  coverFileId?: number
  coverUrl?: string
  authorId?: number
  authorName?: string
  announcementStatus?: number
  announcementStatusName?: string
  publicScope?: string
  isTop?: number
  isImportant?: number
  publishTime?: string
  scheduledPublishTime?: string
  offlineTime?: string
  supersededBy?: number
  ruleVersion?: string
  viewCount?: number
  createTime?: string
  updateTime?: string
}

export interface AnnouncementDetail extends AnnouncementListItem {
  contentText?: string
  contentTextEn?: string
}

export interface AnnouncementAddReq {
  categoryCode?: string
  title?: string
  titleEn?: string
  summary?: string
  summaryEn?: string
  coverFileId?: number
  contentText?: string
  contentTextEn?: string
  publicScope?: string
  isImportant?: number
  tournamentId?: number
  seasonId?: number
  ruleVersion?: string
  scheduledPublishTime?: string
}

export interface AnnouncementEditReq {
  categoryCode?: string
  title?: string
  titleEn?: string
  summary?: string
  summaryEn?: string
  coverFileId?: number
  contentText?: string
  contentTextEn?: string
  publicScope?: string
  isImportant?: number
  tournamentId?: number
  seasonId?: number
  ruleVersion?: string
  scheduledPublishTime?: string
}

export interface AnnouncementRuleSupersedeReq {
  oldAnnouncementId: number
  seasonId?: number
  tournamentId?: number
  ruleVersion?: string
}

export interface AnnouncementScheduleReq {
  scheduledPublishTime: string
}

export interface HomeBannerQuery extends PageDomain {
  positionCode?: string
  bannerStatus?: number
  keyword?: string
}

export interface HomeBannerListItem {
  id: number
  bannerName?: string
  title?: string
  titleEn?: string
  subtitle?: string
  subtitleEn?: string
  imageFileId?: number
  imageUrl?: string
  positionCode?: string
  targetType?: string
  targetValue?: string
  startTime?: string
  endTime?: string
  bannerStatus?: number
  bannerStatusName?: string
  sortNo?: number
  createTime?: string
  updateTime?: string
}

export interface HomeBannerAddReq {
  bannerName?: string
  title?: string
  titleEn?: string
  subtitle?: string
  subtitleEn?: string
  imageFileId?: number
  positionCode?: string
  targetType?: string
  targetValue?: string
  startTime?: string
  endTime?: string
  sortNo?: number
}

export interface HomeBannerEditReq {
  bannerName?: string
  title?: string
  titleEn?: string
  subtitle?: string
  subtitleEn?: string
  imageFileId?: number
  positionCode?: string
  targetType?: string
  targetValue?: string
  startTime?: string
  endTime?: string
  sortNo?: number
}

export interface PartnerQuery extends PageDomain {
  partnerType?: string
  partnerStatus?: number
  keyword?: string
  partnerCode?: string
}

export interface PartnerListItem {
  id: number
  partnerCode?: string
  partnerName?: string
  logoFileId?: number
  logoUrl?: string
  websiteUrl?: string
  publicDescription?: string
  partnerType?: string
  cooperationStartDate?: string
  cooperationEndDate?: string
  partnerStatus?: number
  partnerStatusName?: string
  sortNo?: number
  remark?: string
  createTime?: string
  updateTime?: string
  totalSupportAmount?: number
  supportCount?: number
  latestSupportTime?: string
}

export interface PartnerAddReq {
  partnerName?: string
  logoFileId?: number
  websiteUrl?: string
  publicDescription?: string
  partnerType?: string
  cooperationStartDate?: string
  cooperationEndDate?: string
  partnerStatus?: number
  sortNo?: number
  remark?: string
}

export interface PartnerEditReq {
  partnerName?: string
  logoFileId?: number
  websiteUrl?: string
  publicDescription?: string
  partnerType?: string
  cooperationStartDate?: string
  cooperationEndDate?: string
  sortNo?: number
  remark?: string
}

export interface PartnerSupportQuery extends PageDomain {
  partnerId?: number
  partnerName?: string
  supportType?: string
  publishStatus?: number
  supportDateBegin?: string
  supportDateEnd?: string
}

export interface PartnerSupportListItem {
  id: number
  partnerId?: number
  partnerCode?: string
  partnerName?: string
  partnerLogoUrl?: string
  supportType?: string
  supportTypeName?: string
  supportAmount?: number
  supportDate?: string
  publicDescription?: string
  publishStatus?: number
  publishStatusName?: string
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface PartnerSupportAddReq {
  partnerId?: number
  partnerName?: string
  supportType?: string
  supportAmount?: number
  supportDate?: string
  publicDescription?: string
  publishStatus?: number
  sortOrder?: number
}

export interface PartnerSupportEditReq extends PartnerSupportAddReq {}

export const PARTNER_SUPPORT_TYPE_OPTIONS = [
  { value: 'CASH', label: '现金赞助' },
  { value: 'GOODS', label: '物资支持' },
  { value: 'TECHNOLOGY', label: '技术支持' },
  { value: 'VENUE', label: '场地支持' },
  { value: 'MEDIA', label: '媒体支持' },
  { value: 'SERVICE', label: '服务支持' },
  { value: 'OTHER', label: '其他支持' }
]

export const PARTNER_SUPPORT_STATUS_OPTIONS = [
  { value: 1, label: '已公开' },
  { value: 0, label: '已停用' }
]

/** Open报名状态（同报名状态） */
export const OPEN_REGISTRATION_STATUS_OPTIONS = REGISTRATION_AUDIT_STATUS_OPTIONS

/** 参赛状态 */
export const PARTICIPATION_STATUS_OPTIONS = [
  { value: 1, label: '参赛中' },
  { value: 2, label: '已退赛' },
  { value: 3, label: '取消资格' },
  { value: 4, label: '已完赛' }
]

/** 阵容锁定状态 */
export const ROSTER_LOCK_STATUS_OPTIONS = [
  { value: 0, label: '未锁定' },
  { value: 1, label: '已锁定' }
]

/** 赛季阵容状态 */
export const SEASON_ROSTER_STATUS_OPTIONS = [
  { value: 0, label: '未生效' },
  { value: 1, label: '生效中' },
  { value: 2, label: '已转出' },
  { value: 3, label: '已移除' }
]

/** 阵容变更类型 */
export const ROSTER_CHANGE_TYPE_OPTIONS = [
  { value: 'TRANSFER', label: '转会' },
  { value: 'FREE_AGENT_ADD', label: '自由球员补员' },
  { value: 'CAPTAIN_TRANSFER', label: '队长变更' }
]

/** 阵容变更状态 */
export const ROSTER_CHANGE_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' },
  { value: 3, label: '已撤回' }
]

/** 阵容操作类型 */
export const ROSTER_OPERATION_TYPE_OPTIONS = [
  { value: 'FORCE_REMOVE', label: '强制移除' },
  { value: 'UPDATE_ROLE', label: '更新角色' }
]

export interface OpenRegistrationAuditQuery extends PageDomain, BaseEntity {
  seasonId?: number
  registrationNo?: string
  registrationStatus?: number
  auditStage?: string
}

export interface OpenRegistrationAuditListItem {
  id: number
  registrationNo?: string
  seasonId?: number
  seasonName?: string
  memberId?: number
  playerId?: number
  playerNickname?: string
  platform?: string
  displayGameId?: string
  registrationStatus?: number
  registrationStatusName?: string
  submittedTime?: string
  auditTime?: string
}

export interface OpenRegistrationAuditDetail extends OpenRegistrationAuditListItem {
  playerNo?: string
  normalizedGameId?: string
  positionCode?: string
  remark?: string
  resultRosterId?: number
}

export interface OpenRegistrationAuditRecord {
  id?: number
  auditAction?: string
  beforeStatus?: number
  afterStatus?: number
  auditorName?: string
  auditRemark?: string
  auditTime?: string
}

export interface OpenRegistrationAuditApproveReq {
  auditRemark?: string
}

export interface OpenRegistrationAuditRejectReq {
  rejectReason: string
  auditRemark?: string
}

export interface EventTeamQuery extends PageDomain, BaseEntity {
  seasonId?: number
  eventTeamNo?: string
  teamName?: string
  participationStatus?: number
  rosterLockStatus?: number
}

export interface EventTeamListItem {
  id: number
  seasonId?: number
  seasonName?: string
  tournamentName?: string
  teamId?: number
  teamNo?: string
  registrationId?: number
  eventTeamNo?: string
  teamNameSnapshot?: string
  gameTerminal?: string
  captainPlayerId?: number
  captainNameSnapshot?: string
  participationStatus?: number
  participationStatusName?: string
  rosterLockStatus?: number
  rosterLockStatusName?: string
  rosterLockNeedsRepair?: boolean
  approvedTime?: string
}

export interface EventTeamDetail extends EventTeamListItem {
  shortNameSnapshot?: string
  teamRegionSnapshot?: string
  descriptionSnapshot?: string
  registrationSource?: string
  displayName?: string
  logoFileId?: number
  rosterLockedTime?: string
  remark?: string
}

export interface EventTeamParticipationStatusReq {
  participationStatus: number
  reason?: string
}

export interface SeasonRosterQuery extends PageDomain, BaseEntity {
  seasonId?: number
  eventTeamId?: number
  playerId?: number
  rosterStatus?: number
}

export interface SeasonRosterListItem {
  id: number
  rosterNo?: string
  seasonId?: number
  eventTeamId?: number
  eventTeamNo?: string
  eventTeamName?: string
  teamId?: number
  playerId?: number
  memberId?: number
  playerNicknameSnapshot?: string
  platformSnapshot?: string
  gameIdDisplaySnapshot?: string
  rosterRole?: string
  isCaptain?: number
  isStarting?: number
  isSubstitute?: number
  sourceType?: string
  sourceTypeName?: string
  rosterStatus?: number
  rosterStatusName?: string
  lockStatus?: number
  activeSlot?: string
  effectiveTime?: string
  invalidTime?: string
}

export interface SeasonRosterChangeRecord {
  id?: number
  seasonId?: number
  changeRequestId?: number
  changeType?: string
  changeTypeName?: string
  playerId?: number
  sourceEventTeamId?: number
  targetEventTeamId?: number
  beforeSnapshot?: string
  afterSnapshot?: string
  operatorName?: string
  operateReason?: string
  effectiveTime?: string
  createTime?: string
}

export interface RosterChangeAuditQuery extends PageDomain, BaseEntity {
  seasonId?: number
  changeType?: string
  requestStatus?: number
}

export interface RosterChangeAuditListItem {
  id: number
  requestNo?: string
  seasonId?: number
  seasonName?: string
  changeType?: string
  changeTypeName?: string
  requestStatus?: number
  requestStatusName?: string
  playerId?: number
  playerNickname?: string
  sourceEventTeamId?: number
  targetEventTeamId?: number
  submitTime?: string
  reviewedTime?: string
}

export interface RosterChangeAuditDetail extends RosterChangeAuditListItem {
  applicantMemberId?: number
  sourceRosterId?: number
  newCaptainPlayerId?: number
  targetRosterRole?: string
  targetIsStarting?: number
  targetIsSubstitute?: number
  applyReason?: string
  rejectReason?: string
  reviewRemark?: string
  resultRosterId?: number
}

export interface RosterChangeAuditApproveReq {
  auditRemark?: string
}

export interface RosterChangeAuditRejectReq {
  rejectReason: string
  auditRemark?: string
}

export interface RosterOperationLogQuery extends PageDomain, BaseEntity {
  seasonId?: number
  eventTeamId?: number
  operationType?: string
}

export interface RosterOperationLogItem {
  id: number
  seasonId?: number
  eventTeamId?: number
  operationType?: string
  targetRosterId?: number
  targetPlayerId?: number
  beforeValue?: string
  afterValue?: string
  operateReason?: string
  effectiveTime?: string
  operatorName?: string
  createTime?: string
}

export interface RosterOperationForceRemoveReq {
  rosterId: number
  operateReason: string
}

export interface RosterOperationUpdateRoleReq {
  rosterId: number
  rosterRole?: string
  isStarting?: number
  isSubstitute?: number
  operateReason: string
}

