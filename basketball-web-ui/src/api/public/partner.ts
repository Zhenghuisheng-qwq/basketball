import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PublicPartnersResp } from '@/api/types/public'

export function getPublicPartners(params?: { currentPage?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PublicPartnersResp>>('/basket/public/partner/partners', {
    params,
  })
}
