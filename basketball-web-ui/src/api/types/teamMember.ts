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
  playerId: string
  playerNo?: string
  gameNickname?: string
  nickname?: string
  gameTerminal?: string
  gameAccount?: string
  avatar?: string
  currentTeamId?: string
  canInvite?: boolean
  cannotInviteReason?: string
  positionCode?: string
  positionName?: string
  certificationStatus?: number
}

export interface TeamMemberUpdateReq {
  jerseyNo?: string
  rosterType?: string
  remark?: string
}
