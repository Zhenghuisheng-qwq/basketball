import request from '@/api/request'

export type EmailCodePurpose = 'REGISTER' | 'LOGIN'

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export interface EmailCodeSendResult {
  bizNo: string
  expireSeconds: number
  resendSeconds: number
}

export interface MemberSummary {
  memberId: string
  accountNo: string
  nickname: string
  avatarUrl?: string | null
  emailMasked: string
  mobileMasked: string
  emailVerified: boolean
  mobileVerified: boolean
  accountStatus: number
}

export interface MemberLoginResult {
  token: string
  tokenType: string
  expireSeconds: number
  member: MemberSummary
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

export function registerMember(payload: {
  email: string
  password: string
  confirmPassword: string
  nickname: string
  mobile: string
  mobileCountryCode?: string
  terminalType?: string
  verifyBizNo: string
  verifyCode: string
  agreeUserAgreement: boolean
  agreePrivacyPolicy: boolean
}) {
  return request.post<any, ApiResult<MemberLoginResult>>('/basket/member/auth/register', payload)
}

export function loginByPassword(payload: { account: string; password: string; terminalType?: string }) {
  return request.post<any, ApiResult<MemberLoginResult>>('/basket/member/auth/login/password', payload)
}

export function loginByEmailCode(payload: {
  email: string
  verifyBizNo: string
  verifyCode: string
  terminalType?: string
}) {
  return request.post<any, ApiResult<MemberLoginResult>>('/basket/member/auth/login/code', payload)
}

export function getMemberSession() {
  return request.get<any, ApiResult<MemberSessionResult>>('/basket/member/auth/session')
}

export function logoutMember() {
  return request.post<any, ApiResult<null>>('/basket/member/auth/logout')
}
