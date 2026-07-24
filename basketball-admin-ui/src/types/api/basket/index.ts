import type { AjaxResult, BaseEntity, PageDomain } from '../common'

/** 球员认证状态 */
export const PLAYER_CERT_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '审核中' },
  { value: 3, label: '已通过' },
  { value: 4, label: '已驳回' },
  { value: 5, label: '已取消' }
]

/** 队长资格状态 */
export const CAPTAIN_QUALIFICATION_STATUS_OPTIONS = [
  { value: 0, label: '已撤销' },
  { value: 1, label: '有效' },
  { value: 2, label: '已过期' }
]

/** 球队审核状态 */
export const TEAM_AUDIT_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' },
  { value: 3, label: '审核中' }
]

/** 赛事状态 */
export const TOURNAMENT_STATUS_OPTIONS = [
  { value: 0, label: '已禁用' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已结束' },
  { value: 3, label: '草稿' }
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

/** 报名状态 */
export const REGISTRATION_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '审核中' },
  { value: 3, label: '已通过' },
  { value: 4, label: '已驳回' },
  { value: 5, label: '已撤回' },
  { value: 6, label: '已取消' }
]

/** 阵容校验状态 */
export const REGISTRATION_VALIDATION_STATUS_OPTIONS = [
  { value: 0, label: '未校验' },
  { value: 1, label: '通过' },
  { value: 2, label: '未通过' }
]

export interface PlayerCertQuery extends PageDomain, BaseEntity {
  applicationNo?: string
  realName?: string
  applicationStatus?: number
  memberId?: number
}

export interface PlayerCertListItem {
  id: number
  applicationNo?: string
  memberId?: number
  realName?: string
  applicationStatus?: number
  applicationStatusName?: string
  submittedTime?: string
  reviewedTime?: string
}

export interface PlayerCertDetail extends PlayerCertListItem {
  idType?: string
  idNoMasked?: string
  gameTerminal?: string
  gameAccount?: string
  gameNickname?: string
  positionCode?: string
  contactMobile?: string
  reviewRemark?: string
  currentAuditorId?: number
  materials?: PlayerCertMaterial[]
}

export interface PlayerCertMaterial {
  id?: number
  materialType?: string
  fileId?: number
  materialName?: string
  sortNo?: number
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
  rejectReason: string
  auditRemark?: string
}

export interface CaptainQualificationQuery extends PageDomain, BaseEntity {
  qualificationNo?: string
  memberId?: number
  playerId?: number
  qualificationStatus?: number
}

export interface CaptainQualificationListItem {
  id: number
  qualificationNo?: string
  memberId?: number
  playerId?: number
  qualificationStatus?: number
  isCurrent?: number
  paymentConfirmed?: number
  grantedTime?: string
  validTo?: string
}

export interface CaptainQualificationDetail extends CaptainQualificationListItem {
  paymentReference?: string
  grantReason?: string
  validFrom?: string
  revokeReason?: string
}

export interface EligibleCaptain {
  playerId: number
  memberId?: number
  playerNo?: string
  realName?: string
  certificationStatus?: number
}

export interface CaptainQualificationGrantReq {
  playerId: number
  paymentConfirmed?: number
  paymentReference?: string
  validFrom?: string
  validTo?: string
  grantRemark?: string
}

export interface CaptainQualificationUpdateReq {
  paymentConfirmed?: number
  paymentReference?: string
  validFrom?: string
  validTo?: string
  grantRemark?: string
}

export interface CaptainQualificationRevokeReq {
  revokeReason: string
}

export interface TeamAuditQuery extends PageDomain, BaseEntity {
  teamNo?: string
  teamName?: string
  auditStatus?: number
}

export interface TeamAuditListItem {
  id: number
  teamNo?: string
  teamName?: string
  auditStatus?: number
  auditStatusName?: string
  currentMemberCount?: number
  createTime?: string
}

export interface TeamAuditDetail extends TeamAuditListItem {
  shortName?: string
  regionName?: string
  gameTerminal?: string
  description?: string
  captainMemberId?: number
  captainPlayerId?: number
}

export interface TeamAuditApproveReq {
  auditRemark?: string
}

export interface TeamAuditRejectReq {
  rejectReason: string
  auditRemark?: string
}

