import request from '@/utils/request'
import type { AjaxResult, SiteSocialLinkEditReq, SiteSocialLinkItem } from '@/types'

export function listSocialLinks(): Promise<AjaxResult<SiteSocialLinkItem[]>> {
  return request({
    url: '/basket/site/social-links',
    method: 'get'
  })
}

export function updateSocialLink(id: number, data: SiteSocialLinkEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/site/social-links/${id}`,
    method: 'put',
    data
  })
}
