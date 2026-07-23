export function getAvatarInitials(nickname?: string | null): string | null {
  const name = (nickname || '').trim()
  if (!name) return null
  if (/^\d+$/.test(name)) return null
  return name.slice(0, 2).toUpperCase()
}

export function shouldUseAvatarInitials(nickname?: string | null): boolean {
  return getAvatarInitials(nickname) !== null
}
