export interface TeamInvitationItemDto {
  invitationId: string
  invitationNo?: string
  teamId?: string
  teamName?: string
  inviterMemberId?: string
  inviteeMemberId?: string
  inviteeNickname?: string
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
