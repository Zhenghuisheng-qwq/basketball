import request from '@/utils/request'
import type { AjaxResult, SiteThemeImpact, SiteThemeResp, SiteThemeSaveReq } from '@/types'

export function listSiteTheme(): Promise<AjaxResult<SiteThemeResp[]>> {
  return request({ url: '/basket/site-theme/list', method: 'get' })
}

export function getSiteTheme(id: number): Promise<AjaxResult<SiteThemeResp>> {
  return request({ url: `/basket/site-theme/${id}`, method: 'get' })
}

export function saveSiteTheme(data: SiteThemeSaveReq): Promise<AjaxResult<SiteThemeResp>> {
  return request({ url: '/basket/site-theme/save', method: 'post', data })
}

export function publishSiteTheme(data: SiteThemeSaveReq): Promise<AjaxResult> {
  return request({ url: '/basket/site-theme/publish', method: 'post', data })
}

export function copySiteTheme(id: number): Promise<AjaxResult<number>> {
  return request({ url: `/basket/site-theme/${id}/copy`, method: 'post' })
}

export function offlineSiteTheme(id: number, versionNo: number): Promise<AjaxResult> {
  return request({ url: `/basket/site-theme/${id}/offline`, method: 'post', params: { versionNo } })
}

export function unbindSiteTheme(scopeType: string, seasonId?: number): Promise<AjaxResult> {
  return request({ url: '/basket/site-theme/unbind', method: 'post', params: { scopeType, seasonId } })
}

export function deleteSiteTheme(id: number): Promise<AjaxResult> {
  return request({ url: `/basket/site-theme/${id}`, method: 'delete' })
}

export function impactSiteTheme(themeId: number, scopeType: string, seasonId?: number): Promise<AjaxResult<SiteThemeImpact>> {
  return request({ url: '/basket/site-theme/impact', method: 'get', params: { themeId, scopeType, seasonId } })
}
