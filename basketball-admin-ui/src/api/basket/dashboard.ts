import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export function getDashboardSummary(): Promise<AjaxResult<any>> {
  return request({ url: '/basket/dashboard/summary', method: 'get' })
}
