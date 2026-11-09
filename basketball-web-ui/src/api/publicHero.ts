import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PublicPageHeroResp } from '@/api/types/public'

export function getPublicPageHero(pageKey: string, seasonId?: string) {
  return request.get<any, ApiResult<PublicPageHeroResp>>('/basket/public/hero', {
    params: {
      pageKey,
      ...(seasonId ? { seasonId } : {}),
    },
  })
}
