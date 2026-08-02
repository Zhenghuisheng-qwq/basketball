/** 官网公开接口 ID 统一转字符串，避免 number/string 严格比较失败 */
export function normalizePublicId(value: unknown): string {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

export function isSamePublicId(a: unknown, b: unknown): boolean {
  return normalizePublicId(a) === normalizePublicId(b)
}
