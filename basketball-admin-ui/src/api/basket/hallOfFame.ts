import request from '@/utils/request'
import type {
  AjaxResult,
  HonorRecordAddReq,
  HonorRecordDetail,
  HonorRecordEditReq,
  HonorRecordListItem,
  HonorRecordQuery,
  TableDataInfo
} from '@/types'

export function listHallOfFame(query: HonorRecordQuery): Promise<TableDataInfo<HonorRecordListItem[]>> {
  return request({
    url: '/basket/hall-of-fame/list',
    method: 'get',
    params: query
  })
}

export function getHallOfFame(id: number): Promise<AjaxResult<HonorRecordDetail>> {
  return request({
    url: `/basket/hall-of-fame/${id}`,
    method: 'get'
  })
}

export function addHallOfFame(data: HonorRecordAddReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/hall-of-fame',
    method: 'post',
    data
  })
}

export function updateHallOfFame(id: number, data: HonorRecordEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/hall-of-fame/${id}`,
    method: 'put',
    data
  })
}

export function publishHallOfFame(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/hall-of-fame/${id}/publish`,
    method: 'post'
  })
}

export function offlineHallOfFame(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/hall-of-fame/${id}/offline`,
    method: 'post'
  })
}

export function delHallOfFame(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/hall-of-fame/${id}`,
    method: 'delete'
  })
}
