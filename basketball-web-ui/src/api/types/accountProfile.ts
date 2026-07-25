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
