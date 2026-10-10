import { translate } from '@/i18n'

/** Map a stable code to a message. Unknown codes stay as codes so a missing key is visible. */
export function codeLabel(namespace: string, code?: string | null, fallback = '--'): string {
  const normalized = String(code || '').trim()
  if (!normalized) return fallback
  const key = `${namespace}.${normalized.toUpperCase()}`
  const text = translate(key)
  if (!text || text === key) return normalized
  return text
}
