export interface TeamInvitationItemDto {
  invitationId: string
  invitationNo?: string
  teamId?: string
  teamName?: string
  divisionId?: string
  divisionName?: string
  seasonName?: string
  seriesName?: string
  platformCode?: string
  gameMode?: string
  inviterMemberId?: string
  inviteeMemberId?: string
  inviteeNickname?: string
  inviteeAvatarUrl?: string
  invitationMessage?: string
  invitationStatus?: number
  invitationStatusText?: string
  canRespond?: boolean
  canAccept?: boolean
  cannotAcceptReason?: string
  expireTime?: string
  createTime?: string
}

export interface TeamInvitationCreateReq {
  inviteePlayerId?: string
  inviteeMemberId?: string
  invitationMessage?: string
}

export interface TeamInvitationRespondReq {
  action?: 'ACCEPT' | 'REJECT'
  accept?: boolean
  responseRemark?: string
}
