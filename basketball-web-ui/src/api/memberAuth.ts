import request from '@/api/request'

export type EmailCodePurpose = 'REGISTER' | 'RESET_PASSWORD' | 'CHANGE_EMAIL'

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export interface EmailCodeSendResult {
  bizNo: string
  expireSeconds: number
  resendSeconds: number
  /** 开发模式：未发真实邮件，任意 6 位数字可通过 */
  devBypass?: boolean
}

export interface MemberSummary {
  memberId: string
  accountNo: string
  nickname: string
  avatarUrl?: string | null
  emailMasked: string
  emailVerified: boolean
  accountStatus: number
}

export interface MemberLoginResult {
  token: string
  tokenType: string
  expireSeconds: number
  member: MemberSummary
}

export interface MemberRegisterResult {
  email: string
}

export interface MemberSessionResult extends MemberSummary {
  registerSource?: string
  registerTerminal?: string
}

export function sendEmailCode(email: string, purpose: EmailCodePurpose) {
  return request.post<any, ApiResult<EmailCodeSendResult>>('/basket/member/auth/email-code/send', {
    email,
    purpose,
  })
}

export function sendChangeEmailCode(email: string) {
  return request.post<any, ApiResult<EmailCodeSendResult>>('/basket/account/security/change-email/send-code', {
    email,
    purpose: 'CHANGE_EMAIL',
  })
}

export function registerMember(payload: {
  email: string
  password: string
  confirmPassword: string
  terminalType?: string
  verifyBizNo: string
  verifyCode: string
  agreeUserAgreement: boolean
  agreePrivacyPolicy: boolean
}) {
  return request.post<any, ApiResult<MemberRegisterResult>>('/basket/member/auth/register', payload)
}

export function loginByPassword(payload: { email: string; password: string; terminalType?: string }) {
  return request.post<any, ApiResult<MemberLoginResult>>('/basket/member/auth/login/password', payload)
}

export function getMemberSession() {
  return request.get<any, ApiResult<MemberSessionResult>>('/basket/member/auth/session')
}

export function logoutMember() {
  return request.post<any, ApiResult<null>>('/basket/member/auth/logout')
}

export function resetMemberPassword(payload: {
  email: string
  verifyBizNo: string
  verifyCode: string
  password: string
  confirmPassword: string
}) {
  return request.post<any, ApiResult<null>>('/basket/member/auth/password/reset', payload)
}

export function changeMemberEmail(payload: {
  email: string
  verifyBizNo: string
  verifyCode: string
}) {
  return request.post<any, ApiResult<{ needRelogin: boolean }>>('/basket/account/security/change-email', payload)
}
