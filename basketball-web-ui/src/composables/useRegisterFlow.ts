const REGISTER_EMAIL_KEY = 'cnpa_register_email'

export function maskEmail(email: string): string {
  const [local, domain] = email.split('@')
  if (!local || !domain) return 'zlz****@example.com'
  const visible = local.slice(0, Math.min(3, local.length))
  return `${visible}****@${domain}`
}

export function setRegisterEmail(email: string) {
  sessionStorage.setItem(REGISTER_EMAIL_KEY, email)
}

export function getRegisterEmail(): string {
  return sessionStorage.getItem(REGISTER_EMAIL_KEY) || 'zlz@example.com'
}

export function getMaskedRegisterEmail(): string {
  return maskEmail(getRegisterEmail())
}