export interface TournamentQuery extends PageDomain, BaseEntity {
  tournamentNo?: string
  tournamentName?: string
  tournamentStatus?: number
}

export interface TournamentListItem {
  id: number
  tournamentNo?: string
  tournamentName?: string
  tournamentStatus?: number
  tournamentStatusName?: string
  displayOrder?: number
  createTime?: string
}

export interface TournamentDetail extends TournamentListItem {
  shortName?: string
  englishName?: string
  logoFileId?: number
  coverFileId?: number
  tournamentType?: string
  gameTerminal?: string
  formatCode?: string
  organizerName?: string
  description?: string
  publicStatus?: number
}

export interface TournamentCreateReq {
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
  publicStatus?: number
  displayOrder?: number
}

export interface TournamentUpdateReq extends TournamentCreateReq {}

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

export interface TournamentRule {
  id: number
  seasonId?: number
  ruleType?: string
  ruleTitle?: string
  ruleContent?: string
  versionNo?: string
  effectiveTime?: string
  ruleStatus?: number
  displayOrder?: number
}

export interface TournamentRuleCreateReq {
  ruleType?: string
  ruleTitle: string
  ruleContent?: string
  versionNo?: string
  effectiveTime?: string
  ruleStatus?: number
  displayOrder?: number
}

export interface TournamentRuleUpdateReq extends TournamentRuleCreateReq {}

export interface RegistrationAuditQuery extends PageDomain, BaseEntity {
  registrationNo?: string
  seasonId?: number
  teamId?: number
  registrationStatus?: number
}

export interface RegistrationAuditListItem {
  id: number
  registrationNo?: string
  seasonId?: number
  teamId?: number
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
  auditTime?: string
}

export interface RegistrationPlayer {
  id?: number
  playerId?: number
  memberId?: number
  rosterRole?: string
  jerseyNo?: string
  isCaptain?: number
  validationStatus?: number
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

/** 比赛数据状态 */
export const MATCH_DATA_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已确认' },
  { value: 3, label: '已重开' }
]

export interface StageQuery extends PageDomain, BaseEntity {
  tournamentId?: number
  seasonId?: number
  stageName?: string
  stageType?: string
  stageStatus?: number
}

export interface StageListItem {
  id: number
  seasonId?: number
  seasonName?: string
  tournamentName?: string
  stageNo?: string
  stageName?: string
  stageType?: string
  formatType?: string
  startTime?: string
  endTime?: string
  sequenceNo?: number
  stageStatus?: number
  stageStatusName?: string
  publicStatus?: number
  createTime?: string
}

export interface StageDetail extends StageListItem {
  tournamentId?: number
  qualificationRuleText?: string
  updateTime?: string
  canEdit?: boolean
  canActivate?: boolean
  canFinish?: boolean
  canCancel?: boolean
  canDelete?: boolean
}

export interface StageCreateReq {
  seasonId: number
  stageName: string
  stageType?: string
  formatType?: string
  startTime?: string
  endTime?: string
  sequenceNo?: number
  qualificationRuleText?: string
  publicStatus?: number
}

export interface StageUpdateReq {
  stageName?: string
  stageType?: string
  formatType?: string
  startTime?: string
  endTime?: string
  sequenceNo?: number
  qualificationRuleText?: string
  publicStatus?: number
}

export interface RoundListItem {
  id: number
  seasonId?: number
  stageId?: number
  roundNo?: string
  roundName?: string
  roundType?: string
  sequenceNo?: number
  bestOf?: number
  startTime?: string
  endTime?: string
  roundStatus?: number
  roundStatusName?: string
  createTime?: string
}

export interface RoundDetail extends RoundListItem {
  stageName?: string
  updateTime?: string
  canEdit?: boolean
  canActivate?: boolean
  canFinish?: boolean
  canCancel?: boolean
  canDelete?: boolean
}

export interface RoundCreateReq {
  roundName: string
  roundType?: string
  sequenceNo?: number
  bestOf?: number
  startTime?: string
  endTime?: string
}

export interface RoundUpdateReq extends RoundCreateReq {}

export interface GroupListItem {
  id: number
  seasonId?: number
  stageId?: number
  groupCode?: string
  groupName?: string
  qualifyCount?: number
  sequenceNo?: number
  groupStatus?: number
  groupStatusName?: string
  teamCount?: number
  createTime?: string
}

