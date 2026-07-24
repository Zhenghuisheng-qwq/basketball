/**
 * 根据球员编号或 ID 稳定映射 0-9 默认头像主题。
 * 后续替换为 src/assets/default-avatar/avatar-01.webp ... avatar-10.webp 时无需改数据库。
 */
export function hashStable(input: string): number {
  const text = (input || '').trim()
  if (!text) return 0
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    hash = (hash * 31 + text.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

export function resolveDefaultAvatarTheme(playerNo?: string | null, playerId?: string | number | null): number {
  const key = (playerNo && playerNo.trim()) || (playerId != null ? String(playerId) : '')
  return hashStable(key) % 10
}

export const DEFAULT_AVATAR_THEMES = [
  { bg: '#7f1d1d', fg: '#fecaca', border: '#991b1b' },
  { bg: '#1e3a8a', fg: '#bfdbfe', border: '#1d4ed8' },
  { bg: '#14532d', fg: '#bbf7d0', border: '#15803d' },
  { bg: '#581c87', fg: '#e9d5ff', border: '#7e22ce' },
  { bg: '#78350f', fg: '#fed7aa', border: '#9a3412' },
  { bg: '#134e4a', fg: '#99f6e4', border: '#0f766e' },
  { bg: '#312e81', fg: '#c7d2fe', border: '#4338ca' },
  { bg: '#701a75', fg: '#f5d0fe', border: '#86198f' },
  { bg: '#374151', fg: '#e5e7eb', border: '#4b5563' },
  { bg: '#92400e', fg: '#fde68a', border: '#b45309' },
] as const

/** 预留真实默认头像路径，当前组件仍使用 CSS 主题。 */
export function defaultAvatarAssetPath(themeIndex: number): string {
  const no = String((themeIndex % 10) + 1).padStart(2, '0')
  return `/src/assets/default-avatar/avatar-${no}.webp`
}

export function avatarInitial(name?: string | null, fallback = '球'): string {
  const text = (name || '').trim()
  if (!text) return fallback
  return text.charAt(0)
}
