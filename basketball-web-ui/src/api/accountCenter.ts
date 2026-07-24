import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { AccountCenterOverview } from '@/api/types/accountCenter'

export function getAccountCenterOverview() {
  return request.get<any, ApiResult<AccountCenterOverview>>('/basket/account/center/overview')
}
