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
  primaryActionText: string
  primaryActionRoute: string
  secondaryActionText: string
}

export interface NotificationSummary {
  total: number
  unread: number
}

export type NotificationTabKey = 'ALL' | 'UNREAD'
