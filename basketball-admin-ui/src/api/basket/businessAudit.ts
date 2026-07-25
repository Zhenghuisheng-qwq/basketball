import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface BusinessAuditQuery {
  businessModule?: string
  operationType?: string
  tournamentId?: number
  targetType?: string
  targetId?: number
  keyword?: string
}

export function listBusinessAudit(query: BusinessAuditQuery): Promise<AjaxResult<any[]>> {
  return request({ url: '/basket/businessAudit/list', method: 'get', params: query })
}

export function getBusinessAudit(id: number): Promise<AjaxResult<any>> {
  return request({ url: `/basket/businessAudit/${id}`, method: 'get' })
}
