const REGISTER_DRAFT_KEY = 'cnpa_register_draft'

export interface RegisterDraft {
  username: string
  password: string
  email?: string
  verifyBizNo?: string
  agreeUserAgreement: boolean
  agreePrivacyPolicy: boolean
}

export function maskEmail(email: string): string {
  const [local, domain] = email.split('@')
  if (!local || !domain) return '****@example.com'
  const visible = local.slice(0, Math.min(2, local.length))
  return `${visible}***@${domain}`
}

export function setRegisterDraft(draft: RegisterDraft) {
  sessionStorage.setItem(REGISTER_DRAFT_KEY, JSON.stringify(draft))
}

export function getRegisterDraft(): RegisterDraft | null {
  const raw = sessionStorage.getItem(REGISTER_DRAFT_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as RegisterDraft
  } catch {
    return null
  }
}

export function updateRegisterEmail(email: string, bizNo: string) {
  const draft = getRegisterDraft()
  if (!draft) return
  draft.email = email
  draft.verifyBizNo = bizNo
  setRegisterDraft(draft)
}

export function updateRegisterBizNo(bizNo: string) {
  const draft = getRegisterDraft()
  if (!draft) return
  draft.verifyBizNo = bizNo
  setRegisterDraft(draft)
}

export function getRegisterUsername(): string {
  return getRegisterDraft()?.username || ''
}

export function getRegisterEmail(): string {
  return getRegisterDraft()?.email || ''
}

export function getMaskedRegisterEmail(): string {
  const email = getRegisterEmail()
  return email ? maskEmail(email) : '****@example.com'
}

export function clearRegisterDraft() {
  sessionStorage.removeItem(REGISTER_DRAFT_KEY)
}
