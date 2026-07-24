export interface LoginLogItemDto {
  logId: string
  loginType?: string
  loginResult?: number
  failureReason?: string
  clientIpMasked?: string
  clientRegion?: string
  terminalType?: string
  userAgentMasked?: string
  loginTime?: string
}

export interface SecurityLogItemDto {
  logId: string
  eventType?: string
  eventTitle?: string
  oldValueMasked?: string
  newValueMasked?: string
  operatorType?: string
  clientIpMasked?: string
  terminalType?: string
  eventTime?: string
  remark?: string
}
