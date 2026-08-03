import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  V4AvailableDivision,
  V4MyEvent,
  V4MyRegistration,
  V4MyScheduleItem,
  V4RegistrationIdReq,
  V4RegistrationSaveReq,
} from '@/api/types/v4PlayerEvents'

export function getV4AvailableDivisions() {
  return request.get<any, ApiResult<V4AvailableDivision[]>>('/basket/v4/player/registration/available-divisions')
}

export function getV4MyRegistrations() {
  return request.get<any, ApiResult<V4MyRegistration[]>>('/basket/v4/player/registration/my-list')
}

export function saveV4Registration(data: V4RegistrationSaveReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/registration/save', data)
}

export function submitV4Registration(data: V4RegistrationIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/registration/submit', data)
}

export function withdrawV4Registration(data: V4RegistrationIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/registration/withdraw', data)
}

export function getV4MyEvents() {
  return request.get<any, ApiResult<V4MyEvent[]>>('/basket/v4/player/my-events')
}

export function getV4MySchedule(params?: {
  seasonId?: string
  divisionId?: string
  status?: string
  includeFinished?: boolean
  onlyPendingSubmit?: boolean
}) {
  return request.get<any, ApiResult<V4MyScheduleItem[]>>('/basket/v4/player/my-schedule', { params })
}
