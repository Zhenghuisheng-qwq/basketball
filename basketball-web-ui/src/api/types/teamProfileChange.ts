export interface TeamProfileChangeItemDto {
  changeId: string
  changeNo: string
  changeSummary: string
  auditStatus: number
  auditStatusText: string
  submitTime?: string
  auditTime?: string
  auditRemark?: string
}

export interface TeamProfileChangeDetailDto {
  changeId: string
  changeNo: string
  teamId: string
  oldTeamName?: string
  newTeamName?: string
  oldTeamShortName?: string
  newTeamShortName?: string
  oldLogoUrl?: string
  newLogoUrl?: string
  oldDescription?: string
  newDescription?: string
  oldPublicStatus?: number
  newPublicStatus?: number
  changeReason?: string
  auditStatus: number
  auditStatusText: string
  auditRemark?: string
  submitUserId?: string
  submitUserName?: string
  submitPlayerNo?: string
  submitTime?: string
  auditTime?: string
  changeSummary?: string
}

export interface TeamProfileEditDto {
  officialProfile: import('@/api/types/teamAccount').TeamDetailDto
  pendingChange?: TeamProfileChangeDetailDto
  recentHistory: TeamProfileChangeItemDto[]
  canSubmitChange?: boolean
  hasPendingChange?: boolean
  isCaptain?: boolean
}

export interface TeamProfileChangeSubmitReq {
  teamName: string
  teamShortName?: string
  logoFileId?: string
  description?: string
  changeReason?: string
}

export interface TeamProfileChangeSubmitResp {
  changeId: string
  changeNo: string
  auditStatus: number
  auditStatusText: string
}

export interface TeamProfileChangeHistoryPage {
  records: TeamProfileChangeItemDto[]
  total: number
  pageNum: number
  pageSize: number
  current: number
  size: number
  pages: number
}
