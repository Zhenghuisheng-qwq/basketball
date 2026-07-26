import type { PageResult } from '@/api/types/common'
import type { TeamSummaryDto } from '@/api/types/teamAccount'
import type { CandidatePlayerItemDto } from '@/api/types/teamMember'

export interface JoinableTeamPageDto extends PageResult<TeamSummaryDto> {
  emptyReason?: string
}

export interface CandidatePlayerPageDto extends PageResult<CandidatePlayerItemDto> {
  emptyReason?: string
}
