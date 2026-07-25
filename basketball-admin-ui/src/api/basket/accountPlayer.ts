import request from '@/utils/request'
import type {
  AccountPlayerFullDetail,
  AccountPlayerListItem,
  AccountPlayerQuery,
  AjaxResult,
  TableDataInfo
} from '@/types'

export function listAccountPlayer(query: AccountPlayerQuery): Promise<TableDataInfo<AccountPlayerListItem[]>> {
  return request({
    url: '/basket/accountPlayer/list',
    method: 'get',
    params: query
  })
}

export function getAccountPlayer(memberId: number): Promise<AjaxResult<AccountPlayerFullDetail>> {
  return request({
    url: `/basket/accountPlayer/${memberId}`,
    method: 'get'
  })
}
