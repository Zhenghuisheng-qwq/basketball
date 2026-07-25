export interface TeamSummaryDto {
  teamId: string
  teamNo?: string
  teamName: string
  shortName?: string
  logoUrl?: string
  regionName?: string
  gameTerminal?: string
  currentMemberCount?: number
  maxMemberCount?: number
  teamStatus?: number
  auditStatus?: number
  publicStatus?: number
  myRole?: string
  isCaptain?: boolean
  createTime?: string
}

export interface TeamDetailDto {
  teamId: string
  teamNo?: string
  teamName: string
  shortName?: string
  logoFileId?: string
  logoUrl?: string
  regionCode?: string
  regionName?: string
  gameTerminal?: string
  description?: string
  currentMemberCount?: number
  maxMemberCount?: number
  teamStatus?: number
  auditStatus?: number
  publicStatus?: number
  isCaptain?: boolean
  myRole?: string
  establishedDate?: string
  createTime?: string
}

export interface TeamCreateReq {
  teamName: string
  shortName?: string
  gameTerminal?: string
  regionCode?: string
  regionName?: string
  description?: string
  /** 招募状态：0 不公开招募，1 公开招募 */
  publicStatus?: number
  logoFileId?: string
}

export interface TeamUpdateReq {
  teamName?: string
  shortName?: string
  gameTerminal?: string
  regionCode?: string
  regionName?: string
  description?: string
  publicStatus?: number
}

export interface TeamLogoBindReq {
  fileId: string
}
