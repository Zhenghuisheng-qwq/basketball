import request from '@/utils/request'
import type {
  AjaxResult,
  PartnerSupportAddReq,
  PartnerSupportEditReq,
  PartnerSupportListItem,
  PartnerSupportQuery,
  TableDataInfo
} from '@/types'

export function listPartnerSupport(query: PartnerSupportQuery): Promise<TableDataInfo<PartnerSupportListItem[]>> {
  return request({
    url: '/basket/partner-support/list',
    method: 'get',
    params: query
  })
}

export function getPartnerSupport(id: number): Promise<AjaxResult<PartnerSupportListItem>> {
  return request({
    url: `/basket/partner-support/${id}`,
    method: 'get'
  })
}

export function addPartnerSupport(data: PartnerSupportAddReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/partner-support',
    method: 'post',
    data
  })
}

export function updatePartnerSupport(id: number, data: PartnerSupportEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/partner-support/${id}`,
    method: 'put',
    data
  })
}

export function updatePartnerSupportStatus(id: number, publishStatus: number): Promise<AjaxResult> {
  return request({
    url: `/basket/partner-support/${id}/status`,
    method: 'put',
    params: { publishStatus }
  })
}

export function delPartnerSupport(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/partner-support/${id}`,
    method: 'delete'
  })
}
