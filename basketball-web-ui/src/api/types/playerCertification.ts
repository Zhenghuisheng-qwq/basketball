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
  gameTerminal?: string
  gameAccount?: string
  gameNickname?: string
  positionCode?: string
  materials?: PlayerCertMaterialDto[]
  canEdit?: boolean
  canSubmit?: boolean
}

export interface PlayerCertDraftPayload {
  gameTerminal: string
  gameAccount: string
  gameNickname?: string
  positionCode?: string
}

export interface PlayerCertMaterialBindPayload {
  applicationId: string
  fileId: string
  materialType: string
}

export interface PlayerCertSubmitPayload {
  applicationId: string
}
