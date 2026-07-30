export function maskEmail(email: string): string {
  const [local, domain] = email.split('@')
  if (!local || !domain) return '****@example.com'
  const visible = local.slice(0, Math.min(2, local.length))
  return `${visible}***@${domain}`
}
