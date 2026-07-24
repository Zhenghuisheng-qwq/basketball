export interface TeamJoinApplicationItemDto {
  applicationId: string
  applicationNo?: string
  teamId?: string
  teamName?: string
  applicantMemberId?: string
  applicantNickname?: string
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
