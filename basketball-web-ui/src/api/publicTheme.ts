import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PublicSiteThemeResp } from '@/api/types/public'

export function getPublicSiteTheme(scope: string, seasonId?: string) {
  return request.get<any, ApiResult<PublicSiteThemeResp>>('/basket/public/theme', {
    params: {
      scope,
      ...(seasonId ? { seasonId } : {}),
    },
  })
}
