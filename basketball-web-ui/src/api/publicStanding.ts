import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PublicStandingItemVo } from '@/api/types/public'

export function getPublicSeasonStanding(
  seasonId: string | number,
  params?: { stageId?: string | number },
) {
  return request.get<any, ApiResult<PublicStandingItemVo[]>>(
    `/basket/public/season/${seasonId}/standing`,
    { params },
  )
}

export function getPublicStageStanding(stageId: string | number) {
  return request.get<any, ApiResult<PublicStandingItemVo[]>>(
    `/basket/public/stage/${stageId}/standing`,
  )
}

export function getPublicGroupStanding(groupId: string | number) {
  return request.get<any, ApiResult<PublicStandingItemVo[]>>(
    `/basket/public/group/${groupId}/standing`,
  )
}
