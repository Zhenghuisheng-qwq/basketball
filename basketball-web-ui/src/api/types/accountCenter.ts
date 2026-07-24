import type { MessageItemDto } from '@/api/types/accountMessage'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'

export interface AccountSummaryDto {
  accountId: string
  accountNo: string
  nickname: string
  avatarUrl?: string
  emailMasked: string
  mobileMasked?: string
  emailVerified: boolean
  mobileVerified?: boolean
  accountStatus: number
  registerTime?: string
  lastLoginTime?: string
}

export interface PlayerIdentityDto {
  certificationStatus?: number
  playerNo?: string
  realName?: string
  gameTerminal?: string
  gameAccount?: string
  positionCode?: string
  certifiedTime?: string
  applicationNo?: string
  applicationStatus?: number
  submittedTime?: string
  reviewedTime?: string
  reviewRemark?: string
}

export interface CaptainQualificationDto {
  qualified?: boolean
  captainStatus?: number
  captainStatusText?: string
  remark?: string
}

export interface PendingTaskDto {
  taskType: string
  title: string
  description: string
  actionPath: string
}

export interface AccountCenterOverview {
  accountSummary: AccountSummaryDto
  playerIdentity?: PlayerIdentityDto | null
  captainQualification?: CaptainQualificationDto | null
  currentTournaments?: AccountTournamentItemDto[]
  pendingTasks?: PendingTaskDto[]
  recentMessages?: MessageItemDto[]
  unreadMessageCount?: number
}
