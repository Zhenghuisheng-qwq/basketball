/** 球队 Logo 占位文案：简称 > 名称首字母 > CNPA */
export function resolveTeamLogoFallback(shortName?: string | null, teamName?: string | null): string {
  const short = shortName?.trim()
  if (short) return short.slice(0, 4).toUpperCase()
  const name = teamName?.trim()
  if (name) return name.slice(0, 2).toUpperCase()
  return 'CNPA'
}

/** 解析可用于 img src 的 Logo URL */
export function resolveTeamLogoUrl(logoUrl?: string | null): string {
  return logoUrl?.trim() || ''
}