export interface GroupDetail extends GroupListItem {
  stageName?: string
  updateTime?: string
}

export interface GroupCreateReq {
  groupName: string
  qualifyCount?: number
  sequenceNo?: number
  groupStatus?: number
}

export interface GroupUpdateReq extends GroupCreateReq {}

export interface GroupTeam {
  id?: number
  teamId?: number
  teamName?: string
  seedNo?: number
  drawOrder?: number
  groupTeamStatus?: number
}

export interface AvailableTeam {
  teamId: number
  teamName?: string
}

export interface GroupTeamAssignReq {
  teamIds: number[]
}

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

export type RoundListResult = AjaxResult<RoundListItem[]>
export type GroupListResult = AjaxResult<GroupListItem[]>
export type GroupTeamListResult = AjaxResult<GroupTeam[]>
export type AvailableTeamListResult = AjaxResult<AvailableTeam[]>
export type VenueListResult = AjaxResult<VenueSimple[]>
export type MatchLineupResult = AjaxResult<MatchLineup>
export type AvailableMatchPlayerListResult = AjaxResult<AvailableMatchPlayer[]>
export type MatchScoreResult = AjaxResult<MatchScore>
export type MatchTeamStatListResult = AjaxResult<MatchTeamStat[]>
export type MatchPlayerStatListResult = AjaxResult<MatchPlayerStat[]>
export type MatchAuditLogResult = AjaxResult<MatchAuditLog>

export type SeasonListResult = AjaxResult<SeasonListItem[]>
export type TournamentRuleListResult = AjaxResult<TournamentRule[]>
export type EligibleCaptainListResult = AjaxResult<EligibleCaptain[]>
export type RegistrationPlayerListResult = AjaxResult<RegistrationPlayer[]>

/** 球队排行维度 */
export const TEAM_RANK_TYPE_OPTIONS = [
  { value: 'WIN_RATE', label: '胜率' },
  { value: 'AVG_POINTS', label: '场均得分' },
  { value: 'AVG_POINTS_AGAINST', label: '场均失分' },
  { value: 'AVG_REBOUNDS', label: '场均篮板' },
  { value: 'AVG_ASSISTS', label: '场均助攻' },
  { value: 'AVG_STEALS', label: '场均抢断' },
  { value: 'AVG_BLOCKS', label: '场均盖帽' },
  { value: 'FIELD_GOAL_PCT', label: '投篮命中率' },
  { value: 'THREE_POINT_PCT', label: '三分命中率' },
  { value: 'FREE_THROW_PCT', label: '罚球命中率' }
]

/** 球员排行维度 */
export const PLAYER_RANK_TYPE_OPTIONS = [
  { value: 'AVG_POINTS', label: '场均得分' },
  { value: 'AVG_REBOUNDS', label: '场均篮板' },
  { value: 'AVG_ASSISTS', label: '场均助攻' },
  { value: 'AVG_STEALS', label: '场均抢断' },
  { value: 'AVG_BLOCKS', label: '场均盖帽' },
  { value: 'FIELD_GOAL_PCT', label: '投篮命中率' },
  { value: 'THREE_POINT_PCT', label: '三分命中率' },
  { value: 'FREE_THROW_PCT', label: '罚球命中率' },
  { value: 'AVG_EFFICIENCY', label: '场均效率值' }
]

export interface SeasonSettlementSummary {
  seasonId: number
  seasonName?: string
  tournamentId?: number
  tournamentName?: string
  seasonStatus?: number
  confirmedMatchCount?: number
  unfinishedMatchCount?: number
  reopenedMatchCount?: number
  cancelledMatchCount?: number
  standingCount?: number
  teamSeasonStatCount?: number
  playerSeasonStatCount?: number
  teamRankCount?: number
  playerRankCount?: number
  standingRuleConfigured?: boolean
  lastStandingTime?: string
  lastTeamStatTime?: string
  lastPlayerStatTime?: string
  lastSettlementTime?: string
  lastOperatorName?: string
}

export interface SettlementValidationError {
  matchId?: number
  matchNo?: string
  reason?: string
}

export interface SettlementValidationResult {
  seasonId?: number
  passed?: boolean
  confirmedMatchCount?: number
  unfinishedMatchCount?: number
  reopenedMatchCount?: number
  cancelledMatchCount?: number
  errorCount?: number
  standingRuleConfigured?: boolean
  errors?: SettlementValidationError[]
}

