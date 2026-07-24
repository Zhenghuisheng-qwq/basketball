import request from '@/utils/request'
import type {
  AjaxResult,
  AnnouncementAddReq,
  AnnouncementDetail,
  AnnouncementEditReq,
  AnnouncementListItem,
  AnnouncementQuery,
  TableDataInfo
} from '@/types'

export function listAnnouncement(query: AnnouncementQuery): Promise<TableDataInfo<AnnouncementListItem[]>> {
  return request({
    url: '/basket/announcement/list',
    method: 'get',
    params: query
  })
}

export function getAnnouncement(id: number): Promise<AjaxResult<AnnouncementDetail>> {
  return request({
    url: `/basket/announcement/${id}`,
    method: 'get'
  })
}

export function addAnnouncement(data: AnnouncementAddReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/announcement',
    method: 'post',
    data
  })
}

export function updateAnnouncement(id: number, data: AnnouncementEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/announcement/${id}`,
    method: 'put',
    data
  })
}

export function publishAnnouncement(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/announcement/${id}/publish`,
    method: 'post'
  })
}

export function offlineAnnouncement(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/announcement/${id}/offline`,
    method: 'post'
  })
}

export function topAnnouncement(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/announcement/${id}/top`,
    method: 'post'
  })
}

export function untopAnnouncement(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/announcement/${id}/untop`,
    method: 'post'
  })
}

export function delAnnouncement(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/announcement/${id}`,
    method: 'delete'
  })
}
