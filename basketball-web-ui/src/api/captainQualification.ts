import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { CaptainQualificationDto } from '@/api/types/teamCaptain'

export function getCaptainQualificationCurrent() {
  return request.get<any, ApiResult<CaptainQualificationDto>>('/basket/team/captain/qualification/current')
}
