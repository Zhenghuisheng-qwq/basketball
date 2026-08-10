import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { CaptainQualificationDto } from '@/api/types/teamCaptain'

export function getCaptainQualificationCurrent(params?: {
  divisionId?: string
  seasonId?: string
  teamId?: string
}) {
  return request.get<any, ApiResult<CaptainQualificationDto>>(
    '/basket/team/captain/qualification/current',
    { params },
  )
}

export function getCaptainQualificationList() {
  return request.get<any, ApiResult<CaptainQualificationDto[]>>(
    '/basket/team/captain/qualification/list',
  )
}
