export interface TeamMemberItemDto {
  teamMemberId: string
  memberId?: string
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
  positionCode?: string
  positionName?: string
  certificationStatus?: number
}

export interface TeamMemberUpdateReq {
  jerseyNo?: string
  rosterType?: string
  remark?: string
}
