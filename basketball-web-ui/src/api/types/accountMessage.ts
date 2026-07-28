export interface MessageTeamInviteInfoDto {
  inviteId: string
  teamId?: string
  teamName?: string
  teamLogo?: string
  inviteStatus?: number
  inviteStatusText?: string
  canRespond?: boolean
  canAccept?: boolean
  cannotAcceptReason?: string
  invitationMessage?: string
  inviterName?: string
  inviteTime?: string
  expireTime?: string
}

export interface MessageItemDto {
  receiverId: string
  messageType?: string
  title: string
  content?: string
  bizType?: string
  bizId?: string
  readStatus?: number
  sentTime?: string
  readTime?: string
  redirectType?: string
  redirectValue?: string
  teamInvite?: MessageTeamInviteInfoDto
}

export interface MessagePageResult {
  records: MessageItemDto[]
  total: number
  size: number
  current: number
  pages: number
  allCount?: number
  unreadCount?: number
}
