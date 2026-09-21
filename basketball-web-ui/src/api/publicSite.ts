import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PublicSocialLinkResp } from '@/api/types/public'

export function getPublicSocialLinks() {
  return request.get<any, ApiResult<PublicSocialLinkResp[]>>('/basket/public/site/social-links')
}
