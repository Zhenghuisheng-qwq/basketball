import request from '@/utils/request'
import type {
  AjaxResult,
  HomeBannerAddReq,
  HomeBannerEditReq,
  HomeBannerListItem,
  HomeBannerQuery,
  TableDataInfo
} from '@/types'

export function listHomeBanner(query: HomeBannerQuery): Promise<TableDataInfo<HomeBannerListItem[]>> {
  return request({
    url: '/basket/home-content/list',
    method: 'get',
    params: query
  })
}

export function getHomeBanner(id: number): Promise<AjaxResult<HomeBannerListItem>> {
  return request({
    url: `/basket/home-content/${id}`,
    method: 'get'
  })
}

export function addHomeBanner(data: HomeBannerAddReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/home-content',
    method: 'post',
    data
  })
}

export function updateHomeBanner(id: number, data: HomeBannerEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/home-content/${id}`,
    method: 'put',
    data
  })
}

export function publishHomeBannerGroup(): Promise<AjaxResult> {
  return request({
    url: '/basket/home-content/publish-group',
    method: 'post'
  })
}

export function publishHomeBanner(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/home-content/${id}/publish`,
    method: 'post'
  })
}

export function offlineHomeBanner(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/home-content/${id}/offline`,
    method: 'post'
  })
}

export function delHomeBanner(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/home-content/${id}`,
    method: 'delete'
  })
}
