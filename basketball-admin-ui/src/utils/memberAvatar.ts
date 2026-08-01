/** 会员默认头像（COS 公共图） */
export const DEFAULT_MEMBER_AVATAR_URL =
  'https://basketball-1453992653.cos.ap-shanghai.myqcloud.com/basketball/public/2026/08/01/b1f4d9a28beb48beb90fed28439e8e4a.png'

export function resolveMemberAvatarUrl(avatarUrl?: string | null): string {
  const url = avatarUrl?.trim()
  return url || DEFAULT_MEMBER_AVATAR_URL
}
