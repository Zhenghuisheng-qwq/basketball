export interface InheritableTeamSummaryDto {
  teamId: string
  teamNo?: string
  teamName?: string
  shortName?: string
  logoUrl?: string
  auditStatus?: number
  auditStatusText?: string
  currentMemberCount?: number
  previousEventName?: string
  honorCount?: number
  canInherit?: boolean
  cannotInheritReason?: string
  recommended?: boolean
  lastApprovedTime?: string
}

export interface CaptainGrantBindTeamReq {
  teamId: string
  divisionId?: string
}
