export type AccountNotificationType =
  | 'MATCH_DATA'
  | 'REGISTRATION'
  | 'TEAM_INVITE'
  | 'PLAYER_VERIFY'
  | 'ACCOUNT_SECURITY'
  | 'SYSTEM'

export type AccountNotificationStatus = 'UNREAD' | 'READ'

export type AccountNotificationResultStatus = 'SUCCESS' | 'PENDING' | 'WARNING' | 'FAILED'

export interface AccountNotificationMetaItem {
  label: string
  value: string
}

export interface AccountNotificationTeamInvite {
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

export interface AccountNotificationItem {
  id: string
  type: AccountNotificationType
  typeText: string
  title: string
  summary: string
  status: AccountNotificationStatus
  isUnread: boolean
  createdAt: string
  detailTitle: string
  detailSubtitle: string
  body: string[]
  meta: AccountNotificationMetaItem[]
  resultText: string
  resultStatus: AccountNotificationResultStatus
  teamInvite?: AccountNotificationTeamInvite
}

export interface NotificationSummary {
  total: number
  unread: number
}

export type NotificationTabKey = 'ALL' | 'UNREAD'
