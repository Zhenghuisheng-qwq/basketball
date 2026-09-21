import type { CommunityLink } from '@/views/events/types/events'
import iconKook from '@/assets/icon/home/social_kook.png'
import iconWx from '@/assets/icon/home/social_wx.png'
import iconBili from '@/assets/icon/home/social_bili.png'
import iconDouyin from '@/assets/icon/home/social_douyin.png'

export const SOCIAL_PLATFORM_ICONS: Record<string, string> = {
  KOOK: iconKook,
  WECHAT: iconWx,
  BILIBILI: iconBili,
  DOUYIN: iconDouyin,
}

/** @deprecated 业务文案与 URL 已改为后台配置，仅保留类型兼容 */
export const communityLinks: CommunityLink[] = []

export function isSafeHttpUrl(url?: string | null): boolean {
  if (!url || !url.trim()) {
    return false
  }
  const trimmed = url.trim()
  if (trimmed.length > 512) {
    return false
  }
  const lower = trimmed.toLowerCase()
  if (
    lower.startsWith('javascript:') ||
    lower.startsWith('data:') ||
    lower.startsWith('file:') ||
    lower.startsWith('vbscript:')
  ) {
    return false
  }
  try {
    const parsed = new URL(trimmed)
    return parsed.protocol === 'https:' || parsed.protocol === 'http:'
  } catch {
    return false
  }
}
