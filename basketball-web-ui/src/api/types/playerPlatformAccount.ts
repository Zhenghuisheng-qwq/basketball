export interface PlatformAccountItemDto {
  platformAccountId?: string
  platform: string
  displayGameId?: string
  accountStatus?: number | null
  accountStatusText?: string
  primary?: boolean
  pendingChange?: boolean
}

export interface PlatformChangeHistoryItemDto {
  requestNo?: string
  changeType?: string
  platform?: string
  oldDisplayGameId?: string
  newDisplayGameId?: string
  requestStatus?: number
  requestStatusText?: string
  submitTime?: string
  reviewedTime?: string
}

export interface PlatformChangeSubmitDto {
  requestNo?: string
  requestId?: string
}

export interface PlayerEligibilityStatusDto {
  playerNo?: string
  playerStatus?: number
  playerStatusText?: string
  eligible?: boolean
  notice?: string | null
}

export interface PlatformAccountAddPayload {
  platform: string
  gameId: string
  proofFileId: string
}

export interface PlatformAccountChangeGameIdPayload {
  newGameId: string
  proofFileId: string
}
