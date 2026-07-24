import request from '@/utils/request'
import type {
  AjaxResult,
  PartnerAddReq,
  PartnerEditReq,
  PartnerListItem,
  PartnerQuery,
  TableDataInfo
} from '@/types'

export function listPartner(query: PartnerQuery): Promise<TableDataInfo<PartnerListItem[]>> {
  return request({
    url: '/basket/partner/list',
    method: 'get',
    params: query
  })
}

export function getPartner(id: number): Promise<AjaxResult<PartnerListItem>> {
  return request({
    url: `/basket/partner/${id}`,
    method: 'get'
  })
}

export function addPartner(data: PartnerAddReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/partner',
    method: 'post',
    data
  })
}

export function updatePartner(id: number, data: PartnerEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/partner/${id}`,
    method: 'put',
    data
  })
}

export function publishPartner(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/partner/${id}/publish`,
    method: 'post'
  })
}

export function offlinePartner(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/partner/${id}/offline`,
    method: 'post'
  })
}

export function delPartner(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/partner/${id}`,
    method: 'delete'
  })
}
