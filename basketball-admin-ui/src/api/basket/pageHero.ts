import request from '@/utils/request'
import type { AjaxResult, PageHeroResp, PageHeroSaveReq } from '@/types'

export function getPageHero(pageKey: string, seasonId?: number): Promise<AjaxResult<PageHeroResp>> {
  return request({
    url: '/basket/page-hero',
    method: 'get',
    params: { pageKey, seasonId: seasonId || undefined }
  })
}

export function savePageHero(data: PageHeroSaveReq): Promise<AjaxResult<PageHeroResp>> {
  return request({
    url: '/basket/page-hero/save',
    method: 'post',
    data
  })
}

export function publishPageHero(data: PageHeroSaveReq): Promise<AjaxResult> {
  return request({
    url: '/basket/page-hero/publish',
    method: 'post',
    data
  })
}
