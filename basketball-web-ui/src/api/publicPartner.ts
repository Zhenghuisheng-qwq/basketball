import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PublicPartnerResp } from '@/api/types/public'

export function getPublicPartnerList() {
  return request.get<any, ApiResult<PublicPartnerResp[]>>('/basket/public/partner/list')
}
