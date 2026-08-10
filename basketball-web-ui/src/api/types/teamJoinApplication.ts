export interface TeamJoinApplicationItemDto {
  applicationId: string
  applicationNo?: string
  teamId?: string
  teamName?: string
  divisionId?: string
  divisionName?: string
  seasonName?: string
  seriesName?: string
  platformCode?: string
  gameMode?: string
  applicantMemberId?: string
  applicantNickname?: string
  playerNo?: string
  positionCode?: string
  positionName?: string
  gameTerminal?: string
  certificationStatus?: number
  applyReason?: string
  applicationStatus?: number
  applicationStatusText?: string
  canCancel?: boolean
  canProcess?: boolean
  processRemark?: string
  createTime?: string
}

export interface TeamJoinApplicationCreateReq {
  applyReason?: string
}

export interface TeamJoinApplicationProcessReq {
  action?: 'APPROVE' | 'REJECT'
  approve?: boolean
  processRemark?: string
}
