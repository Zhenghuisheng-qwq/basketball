import type { PageResult } from '@/api/types/common'
import type { TeamSummaryDto } from '@/api/types/teamAccount'
import type { CandidatePlayerItemDto } from '@/api/types/teamMember'

export type PlatformIdentityStatusCode = 'UNBOUND' | 'PENDING' | 'ACTIVE' | 'REJECTED'

export interface JoinableTeamPageDto extends PageResult<TeamSummaryDto> {
  emptyReason?: string
  platformCode?: string
  platformIdentityStatus?: PlatformIdentityStatusCode
  currentTeamId?: string
  currentTeamName?: string
  hasActiveTeam?: boolean
  pendingApplicationStatus?: number | null
  canApply?: boolean
  disabledReason?: string
}

export interface CandidatePlayerPageDto extends PageResult<CandidatePlayerItemDto> {
  emptyReason?: string
}

export const JOINABLE_PLATFORM_TABS = ['PC', 'PS', 'XBOX'] as const
export type JoinablePlatformCode = (typeof JOINABLE_PLATFORM_TABS)[number]

export const PLATFORM_IDENTITY_STATUS_LABELS: Record<PlatformIdentityStatusCode, string> = {
  UNBOUND: '未绑定',
  PENDING: '审核中',
  ACTIVE: '已生效',
  REJECTED: '已驳回',
}
