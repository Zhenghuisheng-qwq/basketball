export interface AccountProfileChangeDto {
  changeId: string
  changeNo?: string
  oldNickname?: string
  newNickname?: string
  oldAvatarUrl?: string
  newAvatarUrl?: string
  changeSummary?: string
  auditStatus?: number
  auditStatusName?: string
  auditRemark?: string
  submitTime?: string
  auditTime?: string
}

export interface AccountProfileDto {
  accountId: string
  accountNo: string
  nickname: string
  avatarUrl?: string
  emailMasked: string
  emailVerified: boolean
  gender?: number
  birthday?: string
  provinceCode?: string
  provinceName?: string
  cityCode?: string
  cityName?: string
  bio?: string
  registerTime?: string
  lastLoginTime?: string
  hasPendingChange?: boolean
  pendingChange?: AccountProfileChangeDto | null
  latestRejectedChange?: AccountProfileChangeDto | null
}

export interface AccountProfileUpdatePayload {
  nickname?: string
  gender?: number
  birthday?: string
  provinceCode?: string
  provinceName?: string
  cityCode?: string
  cityName?: string
  bio?: string
}

export interface AccountProfileChangeSubmitPayload {
  nickname?: string
  avatarFileId?: string
}
