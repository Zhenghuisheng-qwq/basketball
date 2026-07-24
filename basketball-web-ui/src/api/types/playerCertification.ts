export interface PlayerCertMaterialDto {
  materialId: string
  materialType: string
  fileId: string
  accessUrl?: string
  materialName?: string
}

export interface PlayerCertCurrentDto {
  hasPlayerProfile: boolean
  playerId?: string
  playerNo?: string
  certificationStatus?: number
  currentApplicationId?: string
  applicationNo?: string
  applicationStatus?: number
  applicationStatusText?: string
  submittedTime?: string
  reviewedTime?: string
  rejectReason?: string
  realName?: string
  idType?: string
  idNoMasked?: string
  gameTerminal?: string
  gameAccount?: string
  gameNickname?: string
  positionCode?: string
  contactMobile?: string
  materials?: PlayerCertMaterialDto[]
  canEdit?: boolean
  canSubmit?: boolean
}

export interface PlayerCertDraftPayload {
  realName: string
  idType: string
  idNo: string
  gameTerminal: string
  gameAccount: string
  gameNickname: string
  positionCode?: string
  contactMobile: string
}

export interface PlayerCertMaterialBindPayload {
  applicationId: string
  fileId: string
  materialType: string
}

export interface PlayerCertSubmitPayload {
  applicationId: string
}