export interface SettlementRebuildResult {
  seasonId?: number
  stageId?: number
  groupId?: number
  matchId?: number
  triggerType?: string
  matchCount?: number
  standingCount?: number
  teamStatCount?: number
  playerStatCount?: number
  teamRankCount?: number
  playerRankCount?: number
  success?: boolean
  message?: string
}

export interface StandingQuery extends PageDomain {
  seasonId?: number
  stageId?: number
  groupId?: number
  teamId?: number
  teamName?: string
}

export interface StandingListItem {
  id?: number
  seasonId?: number
  seasonName?: string
  stageId?: number
  stageName?: string
  groupId?: number
  groupName?: string
  teamId?: number
  teamNo?: string
  teamName?: string
  shortName?: string
  logoUrl?: string
  rankNo?: number
  matchesPlayed?: number
  wins?: number
  losses?: number
  pointsFor?: number
  pointsAgainst?: number
  pointDiff?: number
  standingPoints?: number
  winRate?: number
  streakType?: string
  streakCount?: number
  qualifiedFlag?: number
  snapshotTime?: string
}

export interface TeamSeasonStatQuery extends PageDomain {
  seasonId?: number
  stageId?: number
  teamId?: number
  keyword?: string
}

export interface TeamSeasonStatListItem {
  id?: number
  seasonId?: number
  seasonName?: string
  stageId?: number
  stageName?: string
  teamId?: number
  teamNo?: string
  teamName?: string
  shortName?: string
  logoUrl?: string
  games?: number
  wins?: number
  losses?: number
  pointsFor?: number
  pointsAgainst?: number
  avgPoints?: number
  avgPointsAgainst?: number
  avgRebounds?: number
  avgAssists?: number
  avgSteals?: number
  avgBlocks?: number
  avgTurnovers?: number
  fieldGoalPct?: number
  threePointPct?: number
  freeThrowPct?: number
  winRate?: number
  lastCalculatedTime?: string
}

export interface PlayerSeasonStatQuery extends PageDomain {
  seasonId?: number
  stageId?: number
  teamId?: number
  playerId?: number
  keyword?: string
}

export interface PlayerSeasonStatListItem {
  id?: number
  seasonId?: number
  seasonName?: string
  stageId?: number
  stageName?: string
  teamId?: number
  teamName?: string
  playerId?: number
  playerNo?: string
  displayName?: string
  avatarUrl?: string
  positionCode?: string
  games?: number
  starts?: number
  minutesSeconds?: number
  points?: number
  rebounds?: number
  assists?: number
  steals?: number
  blocks?: number
  turnovers?: number
  fouls?: number
  avgMinutes?: number
  avgPoints?: number
  avgRebounds?: number
  avgAssists?: number
  avgSteals?: number
  avgBlocks?: number
  fieldGoalPct?: number
  threePointPct?: number
  freeThrowPct?: number
  avgEfficiency?: number
  doubleDoubleCount?: number
  tripleDoubleCount?: number
  lastCalculatedTime?: string
}

export interface RankingQuery extends PageDomain {
  seasonId?: number
  stageId?: number
  rankType?: string
  teamId?: number
  keyword?: string
  subjectType?: string
}

export interface TeamRankListItem {
  id?: number
  seasonId?: number
  stageId?: number
  rankType?: string
  rankTypeName?: string
  teamId?: number
  teamNo?: string
  teamName?: string
  shortName?: string
  logoUrl?: string
  rankNo?: number
  previousRank?: number
  rankChange?: number
  statValue?: number
  games?: number
  snapshotDate?: string
}

export interface PlayerRankListItem {
  id?: number
  seasonId?: number
  stageId?: number
  rankType?: string
  rankTypeName?: string
  playerId?: number
  playerNo?: string
  displayName?: string
  avatarUrl?: string
  teamId?: number
  teamName?: string
  rankNo?: number
  previousRank?: number
  rankChange?: number
  statValue?: number
  games?: number
  snapshotDate?: string
}

export type SeasonSettlementSummaryListResult = AjaxResult<SeasonSettlementSummary[]>
export type SettlementValidationResultResult = AjaxResult<SettlementValidationResult>
export type SettlementRebuildResultResult = AjaxResult<SettlementRebuildResult>
