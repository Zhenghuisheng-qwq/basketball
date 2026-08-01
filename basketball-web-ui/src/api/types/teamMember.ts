export interface TeamMemberItemDto {
  teamMemberId: string
  /** 用户ID（历史字段 memberId，登录账号主键） */
  memberId?: string
  userId?: string
  playerId?: string
  playerNo?: string
  nickname?: string
  realName?: string
  gameNickname?: string
  roleType?: string
  rosterType?: string
  positionCode?: string
  positionName?: string
  certificationStatus?: number
  avatarUrl?: string
  gameTerminal?: string
  gameAccount?: string
  platformGameIds?: Array<{ platformCode?: string; gameId?: string }>
  gameIdSummary?: string
  playerStatus?: number
  currentTeamId?: string
  membershipStatusText?: string
  rosterTypeText?: string
  jerseyNo?: string
  membershipStatus?: number
  remark?: string
  joinTime?: string
}

export interface CandidatePlayerItemDto {
  memberId: string
  userId?: string
  playerId: string
  playerNo?: string
  gameNickname?: string
  nickname?: string
  gameTerminal?: string
  platformCode?: string
  platformGameId?: string
  platformIdentityStatus?: string
  platformIdentityStatusText?: string
  platformTeamId?: string
  platformTeamName?: string
  gameAccount?: string
  avatar?: string
  currentTeamId?: string
  canInvite?: boolean
  cannotInviteReason?: string
  positionCode?: string
  positionName?: string
  certificationStatus?: number
  certificationStatusText?: string
  currentTeamStatusText?: string
  platformAccountSummary?: string
}

export interface TeamMemberUpdateReq {
  jerseyNo?: string
  rosterType?: string
  remark?: string
}

/** 队内成员私有详情（需登录 + 同队权限） */
export interface TeamMemberDetailDto {
  teamMemberId: string
  memberId?: string
  userId?: string
  playerId?: string
  playerNo?: string
  nickname?: string
  avatarUrl?: string
  positionCode?: string
  positionName?: string
  roleInTeam?: string
  roleInTeamText?: string
  memberStatus?: number
  memberStatusText?: string
  rosterType?: string
  rosterTypeText?: string
  joinTime?: string
  jerseyNo?: string
  platformGameIds?: Array<{ platformCode?: string; platformName?: string; gameId?: string }>
  gameIdSummary?: string
  teamId?: string
  teamName?: string
  isCaptain?: boolean
  canManage?: boolean
  currentRosterStatus?: string
  sourceType?: string
  sourceTypeText?: string
  participatedEventCount?: number | null
  currentEventName?: string | null
  currentEventPlatform?: string | null
  latestMatchTime?: string | null
  latestMatchSummary?: string | null
  totalMatches?: number | null
  totalPoints?: number | null
  totalRebounds?: number | null
  totalAssists?: number | null
  avgPoints?: string | null
  avgRebounds?: string | null
  avgAssists?: string | null
  recentMatches?: unknown[]
  auditRecords?: unknown[]
  punishmentRecords?: unknown[]
}
