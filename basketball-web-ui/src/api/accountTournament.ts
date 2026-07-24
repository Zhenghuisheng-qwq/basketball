import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'

export function getAccountTournamentPage(params: {
  currentPage?: number
  pageSize?: number
  status?: number
  keyword?: string
}) {
  return request.get<any, ApiResult<PageResult<AccountTournamentItemDto>>>('/basket/account/tournament/page', {
    params,
  })
}

export function getAccountTournamentDetail(registrationId: string | number) {
  return request.get<any, ApiResult<AccountTournamentItemDto>>(`/basket/account/tournament/${registrationId}`)
}
